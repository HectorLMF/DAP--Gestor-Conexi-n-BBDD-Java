package org.example.db.postgres;

import org.example.db.DBConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Conexión a Postgres implementada mediante sockets (cliente mínimo del protocolo)
 * Esta clase implementa una versión reducida del protocolo de Postgres para
 * arrancar sesión y ejecutar consultas en modo texto. Está pensada para
 * demostración/desarrollo y no reemplaza al driver JDBC en producción.
 */
public class PostgressConnection implements DBConnection {
    private final String name;

    // Parámetros de conexión (tomados de variables de entorno / properties o valores por defecto)
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    // Socket/native connection
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    // Estado
    private boolean connected = false;
    private boolean nativeConnected = false; // true si la comunicación nativa con el servidor real

    // Config cargada desde resources/db.properties si existe
    private static final Properties FILE_CONFIG = loadConfigFile();

    public PostgressConnection(String name) {
        this(
                name,
                // Buscar en orden: fichero de config (db.properties), variables de entorno, properties del sistema, valor por defecto
                getConfigValue("PGHOST", "POSTGRES_HOST", "localhost"),
                Integer.parseInt(getConfigValue("PGPORT", "POSTGRES_PORT", "5432")),
                getConfigValue("PGDATABASE", "POSTGRES_DB", "postgres"),
                getConfigValue("PGUSER", "POSTGRES_USER", "postgres"),
                // IMPORTANT: algunos setups (docker official image) usan POSTGRES_PASSWORD; también permitimos fallback a "postgres" para entornos de desarrollo
                getConfigValue("PGPASSWORD", "POSTGRES_PASSWORD", "postgres")
        );
    }

    public PostgressConnection(String name, String host, int port, String database, String user, String password) {
        this.name = "postgres:" + name;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Intenta abrir una conexión nativa a Postgres mediante JDBC; si falla, queda en modo simulación local.
     */
    @Override
    public void connect() {
        System.out.println("[postgres-socket] Intentando conectar via sockets a " + host + ":" + port + " (db=" + database + ") as user=" + user);
        try {
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            sendStartup();

            // handshake loop
            boolean authOk = false;
            while (true) {
                int t = in.read();
                if (t == -1) throw new RuntimeException("Server closed connection during startup");
                char type = (char) t;
                int len = readInt(in);
                int payloadLen = len - 4;
                byte[] payload = readBytes(in, payloadLen);
                switch (type) {
                    case 'R': { // Authentication
                        int authType = ByteBuffer.wrap(payload).getInt();
                        if (authType == 0) {
                            authOk = true;
                        } else if (authType == 3) { // cleartext
                            sendPasswordCleartext();
                        } else if (authType == 5) { // md5
                            // payload contains 4 byte salt
                            byte[] salt = Arrays.copyOfRange(payload, 4, 8);
                            sendPasswordMD5(salt);
                        } else {
                            throw new RuntimeException("Unsupported auth type: " + authType);
                        }
                        break;
                    }
                    case 'E': { // ErrorResponse
                        String err = parseError(payload);
                        throw new RuntimeException("Authentication/Startup error: " + err);
                    }
                    case 'S': // ParameterStatus - ignore
                    case 'K': // BackendKeyData - ignore
                        break;
                    case 'Z': // ReadyForQuery
                        if (authOk) {
                            nativeConnected = true;
                            connected = true;
                            System.out.println("[postgres-socket] ✓ Connected to " + host + ":" + port + " as user '" + user + "' database '" + database + "'.");
                            return;
                        }
                        break;
                    default:
                        // ignore other messages during startup
                        break;
                }
            }

        } catch (Exception e) {
            nativeConnected = false;
            connected = false;
            closeSocket();
            System.err.println("[postgres-socket] ✗ FAILED to connect via sockets to " + host + ":" + port);
            System.err.println("[postgres-socket]   Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot establish socket connection to PostgreSQL. Check Docker and credentials.", e);
        }
    }

    @Override
    public void disconnect() {
        this.connected = false;
        nativeConnected = false;
        closeSocket();
        System.out.println("[postgres-socket] Disconnected");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Indica si la conexión nativa por sockets fue establecida con éxito.
     * Útil para distinguir entre modo nativo y fallback simulado.
     *
     * @return true si la comunicación nativa con el servidor Postgres está activa
     */
    public boolean isNativeConnected() {
        return nativeConnected;
    }

    @Override
    public List<Map<String, Object>> execute(String sql) {
        if (!isConnected()) {
            throw new IllegalStateException("Connection is not open: " + getName());
        }
        if (!nativeConnected || socket == null || out == null || in == null) {
            throw new IllegalStateException("Socket connection is not established. Cannot execute query.");
        }
        try {
            System.out.println("[postgres-socket] Executing native SQL: " + sql);
            List<Map<String, Object>> res = executeNativeQuerySocket(sql);
            System.out.println("[postgres-socket] Native query returned rows: " + (res == null ? 0 : res.size()));
            return res;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query: " + sql + ". Error: " + e.getMessage(), e);
        }
    }

    // ----------------- Implementación JDBC para queries -----------------
    // Implementación mínima del protocolo PostgreSQL para ejecutar queries simples (texto)
    private List<Map<String, Object>> executeNativeQuerySocket(String sql) throws Exception {
        // enviar Query message
        sendQuery(sql);

        List<String> columnNames = null;
        List<Map<String, Object>> rows = new ArrayList<>();

        boolean done = false;
        while (!done) {
            int t = in.read();
            if (t == -1) throw new RuntimeException("Server closed connection during query");
            char type = (char) t;
            int len = readInt(in);
            int payloadLen = len - 4;
            byte[] payload = readBytes(in, payloadLen);
            switch (type) {
                case 'T': { // RowDescription
                    ByteBuffer bb = ByteBuffer.wrap(payload);
                    int fieldCount = bb.getShort() & 0xffff;
                    columnNames = new ArrayList<>();
                    for (int i = 0; i < fieldCount; i++) {
                        String name = readNullTerminatedString(bb);
                        // skip table oid (4), col attr (2), dataType (4), size(2), typeMod(4), format(2)
                        int skip = 4 + 2 + 4 + 2 + 4 + 2;
                        bb.position(bb.position() + skip);
                        columnNames.add(name);
                    }
                    break;
                }
                case 'D': { // DataRow
                    ByteBuffer bb = ByteBuffer.wrap(payload);
                    int colCount = bb.getShort() & 0xffff;
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < colCount; i++) {
                        int colLen = bb.getInt();
                        if (colLen == -1) {
                            row.put(columnNames.get(i), null);
                        } else {
                            byte[] b = new byte[colLen];
                            bb.get(b);
                            row.put(columnNames.get(i), new String(b, StandardCharsets.UTF_8));
                        }
                    }
                    rows.add(row);
                    break;
                }
                case 'C': { // CommandComplete
                    // ignore
                    break;
                }
                case 'Z': // ReadyForQuery
                    done = true;
                    break;
                case 'E': {
                    String err = parseError(payload);
                    throw new RuntimeException("Query error: " + err);
                }
                default:
                    // ignore others
                    break;
            }
        }

        if (columnNames == null) {
            return Collections.emptyList();
        }
        return rows;
        }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null; in = null; out = null;
    }

    // ----------------- Protocol helpers -----------------
    private void sendStartup() throws Exception {
        // StartupMessage: int32 len, int32 protocol(196608), series of key\0value\0, terminating 0
        ByteArrayOutputStreamEx bout = new ByteArrayOutputStreamEx();
        bout.writeInt32(0); // placeholder
        bout.writeInt32(196608);
        bout.writeString("user");
        bout.writeString(user);
        bout.writeString("database");
        bout.writeString(database);
        bout.writeByte(0);
        byte[] body = bout.toByteArray();
        // fill length
        ByteBuffer.wrap(body).putInt(0, body.length);
        out.write(body);
        out.flush();
    }

    private void sendPasswordCleartext() throws Exception {
        byte[] pwd = (password + "\0").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStreamEx bout = new ByteArrayOutputStreamEx();
        bout.writeByte((byte) 'p');
        bout.writeInt32(pwd.length + 4);
        bout.writeBytes(pwd);
        out.write(bout.toByteArray());
        out.flush();
    }

    private void sendPasswordMD5(byte[] salt) throws Exception {
        // MD5: "md5" + hex(md5(md5(password + user) + salt))
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update((password + user).getBytes(StandardCharsets.UTF_8));
        byte[] inner = md.digest();
        // hex of inner
        String innerHex = toHex(inner);
        md.reset();
        md.update((innerHex + new String(salt, StandardCharsets.ISO_8859_1)).getBytes(StandardCharsets.ISO_8859_1));
        byte[] outer = md.digest();
        String outerHex = toHex(outer);
        String result = "md5" + outerHex + "\0";
        ByteArrayOutputStreamEx bout = new ByteArrayOutputStreamEx();
        bout.writeByte((byte) 'p');
        bout.writeInt32(result.getBytes(StandardCharsets.UTF_8).length + 4);
        bout.writeString(result);
        out.write(bout.toByteArray());
        out.flush();
    }

    private void sendQuery(String sql) throws Exception {
        byte[] q = (sql + "\0").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStreamEx bout = new ByteArrayOutputStreamEx();
        bout.writeByte((byte) 'Q');
        bout.writeInt32(q.length + 4);
        bout.writeBytes(q);
        out.write(bout.toByteArray());
        out.flush();
    }

    private static int readInt(InputStream in) throws Exception {
        byte[] b = readBytes(in, 4);
        return ByteBuffer.wrap(b).getInt();
    }

    private static byte[] readBytes(InputStream in, int n) throws Exception {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r == -1) throw new RuntimeException("Unexpected EOF");
            off += r;
        }
        return buf;
    }

    private static String parseError(byte[] payload) {
        // payload: series of fields type(byte) + string terminated by 0; end with 0
        int idx = 0;
        StringBuilder sb = new StringBuilder();
        while (idx < payload.length) {
            byte fieldType = payload[idx++];
            if (fieldType == 0) break;
            int start = idx;
            while (idx < payload.length && payload[idx] != 0) idx++;
            String val = new String(payload, start, idx - start, StandardCharsets.UTF_8);
            idx++; // skip 0
            sb.append((char) fieldType).append('=').append(val).append(';');
        }
        return sb.toString();
    }

    private static String readNullTerminatedString(ByteBuffer bb) {
        StringBuilder sb = new StringBuilder();
        while (bb.hasRemaining()) {
            byte b = bb.get();
            if (b == 0) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xff));
        return sb.toString();
    }

    // Small helper to build message bodies
    private static class ByteArrayOutputStreamEx extends java.io.ByteArrayOutputStream {
        void writeInt32(int v) {
            this.write((v >> 24) & 0xFF);
            this.write((v >> 16) & 0xFF);
            this.write((v >> 8) & 0xFF);
            this.write(v & 0xFF);
        }
        void writeString(String s) {
            try { this.write(s.getBytes(StandardCharsets.UTF_8)); } catch (Exception ignored) {}
            this.write(0);
        }
        public void writeBytes(byte[] b) { this.write(b, 0, b.length); }
        public void writeByte(int b) { this.write(b); }
    }

    

    // ----------------- Nuevo: lectura de fichero de configuración -----------------
    private static Properties loadConfigFile() {
        Properties p = new Properties();
        try (InputStream in = PostgressConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
                System.out.println("[postgres-socket] Loaded db.properties from classpath");
            }
        } catch (Exception ignored) {}
        return p;
    }

    private static String getConfigValue(String primaryEnv, String secondaryEnv, String defaultValue) {
        // 1) fichero
        String v = FILE_CONFIG.getProperty(primaryEnv);
        if (v != null && !v.isEmpty()) return v;
        v = FILE_CONFIG.getProperty(secondaryEnv);
        if (v != null && !v.isEmpty()) return v;
        // 2) env
        v = System.getenv(primaryEnv);
        if (v != null && !v.isEmpty()) return v;
        v = System.getenv(secondaryEnv);
        if (v != null && !v.isEmpty()) return v;
        // 3) system properties
        v = System.getProperty(primaryEnv);
        if (v != null && !v.isEmpty()) return v;
        v = System.getProperty(secondaryEnv);
        if (v != null && !v.isEmpty()) return v;
        // 4) default
        return defaultValue;
    }
}

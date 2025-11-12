package org.example.db.mysql;

import org.example.db.DBConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

// IMPORTANTE: Necesitarás una clase para el 'hasheo' de la contraseña de MySQL
// import java.security.MessageDigest;

/**
 * Conexión a MySQL implementada mediante sockets (cliente mínimo del protocolo)
 * * TAREA: Adaptar la lógica de PostgresConnection a esta clase,
 * pero siguiendo el protocolo de MySQL (HandshakeV10, COM_QUERY, etc.)
 */
public class MySQLConnection implements DBConnection {

    private final String name;

    // Parámetros de conexión (leídos de db.properties o entorno)
    private final String host;
    private final int port;
    private final String database; // MySQL lo necesita para el handshake
    private final String user;
    private final String password;

    // Socket/native connection
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    // Estado
    private boolean connected = false;

    // Config cargada desde resources/db.properties si existe
    private static final Properties FILE_CONFIG = loadConfigFile();

    /**
     * Constructor "inteligente" que se autoconfigura.
     * Lee de db.properties y variables de entorno.
     */
    public MySQLConnection(String name) {
        this.name = "mysql:" + name;
        this.host = getConfigValue("MYSQL_HOST", "mysql.host", "localhost");
        this.port = Integer.parseInt(getConfigValue("MYSQL_PORT", "mysql.port", "3306"));
        this.database = getConfigValue("MYSQL_DB", "mysql.dbname", "demo");
        this.user = getConfigValue("MYSQL_USER", "mysql.user", "root");
        this.password = getConfigValue("MYSQL_PASS", "mysql.password", "password"); // Cambia "password" por tu default
    }

    // Constructor de prueba (opcional)
    public MySQLConnection(String name, String host, int port, String database, String user, String password) {
        this.name = "mysql:" + name;
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
     * Intenta abrir una conexión nativa a MySQL mediante sockets.
     */
    @Override
    public void connect() {
        System.out.println("[mysql-socket] Intentando conectar via sockets a " + host + ":" + port + " (db=" + database + ") as user=" + user);

        try {
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            byte[] header = readBytes(in, 4); // 3 bytes len LE + 1 byte seq
            int serverPacketLen = (header[0] & 0xFF) | ((header[1] & 0xFF) << 8) | ((header[2] & 0xFF) << 16);
            int serverSeq = header[3] & 0xFF;
            byte[] payload = readBytes(in, serverPacketLen);

            // Validación mínima
            if (payload.length == 0 || payload[0] != 0x0A) {
                throw new RuntimeException("Unexpected handshake packet (not HandshakeV10)");
            }

            // Parse HandshakeV10
            int pos = 1; // after protocol byte
            // server version (NUL terminated)
            while (pos < payload.length && payload[pos] != 0) pos++;
            pos++; // skip NUL
            // connection id (4 bytes)
            pos += 4;
            // auth-plugin-data-part-1 (8 bytes)
            if (pos + 8 > payload.length) throw new RuntimeException("Handshake packet truncated (part1)");
            byte[] part1 = Arrays.copyOfRange(payload, pos, pos + 8);
            pos += 8;
            // filler
            pos += 1;
            if (pos + 2 > payload.length) throw new RuntimeException("Handshake packet truncated (cap lower)");
            int capabilityLower = (payload[pos] & 0xFF) | ((payload[pos + 1] & 0xFF) << 8);
            pos += 2;
            // charset (1) + status (2) + capability upper (2)
            int charset = (pos < payload.length) ? (payload[pos] & 0xFF) : 33;
            pos += 1;
            pos += 2;
            int capabilityUpper = 0;
            if (pos + 2 <= payload.length) {
                capabilityUpper = (payload[pos] & 0xFF) | ((payload[pos + 1] & 0xFF) << 8);
            }
            pos += 2;
            int serverCapabilities = capabilityLower | (capabilityUpper << 16);

            // auth-plugin-data length (if plugin auth present)
            int authPluginDataLen = 0;
            final int CLIENT_PLUGIN_AUTH = 0x00080000;
            if ((serverCapabilities & CLIENT_PLUGIN_AUTH) != 0 && pos < payload.length) {
                authPluginDataLen = payload[pos] & 0xFF;
                pos += 1;
            } else {
                pos += 1;
            }

            // reserved 10 bytes
            pos += 10;
            if (pos > payload.length) pos = payload.length;

            // part2: length = max(13, authPluginDataLen - 8)
            int part2Len = 0;
            if (authPluginDataLen > 8) part2Len = Math.max(13, authPluginDataLen - 8);
            else part2Len = Math.max(13, payload.length - pos); // best-effort fallback
            if (pos + part2Len > payload.length) part2Len = Math.max(0, payload.length - pos);
            byte[] part2 = (part2Len > 0) ? Arrays.copyOfRange(payload, pos, pos + part2Len) : new byte[0];

            // build scramble (concatenate part1 + part2)
            byte[] scramble;
            if (part2.length > 0) {
                int part2ActualLength = part2.length - 1;
                scramble = new byte[part1.length + part2ActualLength];
                System.arraycopy(part1, 0, scramble, 0, part1.length);
                System.arraycopy(part2, 0, scramble, part1.length, part2ActualLength); // <-- Usar part2ActualLength
            } else {
                scramble = part1;
            }

            // compute auth response (mysql_native_password)
            byte[] authResponse;
            if (password == null || password.isEmpty()) {
                authResponse = new byte[0];
            } else {
                try {
                    java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
                    byte[] hash1 = sha1.digest(password.getBytes(StandardCharsets.UTF_8)); // SHA1(password)
                    byte[] hash2 = sha1.digest(hash1); // SHA1(SHA1(password))
                    sha1.reset();
                    sha1.update(scramble);
                    sha1.update(hash2);
                    byte[] scrambleHash = sha1.digest(); // SHA1(scramble + hash2)
                    authResponse = new byte[hash1.length];
                    for (int i = 0; i < hash1.length; i++) {
                        authResponse[i] = (byte) (hash1[i] ^ scrambleHash[i]);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed computing auth token: " + ex.getMessage(), ex);
                }
            }

            // Build HandshakeResponse41 payload
            ByteArrayOutputStreamEx bout = new ByteArrayOutputStreamEx();
            // client flags (LE)
            final int CLIENT_LONG_PASSWORD = 0x00000001;
            final int CLIENT_CONNECT_WITH_DB = 0x00000008;
            final int CLIENT_PROTOCOL_41 = 0x00000200;
            final int CLIENT_SECURE_CONNECTION = 0x00008000;
            int clientFlags = CLIENT_PROTOCOL_41 | CLIENT_SECURE_CONNECTION | CLIENT_PLUGIN_AUTH | CLIENT_CONNECT_WITH_DB | CLIENT_LONG_PASSWORD;
            bout.writeInt32LE(clientFlags);
            // max packet size (4 bytes LE)
            bout.writeInt32LE(0x01000000);
            // charset (1 byte)
            bout.writeByte(33); // utf8_general_ci typical
            // 23 bytes filler zeros
            for (int i = 0; i < 23; i++) bout.writeByte(0);

            // username NUL-terminated
            if (user != null) bout.writeBytes(user.getBytes(StandardCharsets.UTF_8));
            bout.writeByte(0);

            // auth-response: length + bytes
            bout.writeByte(authResponse.length);
            if (authResponse.length > 0) bout.writeBytes(authResponse);

            // database (if provided) NUL-terminated
            if (database != null && !database.isEmpty()) {
                bout.writeBytes(database.getBytes(StandardCharsets.UTF_8));
                bout.writeByte(0);
            }

            // auth plugin name NUL-terminated
            String pluginName = "mysql_native_password";
            bout.writeBytes(pluginName.getBytes(StandardCharsets.UTF_8));
            bout.writeByte(0);

            // Send packet (3 bytes len LE + 1 byte seq)
            byte[] respPayload = bout.toByteArray();
            int respLen = respPayload.length;
            out.write(respLen & 0xFF);
            out.write((respLen >> 8) & 0xFF);
            out.write((respLen >> 16) & 0xFF);
            // sequence: server used seq 0 for handshake, client uses 1
            out.write(1);
            out.write(respPayload);
            out.flush();

            // Read server response to auth
            byte[] authHeader = readBytes(in, 4);
            int authLen = (authHeader[0] & 0xFF) | ((authHeader[1] & 0xFF) << 8) | ((authHeader[2] & 0xFF) << 16);
            byte[] authReply = readBytes(in, authLen);
            if (authReply.length == 0) {
                throw new RuntimeException("Empty auth reply from server");
            }
            int marker = authReply[0] & 0xFF;
            if (marker == 0x00) {
                // OK_Packet
                this.connected = true;
                System.out.println("[mysql-socket] Handshake OK");
            } else if (marker == 0xFF) {
                // ERR_Packet: parse error message if possible
                int errCode = ((authReply[1] & 0xFF)) | ((authReply[2] & 0xFF) << 8);
                String errMsg = new String(Arrays.copyOfRange(authReply, 9, authReply.length), StandardCharsets.UTF_8); // best-effort
                throw new RuntimeException("MySQL ERR_Packet during auth. Code=" + errCode + " msg=" + errMsg);
            } else {
                // Auth plugin request or other: not implemented in this minimal client
                throw new RuntimeException("Unexpected auth reply marker: 0x" + Integer.toHexString(marker));
            }

            // Si todo lo anterior funciona:
            this.connected = true;
            System.out.println("[mysql-socket] ✓ Connected to " + host + ":" + port + " as user '" + user + "'.");

        } catch (Exception e) {
            this.connected = false;
            closeSocket();
            System.err.println("[mysql-socket] ✗ FAILED to connect via sockets to " + host + ":" + port);
            System.err.println("[mysql-socket]   Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot establish socket connection to MySQL.", e);
        }
    }

    @Override
    public void disconnect() {
        this.connected = false;
        closeSocket();
        System.out.println("[mysql-socket] Disconnected");
    }

    @Override
    public boolean isConnected() {
        return connected && this.socket != null && !this.socket.isClosed();
    }


    @Override
    public List<Map<String, Object>> execute(String sql) {
        if (!isConnected()) {
            throw new IllegalStateException("Connection is not open: " + getName());
        }

        // Enviar COM_QUERY y parsear resultado
        try {
            // 1) Construir y enviar COM_QUERY packet
            ByteArrayOutputStreamEx qout = new ByteArrayOutputStreamEx();
            qout.writeByte(0x03); // COM_QUERY
            qout.writeBytes(sql.getBytes(StandardCharsets.UTF_8));
            byte[] qpayload = qout.toByteArray();
            int qlen = qpayload.length;
            // header: 3 bytes length LE + 1 byte seq (0)
            out.write(qlen & 0xFF);
            out.write((qlen >> 8) & 0xFF);
            out.write((qlen >> 16) & 0xFF);
            out.write(0); // sequence
            out.write(qpayload);
            out.flush();

            // Helper local parser para Length-Encoded Integers / Strings
            class Parser {
                final byte[] buf;
                int pos = 0;
                Parser(byte[] b) { this.buf = b; this.pos = 0; }

                long readLenEncInt() {
                    int x = buf[pos++] & 0xFF;
                    if (x < 0xFB) return x;
                    if (x == 0xFC) {
                        long v = (buf[pos] & 0xFFL) | ((buf[pos + 1] & 0xFFL) << 8);
                        pos += 2;
                        return v;
                    }
                    if (x == 0xFD) {
                        long v = (buf[pos] & 0xFFL) | ((buf[pos + 1] & 0xFFL) << 8) | ((buf[pos + 2] & 0xFFL) << 16);
                        pos += 3;
                        return v;
                    }
                    if (x == 0xFE) {
                        long v = 0;
                        for (int i = 0; i < 8; i++) v |= ((long) (buf[pos + i] & 0xFF)) << (8 * i);
                        pos += 8;
                        return v;
                    }
                    throw new RuntimeException("Unsupported len-encoded int marker: 0x" + Integer.toHexString(x));
                }

                String readLenEncString() {
                    long len = readLenEncInt();
                    if (len == 0) return "";
                    if (len < 0 || pos + (int) len > buf.length) return "";
                    String s = new String(buf, pos, (int) len, StandardCharsets.UTF_8);
                    pos += (int) len;
                    return s;
                }
            }

            // 2) Leer primer paquete de respuesta
            byte[] header = readBytes(in, 4);
            int plen = (header[0] & 0xFF) | ((header[1] & 0xFF) << 8) | ((header[2] & 0xFF) << 16);
            byte[] firstPayload = readBytes(in, plen);

            // Manejar OK / ERR packets (no resultset)
            if (firstPayload.length > 0) {
                int marker = firstPayload[0] & 0xFF;
                if (marker == 0x00) {
                    // OK packet (p.ej. INSERT/UPDATE or no-result)
                    return Collections.emptyList();
                } else if (marker == 0xFF) {
                    int errCode = (firstPayload[1] & 0xFF) | ((firstPayload[2] & 0xFF) << 8);
                    String errMsg = new String(Arrays.copyOfRange(firstPayload, 9, firstPayload.length), StandardCharsets.UTF_8);
                    throw new RuntimeException("MySQL ERR_Packet during query. Code=" + errCode + " msg=" + errMsg);
                }
            }

            // 3) ResultSetHeader: contiene column count como LenEncInt
            Parser pHdr = new Parser(firstPayload);
            long columnCountL = pHdr.readLenEncInt();
            int columnCount = (int) Math.min(columnCountL, Integer.MAX_VALUE);

            // 4) Leer N FieldPacket (column definitions)
            List<String> columnNames = new ArrayList<>(Math.max(0, columnCount));
            for (int i = 0; i < columnCount; i++) {
                byte[] fhead = readBytes(in, 4);
                int flen = (fhead[0] & 0xFF) | ((fhead[1] & 0xFF) << 8) | ((fhead[2] & 0xFF) << 16);
                byte[] fpayload = readBytes(in, flen);
                // Detect EOF (very short 0xFE packet) just in case
                if (fpayload.length > 0 && (fpayload[0] & 0xFF) == 0xFE && fpayload.length < 9) {
                    // Unexpected EOF, break
                    break;
                }
                Parser pf = new Parser(fpayload);
                // FieldPacket: catalog, db, table, org_table, name, org_name, ... -> queremos 'name' (5th)
                pf.readLenEncString(); // catalog
                pf.readLenEncString(); // db
                pf.readLenEncString(); // tableName
                pf.readLenEncString(); // orgTable
                String name = pf.readLenEncString(); // name
                // consume org_name
                pf.readLenEncString();
                columnNames.add(name);
            }

            // 5) Leer EOF packet que termina la definición de columnas
            byte[] eofHead = readBytes(in, 4);
            int eofLen = (eofHead[0] & 0xFF) | ((eofHead[1] & 0xFF) << 8) | ((eofHead[2] & 0xFF) << 16);
            byte[] eofPayload = readBytes(in, eofLen);
            if (eofPayload.length > 0 && (eofPayload[0] & 0xFF) == 0xFF) {
                int errCode = (eofPayload[1] & 0xFF) | ((eofPayload[2] & 0xFF) << 8);
                String errMsg = new String(Arrays.copyOfRange(eofPayload, 9, eofPayload.length), StandardCharsets.UTF_8);
                throw new RuntimeException("MySQL ERR_Packet after fields. Code=" + errCode + " msg=" + errMsg);
            }

            // 6) Leer RowDataPacket(s)
            List<Map<String, Object>> result = new ArrayList<>();
            while (true) {
                byte[] rhead = readBytes(in, 4);
                int rlen = (rhead[0] & 0xFF) | ((rhead[1] & 0xFF) << 8) | ((rhead[2] & 0xFF) << 16);
                byte[] rpayload = readBytes(in, rlen);
                if (rpayload.length > 0) {
                    int m = rpayload[0] & 0xFF;
                    // EOF (end of rows)
                    if (m == 0xFE && rpayload.length < 9) break;
                    // ERR
                    if (m == 0xFF) {
                        int errCode = (rpayload[1] & 0xFF) | ((rpayload[2] & 0xFF) << 8);
                        String errMsg = new String(Arrays.copyOfRange(rpayload, 9, rpayload.length), StandardCharsets.UTF_8);
                        throw new RuntimeException("MySQL ERR_Packet in rows. Code=" + errCode + " msg=" + errMsg);
                    }
                }

                // Parse row values (length-encoded values)
                Parser pr = new Parser(rpayload);
                Map<String, Object> row = new LinkedHashMap<>();
                for (int c = 0; c < columnNames.size(); c++) {
                    // NULL value marker 0xFB
                    if (pr.pos < pr.buf.length && (pr.buf[pr.pos] & 0xFF) == 0xFB) {
                        pr.pos++;
                        row.put(columnNames.get(c), null);
                    } else {
                        String val = pr.readLenEncString();
                        row.put(columnNames.get(c), val);
                    }
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            // Este es el ÚNICO catch block que debe tener el método
            throw new RuntimeException("Failed to execute COM_QUERY: " + e.getMessage(), e);
        }
    }

    private void closeSocket() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null; in = null; out = null;
    }

    /**
     * ¡CUIDADO! Lee un Int32 (4 bytes) Big Endian.
     * El protocolo de MySQL usa Little Endian y longitudes variables.
     * Es posible que este helper no te sirva para todo.
     */
    private int readInt(InputStream in) throws Exception {
        byte[] b = readBytes(in, 4);
        // MySQL es Little Endian, Postgres es Big Endian. ¡DIFERENTE!
        // return ByteBuffer.wrap(b).getInt(); // Big Endian
        return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24); // Little Endian
    }

    /**
     * Lee N bytes exactos de un stream. Este helper es ORO.
     */
    private byte[] readBytes(InputStream in, int n) throws Exception {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r == -1) throw new RuntimeException("Unexpected EOF");
            off += r;
        }
        return buf;
    }

    /**
     * Lee una string terminada en 0x00. Útil para MySQL.
     */
    private String readNullTerminatedString(InputStream in) throws IOException {
        ByteArrayOutputStreamEx bos = new ByteArrayOutputStreamEx();
        int b;
        while ((b = in.read()) != 0) {
            if (b == -1) throw new IOException("Unexpected EOF");
            bos.write(b);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Helper para construir paquetes de bytes. ¡Útil!
     */
    private static class ByteArrayOutputStreamEx extends java.io.ByteArrayOutputStream {
        // Escribe un Int32 en Big Endian (Postgres)
        void writeInt32BE(int v) {
            this.write((v >> 24) & 0xFF);
            this.write((v >> 16) & 0xFF);
            this.write((v >> 8) & 0xFF);
            this.write(v & 0xFF);
        }

        // Escribe un Int32 en Little Endian (MySQL)
        void writeInt32LE(int v) {
            this.write(v & 0xFF);
            this.write((v >> 8) & 0xFF);
            this.write((v >> 16) & 0xFF);
            this.write((v >> 24) & 0xFF);
        }

        // Escribe un String terminado en 0
        void writeString(String s) {
            try { this.write(s.getBytes(StandardCharsets.UTF_8)); } catch (Exception ignored) {}
            this.write(0);
        }
        public void writeBytes(byte[] b) { this.write(b, 0, b.length); }
        public void writeByte(int b) { this.write(b); }
    }

    // ----------------- Lectura de fichero de configuración -----------------

    private static Properties loadConfigFile() {
        Properties p = new Properties();
        // Busca 'db.properties' en la carpeta 'resources'
        try (InputStream in = MySQLConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
                System.out.println("[mysql-socket] Loaded db.properties from classpath");
            } else {
                System.out.println("[mysql-socket] db.properties not found in classpath.");
            }
        } catch (Exception e) {
            System.err.println("[mysql-socket] Error loading db.properties: " + e.getMessage());
        }
        return p;
    }

    /**
     * Busca un valor de configuración en orden:
     * 1. Fichero db.properties (con la 1ª clave)
     * 2. Fichero db.properties (con la 2ª clave, ej. mysql.host)
     * 3. Variable de Entorno (con la 1ª clave, ej. MYSQL_HOST)
     * 4. Propiedad del sistema Java (con la 1ª clave)
     * 5. Valor por defecto
     */
    private static String getConfigValue(String primaryKey, String secondaryKey, String defaultValue) {
        // 1) fichero (primary)
        String v = FILE_CONFIG.getProperty(primaryKey);
        if (v != null && !v.isEmpty()) return v;

        // 2) fichero (secondary)
        v = FILE_CONFIG.getProperty(secondaryKey);
        if (v != null && !v.isEmpty()) return v;

        // 3) env
        v = System.getenv(primaryKey);
        if (v != null && !v.isEmpty()) return v;

        // 4) system properties
        v = System.getProperty(primaryKey);
        if (v != null && !v.isEmpty()) return v;

        // 5) default
        return defaultValue;
    }
}
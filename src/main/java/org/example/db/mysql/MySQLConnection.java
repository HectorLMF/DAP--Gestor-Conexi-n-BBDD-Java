package org.example.db.mysql;

import org.example.db.DBConnection;
import org.example.db.utilities.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Conexión a MySQL implementada mediante sockets (cliente mínimo del protocolo)
 * con fallback automático a JDBC si la conexión nativa falla.
 * 
 * Esta implementación soporta:
 * - Conexión nativa por socket (protocolo MySQL)
 * - Autenticación mysql_native_password (MySQL 5.x, 8.x legacy)
 * - Fallback automático a JDBC si falla socket
 * - Ejecución de queries via protocolo nativo o JDBC
 */
public class MySQLConnection implements DBConnection {
    private final String name;
    
    // Parámetros de conexión
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    
    // Socket/native connection
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    
    // JDBC fallback
    private Connection jdbcConnection;
    
    // Estado
    private boolean connected = false;
    private boolean nativeConnected = false;
    private boolean jdbcFallbackMode = false;

    public MySQLConnection(String name) {
        this(
                name,
                ConnectionConfig.getConfigValue("MYSQL_HOST", "MYSQL_HOST", "localhost"),
                ConnectionConfig.getConfigValueAsInt("MYSQL_PORT", "MYSQL_PORT", 3306),
                ConnectionConfig.getConfigValue("MYSQL_DATABASE", "MYSQL_DATABASE", "test"),
                ConnectionConfig.getConfigValue("MYSQL_USER", "MYSQL_USER", "root"),
                ConnectionConfig.getConfigValue("MYSQL_PASSWORD", "MYSQL_PASSWORD", "root")
        );
    }

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

    @Override
    public void connect() {
        System.out.println("[mysql-socket] Intentando conectar via sockets a " + host + ":" + port + " (db=" + database + ") as user=" + user);
        try {
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            // Leer Initial Handshake Packet
            byte[] handshake = readPacket();
            
            // Parsear server capabilities y auth plugin
            int offset = 0;
            byte protocolVersion = handshake[offset++];
            
            // Server version (null-terminated string)
            StringBuilder serverVersion = new StringBuilder();
            while (handshake[offset] != 0) {
                serverVersion.append((char) handshake[offset++]);
            }
            offset++; // skip null
            
            // Connection ID (4 bytes)
            offset += 4;
            
            // Auth plugin data part 1 (8 bytes)
            byte[] authData1 = Arrays.copyOfRange(handshake, offset, offset + 8);
            offset += 8;
            
            // Filler (1 byte)
            offset++;
            
            // Capability flags lower 2 bytes
            int capLower = ((handshake[offset] & 0xFF) | ((handshake[offset + 1] & 0xFF) << 8));
            offset += 2;
            
            // Skip charset, status flags, capability upper
            offset += 1 + 2 + 2;
            
            // Auth plugin data length
            int authDataLen = handshake[offset++] & 0xFF;
            
            // Skip reserved
            offset += 10;
            
            // Auth plugin data part 2
            int part2Len = Math.max(13, authDataLen - 8);
            byte[] authData2 = Arrays.copyOfRange(handshake, offset, offset + part2Len - 1); // -1 for null terminator
            offset += part2Len;
            
            // Auth plugin name
            StringBuilder authPlugin = new StringBuilder();
            while (offset < handshake.length && handshake[offset] != 0) {
                authPlugin.append((char) handshake[offset++]);
            }
            
            System.out.println("[mysql-socket] Server version: " + serverVersion + ", Auth plugin: " + authPlugin);
            
            // Si el plugin de autenticación no es mysql_native_password, usar fallback JDBC
            if (!authPlugin.toString().equals("mysql_native_password")) {
                System.err.println("[mysql-socket] Auth plugin '" + authPlugin + "' no soportado por socket client. Usando JDBC fallback...");
                throw new RuntimeException("Unsupported auth plugin: " + authPlugin + ". Falling back to JDBC.");
            }
            
            // Combinar auth data
            byte[] salt = new byte[authData1.length + authData2.length];
            System.arraycopy(authData1, 0, salt, 0, authData1.length);
            System.arraycopy(authData2, 0, salt, authData1.length, authData2.length);
            
            // Enviar Handshake Response Packet (autenticación)
            sendHandshakeResponse(salt, authPlugin.toString());
            
            // Leer respuesta de autenticación
            byte[] authResponse = readPacket();
            if (authResponse[0] == 0x00) {
                // OK packet
                nativeConnected = true;
                connected = true;
                System.out.println("[mysql-socket] ✓ Connected to " + host + ":" + port + " database '" + database + "'");
                return;
            } else if (authResponse[0] == (byte) 0xFF) {
                // Error packet
                String error = new String(Arrays.copyOfRange(authResponse, 3, authResponse.length), StandardCharsets.UTF_8);
                throw new RuntimeException("MySQL authentication error: " + error);
            } else {
                throw new RuntimeException("Unexpected auth response");
            }

        } catch (Exception e) {
            nativeConnected = false;
            closeSocket();
            System.err.println("[mysql-socket] ✗ FAILED to connect via sockets to " + host + ":" + port);
            System.err.println("[mysql-socket]   Error: " + e.getMessage());
            System.err.println("[mysql-socket]   Intentando fallback a JDBC...");
            
            // Fallback: intentar conexión JDBC usando la utilidad
            try {
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
                jdbcConnection = JDBCConnectionHelper.createConnection(jdbcUrl, user, password);
                jdbcFallbackMode = true;
                connected = true;
                System.out.println("[mysql-jdbc] ✓ Connected via JDBC fallback to " + host + ":" + port + " database '" + database + "'");
                return;
            } catch (SQLException jdbcEx) {
                connected = false;
                jdbcFallbackMode = false;
                System.err.println("[mysql-jdbc] ✗ JDBC fallback also failed: " + jdbcEx.getMessage());
                throw new RuntimeException("Cannot establish connection to MySQL (socket and JDBC both failed). Check Docker and credentials.", jdbcEx);
            }
        }
    }

    @Override
    public void disconnect() {
        this.connected = false;
        nativeConnected = false;
        jdbcFallbackMode = false;
        closeSocket();
        closeJdbc();
        System.out.println("[mysql-socket] Disconnected");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    public boolean isNativeConnected() {
        return nativeConnected;
    }

    @Override
    public List<Map<String, Object>> execute(String sql) {
        if (!isConnected()) {
            throw new IllegalStateException("Connection is not open: " + getName());
        }
        
        // Limpiar y normalizar el SQL
        sql = SQLCleaner.cleanSql(sql);
        
        // Si estamos en modo JDBC fallback, usar JDBC
        if (jdbcFallbackMode && jdbcConnection != null) {
            return executeJdbc(sql);
        }
        
        // Si no, usar socket nativo
        if (!nativeConnected || socket == null || out == null || in == null) {
            throw new IllegalStateException("Socket connection is not established. Cannot execute query.");
        }
        
        try {
            System.out.println("[mysql-socket] Executing native SQL: " + sql);
            List<Map<String, Object>> res = executeNativeQuerySocket(sql);
            System.out.println("[mysql-socket] Native query returned rows: " + (res == null ? 0 : res.size()));
            return res;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query: " + sql + ". Error: " + e.getMessage(), e);
        }
    }
    
    // Ejecutar query via JDBC
    private List<Map<String, Object>> executeJdbc(String sql) {
        System.out.println("[mysql-jdbc] Executing JDBC SQL: " + sql);
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (Statement stmt = jdbcConnection.createStatement()) {
            // Usar SQLCleaner para detectar el tipo de consulta
            boolean isSelect = SQLCleaner.isSelectQuery(sql);
            
            if (isSelect) {
                // Para SELECT, usar executeQuery y convertir con utilidad
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    rows = ResultSetConverter.convertToList(rs);
                    System.out.println("[mysql-jdbc] JDBC query returned rows: " + rows.size());
                }
            } else {
                // Para DDL/DML, usar executeUpdate y crear respuesta con utilidad
                int affectedRows = stmt.executeUpdate(sql);
                System.out.println("[mysql-jdbc] JDBC statement executed. Affected rows: " + affectedRows);
                rows.add(QueryResponseBuilder.createSuccessResponse(affectedRows));
            }
            
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute JDBC query: " + sql + ". Error: " + e.getMessage(), e);
        }
    }
    
    // Ejecutar query via protocolo MySQL nativo
    private List<Map<String, Object>> executeNativeQuerySocket(String sql) throws Exception {
        // Enviar COM_QUERY
        sendQuery(sql);
        
        // Leer respuesta
        byte[] response = readPacket();
        
        if (response[0] == (byte) 0xFF) {
            // Error packet
            String error = new String(Arrays.copyOfRange(response, 3, response.length), StandardCharsets.UTF_8);
            throw new RuntimeException("MySQL query error: " + error);
        }
        
        if (response[0] == 0x00) {
            // OK packet (para UPDATE, INSERT, DELETE)
            return Collections.emptyList();
        }
        
        // Result set
        // First packet contains column count
        int columnCount = response[0] & 0xFF;
        
        // Read column definitions
        List<String> columnNames = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            byte[] colDef = readPacket();
            // Parsear column definition (simplificado)
            // Catalog, schema, table, org_table, name, org_name...
            // Por simplicidad, saltar a name (hay offsets variables por length-encoded strings)
            // Aquí solo parseamos básico
            int pos = 0;
            // Skip catalog
            pos += getLengthEncodedInt(colDef, pos);
            pos += colDef[pos] & 0xFF;
            // Skip schema
            pos += getLengthEncodedInt(colDef, pos);
            pos += colDef[pos] & 0xFF;
            // Skip table
            pos += getLengthEncodedInt(colDef, pos);
            pos += colDef[pos] & 0xFF;
            // Skip org_table
            pos += getLengthEncodedInt(colDef, pos);
            pos += colDef[pos] & 0xFF;
            // Name
            int nameLen = getLengthEncodedInt(colDef, pos);
            pos++;
            String colName = new String(colDef, pos, nameLen, StandardCharsets.UTF_8);
            columnNames.add(colName);
        }
        
        // EOF packet
        readPacket();
        
        // Read rows
        List<Map<String, Object>> rows = new ArrayList<>();
        while (true) {
            byte[] row = readPacket();
            if (row[0] == (byte) 0xFE && row.length < 9) {
                // EOF packet
                break;
            }
            
            Map<String, Object> rowMap = new LinkedHashMap<>();
            int pos = 0;
            for (String colName : columnNames) {
                if (row[pos] == (byte) 0xFB) {
                    // NULL
                    rowMap.put(colName, null);
                    pos++;
                } else {
                    int len = getLengthEncodedInt(row, pos);
                    pos++;
                    String value = new String(row, pos, len, StandardCharsets.UTF_8);
                    rowMap.put(colName, value);
                    pos += len;
                }
            }
            rows.add(rowMap);
        }
        
        return rows;
    }
    
    // =============== Protocol Helper Methods ===============
    
    private byte[] readPacket() throws Exception {
        return MySQLProtocolHelper.readPacket(in);
    }
    
    private void writePacket(byte[] payload, int sequenceId) throws Exception {
        MySQLProtocolHelper.writePacket(out, payload, sequenceId);
    }
    
    private int getLengthEncodedInt(byte[] data, int offset) {
        return MySQLProtocolHelper.getLengthEncodedInt(data, offset);
    }
    
    private void sendQuery(String sql) throws Exception {
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        bout.write(0x03); // COM_QUERY
        bout.write(sql.getBytes(StandardCharsets.UTF_8));
        writePacket(bout.toByteArray(), 0);
    }
    
    private void sendHandshakeResponse(byte[] salt, String authPlugin) throws Exception {
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        
        // Client capabilities (32-bit)
        int capabilities = 0x000F_FFFF; // CLIENT_LONG_PASSWORD | CLIENT_PROTOCOL_41 | etc.
        bout.write(capabilities & 0xFF);
        bout.write((capabilities >> 8) & 0xFF);
        bout.write((capabilities >> 16) & 0xFF);
        bout.write((capabilities >> 24) & 0xFF);
        
        // Max packet size (4 bytes)
        bout.write(new byte[]{0, 0, 0, 1});
        
        // Character set (1 byte) - utf8_general_ci
        bout.write(33);
        
        // Reserved (23 bytes)
        bout.write(new byte[23]);
        
        // Username (null-terminated)
        MySQLProtocolHelper.writeNullTerminatedString(bout, user);
        
        // Auth response
        if (authPlugin.contains("mysql_native_password")) {
            // mysql_native_password: SHA1( password ) XOR SHA1( salt + SHA1( SHA1( password ) ) )
            byte[] passwordHash = MySQLProtocolHelper.sha1(password.getBytes(StandardCharsets.UTF_8));
            byte[] passwordHashHash = MySQLProtocolHelper.sha1(passwordHash);
            
            byte[] combined = new byte[salt.length + passwordHashHash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(passwordHashHash, 0, combined, salt.length, passwordHashHash.length);
            
            byte[] hash = MySQLProtocolHelper.sha1(combined);
            byte[] authResponse = MySQLProtocolHelper.xor(passwordHash, hash);
            
            // Length + auth response
            bout.write(authResponse.length);
            bout.write(authResponse);
        } else {
            // Sin autenticación
            bout.write(0);
        }
        
        // Database (null-terminated)
        if (database != null && !database.isEmpty()) {
            MySQLProtocolHelper.writeNullTerminatedString(bout, database);
        }
        
        // Auth plugin name
        if (authPlugin != null && !authPlugin.isEmpty()) {
            MySQLProtocolHelper.writeNullTerminatedString(bout, authPlugin);
        }
        
        writePacket(bout.toByteArray(), 1);
    }
    
    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null; in = null; out = null;
    }
    
    private void closeJdbc() {
        try { if (jdbcConnection != null) jdbcConnection.close(); } catch (Exception ignored) {}
        jdbcConnection = null;
    }
}

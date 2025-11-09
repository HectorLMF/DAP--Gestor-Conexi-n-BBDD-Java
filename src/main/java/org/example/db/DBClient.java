package org.example.db;

import java.util.List;
import java.util.Map;

/**
 * Cliente genérico que utiliza una DBFactory para crear conexiones y consultas.
 * Este cliente sigue el patrón Abstract Factory y expone un modo simple
 * para ejecutar consultas en texto.
 */
public class DBClient {
    private final DBFactory factory;
    private DBConnection conn;
    private final String connName;

    public DBClient(DBFactory factory, String connName) {
        this.factory = factory;
        this.connName = connName;
    }

    public void connect() {
        this.conn = factory.createConnection(connName);
        this.conn.connect();
    }

    public void disconnect() {
        if (this.conn != null) this.conn.disconnect();
    }

    /**
     * Ejecuta una consulta SQL en modo texto y devuelve las filas (columna->valor).
     */
    public List<Map<String,Object>> executeText(String sql) {
        if (conn == null) throw new IllegalStateException("No connection. Call connect() first.");
        DBQuery q = factory.createQuery(conn);
        q.setSql(sql);
        return q.execute();
    }

    public DBConnection getConnection() { return conn; }
}

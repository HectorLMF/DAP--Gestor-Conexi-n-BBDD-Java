package org.example.db.mysql;

import org.example.db.DBConnection;
import org.example.db.DBQuery;
import java.util.List;
import java.util.Map;

/**
 * Implementación de una consulta para MySQL.
 * Guarda la sentencia SQL y delega su ejecución en la conexión.
 */
public class MySQLQuery implements DBQuery {

    /** Conexión asociada. */
    private final DBConnection conn;
    /** Sentencia SQL configurada para esta query. */
    private String sql;

    /**
     * Constructor.
     *
     * @param conn Conexión asociada (no nula)
     */
    public MySQLQuery(DBConnection conn) {
        if (conn == null) {
            throw new IllegalArgumentException("La conexión no puede ser nula");
        }
        this.conn = conn;
    }

    /**
     * Establece la sentencia SQL.
     *
     * @param sql Sentencia SQL en texto plano
     */
    @Override
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * Ejecuta la consulta (previamente guardada con setSql).
     *
     * Invoca la ejecución sobre la conexión (conn.execute(sql)).
     *
     * @return Lista de filas (Map columna->valor)
     */
    @Override
    public List<Map<String, Object>> execute() {

        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalStateException("El SQL no ha sido establecido (usa setSql)");
        }

        try {
            // Asegura que la conexión esté abierta
            if (!conn.isConnected()) {
                System.out.println("[MySQLQuery] Conexión no está activa. Llamando a connect()...");
                conn.connect();
            }
            // Delegar en la conexión
            return conn.execute(sql);

        } catch (Exception e) {
            System.err.println("Error fatal en MySQLQuery.execute: " + e.getMessage());
            // Intenta desconectar para limpiar
            try {
                if (conn.isConnected()) {
                    conn.disconnect();
                }
            } catch (Exception ignored) {}

            throw new RuntimeException("Fallo la consulta SQL: " + sql + ": " + e.getMessage(), e);
        }
    }
}
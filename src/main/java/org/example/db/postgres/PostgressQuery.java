package org.example.db.postgres;

import org.example.db.DBConnection;
import org.example.db.DBQuery;
import java.util.List;
import java.util.Map;

/**
 * @class PostgressQuery
 * @brief Implementación simulada de una consulta para Postgres.
 *
 * Representa una consulta SQL asociada a una conexión Postgres simulada. Guarda
 * la sentencia SQL y delega su ejecución en la conexión subyacente.
 */
public class PostgressQuery implements DBQuery {
    /** Conexión asociada. */
    private final DBConnection conn;
    /** Sentencia SQL configurada para esta query. */
    private String sql;

    /**
     * @brief Constructor.
     *
     * @param conn Conexión asociada (no nula)
     */
    public PostgressQuery(DBConnection conn) {
        this.conn = conn;
    }

    /**
     * @brief Establece la sentencia SQL.
     *
     * @param sql Sentencia SQL en texto plano
     */
    @Override
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * @brief Ejecuta la consulta y devuelve los resultados.
     *
     * Esta implementación invoca la ejecución sobre la conexión (conn.execute(sql)).
     * Si la conexión no está abierta se lanzará IllegalStateException por parte de la conexión.
     *
     * @return Lista de filas (Map columna->valor)
     */
    @Override
    public List<Map<String, Object>> execute() {
        if (conn == null) {
            throw new IllegalStateException("No connection associated with this query");
        }
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalStateException("SQL is not set for this query");
        }
        // Delegar en la conexión: ésta validará si está abierta y realizará la simulación
        return conn.execute(sql);
    }
}

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
     * Se espera que esta implementación invoque la ejecución sobre la
     * conexión (por ejemplo conn.execute(sql)). Actualmente está marcada como
     * TODO y lanza UnsupportedOperationException.
     *
     * @return Lista de filas (Map columna->valor)
     * @throws UnsupportedOperationException si no está implementado
     */
    @Override
    public List<Map<String, Object>> execute() {
        // TODO: implementar delegación a la conexión
        throw new UnsupportedOperationException("Not implemented");
    }
}

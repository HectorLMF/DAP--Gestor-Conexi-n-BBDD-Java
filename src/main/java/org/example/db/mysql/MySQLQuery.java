package org.example.db.mysql;

import org.example.db.DBConnection;
import org.example.db.DBQuery;
import java.util.List;
import java.util.Map;

/**
 * Implementación simulada de una consulta para MySQL.
 *
 * Esta clase representa una consulta SQL asociada a una {@link DBConnection}.
 * Se utiliza para encapsular la sentencia SQL y solicitar su ejecución a la
 * conexión subyacente. Actualmente la ejecución está pendiente de implementar
 * y está marcada con TODO.
 */
public class MySQLQuery implements DBQuery {
    /** Conexión asociada a esta query. */
    private final DBConnection conn;
    /** Sentencia SQL a ejecutar. */
    private String sql;

    /**
     * Constructor.
     *
     * @param conn Conexión sobre la que se ejecutarán las consultas (no nulo)
     */
    public MySQLQuery(DBConnection conn) {
        this.conn = conn;
    }

    /**
     * Establece la sentencia SQL de la query.
     *
     * @param sql Sentencia SQL en texto plano (por ejemplo: "SELECT * FROM users")
     */
    @Override
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * Ejecuta la query delegando en la conexión subyacente.
     *
     * La implementación prevista es que este método invoque algo similar a
     * `conn.execute(sql)` y realice transformaciones mínimas sobre el resultado.
     * Actualmente no está implementado.
     *
     * @return Lista de filas (cada fila: Map nombreColumna->valor)
     * @throws UnsupportedOperationException si no está implementado
     */
    @Override
    public List<Map<String, Object>> execute() {
        // TODO: delegar en la conexión la ejecución simulada
        throw new UnsupportedOperationException("Not implemented");
    }
}

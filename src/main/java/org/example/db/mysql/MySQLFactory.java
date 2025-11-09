package org.example.db.mysql;

import org.example.db.DBConnection;
import org.example.db.DBFactory;
import org.example.db.DBQuery;

/**
 * Fábrica para la implementación de MySQL, siguiendo el patrón de Postgres.
 * Crea instancias de MySQLConnection y MySQLQuery.
 */
public class MySQLFactory implements DBFactory {

    /**
     * Crea una conexión MySQL "inteligente".
     *
     * @param name Nombre lógico para la conexión (ej. "demo")
     * @return Instancia de DBConnection (que será tu MySQLConnection)
     */
    @Override
    public DBConnection createConnection(String name) {
        // Llama al constructor "inteligente" de MySQLConnection
        // que automáticamente lee db.properties y usa 'name' como prefijo.
        return new MySQLConnection(name);
    }

    /**
     * Crea una query asociada a la conexión MySQL proporcionada.
     *
     * @param conn Conexión que se asociará con la query
     * @return Instancia de DBQuery (que será tu MySQLQuery)
     */
    @Override
    public DBQuery createQuery(DBConnection conn) {
        return new MySQLQuery(conn);
    }
}
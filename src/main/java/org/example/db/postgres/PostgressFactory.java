package org.example.db.postgres;

import org.example.db.DBConnection;
import org.example.db.DBFactory;
import org.example.db.DBQuery;

/**
 * @class PostgressFactory
 * @brief Fábrica concreta para crear objetos relacionados con Postgres.
 *
 * Implementa {@link org.example.db.DBFactory} y produce instancias concretas
 * de {@link PostgressConnection} y {@link PostgressQuery} que representan la
 * capa de persistencia del proveedor Postgres de forma simulada.
 */
public class PostgressFactory implements DBFactory {

    /**
     * @brief Crea una conexión Postgres simulada.
     *
     * @param name Nombre lógico para la conexión
     * @return Instancia de {@link org.example.db.DBConnection}
     */
    @Override
    public DBConnection createConnection(String name) {
        // TODO: implementar
        return new PostgressConnection(name);
    }

    /**
     * @brief Crea una query asociada a la conexión Postgres proporcionada.
     *
     * @param conn Conexión que se asociará con la query
     * @return Instancia de {@link org.example.db.DBQuery}
     */
    @Override
    public DBQuery createQuery(DBConnection conn) {
        // TODO: implementar
        return new PostgressQuery(conn);
    }
}

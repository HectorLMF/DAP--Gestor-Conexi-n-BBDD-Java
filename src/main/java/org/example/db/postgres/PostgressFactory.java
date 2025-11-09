package org.example.db.postgres;

import org.example.db.DBConnection;
import org.example.db.DBFactory;
import org.example.db.DBQuery;


/**
 * @file PostgressFactory.java
 * @brief Fábrica concreta para el proveedor Postgres.
 *
 * Esta implementación produce instancias de {@link PostgressConnection} y
 * {@link PostgressQuery}. La fábrica centraliza la creación de objetos relacionados
 * con el proveedor Postgres y actúa como punto único donde se podría añadir
 * lógica de configuración (lectura de properties, selección de modo nativo/simulado).
 *
 * Recomendación: al implementar la versión MySQL, proporcionar una fábrica
 * equivalente {@code MySQLFactory} que devuelva conexiones JDBC y queries
 * que deleguen en esas conexiones.
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
        return new PostgressQuery(conn);
    }
}

package org.example.db.mysql;

import org.example.db.DBConnection;
import org.example.db.DBFactory;
import org.example.db.DBQuery;

/**
 * Fábrica concreta para crear objetos relacionados con MySQL.
 *
 * Implementa la interfaz {@link org.example.db.DBFactory} y produce instancias
 * de {@link org.example.db.DBConnection} y {@link org.example.db.DBQuery}
 * específicas para el proveedor MySQL. El propósito es separar la creación de
 * objetos concretos del resto de la aplicación (patrón Abstract Factory).
 */
public class MySQLFactory implements DBFactory {

    /**
     * Crea una conexión MySQL simulada.
     *
     * Crea y devuelve una instancia de {@link MySQLConnection} que actúa como
     * representación de una conexión a MySQL. Actualmente la conexión es un stub
     * con métodos sin implementar.
     *
     * @param name Nombre lógico para la conexión
     * @return Instancia de {@link org.example.db.DBConnection}
     */
    @Override
    public DBConnection createConnection(String name) {
        // TODO: implementar
        return new MySQLConnection(name);
    }

    /**
     * Crea una query asociada a la conexión proporcionada.
     *
     * Devuelve una instancia de {@link MySQLQuery} que delega la ejecución en la
     * conexión asociada. El objeto devuelto implementa {@link org.example.db.DBQuery}.
     *
     * @param conn Conexión sobre la que se ejecutarán las queries
     * @return Instancia de {@link org.example.db.DBQuery}
     */
    @Override
    public DBQuery createQuery(DBConnection conn) {
        // TODO: implementar
        return new MySQLQuery(conn);
    }
}

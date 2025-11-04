/**
 * Package: org.example.db
 */
package org.example.db;

/**
 * Interfaz de la Abstract Factory que produce conexiones y queries según el proveedor.
 *
 * @author Equipo
 */
public interface DBFactory {
    /**
     * Crea una nueva conexión concreta asociada al proveedor.
     *
     * @param name Nombre lógico de la conexión
     * @return instancia de {@link DBConnection}
     */
    DBConnection createConnection(String name);

    /**
     * Crea un objeto {@link DBQuery} asociado a la conexión dada.
     *
     * @param conn Conexión para la que se crea la query
     * @return instancia de {@link DBQuery}
     */
    DBQuery createQuery(DBConnection conn);
}


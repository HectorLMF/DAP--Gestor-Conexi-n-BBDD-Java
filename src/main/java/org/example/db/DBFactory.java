/**
 * Package: org.example.db
 */
package org.example.db;

/**
 * @file DBFactory.java
 * @brief Interfaz de la Abstract Factory para producir objetos relacionados con
 *        la persistencia (conexiones y queries) según el proveedor.
 *
 * DBFactory permite desacoplar la lógica de creación de objetos concretos
 * (por ejemplo, PostgressConnection/PostgressQuery o MySQLConnection/MySQLQuery)
 * del resto de la aplicación. Gracias a esta abstracción el cliente genérico
 * {@link org.example.db.DBClient} puede trabajar con distintos proveedores
 * sin cambios en su código.
 *
 * Recomendaciones para la implementación de nuevas fábricas (p. ej. MySQL):
 * - Implementar createConnection para devolver una conexión configurada.
 * - Implementar createQuery para devolver un objeto que encapsule la
 *   ejecución SQL sobre la conexión.
 * - Documentar en la clase concreta cómo se obtienen variables de entorno
 *   (PGHOST/PGPORT/...) o fichero de propiedades.
 *
 * @author Equipo
 */
public interface DBFactory {
    /**
     * Crea una nueva conexión concreta asociada al proveedor.
     *
     * @param name Nombre lógico de la conexión (ej: "web-demo")
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


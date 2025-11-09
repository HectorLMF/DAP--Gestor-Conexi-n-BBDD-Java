package org.example.db;

import java.util.List;
import java.util.Map;

/**
 * @file DBQuery.java
 * @brief Interfaz que abstrae una consulta SQL ligada a una conexión.
 *
 * DBQuery representa una sentencia SQL que se puede ejecutar sobre una
 * {@link DBConnection}. La intención es desacoplar la construcción de la
 * sentencia (setSql) de su ejecución (execute), permitiendo que diferentes
 * proveedores implementen la ejecución a su manera.
 *
 * Ejemplo de uso:
 * - Crear fábrica concreta (PostgressFactory/MySQLFactory)
 * - Obtener conexión y crear una query con factory.createQuery(conn)
 * - setSql("SELECT ...") y execute()
 *
 * @author Equipo
 */
public interface DBQuery {
    /**
     * Establece la sentencia SQL que se ejecutará.
     *
     * @param sql sentencia SQL en texto plano
     */
    void setSql(String sql);

    /**
     * Ejecuta la sentencia previamente establecida y devuelve los resultados.
     *
     * @return lista de filas (cada fila: Map nombreColumna -> valor)
     * @throws IllegalStateException si no se ha establecido SQL o si la
     *         conexión subyacente no está abierta
     */
    List<Map<String,Object>> execute();
}


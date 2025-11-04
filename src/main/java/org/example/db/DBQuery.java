package org.example.db;

import java.util.List;
import java.util.Map;

/**
 * Interfaz que representa una consulta que se puede ejecutar sobre una {@link DBConnection}.
 */
public interface DBQuery {
    /**
     * Establece la sentencia SQL de la query.
     * @param sql sentencia SQL
     */
    void setSql(String sql);

    /**
     * Ejecuta la query y devuelve los resultados.
     * @return lista de filas como mapas columna->valor
     */
    List<Map<String,Object>> execute();
}


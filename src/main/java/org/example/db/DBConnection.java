package org.example.db;

import java.util.List;
import java.util.Map;

/**
 * Interfaz que representa una conexión a base de datos (simulada).
 */
public interface DBConnection {
    /**
     * Obtiene el nombre de la conexión (identificador único).
     * @return nombre de la conexión
     */
    String getName();

    /**
     * Abre/establece la conexión.
     */
    void connect();

    /**
     * Cierra la conexión.
     */
    void disconnect();

    /**
     * Indica si la conexión está establecida.
     * @return true si está conectada
     */
    boolean isConnected();

    /**
     * Ejecuta una sentencia SQL (simulada) y devuelve filas como lista de mapas.
     * Este método es un stub; la implementación real la hará cada proveedor.
     *
     * @param sql sentencia SQL
     * @return lista de filas (cada fila: nombreColumna -> valor)
     */
    List<Map<String,Object>> execute(String sql);
}


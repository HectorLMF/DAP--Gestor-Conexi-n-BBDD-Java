package org.example.db.postgres;

import org.example.db.DBConnection;
import java.util.List;
import java.util.Map;

/**
 * @class PostgressConnection
 * @brief Conexión simulada para el proveedor Postgres.
 *
 * Implementa {@link org.example.db.DBConnection} para ofrecer una API similar a
 * una conexión real de Postgres, pero sin usar drivers reales. Está pensada
 * como stub para pruebas y para que la capa web pueda interactuar con una
 * representación consistente del proveedor Postgres.
 */
public class PostgressConnection implements DBConnection {
    /** Identificador lógico de la conexión (prefijo "postgres:"). */
    private final String name;

    /**
     * @brief Constructor.
     *
     * @param name Nombre lógico de la conexión
     */
    public PostgressConnection(String name) {
        this.name = "postgres:" + name;
    }

    /**
     * @brief Devuelve el nombre/identificador de la conexión.
     *
     * @return Cadena con el nombre de la conexión (no nula)
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @brief Abre la conexión (simulada).
     *
     * Debe actualizar el estado interno para reflejar que la conexión está
     * activa. Actualmente lanza UnsupportedOperationException hasta que se
     * implemente.
     */
    @Override
    public void connect() {
        // TODO: implementar
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * @brief Cierra la conexión (simulada).
     *
     * Debe actualizar el estado interno liberando recursos simulados.
     */
    @Override
    public void disconnect() {
        // TODO: implementar
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * @brief Indica si la conexión está establecida.
     *
     * @return true si la conexión se considera activa; false en caso contrario
     */
    @Override
    public boolean isConnected() {
        // TODO: implementar
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * @brief Ejecuta una sentencia SQL de forma simulada y devuelve filas.
     *
     * @param sql Sentencia SQL (texto)
     * @return Lista de filas (cada fila: Map columna->valor)
     */
    @Override
    public List<Map<String, Object>> execute(String sql) {
        // TODO: implementar
        throw new UnsupportedOperationException("Not implemented");
    }
}

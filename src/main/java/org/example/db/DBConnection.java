package org.example.db;

import java.util.List;
import java.util.Map;

/**
 * @file DBConnection.java
 * @brief Interfaz que representa una conexión a una base de datos.
 *
 * Esta interfaz define el contrato mínimo que debe implementar cualquier
 * conexión de base de datos en el proyecto (Postgres, MySQL u otros).
 * Se utiliza por las fábricas ({@link DBFactory}) y por el cliente genérico
 * {@link org.example.db.DBClient} para abrir/cerrar conexiones y ejecutar
 * consultas en texto.
 *
 * Notas para el equipo:
 * - Las implementaciones pueden ser simuladas (in-memory) o nativas (JDBC
 *   o cliente de protocolo). Deben respetar los modos: connect(), disconnect(),
 *   isConnected() y execute(sql).
 * - El método execute devuelve una lista de filas representadas como mapas
 *   (nombreColumna -> valor). Esto facilita que el middleware trabaje con
 *   resultados de diferentes proveedores sin convertir a objetos de dominio.
 *
 * @author Equipo
 */
public interface DBConnection {
    /**
     * Obtiene el nombre identificador de la conexión.
     * El nombre suele incluir un prefijo por proveedor (por ejemplo: "postgres:demo").
     *
     * @return nombre lógico de la conexión (no nulo)
     */
    String getName();

    /**
     * Abre/establece la conexión. Debe preparar recursos necesarios.
     * Implementaciones JDBC pueden abrir un DataSource/Connection; las
     * implementaciones simuladas deben inicializar estructuras en memoria.
     *
     * Si la conexión no puede establecerse se debe lanzar una RuntimeException
     * con un mensaje descriptivo.
     */
    void connect();

    /**
     * Cierra la conexión y libera recursos asociados.
     * Este método debe ser seguro de llamar varias veces (idempotente).
     */
    void disconnect();

    /**
     * Indica si la conexión está actualmente abierta.
     *
     * @return true si la conexión está establecida; false en otro caso
     */
    boolean isConnected();

    /**
     * Ejecuta una sentencia SQL (modo texto) y devuelve las filas resultantes.
     *
     * La forma de ejecución depende de la implementación:
     * - Simulada: parseo mínimo y operaciones sobre estructuras en memoria.
     * - JDBC: ejecución mediante Statement/PreparedStatement.
     * - Cliente nativo: envío por socket del mensaje Query.
     *
     * @param sql sentencia SQL en texto
     * @return lista de filas, cada fila representada como Map<columna, valor>
     * @throws IllegalStateException si la conexión no está abierta
     */
    List<Map<String,Object>> execute(String sql);
}


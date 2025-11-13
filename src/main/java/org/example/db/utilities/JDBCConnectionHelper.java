package org.example.db.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad para gestionar conexiones JDBC de forma centralizada.
 * 
 * Esta clase proporciona métodos para crear conexiones JDBC con reintentos
 * automáticos y manejo de errores consistente.
 */
public class JDBCConnectionHelper {
    
    /**
     * Crea una conexión JDBC con la URL, usuario y contraseña proporcionados.
     * 
     * @param jdbcUrl URL de conexión JDBC
     * @param user Usuario de la base de datos
     * @param password Contraseña del usuario
     * @return Conexión JDBC establecida
     * @throws SQLException Si no se puede establecer la conexión
     */
    public static Connection createConnection(String jdbcUrl, String user, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }
    
    /**
     * Crea una conexión JDBC con reintentos automáticos.
     * 
     * @param jdbcUrl URL de conexión JDBC
     * @param user Usuario de la base de datos
     * @param password Contraseña del usuario
     * @param maxRetries Número máximo de reintentos
     * @param retryDelayMs Milisegundos de espera entre reintentos
     * @return Conexión JDBC establecida
     * @throws SQLException Si no se puede establecer la conexión tras todos los reintentos
     */
    public static Connection createConnectionWithRetry(String jdbcUrl, String user, String password, 
                                                       int maxRetries, long retryDelayMs) throws SQLException {
        SQLException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return DriverManager.getConnection(jdbcUrl, user, password);
            } catch (SQLException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    System.out.println("[JDBCConnectionHelper] Connection attempt " + attempt + " failed. Retrying in " + retryDelayMs + "ms...");
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Connection interrupted", ie);
                    }
                }
            }
        }
        
        throw new SQLException("Failed to connect after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Cierra una conexión de forma segura.
     * 
     * @param connection Conexión a cerrar
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[JDBCConnectionHelper] Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica si una conexión está activa.
     * 
     * @param connection Conexión a verificar
     * @return true si la conexión está activa, false en caso contrario
     */
    public static boolean isConnectionValid(Connection connection) {
        if (connection == null) {
            return false;
        }
        
        try {
            return !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     */
    private JDBCConnectionHelper() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}

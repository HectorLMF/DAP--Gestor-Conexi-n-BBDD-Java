package org.example.db.utilities;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utilidad para cargar y gestionar configuración de conexiones a bases de datos.
 * 
 * Esta clase proporciona métodos para obtener valores de configuración desde:
 * 1. Variables de entorno
 * 2. Archivo de propiedades (db.properties)
 * 3. Valores por defecto
 */
public class ConnectionConfig {
    
    private static final Properties FILE_CONFIG = loadConfigFile();
    
    /**
     * Carga el archivo de configuración db.properties si existe.
     * 
     * @return Properties con la configuración cargada, o Properties vacío si no existe
     */
    private static Properties loadConfigFile() {
        Properties props = new Properties();
        try (InputStream is = ConnectionConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
                System.out.println("[ConnectionConfig] Loaded db.properties");
            }
        } catch (Exception e) {
            System.err.println("[ConnectionConfig] Could not load db.properties: " + e.getMessage());
        }
        return props;
    }
    
    /**
     * Obtiene un valor de configuración con prioridad: ENV > File > Default
     * 
     * @param envKey Clave de variable de entorno
     * @param fileKey Clave en archivo de propiedades
     * @param defaultValue Valor por defecto si no se encuentra
     * @return El valor configurado
     */
    public static String getConfigValue(String envKey, String fileKey, String defaultValue) {
        // 1. Primero intenta variable de entorno
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) {
            return envVal;
        }
        
        // 2. Luego archivo de propiedades
        String fileVal = FILE_CONFIG.getProperty(fileKey);
        if (fileVal != null && !fileVal.isEmpty()) {
            return fileVal;
        }
        
        // 3. Por último, valor por defecto
        return defaultValue;
    }
    
    /**
     * Obtiene un valor de configuración como entero.
     * 
     * @param envKey Clave de variable de entorno
     * @param fileKey Clave en archivo de propiedades
     * @param defaultValue Valor por defecto
     * @return El valor como entero
     */
    public static int getConfigValueAsInt(String envKey, String fileKey, int defaultValue) {
        String value = getConfigValue(envKey, fileKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     */
    private ConnectionConfig() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}

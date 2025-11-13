package org.example.db.utilities;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilidad para crear respuestas estandarizadas de operaciones DML.
 * 
 * Esta clase proporciona métodos para generar respuestas consistentes
 * cuando se ejecutan operaciones INSERT, UPDATE, DELETE, CREATE, etc.
 */
public class QueryResponseBuilder {
    
    /**
     * Crea una respuesta de éxito para operaciones DML/DDL.
     * 
     * @param affectedRows Número de filas afectadas
     * @return Map con información de éxito
     */
    public static Map<String, Object> createSuccessResponse(int affectedRows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("affected_rows", affectedRows);
        result.put("message", "Statement executed successfully");
        return result;
    }
    
    /**
     * Crea una respuesta de éxito con mensaje personalizado.
     * 
     * @param affectedRows Número de filas afectadas
     * @param message Mensaje personalizado
     * @return Map con información de éxito
     */
    public static Map<String, Object> createSuccessResponse(int affectedRows, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("affected_rows", affectedRows);
        result.put("message", message);
        return result;
    }
    
    /**
     * Crea una respuesta de error.
     * 
     * @param errorMessage Mensaje de error
     * @return Map con información de error
     */
    public static Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "error");
        result.put("message", errorMessage);
        return result;
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     */
    private QueryResponseBuilder() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}

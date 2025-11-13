package org.example.db.utilities;

/**
 * Utilidad para limpiar y normalizar consultas SQL.
 * 
 * Esta clase proporciona métodos estáticos para preparar consultas SQL
 * antes de su ejecución, eliminando saltos de línea, espacios múltiples
 * y otros caracteres que puedan causar problemas de sintaxis.
 */
public class SQLCleaner {
    
    /**
     * Limpia y normaliza una consulta SQL.
     * 
     * Operaciones realizadas:
     * - Reemplaza todos los saltos de línea (\r\n, \r, \n) por espacios
     * - Reemplaza múltiples espacios consecutivos por un solo espacio
     * - Elimina espacios al inicio y final (trim)
     * 
     * @param sql La consulta SQL a limpiar
     * @return La consulta SQL limpia y normalizada, o cadena vacía si el input es null
     */
    public static String cleanSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        
        // Reemplazar saltos de línea por espacios
        sql = sql.replaceAll("\\r\\n|\\r|\\n", " ");
        
        // Reemplazar múltiples espacios por uno solo
        sql = sql.replaceAll("\\s+", " ");
        
        // Trim (eliminar espacios al inicio y final)
        sql = sql.trim();
        
        return sql;
    }
    
    /**
     * Verifica si una consulta SQL es de tipo SELECT (lectura).
     * 
     * @param sql La consulta SQL a verificar
     * @return true si es una consulta de lectura, false en caso contrario
     */
    public static boolean isSelectQuery(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        
        String sqlUpper = sql.trim().toUpperCase();
        return sqlUpper.startsWith("SELECT") || 
               sqlUpper.startsWith("SHOW") || 
               sqlUpper.startsWith("DESCRIBE") || 
               sqlUpper.startsWith("DESC") ||
               sqlUpper.startsWith("EXPLAIN") ||
               sqlUpper.startsWith("TABLE") || // PostgreSQL: TABLE nombre_tabla
               sqlUpper.startsWith("WITH");    // CTEs que pueden devolver resultados
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     * Esta es una clase de utilidades con métodos estáticos.
     */
    private SQLCleaner() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}

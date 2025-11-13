package org.example.db.utilities;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad para convertir ResultSet de JDBC en estructuras de datos genéricas.
 * 
 * Esta clase proporciona métodos para transformar ResultSet en listas de mapas,
 * facilitando el trabajo con resultados de consultas SQL de forma independiente
 * del tipo de base de datos.
 */
public class ResultSetConverter {
    
    /**
     * Convierte un ResultSet en una lista de mapas.
     * Cada fila del ResultSet se convierte en un Map<String, Object> donde
     * la clave es el nombre de la columna y el valor es el dato.
     * 
     * @param rs ResultSet a convertir (debe estar posicionado antes de la primera fila)
     * @return Lista de mapas con los datos
     * @throws SQLException Si hay error al leer el ResultSet
     */
    public static List<Map<String, Object>> convertToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            rows.add(row);
        }
        
        return rows;
    }
    
    /**
     * Convierte un ResultSet en una lista de mapas con nombres de columna en minúsculas.
     * 
     * @param rs ResultSet a convertir
     * @return Lista de mapas con nombres de columna en minúsculas
     * @throws SQLException Si hay error al leer el ResultSet
     */
    public static List<Map<String, Object>> convertToListLowerCase(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
            }
            rows.add(row);
        }
        
        return rows;
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     */
    private ResultSetConverter() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}

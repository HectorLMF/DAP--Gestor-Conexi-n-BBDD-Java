package org.example.db.mysql;

import org.example.db.DBConnection;
import java.util.List;
import java.util.Map;

/**
 * Conexión simulada para el proveedor MySQL.
 *
 * Esta clase implementa la interfaz {@link org.example.db.DBConnection} y actúa como
 * una representación simulada de una conexión a una base de datos MySQL. No usa
 * JDBC ni librerías externas; está pensada para imitar el comportamiento de una
 * conexión real a nivel de API (abrir/cerrar/comprobar estado/ejecutar SQL) sin
 * realizar operaciones de red.
 *
 * El diseño permite que las llamadas a ejecución deleguen internamente a la lógica
 * simulada (actualmente marcadas como TODO). Los métodos lanzan
 * UnsupportedOperationException hasta que se implemente la simulación.
 */
public class MySQLConnection implements DBConnection {
    /** Identificador lógico de la conexión; se prefiere el prefijo "mysql:". */
    private final String name;
    private boolean connected = false;
    // simple in-memory table 'users'
    private final List<java.util.Map<String,Object>> users = new java.util.ArrayList<>();
    private int nextId = 1;

    /**
     * Constructor.
     *
     * Construye una nueva conexión simulada con un nombre lógico. Se añade el
     * prefijo "mysql:" para distinguirla de otros proveedores.
     *
     * @param name Nombre lógico de la conexión (no nulo, recomendado corto y único)
     */
    public MySQLConnection(String name) {
        this.name = "mysql:" + name;
    }

    /**
     * Obtiene el nombre (identificador) de la conexión.
     *
     * Devuelve el identificador con prefijo "mysql:" que se asignó en el constructor.
     *
     * @return Cadena no nula con el nombre de la conexión.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Abre/establece la conexión.
     *
     * Este método debe cambiar el estado interno para reflejar que la conexión
     * está activa. En la versión actual está marcado como pendiente de implementar
     * y lanza UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException si la implementación está incompleta
     */
    @Override
    public void connect() {
        this.connected = true;
        // initialize with some default rows if empty
        if (users.isEmpty()) {
            java.util.Map<String,Object> r1 = new java.util.LinkedHashMap<>(); r1.put("id", nextId++); r1.put("name","Alice"); r1.put("email","alice@example.com"); users.add(r1);
            java.util.Map<String,Object> r2 = new java.util.LinkedHashMap<>(); r2.put("id", nextId++); r2.put("name","Bob"); r2.put("email","bob@example.com"); users.add(r2);
            java.util.Map<String,Object> r3 = new java.util.LinkedHashMap<>(); r3.put("id", nextId++); r3.put("name","Eve"); r3.put("email","eve@example.com"); users.add(r3);
        }
    }

    /**
     * Cierra la conexión.
     *
     * Debe liberar recursos simulados y actualizar el estado interno.
     * Actualmente no está implementado.
     *
     * @throws UnsupportedOperationException si la implementación está incompleta
     */
    @Override
    public void disconnect() {
        this.connected = false;
    }

    /**
     * Indica si la conexión está establecida (simulada).
     *
     * @return true si la conexión se considera activa; false en caso contrario.
     * @throws UnsupportedOperationException si la implementación está incompleta
     */
    @Override
    public boolean isConnected() {
        return this.connected;
    }

    /**
     * Ejecuta una sentencia SQL de forma simulada.
     *
     * La implementación deberá parsear/interpretar la cadena SQL de forma
     * simplificada y devolver una lista de filas representadas como mapas
     * columna->valor. Por ahora lanza UnsupportedOperationException.
     *
     * @param sql Sentencia SQL a ejecutar (texto plano)
     * @return Lista de filas (cada fila: Map nombreColumna->valor)
     * @throws UnsupportedOperationException si la implementación está incompleta
     */
    @Override
    public List<Map<String, Object>> execute(String sql) {
        if (!connected) throw new IllegalStateException("Connection not open");
        String s = sql.trim();
        // Very naive SQL parsing for demo purposes
        if (s.regionMatches(true,0,"SELECT",0,6)) {
            // SELECT * FROM users [WHERE id = N]
            if (s.toLowerCase().contains("where")) {
                int idx = s.toLowerCase().indexOf("where");
                String cond = s.substring(idx+5).trim();
                // expect id = N
                if (cond.toLowerCase().startsWith("id")) {
                    String[] parts = cond.split("=");
                    int id = Integer.parseInt(parts[1].replaceAll("[^0-9]",""));
                    List<Map<String,Object>> res = new java.util.ArrayList<>();
                    for (Map<String,Object> r: users) {
                        if (((Number)r.get("id")).intValue() == id) res.add(r);
                    }
                    return res;
                }
            }
            // return all
            return new java.util.ArrayList<>(users);
        } else if (s.regionMatches(true,0,"INSERT",0,6)) {
            // INSERT INTO users (name,email) VALUES ('X','Y')
            int vIdx = s.toLowerCase().indexOf("values");
            String vals = s.substring(vIdx+6).trim();
            // remove parentheses
            vals = vals.replaceAll("^\\s*\\(","").replaceAll("\\)\\s*$","");
            // split by comma not robust but OK here
            String[] parts = vals.split(",");
            String name = parts[0].replaceAll("[\\'()\"]","" ).trim();
            String email = parts[1].replaceAll("[\\'()\"]","" ).trim();
            java.util.Map<String,Object> row = new java.util.LinkedHashMap<>();
            row.put("id", nextId++);
            row.put("name", name);
            row.put("email", email);
            users.add(row);
            return java.util.Collections.emptyList();
        } else if (s.regionMatches(true,0,"DELETE",0,6)) {
            // DELETE FROM users WHERE id = N
            int idx = s.toLowerCase().indexOf("where");
            String cond = s.substring(idx+5).trim();
            String[] parts = cond.split("=");
            int id = Integer.parseInt(parts[1].replaceAll("[^0-9]",""));
            users.removeIf(r -> ((Number)r.get("id")).intValue() == id);
            return java.util.Collections.emptyList();
        }
        // default: no-op
        return java.util.Collections.emptyList();
    }
}

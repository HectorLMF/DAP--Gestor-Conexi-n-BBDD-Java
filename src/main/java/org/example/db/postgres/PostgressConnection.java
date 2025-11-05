package org.example.db.postgres;

import org.example.db.DBConnection;
import java.util.*;
import java.util.stream.Collectors;

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

    // Estado de la conexión
    private boolean connected = false;

    // Estructura de tablas en memoria: nombreTabla -> lista de filas (mapa columna->valor)
    private final Map<String, List<Map<String, Object>>> tables = new HashMap<>();

    /**
     * @brief Constructor.
     *
     * @param name Nombre lógico de la conexión
     */
    public PostgressConnection(String name) {
        this.name = "postgres:" + name;

        // Inicializar una base de datos estática de ejemplo (tabla `users`)
        List<Map<String, Object>> users = new ArrayList<>();
        Map<String, Object> u1 = new HashMap<>();
        u1.put("id", 1);
        u1.put("name", "Alice");
        u1.put("email", "alice@example.com");
        users.add(u1);

        Map<String, Object> u2 = new HashMap<>();
        u2.put("id", 2);
        u2.put("name", "Bob");
        u2.put("email", "bob@example.com");
        users.add(u2);

        tables.put("users", users);

        // Otra tabla de ejemplo: products
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", 1);
        p1.put("name", "Pen");
        p1.put("price", 1.5);
        products.add(p1);
        tables.put("products", products);
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
     */
    @Override
    public void connect() {
        this.connected = true;
    }

    /**
     * @brief Cierra la conexión (simulada).
     */
    @Override
    public void disconnect() {
        this.connected = false;
    }

    /**
     * @brief Indica si la conexión está establecida.
     *
     * @return true si la conexión se considera activa; false en caso contrario
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * @brief Ejecuta una sentencia SQL de forma simulada y devuelve filas.
     *
     * Soporta formas simples de:
     * - SELECT * FROM table [WHERE col = value]
     * - INSERT INTO table (col1,col2) VALUES (val1,val2)
     * - DELETE FROM table WHERE col = value
     *
     * @param sql Sentencia SQL (texto)
     * @return Lista de filas (cada fila: Map columna->valor)
     */
    @Override
    public List<Map<String, Object>> execute(String sql) {
        if (!isConnected()) {
            throw new IllegalStateException("Connection is not open: " + getName());
        }
        if (sql == null) return Collections.emptyList();

        String trimmed = sql.trim();
        // quitar ; final si existe
        if (trimmed.endsWith(";")) trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        try {
            if (upper.startsWith("SELECT")) {
                return handleSelect(trimmed, upper);
            } else if (upper.startsWith("INSERT")) {
                handleInsert(trimmed, upper);
                return Collections.emptyList();
            } else if (upper.startsWith("DELETE")) {
                handleDelete(trimmed, upper);
                return Collections.emptyList();
            } else {
                // Sentencia no soportada -> devolver vacío
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            // En una implementación real lanzaríamos excepciones más específicas.
            // Para la simulación, retornamos vacío en caso de parseo/ejecución errónea.
            return Collections.emptyList();
        }
    }

    // ----- Métodos auxiliares simples para parseo/ejecución -----
    private List<Map<String, Object>> handleSelect(String original, String upper) {
        // Soportamos: SELECT * FROM table [WHERE col = value]
        int idxFrom = upper.indexOf(" FROM ");
        if (idxFrom == -1) return Collections.emptyList();
        int startTable = idxFrom + 6;
        int idxWhere = upper.indexOf(" WHERE ", startTable);
        int endTable = (idxWhere == -1) ? original.length() : idxWhere;
        String table = original.substring(startTable, endTable).trim();
        table = stripQuotes(table).toLowerCase(Locale.ROOT);

        List<Map<String, Object>> rows = tables.get(table);
        if (rows == null) return Collections.emptyList();

        if (idxWhere == -1) {
            // devolver copia de filas para evitar modificar el original
            return rows.stream().map(r -> new HashMap<>(r)).collect(Collectors.toList());
        }

        String cond = original.substring(idxWhere + 7).trim();
        // soportar sólo igualdad simple: col = value
        String[] parts = cond.split("=", 2);
        if (parts.length != 2) return Collections.emptyList();
        String col = parts[0].trim();
        String rawVal = parts[1].trim();
        Object value = parseValue(rawVal);

        return rows.stream()
                .filter(r -> {
                    Object v = r.get(col);
                    return Objects.equals(v, value) || (v != null && value != null && v.toString().equals(value.toString()));
                })
                .map(r -> new HashMap<>(r))
                .collect(Collectors.toList());
    }

    private void handleInsert(String original, String upper) {
        // Soportamos: INSERT INTO table (col1,col2) VALUES (val1,val2)
        int idxInto = upper.indexOf(" INTO ");
        if (idxInto == -1) return;
        int startTable = idxInto + 6;
        int idxParenCols = original.indexOf('(', startTable);
        String table;
        String colsPart = null;
        String valsPart = null;
        if (idxParenCols != -1) {
            table = original.substring(startTable, idxParenCols).trim();
            int endParenCols = original.indexOf(')', idxParenCols);
            if (endParenCols == -1) return;
            colsPart = original.substring(idxParenCols + 1, endParenCols).trim();
            int idxValues = upper.indexOf(" VALUES ", endParenCols);
            if (idxValues == -1) return;
            int idxValsParen = original.indexOf('(', idxValues);
            int endValsParen = original.indexOf(')', idxValsParen);
            if (idxValsParen == -1 || endValsParen == -1) return;
            valsPart = original.substring(idxValsParen + 1, endValsParen).trim();
        } else {
            // forma no soportada en la simulación
            return;
        }

        table = stripQuotes(table).toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = tables.computeIfAbsent(table, k -> new ArrayList<>());

        String[] cols = Arrays.stream(colsPart.split(",")).map(String::trim).toArray(String[]::new);
        List<String> vals = Arrays.stream(valsPart.split(",")).map(String::trim).collect(Collectors.toList());
        if (cols.length != vals.size()) return;

        Map<String, Object> newRow = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            String col = cols[i];
            Object value = parseValue(vals.get(i));
            newRow.put(col, value);
        }

        // Si existe columna id y no se ha proporcionado, generar id incremental
        if (!newRow.containsKey("id")) {
            int nextId = rows.stream()
                    .map(r -> r.get("id"))
                    .filter(Objects::nonNull)
                    .mapToInt(o -> {
                        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
                    }).max().orElse(0) + 1;
            newRow.put("id", nextId);
        }

        rows.add(newRow);
    }

    private void handleDelete(String original, String upper) {
        // Soportamos: DELETE FROM table WHERE col = value
        int idxFrom = upper.indexOf(" FROM ");
        if (idxFrom == -1) return;
        int startTable = idxFrom + 6;
        int idxWhere = upper.indexOf(" WHERE ", startTable);
        int endTable = (idxWhere == -1) ? original.length() : idxWhere;
        String table = original.substring(startTable, endTable).trim();
        table = stripQuotes(table).toLowerCase(Locale.ROOT);

        List<Map<String, Object>> rows = tables.get(table);
        if (rows == null) return;

        if (idxWhere == -1) {
            // delete all
            rows.clear();
            return;
        }

        String cond = original.substring(idxWhere + 7).trim();
        String[] parts = cond.split("=", 2);
        if (parts.length != 2) return;
        String col = parts[0].trim();
        String rawVal = parts[1].trim();
        Object value = parseValue(rawVal);

        // eliminar filas que cumplan la condición
        rows.removeIf(r -> {
            Object v = r.get(col);
            return Objects.equals(v, value) || (v != null && value != null && v.toString().equals(value.toString()));
        });
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static Object parseValue(String raw) {
        String v = stripQuotes(raw);
        if (v == null) return null;
        // intentar entero
        try {
            if (!v.contains(".")) {
                return Integer.parseInt(v);
            }
        } catch (NumberFormatException ignored) {}
        // intentar double
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ignored) {}
        return v;
    }
}

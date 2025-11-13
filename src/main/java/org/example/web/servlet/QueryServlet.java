package org.example.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.db.DBClient;
import org.example.db.DBFactory;
import org.example.db.postgres.PostgressFactory;
import org.example.db.mysql.MySQLFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @file QueryServlet.java
 * @brief Servlet que recibe POST /query con JSON {"db":"...","sql":"..."}
 *        y devuelve JSON con las filas resultantes.
 */
public class QueryServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = req.getReader().lines().collect(Collectors.joining("\n"));
        String db = extractJsonStringRegex(body, "db");
        String sql = extractJsonStringRegex(body, "sql");

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        if (db == null || sql == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.print("{\"error\":\"invalid request\"}");
            }
            return;
        }

        DBFactory factory = "mysql".equalsIgnoreCase(db) ? new MySQLFactory() : new PostgressFactory();
        DBClient client = new DBClient(factory, "web-servlet");
        try {
            client.connect();
            List<Map<String,Object>> rows = client.executeText(sql);
            String json = toJson(rows);
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = resp.getWriter()) { out.print(json); }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.print("{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}");
            }
        } finally {
            try { client.disconnect(); } catch (Exception ignored) {}
        }
    }

    private static String extractJsonStringRegex(String body, String key) {
        try {
            Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
            Matcher m = p.matcher(body);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return null;
    }

    private static String toJson(List<Map<String,Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstRow = true;
        for (Map<String,Object> r : rows) {
            if (!firstRow) sb.append(',');
            firstRow = false;
            sb.append('{');
            boolean firstCol = true;
            for (Map.Entry<String,Object> e : r.entrySet()) {
                if (!firstCol) sb.append(',');
                firstCol = false;
                sb.append('"').append(escape(e.getKey())).append('"').append(':');
                Object v = e.getValue();
                if (v == null) sb.append("null"); else sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}

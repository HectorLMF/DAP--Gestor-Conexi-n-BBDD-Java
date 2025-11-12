package org.example.web.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.db.DBClient;
import org.example.db.DBFactory;
import org.example.db.postgres.PostgressFactory;
import org.example.db.mysql.MySQLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @file SimpleWebServer.java
 * @brief Servidor HTTP mínimo que expone el endpoint /query para el middleware.
 *
 * Este servidor es una implementación ligera basada en {@code com.sun.net.httpserver.HttpServer}
 * y sirve como ejemplo de cómo exponer una API REST muy simple que actúe como
 * middleware entre clientes web y las fábricas/conexiones de bases de datos.
 *
 * Endpoint principal:
 * - POST /query
 *   Body JSON: { "db":"postgres|mysql", "sql":"SELECT ..." }
 *   Responde JSON con la lista de filas devueltas por la consulta.
 *
 * Consideraciones para la versión MySQL:
 * - El controlador ya soporta la cadena "mysql" y usa {@link MySQLFactory}.
 * - Asegúrate de que la implementación MySQL devuelva filas en el mismo formato
 *   List<Map<String,Object>> para compatibilidad.
 *
 * Seguridad / mejoras futuras:
 * - Validar y sanitizar SQL recibida (actualmente se pasa tal cual al proveedor).
 * - Añadir autenticación/autorización, límites de tiempo y control de concurrencia.
 */
public class SimpleWebServer {
  private HttpServer server;

  public void start(int port) throws IOException {
    // Bind explicitly to all IPv4 interfaces to avoid environments where the
    // default binds only to IPv6 loopback (::1) and prevents IPv4 clients from
    // connecting (PowerShell/curl to 127.0.0.1). Using 0.0.0.0 ensures IPv4
    // connectivity on the host.
    server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
    server.createContext("/query", new QueryHandler());
    server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
    server.start();
    System.out.println("SimpleWebServer started on port " + port);
  }

  public void stop() {
    if (server != null) server.stop(0);
  }

  static class QueryHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      InputStream in = exchange.getRequestBody();
      String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      System.out.println("[web] Received body: " + body);
      // very simple JSON parsing: expect {"db":"postgres","sql":"..."}
      // try regex-based extraction which is more tolerant to spacing/escaping
      String db = extractJsonStringRegex(body, "db");
      String sql = extractJsonStringRegex(body, "sql");
      if (db == null || sql == null) {
        String msg = "{\"error\":\"invalid request\",\"body\":\"" + escape(body) + "\"}";
        byte[] resp = msg.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type","application/json");
        exchange.sendResponseHeaders(400, resp.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
        return;
      }

      DBFactory factory = "mysql".equalsIgnoreCase(db) ? new MySQLFactory() : new PostgressFactory();
      DBClient client = new DBClient(factory, "mysql");

      try {
        client.connect();
        List<Map<String,Object>> rows = client.executeText(sql);
        String json = toJson(rows);
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type","application/json");
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
      } catch (Exception e) {
        String msg = "{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}";
        byte[] resp = msg.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type","application/json");
        exchange.sendResponseHeaders(500, resp.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
      } finally {
        client.disconnect();
      }
    }

    private static String extractJsonString(String body, String key) {
      String look = "\"" + key + "\"";
      int i = body.indexOf(look);
      if (i < 0) return null;
      int colon = body.indexOf(':', i);
      if (colon < 0) return null;
      int firstQuote = body.indexOf('"', colon+1);
      if (firstQuote < 0) return null;
      int secondQuote = body.indexOf('"', firstQuote+1);
      if (secondQuote < 0) return null;
      return body.substring(firstQuote+1, secondQuote);
    }

    private static String extractJsonStringRegex(String body, String key) {
      try {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(body);
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
}
package org.example.db.postgres;

import java.util.List;
import java.util.Map;

public class TestNative {
    public static void main(String[] args) {
        PostgressConnection conn = new PostgressConnection("test");
        System.out.println("=== TEST CONEXION NATIVA SCRAM-SHA-256 ===");
        System.out.println("Conectando a Postgres...");
        conn.connect();
        System.out.println("¿Conectado? " + conn.isConnected());
        System.out.println("¿Nativo? " + conn.isNativeConnected());

        if (conn.isNativeConnected()) {
            System.out.println("\n✓ CONEXION NATIVA EXITOSA!");
            System.out.println("\nEjecutando SELECT * FROM users:");
            List<Map<String, Object>> rows = conn.execute("SELECT * FROM users");
            for (Map<String, Object> row : rows) {
                System.out.println("  " + row);
            }

            System.out.println("\nEjecutando INSERT:");
            conn.execute("INSERT INTO users (name, email) VALUES ('Charlie', 'charlie@test.com')");

            System.out.println("\nVerificando INSERT:");
            rows = conn.execute("SELECT * FROM users ORDER BY id");
            for (Map<String, Object> row : rows) {
                System.out.println("  " + row);
            }
        } else {
            System.out.println("\n✗ Falló la conexión nativa, usando DB simulada");
        }

        conn.disconnect();
        System.out.println("\n=== FIN DEL TEST ===");
    }
}


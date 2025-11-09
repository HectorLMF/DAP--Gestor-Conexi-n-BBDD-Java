package org.example.db.postgres;

import java.util.List;
import java.util.Map;

/**
 * Demo sencillo para probar PostgressConnection.
 */
public class PostgresDemo {
    public static void main(String[] args) {
        // Usar la fábrica concreta y el cliente genérico
        org.example.db.DBFactory factory = new org.example.db.postgres.PostgressFactory();
        org.example.db.DBClient client = new org.example.db.DBClient(factory, "demo");
        client.connect();
        System.out.println("Name: " + client.getConnection().getName());
        System.out.println("Connected: " + client.getConnection().isConnected());
        System.out.println("Native connected: " + ((org.example.db.postgres.PostgressConnection)client.getConnection()).isNativeConnected());

        System.out.println("-- SELECT * FROM users --");
        List<Map<String, Object>> rows = client.executeText("SELECT * FROM users");
        printRows(rows);

        System.out.println("-- INSERT new user --");
        client.executeText("INSERT INTO users (name,email) VALUES ('Charlie','charlie@example.com')");
        rows = client.executeText("SELECT * FROM users");
        printRows(rows);

        System.out.println("-- SELECT WHERE id = 3 --");
        rows = client.executeText("SELECT * FROM users WHERE id = 3");
        printRows(rows);

        System.out.println("-- DELETE id = 2 --");
        client.executeText("DELETE FROM users WHERE id = 2");
        rows = client.executeText("SELECT * FROM users");
        printRows(rows);

        client.disconnect();
        System.out.println("Connected after disconnect: " + client.getConnection().isConnected());
    }

    private static void printRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("(no rows)");
            return;
        }
        for (Map<String, Object> r : rows) {
            System.out.println(r);
        }
    }
}

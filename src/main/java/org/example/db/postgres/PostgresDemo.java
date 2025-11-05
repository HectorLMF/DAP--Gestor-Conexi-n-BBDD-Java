dad package org.example.db.postgres;

import java.util.List;
import java.util.Map;

/**
 * Demo sencillo para probar PostgressConnection.
 */
public class PostgresDemo {
    public static void main(String[] args) {
        PostgressConnection conn = new PostgressConnection("demo");
        System.out.println("Name: " + conn.getName());
        conn.connect();
        System.out.println("Connected: " + conn.isConnected());

        System.out.println("-- SELECT * FROM users --");
        List<Map<String, Object>> rows = conn.execute("SELECT * FROM users");
        printRows(rows);

        System.out.println("-- INSERT new user --");
        conn.execute("INSERT INTO users (name,email) VALUES ('Charlie','charlie@example.com')");
        rows = conn.execute("SELECT * FROM users");
        printRows(rows);

        System.out.println("-- SELECT WHERE id = 3 --");
        rows = conn.execute("SELECT * FROM users WHERE id = 3");
        printRows(rows);

        System.out.println("-- DELETE id = 2 --");
        conn.execute("DELETE FROM users WHERE id = 2");
        rows = conn.execute("SELECT * FROM users");
        printRows(rows);

        conn.disconnect();
        System.out.println("Connected after disconnect: " + conn.isConnected());
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


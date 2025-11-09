package org.example;

import org.example.web.WebServer;

/**
 * @file Main.java
 * @brief Punto de entrada (launcher) de la aplicación middleware.
 *
 * Este launcher arranca el servidor web que actúa como middleware entre clientes
 * HTTP y las bases de datos. Mantiene la JVM en ejecución hasta que el proceso
 * sea detenido (Ctrl+C o señal de terminación).
 *
 * Para desarrollo, usar `mvn -Dexec.mainClass=org.example.Main exec:java` o
 * el script `scripts/start.ps1` que automatiza el arranque (opcionalmente
 * arrancando una instancia de Postgres en Docker).
 */
public class Main {
    /**
     * Arranca el servidor web y mantiene la aplicación en primer plano.
     *
     * @param args argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        System.out.println("DAP-Gestor-BBDD - servidor esqueleto");
        WebServer server = new WebServer();
        // START: arranque del servidor (esqueleto)
        try {
            server.start(8000);
            // Mantener la JVM en marcha para que el servidor HTTP atienda peticiones.
            System.out.println("Press Ctrl+C to stop the server...");
            Thread.currentThread().join();
        } catch (UnsupportedOperationException ex) {
            System.out.println("WebServer aún no implementado: " + ex.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Main interrupted, exiting");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
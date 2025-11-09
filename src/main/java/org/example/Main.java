package org.example;

import org.example.web.WebServer;

/**
 * Punto de entrada del proyecto.
 *
 * Arranca el servidor web esqueleto para la interfaz del gestor. En fases
 * posteriores arrancará la configuración de fábricas, la gestión de conexiones
 * y cualquier subsistema adicional.
 */
public class Main {
    /**
     * Punto de entrada de la aplicación.
     *
     * Intenta arrancar el servidor web en el puerto 8000. Actualmente
     * WebServer.start lanza UnsupportedOperationException porque es un stub,
     * por lo que se captura la excepción para informar en consola.
     *
     * @param args Argumentos de línea de comandos (no usados actualmente)
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
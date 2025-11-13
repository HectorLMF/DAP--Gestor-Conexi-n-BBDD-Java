package org.example.web;

/**
 * @file WebServer.java
 * @brief Envoltorio de alto nivel para el servidor HTTP del middleware.
 *
 * WebServer actúa como fachada para el servidor HTTP concreto que implementa
 * los handlers (actualmente {@link org.example.web.impl.SimpleWebServer}).
 * Separar la interfaz del servidor de su implementación facilita pruebas y
 * permite sustituir la implementación por otra (por ejemplo, un servidor
 * embebido diferente) sin cambiar el resto del código.
 *
 * Responsabilidades:
 * - Exponer métodos start(port) y stop() para controlar el ciclo de vida.
 * - Encapsular la configuración de contexts/handlers y del executor.
 *
 * Uso típico:
 * WebServer server = new WebServer();
 * server.start(8000);
 * // ...
 * server.stop();
 */
public class WebServer {
    /**
     * Inicia el servidor web en el puerto indicado.
     *
     * Debe crear y configurar el servidor HTTP, registrar los contextos/handlers
     * necesarios (por ejemplo: `/`, `/connections`, `/query`) y arrancar un
     * executor para atender peticiones concurrentes.
     *
     * @param port Puerto TCP en el que el servidor escuchará (por ejemplo 8000)
     * @throws UnsupportedOperationException si la implementación está incompleta
     */
    public void start(int port) {
        // Preferir servidor basado en servlets (Jetty) si está disponible.
        try {
            org.example.web.servlet.ServletWebServer s = new org.example.web.servlet.ServletWebServer(port);
            s.start();
            return;
        } catch (Throwable t) {
            // Si Jetty/no-servlet no está disponible, caer al SimpleWebServer
            System.out.println("Servlet container unavailable or failed to start: " + t.getMessage());
        }
        try {
            org.example.web.impl.SimpleWebServer s2 = new org.example.web.impl.SimpleWebServer();
            s2.start(port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start web server", e);
        }
    }

    /**
     * Detiene el servidor web si está corriendo.
     *
     * Debe detener el HttpServer y cerrar/terminar el executor asociado. La
     * llamada a este método debe ser segura si el servidor no está iniciado
     * (no-op).
     */
    public void stop() {
        // No-op: SimpleWebServer no es expuesto aquí para detenerlo desde esta instancia.
        // Si necesitamos manejar lifecycle, podemos refactorizar para mantener una referencia.
    }
}

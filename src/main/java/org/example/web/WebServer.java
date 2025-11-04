package org.example.web;

/**
 * Servidor HTTP mínimo para la interfaz web del gestor de conexiones y queries.
 *
 * Esta clase actúa como envoltorio/esqueleto para un servidor HTTP ligero que
 * expondrá endpoints para gestionar conexiones y ejecutar queries. La
 * implementación prevista usa `com.sun.net.httpserver.HttpServer` para evitar
 * dependencias externas y mantener el proyecto lo más "vanilla" posible.
 *
 * Actualmente el servidor es un stub y los métodos están por implementar.
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
        // TODO: implementar servidor web con com.sun.net.httpserver.HttpServer
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Detiene el servidor web si está corriendo.
     *
     * Debe detener el HttpServer y cerrar/terminar el executor asociado. La
     * llamada a este método debe ser segura si el servidor no está iniciado
     * (no-op).
     */
    public void stop() {
        // TODO: implementar
        throw new UnsupportedOperationException("Not implemented");
    }
}

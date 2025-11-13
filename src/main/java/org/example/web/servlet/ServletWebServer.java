package org.example.web.servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

/**
 * @file ServletWebServer.java
 * @brief Servidor embebido basado en Jetty que registra servlets y sirve
 *        recursos estáticos desde `src/main/resources/static`.
 *
 * Usa Jetty 11 (Jakarta Servlet API). Registra:
 * - {@link QueryServlet} en /query
 * - {@link DefaultServlet} para servir contenido estático en /
 */
public class ServletWebServer {
    private final Server server;

    public ServletWebServer(int port) {
        server = new Server(port);
    }

    public void start() throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        // Resource base: preferir recursos en classpath (/static) para funcionamiento
        // cuando la app está empaquetada; en desarrollo, se hace fallback al
        // directorio src/main/resources/static.
        try {
            Resource cp = Resource.newClassPathResource("/static");
            if (cp != null && cp.exists()) {
                context.setBaseResource(cp);
            } else {
                String resourceBase = System.getProperty("user.dir") + "/src/main/resources/static";
                context.setResourceBase(resourceBase);
            }
        } catch (Exception e) {
            String resourceBase = System.getProperty("user.dir") + "/src/main/resources/static";
            context.setResourceBase(resourceBase);
        }

        // Ensure index.html is treated as welcome file for '/'
        context.setWelcomeFiles(new String[]{"index.html"});

        // Registrar el servlet de consulta en /query
        context.addServlet(new ServletHolder(new QueryServlet()), "/query");

        // Registrar DefaultServlet para servir archivos estáticos
    ServletHolder defaultHolder = new ServletHolder("default", DefaultServlet.class);
    defaultHolder.setInitParameter("dirAllowed", "true");
    // Map to /* so the DefaultServlet handles static resources and welcome files
    context.addServlet(defaultHolder, "/*");

        server.setHandler(context);
        server.start();
        System.out.println("ServletWebServer started on port " + server.getURI().getPort());
    }

    public void stop() throws Exception {
        server.stop();
    }
}

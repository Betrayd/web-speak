package net.betrayd.webspeak.relay;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;

import net.betrayd.webspeak.relay.Servlets.ClientServlet;
import net.betrayd.webspeak.relay.Servlets.ServerAddPlayerServlet;
import net.betrayd.webspeak.relay.Servlets.ServerStartServlet;

public class WebSpeakRelay {

    public static final Map<String, ServerConnection> servers = new HashMap<>(); 

    public static void main(String[] args) {

    }

    private static Server jettyServer;
    private static int port = -1;

    public static void start() throws Exception {
        if (jettyServer != null) {
            throw new IllegalStateException("Server has already started.");
        }

        var server = jettyServer = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        JettyWebSocketServletContainerInitializer.configure(context, null);

        ServletHolder wsHostHolder = new ServletHolder("host", new ServerStartServlet());
        context.addServlet(wsHostHolder, "/host");
        ServletHolder wsConnectionHolder = new ServletHolder("addplayer", new ServerAddPlayerServlet());
        context.addServlet(wsConnectionHolder, "/addplayer");
        ServletHolder wsClientHolder = new ServletHolder("connect", new ClientServlet());
        context.addServlet(wsClientHolder, "/connect");

        server.start();
        jettyServer = server;
    }
}

package net.betrayd.webspeak.relay.Servlets;

import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;

import net.betrayd.webspeak.relay.ServerPlayerConnection;

public class ServerAddPlayerServlet extends JettyWebSocketServlet {

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        // TODO: can we do player validation here?
        factory.setCreator((req, resp) -> {
            return new ServerPlayerConnection();
        });
    }
    
}
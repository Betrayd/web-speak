package net.betrayd.webspeak.impl.jetty;

import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;

import net.betrayd.webspeak.WebSpeakServer;

public class PlayerWSConnectionServlet extends JettyWebSocketServlet {

    private final WebSpeakServer server;

    public PlayerWSConnectionServlet(WebSpeakServer server) {
        this.server = server;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        // TODO: can we do player validation here?
        factory.setCreator((req, resp) -> {
            return new PlayerWSConnection(server);
        });
    }
    
}

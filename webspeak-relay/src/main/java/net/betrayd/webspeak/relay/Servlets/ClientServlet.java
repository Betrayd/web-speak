package net.betrayd.webspeak.relay.Servlets;

import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;

import net.betrayd.webspeak.relay.ClientPlayerConnection;

public class ClientServlet extends JettyWebSocketServlet {

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        // TODO: can we do player validation here?
        factory.setCreator((req, resp) -> {
            //don't need a check here we already made sure that the path contained a / to get here
            String server = req.getRequestPath().split("/")[1];
            return new ClientPlayerConnection(server);
        });
    }
    
}

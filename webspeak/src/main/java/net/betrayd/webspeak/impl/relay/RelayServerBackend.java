package net.betrayd.webspeak.impl.relay;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.impl.ServerBackend;

//Don't bully me about the name. It's late I'm tired and just trying to follow the naming conventions
public class RelayServerBackend implements ServerBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelayServerBackend.class);

    private final WebSpeakServer webSpeakServer;
    private WebSocketClient webSocketClient;

    public RelayServerBackend(WebSpeakServer webSpeakServer)
    {
        this.webSpeakServer = webSpeakServer;
    }

    @Override
    public WebSpeakServer getServer() {
        return webSpeakServer;
    }

    @Override
    public void addPlayer(WebSpeakPlayer player)
    {
        //TODO: add correct connection URI fromn config here
        URI serverURI = URI.create("wss://domain.com/path");

        PlayerRelayConnection connection = new PlayerRelayConnection(webSpeakServer, player);

        try {
            webSocketClient.connect(connection, serverURI);
        } catch (IOException e) {
            LOGGER.error(null, e);
        }
    }

    @Override
    public void start(int port) throws Exception {
        if (webSocketClient != null) {
            throw new IllegalStateException("Server has already started.");
        }
        webSocketClient = new WebSocketClient();

        webSocketClient.start();
    }

    @Override
    public void stop() throws Exception {
        webSocketClient.stop();
    }

    @Override
    public boolean isRunning() {
        return webSocketClient != null && webSocketClient.isRunning();
    }

    @Override
    public int getPort() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPort'");
    }
    
}

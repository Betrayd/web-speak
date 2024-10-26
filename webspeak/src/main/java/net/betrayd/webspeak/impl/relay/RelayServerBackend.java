package net.betrayd.webspeak.impl.relay;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.websocket.api.Session;
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

    private String ourRelayConnectionID = null;

    public RelayServerBackend(WebSpeakServer webSpeakServer)
    {
        this.webSpeakServer = webSpeakServer;
    }

    /**
     * Initialize our base webSocket connection to let the server tell us what we are identified as
     */
    private void createBaseConnection()
    {
        //TODO: add correct base connection IP
        URI serverURI = URI.create("wss://domain.com/path");

        CoreRelay coreConnection = new CoreRelay(this);

        try {
            webSocketClient.connect(coreConnection, serverURI);
        } catch (IOException e) {
            LOGGER.error(null, e);
        }
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

        createBaseConnection();
    }

    @Override
    public void stop() throws Exception {
        ourRelayConnectionID = null;
        webSocketClient.stop();
    }

    @Override
    public boolean isRunning() {
        return webSocketClient != null && ourRelayConnectionID != null && webSocketClient.isRunning();
    }

    @Override
    public int getPort() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPort'");
    }
    
    /*
     * Class for our core connection
     */
    private static class CoreRelay implements Session.Listener.AutoDemanding
    {
        private final RelayServerBackend backend;

        private CoreRelay(RelayServerBackend backend)
        {
            this.backend = backend;
        }

        //nothing fancy here just take the string and use it to tell our class what the heck our ID is. No wrappers.
        @Override
        public void onWebSocketText(String message) {
            this.backend.ourRelayConnectionID = message;
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason)
        {
            try{
                backend.stop();
            }
            catch(Exception e)
            {
                LOGGER.error("Our relay just disconnected. That should not have happened", e);
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            // The WebSocket endpoint failed.
    
            // You may log the error.
            LOGGER.error("Failed to connect to the relay!", cause);
            try{
                backend.stop();
            }
            catch(Exception e)
            {
                LOGGER.error("Tried and failed to stop the server", e);
            }
        }

    }
}

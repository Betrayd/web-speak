package net.betrayd.webspeak.relay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;

public class ServerConnection {

    private String privateID = null;
    private String publicID = null;
    
    public final Map<String, LinkedConnection> connections = new HashMap<>();

    public boolean isKey(String testID)
    {
        if(testID.equals(privateID))
        {
            return true;
        }
        return false;
    }

    @OnWebSocketOpen
    public void onWebSocketOpen(Session session) {

        //generate the connecting server's private ID
        //Probably could be reverse engineered by getting a lot of keys by connecting a lot and then using those to figure out an internal state, but if it gets big enough people care to do that I consider it a win
        privateID = UUID.randomUUID().toString();
    }

    //we need to ask what the server what it's public identifier is. We will return with it's private identifier
    @OnWebSocketMessage
    public void onWebSocketMessage(Session session, String message){

        //I'm not using the whole packet system fot this. Use their message as their ID. Blank is keep alive
        if(message.length() > 0)
        {
            if(publicID == null || WebSpeakRelay.servers.containsKey(message))
            {
                return;
            }
            publicID = message;
            WebSpeakRelay.servers.put(publicID, this);

            session.sendText(privateID, Callback.NOOP);
        }
    }

    @OnWebSocketClose
    public void onWebSocketOpen(int statusCode, String message) {
        WebSpeakRelay.servers.remove(publicID);
    }
    
    public static class LinkedConnection
    {
        public LinkedConnection(ServerPlayerConnection serverConnection)
        {
            this.server = serverConnection;
        }

        public final ServerPlayerConnection server;
        public ClientPlayerConnection client;
    }
}

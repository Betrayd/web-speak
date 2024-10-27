package net.betrayd.webspeak.relay;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import net.betrayd.webspeak.relay.ServerConnection.LinkedConnection;

@WebSocket
public class ServerPlayerConnection {

    private Session session;
    ServerConnection server;
    String sessionID;

    //store this here to handle a lot of packets fast without cpu doing map stuff.
    private ClientPlayerConnection connectedClient = null;

    public void connectedClient(String context, ClientPlayerConnection connection)
    {
        this.connectedClient = connection;

        //TODO: do this properly
        this.send("relayClientConnect;{context:\""+context+"\"}");
    }

    public void disconnectClient(int statusCode, String reason)
    {
        this.connectedClient = null;

        //TODO: do this properly too
        this.send("relayClientDisconnect;{statusCode:"+statusCode+",reason:\""+reason+"\"}");
    }

    public void disconnect()
    {
        session.close();
    }

    public void send(String message)
    {
        session.sendText(message, Callback.NOOP);
    }

    @OnWebSocketOpen
    public void onWebSocketOpen(Session session) {
        System.out.println("Server opened player with params: "+session.getUpgradeRequest().getQueryString()+", server:" + session.getRemoteSocketAddress());
        if (this.session != null) {
            throw new IllegalStateException("Session is already connected!");
        }

        this.session = session;

        String privateServerID = NetUtils.splitQueryString(session.getUpgradeRequest().getQueryString()).get("key");
        String publicServerID = NetUtils.splitQueryString(session.getUpgradeRequest().getQueryString()).get("server");

        server = WebSpeakRelay.servers.get(publicServerID);
        if(server != null && server.isKey(privateServerID))
        {
            sessionID = NetUtils.splitQueryString(session.getUpgradeRequest().getQueryString()).get("id");
            
            //we don't check here to see if the connection exists because the server can worry about it since it's key protected
            server.connections.put(sessionID, new LinkedConnection(this));
        }
    }

    //I don't know about this implementation, as the server is a client so when it closes it's not going to say what we want but EH I don't actually care
    @OnWebSocketClose
    public void OnWebSocketClose(int statusCode, String reason)
    {
        System.out.println("Server player connection closed. PlayerID: "+sessionID+", Server: " + session.getRemoteSocketAddress());
        if(connectedClient == null)
        {
            return;
        }
        connectedClient.close(statusCode, reason);
        server.connections.remove(sessionID);
    }

    @OnWebSocketMessage
    public void onWebSocketMessage(String message)
    {
        if(connectedClient == null)
        {
            return;
        }
        this.connectedClient.send(message);
    }
}

package net.betrayd.webspeak.relay;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;

import net.betrayd.webspeak.relay.ServerConnection.LinkedConnection;


public class ClientPlayerConnection {
    private Session session;

    //store this here to handle a lot of packets fast without cpu doing map stuff.
    private ServerPlayerConnection connectedServer = null;
    private final String serverID;

    public ClientPlayerConnection(String server)
    {
        this.serverID = server;
    }

    public void send(String message)
    {
        session.sendText(message, Callback.NOOP);
    }

    public void close(int statusCode, String reason)
    {
        session.close(statusCode, reason, Callback.NOOP);
    }

    @OnWebSocketOpen
    public void onWebSocketOpen(Session session) {
        if (this.session != null) {
            throw new IllegalStateException("Session is already connected!");
        }

        this.session = session;

        ServerConnection server = WebSpeakRelay.servers.get(serverID);
        if(server == null)
        {
            session.close(StatusCode.INVALID_UPSTREAM_RESPONSE, "The server being connected to does not exist", Callback.NOOP);
        }
        LinkedConnection link = server.connections.get(NetUtils.splitQueryString(session.getUpgradeRequest().getQueryString()).get("id"));
        
        //link the server to the client
        //dang I can already tell I'm making things messy. I need to learn how to strucure code
        connectedServer = link.server;
        connectedServer.connectedClient(session.getRemoteSocketAddress().toString(), this);

        link.client = this;
    }

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason)
    {
        if(connectedServer == null)
        {
            return;
        }
        connectedServer.disconnectClient(statusCode, reason);
    }

    @OnWebSocketMessage
    public void onWebSocketMessage(Session session, String message)
    {
        this.connectedServer.send(message);
    }
}

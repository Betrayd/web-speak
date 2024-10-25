package net.betrayd.webspeak.impl.jetty;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.WebSpeakNet.UnknownPacketException;

//TODO: test for memory leaks
public class PlayerRelayConnection implements PlayerConnection, Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRelayConnection.class);

    private final WebSpeakServer server;
    private final WebSpeakPlayer player;

    private Session session;

    private boolean connected = false;

    public PlayerRelayConnection(WebSpeakServer server, WebSpeakPlayer player) {
        this.server = server;
        this.player = player;
    }

    @Override
    public WebSpeakServer getServer() {
        return this.server;
    }

    @Override
    public WebSpeakPlayer getPlayer() {
        return this.getPlayer();
    }

    @Override
    public boolean isConnected() {
        return session != null && connected;
    }

    @Override
    public void sendText(String message) {
        session.sendText(message, Callback.NOOP);
    }

    @Override
    public void disconnect(int statusCode, String reason) {
        session.close(StatusCode.NORMAL, reason, Callback.NOOP);
    }

    @Override
    public String getRemoteAddress() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRemoteAddress'");
    }

    @Override
    public void onWebSocketOpen(Session session)
    {
        // The WebSocket endpoint has been opened.

        // Store the session to be able to send data to the remote peer.
        this.session = session;
        player.setConnection(this);
    }

    @Override
    public void onWebSocketText(String message)
    {
        //implement something for connection started and ended here
        try {
            WebSpeakNet.applyPacket(player, message);
        } catch (UnknownPacketException e) {
            session.close(StatusCode.BAD_PAYLOAD, "Unknown packet type: " + e.getPacketId(), Callback.NOOP);
            LOGGER.warn("{} sent unknown packet '{}'", player.getPlayerId(), e.getPacketId());
        } catch (Exception e) {
            session.close(StatusCode.SERVER_ERROR, e.getMessage(), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        // The WebSocket endpoint failed.

        // You may log the error.
        LOGGER.error("Failed to connect to the relay!", cause);
    }
}

package net.betrayd.webspeak;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.util.WebSpeakVector;

public abstract class WebSpeakPlayer {

    private final WebSpeakServer server;
    private final String playerId;
    private final String sessionId;

    WsContext wsContext;
    
    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        this.server = server;
        this.playerId = playerId;
        this.sessionId = sessionId;
    }

    /**
     * Get the session ID used in the URL when a websocket connection is made.
     * @return Session ID.
     */
    public final String getPlayerId() {
        return playerId;
    }

    /**
     * Get the public-facing ID used for this player.
     * @return Public-facing ID
     */
    public final String getSessionId() {
        return sessionId;
    }

    public final WebSpeakServer getServer() {
        return server;
    }

    /**
     * Get the global location of this player.
     * @return Player location vector.
     */
    public abstract WebSpeakVector getLocation();

    /**
     * Get the rotation of this player.
     * @return XYZ euler rotation in degrees
     */
    public abstract WebSpeakVector getRotation();

    public abstract boolean isInScope(WebSpeakPlayer other);
    
    /**
     * Get the username of this player. Should be overwritten by game implementations.
     * @return Player username.
     */
    public String getUsername() {
        return playerId;
    }

    public final WsContext getWsContext() {
        return wsContext;
    }
}

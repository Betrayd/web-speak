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
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Get the public-facing ID used for this player.
     * @return Public-facing ID
     */
    public String getSessionId() {
        return sessionId;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    public abstract WebSpeakVector getLocation();
    public abstract boolean isInScope(WebSpeakPlayer other);

    public WsContext getWsContext() {
        return wsContext;
    }
}

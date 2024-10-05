package net.betrayd.webspeak;

import java.util.HashSet;
import java.util.Set;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.packets.SetAudioParamsS2CPacket;
import net.betrayd.webspeak.impl.util.ObservableSet;
import net.betrayd.webspeak.impl.util.URIComponent;
import net.betrayd.webspeak.util.WebSpeakVector;

public abstract class WebSpeakPlayer {

    private final WebSpeakServer server;
    private final String playerId;
    private final String sessionId;

    private final ObservableSet<WebSpeakPlayer> mutedPlayers = new ObservableSet<>(new HashSet<>());

    WsContext wsContext;
    
    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        this.server = server;
        this.playerId = playerId;
        this.sessionId = sessionId;

        mutedPlayers.ON_ADDED.addListener(player -> {
            if (wsContext != null && isInScope(player)) {
                generateAudioParams(player).send(wsContext);
            }
        });

        mutedPlayers.ON_REMOVED.addListener(player -> {
            if (wsContext != null && isInScope(player)) {
                generateAudioParams(player).send(wsContext);
            }
        });
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
     * @return Player location vector, using a Z-up coordinate space.
     */
    public abstract WebSpeakVector getLocation();

    /**
     * Get the forward direction of this player.
     * @return Player forward vector
     */
    public WebSpeakVector getForward() {
        return new WebSpeakVector(0, 0, 1);
    }

    /**
     * Get the up direction of this player.
     * @return Player up vector
     */
    public WebSpeakVector getUp() {
        return new WebSpeakVector(0, 1, 0);
    }

    public abstract boolean isInScope(WebSpeakPlayer other);

    /**
     * Get a set of all muted players. Updates to this set will trigger packets to be sent to the client.
     * @return Muted players set.
     */
    public Set<WebSpeakPlayer> getMutedPlayers() {
        return mutedPlayers;
    }
    
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

    private SetAudioParamsS2CPacket generateAudioParams(WebSpeakPlayer other) {
        return new SetAudioParamsS2CPacket(other.playerId, true, mutedPlayers.contains(other));
    }

    /**
     * Get a URL for clients to connect to this webspeak player.
     * @param frontendAddress Base URL of the frontend.
     * @param backendAddress Base URL of the backend.
     * @return Connection URL.
     */
    public final String getConnectionURL(String frontendAddress, String backendAddress) {
        return getConnectionURL(frontendAddress, backendAddress, getSessionId());
    }

    /**
     * Get a URL for clients to connect to webspeak with a given session ID.
     * 
     * @param frontendAddress Base URL of the frontend.
     * @param backendAddress  Base URL of the backend.
     * @param sessionID       Session ID to connect with.
     * @return Connection URL.
     */
    public static String getConnectionURL(String frontendAddress, String backendAddress, String sessionID) {
        return frontendAddress + "?server=" +
                URIComponent.encode(backendAddress) + "&id=" + sessionID;
    }
}

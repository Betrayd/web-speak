package net.betrayd.webspeak;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.packets.SetAudioParamsS2CPacket;
import net.betrayd.webspeak.impl.net.packets.UpdateTransformS2CPacket;
import net.betrayd.webspeak.impl.util.ObservableSet;
import net.betrayd.webspeak.impl.util.URIComponent;
import net.betrayd.webspeak.util.WebSpeakVector;

public abstract class WebSpeakPlayer {

    private final WebSpeakServer server;
    private final String playerId;
    private final String sessionId;

    private final ObservableSet<WebSpeakPlayer> mutedPlayers = new ObservableSet<>(new HashSet<>());
    private final ObservableSet<WebSpeakPlayer> unspatializedPlayers = new ObservableSet<>(new HashSet<>());
    
    protected final Logger LOGGER;

    WsContext wsContext;
    
    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        LOGGER = LoggerFactory.getLogger("WebSpeak Player (" + playerId + ")");

        this.server = server;
        this.playerId = playerId;
        this.sessionId = sessionId;

        mutedPlayers.ON_ADDED.addListener(player -> {
            if (wsContext != null && server.areInScope(this, player)) {
                new SetAudioParamsS2CPacket(playerId, null, true).send(wsContext);
            }
        });

        mutedPlayers.ON_REMOVED.addListener(player -> {
            if (wsContext != null && server.areInScope(this, player)) {
                new SetAudioParamsS2CPacket(playerId, null, true).send(wsContext);
            }
        });

        unspatializedPlayers.ON_ADDED.addListener(player -> {
            if (wsContext != null && server.areInScope(this, player)) {
                new SetAudioParamsS2CPacket(playerId, false, null).send(wsContext);
            }
        });

        unspatializedPlayers.ON_REMOVED.addListener(player -> {
            if (wsContext != null && server.areInScope(this, player)) {
                new SetAudioParamsS2CPacket(playerId, true, null).send(wsContext);
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

    private WebSpeakChannel channel = null;

    public final WebSpeakChannel getChannel() {
        return channel;
    }

    public synchronized void setChannel(WebSpeakChannel channel) {
        if (channel == this.channel) {
            return;
        }
        if (this.channel != null) {
            this.channel.onRemovePlayer(this);
        }
        this.channel = channel;
        if (channel != null) {
            this.channel.onAddPlayer(this);
        }
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CHANNEL_SWAPS)) {
            // if (channel != null)
                LOGGER.info("Player joined channel " + (channel != null ? channel.getName() : "null"));
        }
    }
    
    
    /**
     * Called when this player has joined the scope of another player.
     * @param other The other player.
     */
    protected void onJoinedScope(WebSpeakPlayer other) {
        if (wsContext != null) {
            UpdateTransformS2CPacket.fromPlayer(other).send(wsContext);
            new SetAudioParamsS2CPacket(other.playerId, isPlayerSpatialized(other), isPlayerMuted(other)).send(wsContext);
        }
    }

    /**
     * Called when this player has left the scope of another player.
     * @param other The other player.
     */
    protected void onLeftScope(WebSpeakPlayer other) {
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
     * Perform any additional ticking this webspeak player desires.
     */
    public void tick() {
    }

    /**
     * Get a set of all muted players. Updates to this set will trigger packets to be sent to the client.
     * @return Muted players set.
     */
    public final Set<WebSpeakPlayer> getMutedPlayers() {
        return mutedPlayers;
    }

    public final void setPlayerMuted(WebSpeakPlayer other, boolean muted) {
        if (muted) {
            mutedPlayers.add(other);
        } else {
            mutedPlayers.remove(other);
        }
    }

    public final boolean isPlayerMuted(WebSpeakPlayer other) {
        return mutedPlayers.contains(other);
    }
    
    /**
     * Get a set of all non-spatialized. Updates to this set will trigger packets to be sent to the client.
     * @return Muted players set.
     */
    public final ObservableSet<WebSpeakPlayer> getUnspatializedPlayers() {
        return unspatializedPlayers;
    }
    
    public final void setPlayerSpatialized(WebSpeakPlayer other, boolean spatialized) {
        if (spatialized) {
            unspatializedPlayers.remove(other);
        } else {
            unspatializedPlayers.add(other);
        }
    }

    public final boolean isPlayerSpatialized(WebSpeakPlayer other) {
        return !unspatializedPlayers.contains(other);
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
    
    public final boolean isConnected() {
        return wsContext != null;
    }
    
    /**
     * Called after the player is removed from the server.
     */
    protected void onRemoved() {

    }

    /**
     * Get a URL for clients to connect to this webspeak player.
     * 
     * @param frontendAddress Base URL of the frontend.
     * @param backendAddress  Base URL of the backend.
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

    @Override
    public String toString() {
        return "WebSpeakPlayer[" + playerId + "]";
    }
}

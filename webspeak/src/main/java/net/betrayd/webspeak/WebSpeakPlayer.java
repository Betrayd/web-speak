package net.betrayd.webspeak;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.packets.SetAudioModifierS2CPacket;
import net.betrayd.webspeak.impl.net.packets.UpdateTransformS2CPacket;
import net.betrayd.webspeak.impl.util.SimpleObservableList;
import net.betrayd.webspeak.impl.util.URIComponent;
import net.betrayd.webspeak.util.AudioModifier;
import net.betrayd.webspeak.util.WebSpeakVector;

public abstract class WebSpeakPlayer {

    private final WebSpeakServer server;
    private final String playerId;
    private final String sessionId;
    
    protected final Logger LOGGER;

    WsContext wsContext;
    
    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        LOGGER = LoggerFactory.getLogger("WebSpeak Player (" + playerId + ")");

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

    // No reason to keep players & groups around that aren't being used.
    private final Map<WebSpeakPlayer, AudioModifier> playerAudioModifiers = new WeakHashMap<>();
    private final Map<WebSpeakPlayerGroup, AudioModifier> groupAudioModifiers = new WeakHashMap<>();

    private final SimpleObservableList<WebSpeakPlayerGroup> groups = new SimpleObservableList<>();
    private final List<WebSpeakPlayerGroup> synchronizedGroups = Collections.synchronizedList(groups);

    /**
     * Return a list with all player groups. Modifications to this list will
     * properly propagate and notify all relevent listeners.
     * 
     * @return Player groups.
     */
    public List<WebSpeakPlayerGroup> getGroups() {
        return synchronizedGroups;
    }

    
    private final Consumer<WebSpeakPlayer> onInvalidateAudioModifier = player -> {
        recalculateAudioModifiers(Collections.singleton(player));
    };

    private final Consumer<Void> onInvalidateGlobal = v -> recalculateAllAudioModifiers();

    protected synchronized void onJoinGroup(Collection<? extends WebSpeakPlayerGroup> groups) {
        for (var group : groups) {
            group.onAddPlayer(this);
            group.ON_INVALIDATE_MODIFIER.addListener(onInvalidateAudioModifier);
            group.ON_INVALIDATE_GLOBAL.addListener(onInvalidateGlobal);
        }
        // Groups can declare global audio modifiers, so we need to recalculate all players.
        recalculateAllAudioModifiers();
    }

    protected synchronized void onLeaveGroup(Collection<? extends WebSpeakPlayerGroup> groups) {
        for (var group : groups) {
            group.onRemovePlayer(this);
            group.ON_INVALIDATE_MODIFIER.removeListener(onInvalidateAudioModifier);
            group.ON_INVALIDATE_GLOBAL.removeListener(onInvalidateGlobal);
        }
        // Groups can declare global audio modifiers, so we need to recalculate all players.
        recalculateAllAudioModifiers();
    }


    protected synchronized void recalculateAllAudioModifiers() {
        var inScope = server.getPlayers().stream().filter(p -> server.areInScope(p, this)).toList();
        recalculateAudioModifiers(inScope);
    }

    /**
     * Recalculate the audio modifiers for a group of players.
     * @param players Players to recalculate.
     */
    protected synchronized Map<WebSpeakPlayer, AudioModifier> recalculateAudioModifiers(Iterable<WebSpeakPlayer> players) {
        Map<WebSpeakPlayer, AudioModifier> modifiers = new HashMap<>();
        for (var player : players) {
            modifiers.put(player, calculateAudioModifier(player));
        }
        for (var entry : modifiers.entrySet()) {
            new SetAudioModifierS2CPacket(entry.getKey().playerId, entry.getValue());
        }
        return modifiers;
    }

    protected synchronized AudioModifier calculateAudioModifier(WebSpeakPlayer player) {
        if (player == this) {
            return AudioModifier.EMPTY;
        }

        AudioModifier modifier = AudioModifier.EMPTY;
        for (var group : getGroups()) {
            AudioModifier.combine(group.getPlayerAudioModifier(player), modifier);
        }
        for (var otherGroup : player.getGroups()) {
            AudioModifier groupModifier = groupAudioModifiers.get(otherGroup);
            if (groupModifier != null) {
                AudioModifier.combine(groupModifier, modifier);
            }
        }
        AudioModifier playerModifier = playerAudioModifiers.get(player);
        if (playerModifier != null) {
            AudioModifier.combine(playerModifier, modifier);
        }

        return modifier;
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
            recalculateAudioModifiers(Collections.singleton(other));
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

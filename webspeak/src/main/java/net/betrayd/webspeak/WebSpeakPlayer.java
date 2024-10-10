package net.betrayd.webspeak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.packets.SetAudioModifierS2CPacket;
import net.betrayd.webspeak.impl.net.packets.UpdateTransformS2CPacket;
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

    private WebSpeakChannel channel = null;

    private List<WebSpeakGroup> groups = new ArrayList<>();

    /**
     * Return an unmodifiable list of groups the player is in. Groups with a greater
     * index take a higher priority over groups with a lesser index.
     * 
     * @return Unmodifiable list.
     */
    public List<WebSpeakGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public boolean isInGroup(WebSpeakGroup group) {
        return groups.contains(group);
    }

    /**
     * Add this player to a group.
     * @param group Group to add.
     * @return If the player was not already in the group.
     */
    public synchronized boolean addGroup(WebSpeakGroup group) {
        if (groups.contains(group)) {
            return false;
        }
        groups.add(group);
        group.onAddPlayer(this);
        onAddGroup(group);
        return true;
    }
    
    /**
     * Add this player to a group at a specified index.
     * @param group Group to add.
     * @param index Index to add at. Higher indexes take priority over lower indexes.
     * @return If the player was not already in the group.
     */
    public synchronized boolean addGroup(WebSpeakGroup group, int index) {
        if (groups.contains(group)) {
            return false;
        }
        groups.add(index, group);
        group.onAddPlayer(this);
        onAddGroup(group);
        return true;
    }

    /**
     * Remove this player from a group.
     * @param group Group to remove from.
     * @return
     */
    public synchronized boolean removeGroup(WebSpeakGroup group) {
        if (groups.remove(group)) {
            group.onRemovePlayer(this);
            onRemoveGroup(group);
            return true;
        }
        return false;
    }

    private void onInvalidateAudioModifiers(Collection<? extends WebSpeakPlayer> players) {
        for (var player : players) {
            updatePlayerAudioModifiers(player);
        }
    }

    private final Consumer<Collection<? extends WebSpeakPlayer>> invalidateAudioModifiersListener = this::onInvalidateAudioModifiers;

    /**
     * Called when this player is added to a group.
     * @param group Group that was added to.
     */
    protected void onAddGroup(WebSpeakGroup group) {
        group.ON_MODIFIER_INVALIDATED.addListener(invalidateAudioModifiersListener);
        onInvalidateAudioModifiers(group.getAudioModifiedPlayers());
    }

    /**
     * Called when this player is removed from a group.
     * @param group Group that was removed from.
     */
    protected void onRemoveGroup(WebSpeakGroup group) {
        group.ON_MODIFIER_INVALIDATED.removeListener(invalidateAudioModifiersListener);
        onInvalidateAudioModifiers(group.getAudioModifiedPlayers());
    }
    
    /**
     * Re-calculate the audio modifiers used on a given player and send to the client.
     * @param player Player to calculate.
     */
    protected synchronized void updatePlayerAudioModifiers(WebSpeakPlayer player) {
        if (!isConnected() || !server.areInScope(this, player))
            return;
        
        AudioModifier modifier = computeAudioModifier(player);
        new SetAudioModifierS2CPacket(playerId, modifier).send(wsContext);
    }

    protected synchronized AudioModifier computeAudioModifier(WebSpeakPlayer player) {
        AudioModifier modifier = AudioModifier.DEFAULT;
        for (var group : groups) {
            modifier = AudioModifier.combine(group.computeAudioModifier(player), modifier);
        }
        return modifier;
    }

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
            updatePlayerAudioModifiers(other);
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

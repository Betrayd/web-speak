package net.betrayd.webspeak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
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

    private final List<WebSpeakGroup> groups = new ArrayList<>();

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
        onAddGroup(group);
        // Call this here because onAddGroup may need to be called without invalidating modifiers.
        onInvalidateAudioModifiers(group.getAudioModifiedPlayers());
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
        onAddGroup(group);
        onInvalidateAudioModifiers(group.getAudioModifiedPlayers());
        return true;
    }

    /**
     * Add a this player to a collection of groups.
     * @param groups Groups to add.
     */
    public synchronized void addGroups(Collection<? extends WebSpeakGroup> groups) {
        Set<WebSpeakGroup> groupSet = new HashSet<>(this.groups);
        Set<WebSpeakPlayer> invalidPlayers = new HashSet<>();
        for (var group : groups) {
            // Easy way to check for duplicates
            if (groupSet.add(group)) {
                this.groups.add(group);
                onAddGroup(group);
                invalidPlayers.addAll(group.getAudioModifiedPlayers());
            }
        }
        onInvalidateAudioModifiers(invalidPlayers);
    }

    /**
     * Remove this player from a group.
     * @param group Group to remove from.
     * @return If the player was in the group.
     */
    public synchronized boolean removeGroup(WebSpeakGroup group) {
        Collection<WebSpeakPlayer> invalidPlayers = null;
        boolean success = false;
        if (groups.remove(group)) {
            // This is called before onRemoveGroup so, in case the group has an audio
            // modifier on itself, this player gets included in that.
            invalidPlayers = group.getAudioModifiedPlayers();
            onRemoveGroup(group);
            success = true;
        }
        if (success) {
            onInvalidateAudioModifiers(invalidPlayers);
        }
        return success;
    }

    /**
     * Remove this player from a collection of groups.
     * @param groups Groups to remove from.
     * @return If the player was in any of the groups.
     */
    public synchronized boolean removeGroups(Collection<? extends WebSpeakGroup> groups) {
        Set<WebSpeakPlayer> invalidPlayers = new HashSet<>();
        // Checks make sure this will never duplicate.
        boolean success = false;
        var iterator = this.groups.iterator();
        while (iterator.hasNext()) {
            var group = iterator.next();
            if (groups.contains(group)) {
                iterator.remove();
                invalidPlayers.addAll(group.getAudioModifiedPlayers());
                onRemoveGroup(group);
                success = true;
            }
        }

        onInvalidateAudioModifiers(invalidPlayers);
        return success;
    }
    
    /**
     * Arbitrarily modify the group list for this player, and call the appropriate
     * callbacks based on what was changed.
     * 
     * @param modifier Function to modify the groups.
     * @throws IllegalArgumentException If the modifier function adds duplicate groups.
     * @implNote Because of the amount of checks & listeners needed, this method is
     *           fairly inefficient. Use the other group methods for simpler tasks.
     */
    public synchronized void modifyGroups(Consumer<? super List<WebSpeakGroup>> modifier) throws IllegalArgumentException {
        List<WebSpeakGroup> prev = List.copyOf(groups);
        List<WebSpeakGroup> modified = new ArrayList<>(groups);
        modifier.accept(modified);

        // Ths could probably be more efficient, but it's called rarely with a small dataset so it doesn't really matter.
        Set<WebSpeakGroup> duplicateChecker = new HashSet<>();
        for (var item : modified) {
            if (!duplicateChecker.add(item)) {
                throw new IllegalArgumentException("Player may not have two instances of a group. (" + item + ")");
            }
        }

        if (prev.equals(modified)) {
            return;
        }

        List<WebSpeakGroup> removedGroups = new ArrayList<>(modified.size());
        List<WebSpeakGroup> addedGroups = new ArrayList<>(modified.size());

        for (var group : modified) {
            if (!prev.contains(group)) {
                addedGroups.add(group);
            }
        }

        for (var group : prev) {
            if (!modified.contains(group)) {
                removedGroups.add(group);
            }
        }
        
        groups.clear();
        groups.addAll(modified);

        // Invalidate all players who are in all groups + removed groups because order
        // could have changed.
        Set<WebSpeakPlayer> invalidModifiers = new HashSet<>();
        for (var group : groups) {
            invalidModifiers.addAll(group.getAudioModifiedPlayers());
        }
        for (var group : removedGroups) {
            invalidModifiers.addAll(group.getAudioModifiedPlayers());
        }

        for (var added : addedGroups) {
            onAddGroup(added);
        }
        for (var removed : removedGroups) {
            onRemoveGroup(removed);
        }
        
        onInvalidateAudioModifiers(invalidModifiers);
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
        group.onAddPlayer(this);
        group.ON_MODIFIER_INVALIDATED.addListener(invalidateAudioModifiersListener);
    }

    /**
     * Called when this player is removed from a group.
     * @param group Group that was removed from.
     */
    protected void onRemoveGroup(WebSpeakGroup group) {
        group.onRemovePlayer(this);
        group.ON_MODIFIER_INVALIDATED.removeListener(invalidateAudioModifiersListener);
    }

    /**
     * Cache the audio modifiers sent to the player so we don't resend. Make sure to clear on left scope.
     */
    private final WeakHashMap<WebSpeakPlayer, AudioModifier> audioModifierCache = new WeakHashMap<>();
    
    /**
     * Re-calculate the audio modifiers used on a given player and send to the client.
     * @param player Player to calculate.
     */
    protected synchronized void updatePlayerAudioModifiers(WebSpeakPlayer player) {
        if (!isConnected() || !server.areInScope(this, player))
            return;
        
        AudioModifier modifier = computeAudioModifier(player);
        if (!modifier.equals(audioModifierCache.get(player))) {
            new SetAudioModifierS2CPacket(player.playerId, modifier).send(wsContext);
            audioModifierCache.put(player, modifier);
        }
    }
    
    /**
     * Compute the audio modifier this player will use for a target player based on
     * both players' groups.
     * 
     * @param player Target player.
     * @return The combined audio modifier.
     * @implNote This method does not modify or rely on the cache in any way. It
     *           always computes the audio modifier from scratch and returns it,
     *           regardless of scope.
     */
    public synchronized AudioModifier computeAudioModifier(WebSpeakPlayer player) {
        AudioModifier modifier = AudioModifier.DEFAULT;
        for (var group : groups) {
            modifier = AudioModifier.combine(group.computeAudioModifier(player), modifier);
        }
        return modifier;
    }
    
    /**
     * Get the audio modifier this player has cached for another player without
     * recalculating. Will only be valid if players are in scope.
     * 
     * @param player Target player.
     * @return The audio modifier, or <code>null</code> if the players aren't in
     *         scope.
     */
    public synchronized AudioModifier getCachedAudioModifier(WebSpeakPlayer player) {
        return audioModifierCache.get(player);
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
     * Called when this player has left the scope of another player. Called for all
     * players in scope when this player's client disconnects.
     * 
     * @param other The other player.
     */
    protected void onLeftScope(WebSpeakPlayer other) {
        audioModifierCache.remove(other);
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

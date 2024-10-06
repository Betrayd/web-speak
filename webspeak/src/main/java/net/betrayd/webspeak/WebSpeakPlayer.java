package net.betrayd.webspeak;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    private final Set<WebSpeakChannel> channels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    

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
     * Get a collection of all channels this player is in.
     * @return Unmodifiable view of channels.
     */
    public Set<WebSpeakChannel> getChannels() {
        return Collections.unmodifiableSet(channels);
    }
    
    /**
     * Join a voice channel.
     * 
     * @param channel Channel to join.
     * @return If the player was not already in this channel.
     */
    public final boolean joinChannel(WebSpeakChannel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (channels.add(channel)) {
            channel.onAddPlayer(this);
            onJoinChannel(channel);
            return true;
        }
        return false;
    }

    /**
     * Leave a voice channel.
     * 
     * @param channel Channel to leave.
     * @return If the player was in the channel.
     */
    public final boolean leaveChannel(WebSpeakChannel channel) {
        if (channels.remove(channel)) {
            channel.onRemovePlayer(this);
            onLeaveChannel(channel);
            return true;
        }
        return false;
    }

    boolean leaveChannelNoCallback(WebSpeakChannel channel) {
        if (channels.remove(channel)) {
            onLeaveChannel(channel);
            return true;
        }
        return false;
    }
    
    /**
     * Remove this player from all channels.
     */
    public synchronized void clearChannels() {
        for (var channel : channels) {
            channel.onRemovePlayer(this);
            onLeaveChannel(channel);
        }

        channels.clear();
    }

    // If the player or channel are no longer being used, no reason to keep them around here.
    private final Map<WebSpeakPlayer, AudioModifier> playerAudioModifiers = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<WebSpeakChannel, AudioModifier> channelAudioModifiers = Collections.synchronizedMap(new WeakHashMap<>());

    public Map<WebSpeakPlayer, AudioModifier> getPlayerAudioModifiers() {
        return Collections.unmodifiableMap(playerAudioModifiers);
    }

    public Map<WebSpeakChannel, AudioModifier> getChannlelAudioModifiers() {
        return Collections.unmodifiableMap(channelAudioModifiers);
    }

    /**
     * Add an audio modifier targeting a specific player.
     * 
     * @param target   Target player.
     * @param modifier Modifier to add, or <code>null</code> to remove the modifier.
     * @return The previous modifier assigned to that player, if any.
     */
    public synchronized AudioModifier setAudioModifier(WebSpeakPlayer target, AudioModifier modifier) {
        AudioModifier prev;
        if (modifier != null) {
            prev = playerAudioModifiers.put(target, modifier);
        } else {
            prev = playerAudioModifiers.remove(target);
        }
        updateAudioModifiers(target);
        return prev;
    }

    /**
     * Remove all player-targeted audio modifiers.
     */
    public synchronized void clearPlayerAudioModifiers() {
        var players = playerAudioModifiers.keySet().toArray(WebSpeakPlayer[]::new);
        playerAudioModifiers.clear();
        for (var player : players) {
            updateAudioModifiers(player);
        }
    }

    /**
     * Get the audio modifier targeting a specific player, if any.
     * @param player Target player.
     * @return The audio modifier, or <code>null</code> if it doesn't exist.
     */
    public final AudioModifier getAudioModifier(WebSpeakPlayer player) {
        return playerAudioModifiers.get(player);
    }

    /**
     * Add an audio modifier targeting an entire channel.
     * 
     * @param target   Target channel.
     * @param modifier Modifier to add, or <code>null</code> to remove the modifier.
     * @return The previous modifier assigned to that channel, if any.
     */
    public synchronized AudioModifier setAudioModifier(WebSpeakChannel channel, AudioModifier modifier) {
        AudioModifier prev;
        if (modifier != null) {
            prev = channelAudioModifiers.put(channel, modifier);
        } else {
            prev = channelAudioModifiers.remove(channel);
        }
        for (var player : channel.getPlayers()) {
            if (player != this)
                updateAudioModifiers(player);
        }
        return prev;
    }

    /**
     * Remove all channel-targeted audio modifiers.
     */
    public synchronized void clearChannelAudioModifiers() {
        var players = channelAudioModifiers.keySet().stream().flatMap(channel -> channel.getPlayers().stream())
                .collect(Collectors.toSet());
        
        channelAudioModifiers.clear();
        for (var player : players) {
            updateAudioModifiers(player);
        }
    }

    /**
     * Get the audio modifier targeting a channel, if any.
     * @param channel Target channel.
     * @return The audio modifier, or <code>null</code> if it doesn't exist.
     */
    public final AudioModifier getAudioModifier(WebSpeakChannel channel) {
        return channelAudioModifiers.get(channel);
    }

    /**
     * Calculate the composite audio modifier this player will use on another player.
     * @param other Other player.
     * @return Audio modifier.
     */
    public synchronized AudioModifier calculateAudioModifiers(WebSpeakPlayer other) {
        // TODO: can we make audio modifier updates more efficient? 
        // Right now, there's a lot of unneeded recursion.
        AudioModifier modifier = AudioModifier.DEFAULT;

        for (var channel : this.getChannels()) {
            modifier.append(channel.calculateAudioModifier(other));
        }

        for (var channel : other.getChannels()) {
            // Modifier will return self if channel does not have modifier.
            modifier = modifier.append(channelAudioModifiers.get(channel));
        }

        return modifier.append(playerAudioModifiers.get(other));
    }

    /**
     * Calculate the composite audio modifier this player will use for another
     * player and send it to the client.
     * 
     * @param other Other player.
     */
    public void updateAudioModifiers(WebSpeakPlayer other) {
        if (other == this || wsContext == null || !server.areInScope(this, other)) {
            return;
        }

        AudioModifier modifier = calculateAudioModifiers(other);
        new SetAudioModifierS2CPacket(other.playerId, modifier).send(wsContext);
    }
    
    /**
     * Called when this player has joined the scope of another player.
     * @param other The other player.
     */
    protected void onJoinScope(WebSpeakPlayer other) {
        if (wsContext != null) {
            UpdateTransformS2CPacket.fromPlayer(other).send(wsContext);
            updateAudioModifiers(other);
        }
    }

    /**
     * Called when this player has left the scope of another player.
     * @param other The other player.
     */
    protected void onLeaveScope(WebSpeakPlayer other) {
        
    }

    /**
     * Called when this player joins a channel.
     * 
     * @param channel Channel that was joined.
     * @apiNote Whether the channel has already been added to this player's list of
     *          channels is undefined.
     */
    protected void onJoinChannel(WebSpeakChannel channel) {
        for (var player : channel.getAudioModifiedPlayers()) {
            updateAudioModifiers(player);
        }
    }

    /**
     * Called when this player leaves a channel.
     * 
     * @param channel Channel that has been left.
     * @apiNote Whether the channel has already been removed from this player's list
     *          of channels is undefined.
     */
    protected void onLeaveChannel(WebSpeakChannel channel) {
        for (var player : channel.getAudioModifiedPlayers()) {
            updateAudioModifiers(player);
        }
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

    /**
     * Check if another player is in scope.
     * 
     * @param other Other player.
     * @return <code>true</code> if the player is in scope.
     * @apiNote <b>Do not call directly!</b> Instead use
     *          {@link WebSpeakServer#areInScope}, as this will use the cached
     *          value.
     */
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
     * Called when this player is removed from the server.
     */
    public void onRemoved() {
        clearChannels();
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

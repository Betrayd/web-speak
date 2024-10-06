package net.betrayd.webspeak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.betrayd.webspeak.util.AudioModifier;

/**
 * A channel that players may be in. Players are only considered for scope with
 * other players that share at least one channel.
 */
public class WebSpeakChannel {

    private Set<WebSpeakPlayer> players = new HashSet<>();

    /**
     * Get a collection of all players in this channel.
     * @return An unmodifiable collection of all players.
     */
    public Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Called when a player joins this channel
     * 
     * @param player Player that joined the channel.
     * @apiNote Whether the channel has already been added to the player's list of
     *          channels is undefined.
     */
    protected synchronized void onAddPlayer(WebSpeakPlayer player) {
        this.players.add(player);
    }

    /**
     * Called when a player leaves this channel.
     * 
     * @param player Player that left the channel.
     * @apiNote Whether the channel has already been removed from the player's list
     *          of channels is undefined.
     */
    protected synchronized void onRemovePlayer(WebSpeakPlayer player) {
        this.players.remove(player);
    }

    /**
     * Add a player to this channel. Shortcut for
     * <code>player.joinChannel(this)</code>
     * 
     * @param player Player to add.
     * @return If the player was not already in the channel.
     * @see WebSpeakPlayer#joinChannel
     */
    public final boolean addPlayer(WebSpeakPlayer player) {
        return player.joinChannel(this);
    }

    /**
     * Remove a player from this channel. Shortcut for
     * <code>player.leaveChannel(this)</code>
     * 
     * @param player Player to remove.
     * @return If the player was in the channel.
     * @see WebSpeakPlayer#leaveChannel
     */
    public final boolean removePlayer(WebSpeakPlayer player) {
        return player.leaveChannel(this);
    }

    /**
     * Remove all players from this channel.
     */
    public final void clearPlayers() {
        for (var player : players) {
            player.leaveChannelNoCallback(this);
        }
        players.clear();
    }

    // If the player or channel are no longer being used, no reason to keep them around here.
    private static Map<WebSpeakPlayer, AudioModifier> playerAudioModifiers = Collections.synchronizedMap(new WeakHashMap<>());
    private static Map<WebSpeakChannel, AudioModifier> channelAudioModifiers = Collections.synchronizedMap(new WeakHashMap<>());
    
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
     * 
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

    private synchronized void updateAudioModifiers(WebSpeakPlayer target) {
        for (var player : players) {
            player.updateAudioModifiers(target);
        }
    }

    /**
     * Calculate the composite audio modifier of a player based on the modifiers in
     * this channel.
     * 
     * @param player Target player.
     * @return Composite audio modifier.
     */
    public AudioModifier calculateAudioModifier(WebSpeakPlayer player) {
        AudioModifier modifier = AudioModifier.EMPTY;

        for (var channel : player.getChannels()) {
            modifier = modifier.append(channelAudioModifiers.get(channel));
        }

        modifier.append(playerAudioModifiers.get(player));
        return modifier;
    }
    
    /**
     * Get all the players that may have their audio modified by this channel.
     * @return All audio-modified players.
     */
    public final Set<WebSpeakPlayer> getAudioModifiedPlayers() {
        return Stream.concat(
                channelAudioModifiers.keySet().stream().flatMap(channel -> channel.getPlayers().stream()),
                playerAudioModifiers.keySet().stream()).collect(Collectors.toSet());
    }
}

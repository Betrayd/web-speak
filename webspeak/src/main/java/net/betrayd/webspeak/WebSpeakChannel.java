package net.betrayd.webspeak;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * A "channel" that players may be in. Players can be connected to channels, and
 * channels can be connected to each other. Only players that have at least one
 * channel connected will be considered for scope.
 * </p>
 * <p>
 * Channels may have various modifiers applied to themselves and their relations
 * with other channels. These modifiers will affect the way players hear other
 * players in the channel. If two players are in multiple connected channels,
 * the sum of all modifiers will be applied.
 * </p>
 * 
 */
public class WebSpeakChannel {
    /**
     * Cache which channels any given player is in so we don't have to go searching for them each time.
     */
    private static final Map<WebSpeakPlayer, Set<WebSpeakChannel>> playerChannels = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Get a collection of all the channels a given player is in.
     * @param player Player to check.
     * @return All the player's channels.
     */
    public static Collection<WebSpeakChannel> getPlayerChannels(WebSpeakPlayer player) {
        Set<WebSpeakChannel> set = playerChannels.get(player);
        if (set != null) {
            return List.copyOf(set);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get a collection of players that are shared across all the passed channels.
     * 
     * @param channels Channels to check.
     * @return Shared players.
     */
    public static Collection<WebSpeakPlayer> getSharedPlayers(WebSpeakChannel... channels) {
        if (channels.length > 2) {
            throw new IllegalArgumentException("Channel array must be at least 2 elements.");
        }
        return channels[0].players.stream().filter(player -> {
            for (int i = 1; i < channels.length; i++) {
                if (!channels[i].players.contains(player))
                    return false;
            }
            return true;
        }).toList();
    }

    /**
     * Get a collection of channels that are shared across all the passed players.
     * 
     * @param players Players to check.
     * @return Shared channels.
     */
    public static Collection<WebSpeakChannel> getSharedChannels(WebSpeakPlayer... players) {
        if (players.length > 2) {
            throw new IllegalArgumentException("Player array must be at least 2 elements.");
        }
        Set<WebSpeakChannel> playerAChannels = playerChannels.get(players[0]);
        if (playerAChannels == null) {
            return Collections.emptyList();
        }

        return playerAChannels.stream().filter(channel -> {
            for (int i = 1; i < players.length; i++) {
                Set<WebSpeakChannel> playerBChannels = playerChannels.get(players[i]);
                if (playerBChannels == null || !playerBChannels.contains(channel))
                    return false;
            }
            return true;
        }).toList();
    }

    private final WebSpeakServer server;
    private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public WebSpeakChannel(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }
    
    public void connectTo(WebSpeakChannel other) {
        server.getChannelManager().connectChannels(this, other);
    }

    public void disconnectFrom(WebSpeakChannel other) {
        server.getChannelManager().disconnectChannels(this, other);
    }

    public Collection<WebSpeakChannel> getConnectedChannels() {
        return server.getChannelManager().getConnectedChannels(this);
    }

    /**
     * Get a set of all players in this channel.
     * @return Unmodifiable view of players.
     */
    public Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }
    
    /**
     * Add a player to this channel.
     * @param player Player to add.
     * @return <code>true</code> if the player wasn't already in the channel.
     */
    public boolean addPlayer(WebSpeakPlayer player) {
        boolean success = players.add(player);
        if (success) {
            onPlayerJoined(player);
        }
        return success;
    }

    /**
     * Remove a player from this channel.
     * @param player Player to remove.
     * @return <code>true</code> if the player was in the channel.
     */
    public boolean removePlayer(WebSpeakPlayer player) {
        boolean success = players.remove(player);
        if (success) {
            onPlayerLeft(player);
        }
        return success;
    }

    /**
     * Check if a given player is in this channel.
     * @param player Player to check.
     * @return Player channel.
     */
    public boolean containsPlayer(WebSpeakPlayer player) {
        return players.contains(player);
    }

    protected void onPlayerJoined(WebSpeakPlayer player) {
        synchronized(playerChannels) {
            playerChannels.computeIfAbsent(player, p -> new HashSet<>()).add(this);
        }
        player.onJoinedChannel(this);
    }

    protected void onPlayerLeft(WebSpeakPlayer player) {
        synchronized(playerChannels) {
            var set = playerChannels.get(player);
            if (set != null) {
                set.remove(this);
            }
        }
        player.onLeftChannel(this);
    }

    protected void onConnectTo(WebSpeakChannel other) {

    }
    
    protected void onDisconnectFrom(WebSpeakChannel other) {
        
    }
}

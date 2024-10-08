package net.betrayd.webspeak;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A channel that a player can be in. Players will only be considered for scope
 * with other players in the same channel.
 * 
 * @apiNote The channel must be added to the server via
 *          {@link WebSpeakServer#addChannel} before any of its players will be
 *          considered for scope.
 */
public class WebSpeakChannel {

    private final String name;

    public WebSpeakChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // If the player is no longer being tracked by the server, no reason to keep it here.
    private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new WeakHashMap<>());

    public final Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Called when a player is added to the channel.
     * @param player Player that was added.
     * @return If the player was not already in the channel.
     * @apiNote Should only be called by the player's <code>setChannel</code> function.
     */
    protected synchronized boolean onAddPlayer(WebSpeakPlayer player) {
        return players.add(player);
    }
    
    /**
     * Called when a player is removed from the channel.
     * @param player Player that was removed.
     * @return If the player was in the channel.
     * @apiNote Should only be called by the player's <code>setChannel</code> function.
     */
    protected synchronized boolean onRemovePlayer(WebSpeakPlayer player) {
        if (players.remove(player)) {
            // Manually remove all players from scope
            // because the scope checker won't check them any more.
            player.getServer().kickScopes(player);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "WebSpeak Channel (" + name + ")";
    }
}

package net.betrayd.webspeak;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A channel that a player can be in. Players will only be considered for scope
 * with other players in the same channel.
 */
public class WebSpeakChannel {

    private final WebSpeakServer server;
    private final String name;

    protected WebSpeakChannel(WebSpeakServer server, String name) {
        this.server = server;
        this.name = name;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    // If the player is no longer being tracked by the server, no reason to keep it here.
    private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new WeakHashMap<>());

    public Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    protected synchronized boolean onAddPlayer(WebSpeakPlayer player) {
        return players.add(player);
    }
    
    protected synchronized boolean onRemovePlayer(WebSpeakPlayer player) {
        if (players.remove(player)) {
            // Manually remove all players from scope
            // because the scope checker won't check them any more.
            player.getServer().kickScopes(player);
            return true;
        }
        return false;
    }

    /**
     * Called when this channel is removed from the server.
     */
    protected synchronized void onRemoved() {
        for (var player : players) {
            if (player.getChannel() == this)
                player.setChannel(null);
        }
    }

    @Override
    public String toString() {
        return "WebSpeak Channel (" + name + ")";
    }
}

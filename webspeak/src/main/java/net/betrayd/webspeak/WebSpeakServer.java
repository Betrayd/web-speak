package net.betrayd.webspeak;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.javalin.Javalin;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.player.WebSpeakPlayer;
import net.betrayd.webspeak.player.WebSpeakPlayerData;

/**
 * The primary WebSpeak server
 * @param <T> The player implementation to use
 */
public class WebSpeakServer<T extends WebSpeakPlayer> {

    private Javalin app;
    
    /**
     * All the players that are relevent to the game
     */
    private final Set<WebSpeakPlayerData<T>> players = new HashSet<>();

    /**
     * All the websocket connections, orginized by their "session ID"
     * The session ID is a unique value stored with each player, defined in
     * WebSpeakPlayerData
     */
    private final BiMap<String, WsContext> wsSessions = HashBiMap.create();

    /**
     * Get the base Javalin app
     */
    public Javalin getApp() {
        return app;
    }

    /**
     * Start the server.
     * @param port The port to start on.
     */
    public void start(int port) {
        app = Javalin.create()
                .get("/", ctx -> ctx.result("Hello World: " + ctx))
                .ws("/connect", this::setupWebsocket);
        
        app.start(port);
    }

    private void setupWebsocket(WsConfig ws) {
        ws.onConnect(ctx -> {
            String sessionId = ctx.queryParam("id");
            if (wsSessions.containsKey(sessionId)) {
                ctx.closeSession(WsCloseStatus.POLICY_VIOLATION, "Session " + sessionId + " already has a client connected.");
            }

            var playerData = players.stream().filter(p -> p.getSessionId().equals(sessionId)).findAny();
            if (playerData.isEmpty()) {
                ctx.closeSession(WsCloseStatus.POLICY_VIOLATION, "No session found with ID " + sessionId);
            }

            wsSessions.put(sessionId, ctx);
        });

        ws.onClose(ctx -> {
            wsSessions.inverse().remove(ctx);
        });
    }

    /**
     * Get all the players in a stream.
     * 
     * @return Player stream.
     */
    public Stream<T> streamPlayers() {
        return players.stream().map(WebSpeakPlayerData::getPlayer);
    }

    /**
     * Get an immutable list of all the players in the server.
     * 
     * @return All webspeak players
     */
    public Set<T> getPlayers() {
        return streamPlayers().collect(Collectors.toSet());
    }

    /**
     * Check if a given player is part of the webspeak server.
     * 
     * @param player Player to check for.
     * @return If the player was there.
     */
    public boolean hasPlayer(Object player) {
        if (player == null)
            return false;

        return players.stream().anyMatch(p -> p.getPlayer().equals(player));
    }

    /**
     * Add a player to the webspeak server.
     * 
     * @param player Player to add.
     * @return <code>true</code> if the player was successfully added.
     *         <code>false</code> if it was not because it was already there.
     */
    public boolean addPlayer(T player) {
        if (player == null)
            throw new NullPointerException("player");

        if (hasPlayer(player))
            return false;

        WebSpeakPlayerData<T> playerData = new WebSpeakPlayerData<>(player,
                UUID.randomUUID().toString(), UUID.randomUUID().toString());
        
        players.add(playerData);
        return true;
    }

    /**
     * Remove a player from the webspeak server.
     * 
     * @param player Player to remove.
     * @return If the player was in the webspeak server.
     */
    public boolean removePlayer(Object player) {
        if (player == null)
            return false;

        boolean success = false;
        var iterator = players.iterator();
        WebSpeakPlayerData<T> playerData;
        while (iterator.hasNext()) {

            playerData = iterator.next();
            if (!playerData.getPlayer().equals(player))
                continue;

            WsContext ws = wsSessions.get(playerData.getSessionId());
            if (ws != null) {
                ws.closeSession(WsCloseStatus.NORMAL_CLOSURE, "Player removed from server");
                wsSessions.remove(playerData.getSessionId());
            }

            success = true;
            iterator.remove();
        }

        return success;
    }
}

package net.betrayd.webspeak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.javalin.Javalin;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.RTCManager;

/**
 * The primary WebSpeak server
 * 
 * @param <T> The player implementation to use
 */
public class WebSpeakServer {

    public static interface WebSpeakPlayerFactory<T extends WebSpeakPlayer> {
        public T create(WebSpeakServer server, String playerId, String sessionId);
    }

    private Javalin app;

    /**
     * All the players that are relevent to the game
     */
    private final Set<WebSpeakPlayer> players = new HashSet<>();

    // private final RelationGraph<WebSpeakPlayer> rtcConnections = new RelationGraph<>();
    private final RTCManager rtcManager = new RTCManager(this);

    /**
     * All the websocket connections, orginized by their "session ID"
     * The session ID is a unique value stored with each player, defined in
     * WebSpeakPlayerData
     */
    private final BiMap<WebSpeakPlayer, WsContext> wsSessions = HashBiMap.create();

    /**
     * Get the base Javalin app
     */
    public Javalin getApp() {
        return app;
    }

    /**
     * Start the server.
     * 
     * @param port The port to start on.
     */
    public void start(int port) {
        app = Javalin.create()
                .get("/", ctx -> ctx.result("Hello World: " + ctx))
                .ws("/connect", this::setupWebsocket);

        app.start(port);
    }

    /**
     * ticks the werver, updates connections on distance ETC.
     */
    public synchronized void tick() {
        rtcManager.tickRTC();
    }

    private void setupWebsocket(WsConfig ws) {
        ws.onConnect(ctx -> {
            synchronized(this) {
                System.out.print(Thread.currentThread().getName());
                String sessionId = ctx.queryParam("id");
                WebSpeakPlayer player = playerFromSessionId(sessionId);
                if (player == null) {
                    ctx.closeSession(WsCloseStatus.POLICY_VIOLATION, "No session found with ID " + sessionId);
                    return;
                }
    
                // Will be non-null if something was already there.
                if (wsSessions.putIfAbsent(player, ctx) != null) {
                    ctx.closeSession(WsCloseStatus.POLICY_VIOLATION, "Session " + sessionId + " already has a client connected.");
                }
                
                player.wsContext = ctx;
                wsSessions.put(player, ctx);
            }
        });

        ws.onClose(ctx -> {
            synchronized(this) {
                WebSpeakPlayer player = wsSessions.inverse().remove(ctx);
                if (player != null) {
                    player.wsContext = null;
                }
            }
        });
    }

    /**
     * Get all the players in a stream.
     * 
     * @return Player stream.
     */
    @Deprecated
    public Stream<WebSpeakPlayer> streamPlayers() {
        return players.stream();
    }

    /**
     * Get an immutable list of all the players in the server.
     * 
     * @return All webspeak players
     */
    public Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
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

        return players.contains(player);
        // return players.stream().anyMatch(p -> p.getPlayer().equals(player));
    }
    
    public <T extends WebSpeakPlayer> T addPlayer(WebSpeakPlayerFactory<T> factory) {
        T player = factory.create(this, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        addPlayer(player, true);
        return player;
    }

    /**
     * Add a player to the webspeak server.
     * 
     * @param player Player to add.
     * @return If the player was added. False if it was already there.
     */
    public boolean addPlayer(WebSpeakPlayer player) {
        return addPlayer(player, false);
    }

    protected boolean addPlayer(WebSpeakPlayer player, boolean noCheck) {
        if (player == null) {
            throw new NullPointerException("player");
        }
        if (players.contains(player))
            return false;

        if (!noCheck) {
            if (player.getServer() != this) {
                throw new IllegalArgumentException("player belongs to the wrong server");
            }
            for (WebSpeakPlayer p : players) {
                if (p.getPlayerId().equals(player.getPlayerId()) || p.getSessionId().equals(player.getSessionId())) {
                    throw new IllegalArgumentException("Player must have a unique player ID and session ID.");
                }
            }
        }
        return players.add(player);
    }

    /**
     * Remove a player from the webspeak server.
     * 
     * @param player Player to remove.
     * @return If the player was in the webspeak server.
     */
    public boolean removePlayer(Object player) {
        if (player == null || !(player instanceof WebSpeakPlayer webPlayer))
            return false;

        if (players.remove(webPlayer)) {
            onRemovePlayer(webPlayer);
            return true;
        }
        return false;
    }

    protected void onRemovePlayer(WebSpeakPlayer player) {
        WsContext ws = wsSessions.remove(player);
        if (ws != null) {
            ws.closeSession(WsCloseStatus.NORMAL_CLOSURE, "Player removed from server");
        }
        player.wsContext = null;
    }
    
    private WebSpeakPlayer playerFromSessionId(String sessionId) {
        for (WebSpeakPlayer player : players) {
            if (player.getSessionId().equals(sessionId))
                return player;
        }
        return null;
    }
}

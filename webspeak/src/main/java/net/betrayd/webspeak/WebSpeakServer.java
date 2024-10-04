package net.betrayd.webspeak;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.javalin.Javalin;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakFlags.WebSpeakFlag;
import net.betrayd.webspeak.impl.PlayerCoordinateManager;
import net.betrayd.webspeak.impl.RTCManager;
import net.betrayd.webspeak.impl.WebSpeakFlagHolder;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.WebSpeakNet.UnknownPacketException;
import net.betrayd.webspeak.impl.net.packets.LocalPlayerInfoS2CPacket;
import net.betrayd.webspeak.impl.net.packets.SetPannerOptionsC2SPacket;
import net.betrayd.webspeak.util.PannerOptions;
import net.betrayd.webspeak.util.WebSpeakEvents;
import net.betrayd.webspeak.util.WebSpeakEvents.WebSpeakEvent;

/**
 * The primary WebSpeak server
 * 
 * @param <T> The player implementation to use
 */
public class WebSpeakServer implements Executor {

    public static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakServer.class);

    public static interface WebSpeakPlayerFactory<T extends WebSpeakPlayer> {
        public T create(WebSpeakServer server, String playerId, String sessionId);
    }

    private Javalin app;

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    /**
     * All the players that are relevent to the game
     */
    // private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, WebSpeakPlayer> players = new ConcurrentHashMap<>();

    // private final RelationGraph<WebSpeakPlayer> rtcConnections = new RelationGraph<>();
    private final RTCManager rtcManager = new RTCManager(this);
    private final PlayerCoordinateManager playerCoordinateManager = new PlayerCoordinateManager(this);

    /**
     * All the websocket connections, orginized by their "session ID"
     * The session ID is a unique value stored with each player, defined in
     * WebSpeakPlayerData
     */
    private final BiMap<WebSpeakPlayer, WsContext> wsSessions = HashBiMap.create();

    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_SESSION_CONNECTED = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_SESSION_DISCONNECTED = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_ADDED = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_REMOVED = WebSpeakEvents.createSimple();
    
    private final WebSpeakFlagHolder flagHolder = new WebSpeakFlagHolder();
    
    public <T> T setFlag(WebSpeakFlag<T> flag, T value) {
        return flagHolder.setFlag(flag, value);
    }

    public <T> T getFlag(WebSpeakFlag<T> flag) {
        return flagHolder.getFlag(flag);
    }

    public Map<WebSpeakFlag<?>, Object> getFlags() {
        return flagHolder.getFlags();
    }

    private final PannerOptions pannerOptions = new PannerOptions();

    /**
     * The object used to control the panning config on the client.
     * Make sure to call {@link #updatePannerOptions} after updating.
     */
    public PannerOptions getPannerOptions() {
        return pannerOptions;
    }

    /**
     * Send an updated copy of the panner options to all clients.
     */
    public void updatePannerOptions() {
        WebSpeakNet.sendPacketToPlayers(getPlayers(), SetPannerOptionsC2SPacket.PACKET, pannerOptions);
    }

    /**
     * Get the base Javalin app
     */
    public Javalin getApp() {
        return app;
    }

    public int getPort() {
        return app != null ? app.port() : -1;
    }

    /**
     * Start the server.
     * 
     * @param port The port to start on.
     */
    public synchronized void start(int port) {
        if (app != null) {
            throw new IllegalStateException("Server is already started.");
        }
        app = Javalin.create()
                .get("/", ctx -> ctx.result("Hello World: " + ctx))
                .ws("/connect", this::setupWebsocket);

        app.start(port);
    }

    /**
     * Synchronously shutdown the server instance. Could block for some time.
     */
    public synchronized void stop() {
        if (app == null)
            return;
        for (var player : List.copyOf(players.values())) {
            removePlayer(player);
        }
        app.stop();
    }

    /**
     * ticks the server: updates connections on distance etc.
     */
    public synchronized void tick() {
        if (app == null)
            return;
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }

        rtcManager.tickRTC();
        playerCoordinateManager.tick();
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
                    return;
                }
                
                player.wsContext = ctx;
                wsSessions.put(player, ctx);
                new LocalPlayerInfoS2CPacket(player.getPlayerId()).send(ctx);

                playerCoordinateManager.onPlayerConnected(player);
                ON_SESSION_CONNECTED.invoker().accept(player);
            }

            WebSpeakNet.sendPacket(ctx, SetPannerOptionsC2SPacket.PACKET, pannerOptions);
        });

        ws.onClose(ctx -> {
            WebSpeakPlayer player;
            synchronized(this) {
                player = wsSessions.inverse().remove(ctx);
                if (player != null) {
                    player.wsContext = null;
                    rtcManager.kickRTC(player);
                    ON_SESSION_DISCONNECTED.invoker().accept(player);
                }
            }
            if (player != null)
                LOGGER.info("Player {} disconnected from voice.", player.getPlayerId());
        });

        ws.onMessage(ctx -> {
            WebSpeakPlayer player;
            synchronized(this) {
                player = wsSessions.inverse().get(ctx);
            }
            if (player == null) {
                LOGGER.warn("Recieved WS message from unknown player: " + ctx.message());
                return;
            }
            
            try {
                WebSpeakNet.applyPacket(player, ctx.message());
            } catch (UnknownPacketException e) {
                ctx.closeSession(WsCloseStatus.UNSUPPORTED_DATA, e.getMessage());
                LOGGER.warn("{} sent unknown packet '{}'", player.getPlayerId(), e.getPacketId());
            } catch (Exception e) {
                ctx.closeSession(WsCloseStatus.PROTOCOL_ERROR, e.getMessage());
                LOGGER.warn("{} was disconnected because packet failed to apply.", player.getPlayerId());
                LOGGER.error("Error applying packet: ", e);
            }
        });
    }

    /**
     * Get an unmodifiable collection of all the players in the server.
     * 
     * @return All webspeak players
     */
    public Collection<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /**
     * Count the number of players connected to WebSpeak.
     * @return Number of players.
     */
    public int numPlayers() {
        return players.size();
    }

    /**
     * Get an unmodifiable collection of all the players in the server.
     * @return All webspeak players
     */
    public Map<String, WebSpeakPlayer> getPlayerMap() {
        return Collections.unmodifiableMap(players);
    }

    /**
     * Check if a given player is part of the webspeak server.
     * 
     * @param player Player to check for.
     * @return If the player was there.
     */
    public boolean hasPlayer(Object player) {
        if (player instanceof WebSpeakPlayer webPlayer) {
            return hasPlayerId(webPlayer.getPlayerId());
        } else {
            return false;
        }
    }

    /**
     * Check if a player exists by its ID.
     * @param playerId Player ID to check for.
     * @return If there was a player wsith that ID.
     */
    public boolean hasPlayerId(String playerId) {
        return players.containsKey(playerId);
    }

    /**
     * Get a player by its ID.
     * 
     * @param playerId Player ID to search for.
     * @return Found player. <code>null</code> if no player exists with that ID.
     */
    public WebSpeakPlayer getPlayer(String playerId) {
        return players.get(playerId);
    }

    /**
     * Create and add a webspeak player, assigning it a random player ID and session
     * ID.
     * 
     * @param <T>     Player type.
     * @param factory Player factory function.
     * @return The newly-created factory.
     */
    public <T extends WebSpeakPlayer> T addPlayer(WebSpeakPlayerFactory<T> factory) {
        T player = factory.create(this, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        addPlayer(player, true);
        return player;
    }

    /**
     * Add a player to the webspeak server, replacing any player that already exists
     * with that ID.
     * 
     * @param player Player to add.
     * @return The previous player with that ID, if any.
     */
    public WebSpeakPlayer addPlayer(WebSpeakPlayer player) {
        return addPlayer(player, false);
    }

    protected WebSpeakPlayer addPlayer(WebSpeakPlayer player, boolean noCheck) {
        if (player == null) throw new NullPointerException("player");
        WebSpeakPlayer old;
        if (!noCheck) {
            if (player.getServer() != this) {
                throw new IllegalArgumentException("Player belongs to the wrong server!");
            }

            synchronized(this) {
                if (players.values().stream().anyMatch(p -> p.getSessionId().equals(player.getSessionId()))) {
                    throw new IllegalArgumentException("A player already exists with this session ID!");
                }
                old = players.put(player.getPlayerId(), player);
            }
        } else {
            old = players.put(player.getPlayerId(), player);
        }

        if (old != null) {
            onRemovePlayer(player);
        }
        ON_PLAYER_ADDED.invoker().accept(player);
        return old;
    }

    /**
     * Remove a player from the webspeak server.
     * 
     * @param player Player to remove.
     * @return If the player was in the webspeak server.
     */
    public boolean removePlayer(Object player) {
        if (player instanceof WebSpeakPlayer webPlayer) {
            return removePlayer(webPlayer.getPlayerId()) != null;
        } else {
            return false;
        }
    }

    /**
     * Remove a player from the webspeak server.
     * 
     * @param playerId ID of the player to remove.
     * @return <code>WebSpeakPlayer</code> instance of the player that was removed.
     *         <code>null</code> if there was no player with that ID.
     */
    public WebSpeakPlayer removePlayer(String playerId) {
        WebSpeakPlayer player = players.remove(playerId);
        if (player != null) {
            onRemovePlayer(player);
        }
        return player;
    }

    protected void onRemovePlayer(WebSpeakPlayer player) {
        WsContext ws = wsSessions.remove(player);
        if (ws != null) {
            ws.closeSession(WsCloseStatus.NORMAL_CLOSURE, "Player removed from server");
            ON_SESSION_DISCONNECTED.invoker().accept(player);
        }
        rtcManager.kickRTC(player);
        player.wsContext = null;
        ON_PLAYER_REMOVED.invoker().accept(player);
    }
    
    private WebSpeakPlayer playerFromSessionId(String sessionId) {
        for (WebSpeakPlayer player : players.values()) {
            if (player.getSessionId().equals(sessionId))
                return player;
        }
        return null;
    }

    public final void onSessionConnected(Consumer<WebSpeakPlayer> listener) {
        ON_SESSION_CONNECTED.addListener(listener);
    }

    public final void onSessionDisconnected(Consumer<WebSpeakPlayer> listener) {
        ON_SESSION_DISCONNECTED.addListener(listener);
    }

    public final void onPlayerAdded(Consumer<WebSpeakPlayer> listener) {
        ON_PLAYER_ADDED.addListener(listener);
    }

    public final void onPlayerRemoved(Consumer<WebSpeakPlayer> listener) {
        ON_PLAYER_REMOVED.addListener(listener);
    }

    /**
     * Queue a task to be run at the beginning of the next tick.
     * @param command Task to queue.
     */
    @Override
    public void execute(Runnable command) {
        tasks.add(command);
    }
}

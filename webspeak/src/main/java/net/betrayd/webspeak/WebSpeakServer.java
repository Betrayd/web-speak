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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakFlags.WebSpeakFlag;
import net.betrayd.webspeak.impl.PlayerCoordinateManager;
import net.betrayd.webspeak.impl.RTCManager;
import net.betrayd.webspeak.impl.RelationGraph;
import net.betrayd.webspeak.impl.WebSpeakFlagHolder;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.WebSpeakNet.UnknownPacketException;
import net.betrayd.webspeak.impl.net.packets.LocalPlayerInfoS2CPacket;
import net.betrayd.webspeak.impl.net.packets.SetPannerOptionsC2SPacket;
import net.betrayd.webspeak.impl.util.WebSpeakUtils;
import net.betrayd.webspeak.util.PannerOptions;
import net.betrayd.webspeak.util.WebSpeakEvents;
import net.betrayd.webspeak.util.WebSpeakEvents.WebSpeakEvent;

/**
 * The primary WebSpeak server
 * 
 * @param <T> The player implementation to use
 */
public class WebSpeakServer implements Executor {
    // This is a bit of a monoclass lol, but I think it's okay.

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
    private final ChannelManager channelManager = new ChannelManager(this);

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
     * Keep track of all players in scope with each other.
     * @apiNote Only players that have a client connected are considered for scope.
     */
    private final RelationGraph<WebSpeakPlayer> scopes = new RelationGraph<>();

    /**
     * All the websocket connections, orginized by their "session ID"
     * The session ID is a unique value stored with each player, defined in
     * WebSpeakPlayerData
     */
    private final BiMap<WebSpeakPlayer, WsContext> wsSessions = HashBiMap.create();

    protected final WebSpeakEvent<Consumer<JavalinConfig>> CONFIGURE_JAVALIN = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<Javalin>> CONFIGURE_ENDPOINTS = WebSpeakEvents.createSimple();

    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_SESSION_CONNECTED = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_SESSION_DISCONNECTED = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_ADDED = WebSpeakEvents.createSimple();
    protected final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_REMOVED = WebSpeakEvents.createSimple();

    protected final WebSpeakEvent<BiConsumer<WebSpeakPlayer, WebSpeakPlayer>> ON_JOIN_SCOPE = WebSpeakEvents.createBiConsumer();
    protected final WebSpeakEvent<BiConsumer<WebSpeakPlayer, WebSpeakPlayer>> ON_LEAVE_SCOPE = WebSpeakEvents.createBiConsumer();
    
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
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            CONFIGURE_JAVALIN.invoker().accept(config);
        })
                .get("/", ctx -> ctx.result("Hello World: " + ctx))
                .ws("/connect", this::setupWebsocket);

        CONFIGURE_ENDPOINTS.invoker().accept(app);
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
     * Add a callback to add additional config options to Javalin.
     * @param callback Config callback.
     */
    public void configureJavalin(Consumer<JavalinConfig> callback) {
        CONFIGURE_JAVALIN.addListener(callback);
    }

    /**
     * Add a callback to add additional endpoints to the web server.
     * @param callback Callback to add endpoints.
     */
    public void configureEndpoints(Consumer<Javalin> callback) {
        CONFIGURE_ENDPOINTS.addListener(callback);
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

        // rtcManager.tickRTC();
        tickScopes();
        playerCoordinateManager.tick();
    }

    private void tickScopes() {
        List<WebSpeakPlayer> connectedPlayers = getPlayers().stream().filter(p -> p.isConnected()).toList();

        for (var pair : WebSpeakUtils.compareAll(connectedPlayers)) {
            if (pair.a().equals(pair.b())) {
                continue;
            }

            boolean wasInScope = scopes.containsRelation(pair.a(), pair.b());
            boolean isInScope = pair.a().isInScope(pair.b());

            if (!wasInScope && isInScope) {
                scopes.add(pair.a(), pair.b());
                joinScope(pair.a(), pair.b());
            } else if (wasInScope && !isInScope) {
                leaveScope(pair.a(), pair.b());
                scopes.remove(pair.a(), pair.b());
            }
        }
    }

    /**
     * Remove the player from the scope of all other players. Useful if player was
     * removed or disconnected.
     * 
     * @param player Player to kick.
     */
    protected synchronized void kickScopes(WebSpeakPlayer player) {
        for (var other : scopes.getRelations(player)) {
            leaveScope(player, other);
        }
        scopes.removeAll(player);
    }

    private void joinScope(WebSpeakPlayer a, WebSpeakPlayer b) {
        rtcManager.connectRTC(a, b);
        a.onJoinedScope(b);
        b.onJoinedScope(a);
        ON_JOIN_SCOPE.invoker().accept(a, b);
    }

    private void leaveScope(WebSpeakPlayer a, WebSpeakPlayer b) {
        rtcManager.disconnectRTC(a, b);
        a.onLeftScope(b);
        b.onLeftScope(a);
        ON_LEAVE_SCOPE.invoker().accept(a, b);
    }

    /**
     * Check if two players are in scope with each other.
     * 
     * @param a Player A.
     * @param b Player B.
     * @return If the players are in scope.
     * @apiNote Players can only be considered for scope when they have a web client
     *          connected.
     * @implNote This doesn't actually re-query the players; it simply checks if the
     *           scope manager thinks they're in scope.
     */
    public final boolean areInScope(WebSpeakPlayer a, WebSpeakPlayer b) {
        return (a == b) || scopes.containsRelation(a, b);
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
                    kickScopes(player);
                    player.wsContext = null;
                    // rtcManager.kickRTC(player);
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
    public <T extends WebSpeakPlayer> T createPlayer(WebSpeakPlayerFactory<T> factory) {
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
        onAddPlayer(player);
        return old;
    }

    /**
     * Create and add a webspeak player if one does not already exist with a given
     * ID.
     * 
     * @param playerID Player ID to use.
     * @param factory  Player factory.
     * @return The existing or new player instance.
     */
    public WebSpeakPlayer getOrCreatePlayer(String playerID, WebSpeakPlayerFactory<?> factory) {
        boolean added = false;
        WebSpeakPlayer player;
        synchronized(this) {
            player = players.get(playerID);
            if (player == null) {
                player = factory.create(this, playerID, UUID.randomUUID().toString());
                players.put(playerID, player);
                onAddPlayer(player);
                added = true;
            };
        }

        // Call this outside the synchronized block to reduce chance of deadlock.
        if (added) {
            onAddPlayer(player);
        }
        return player;
    }

    private void onAddPlayer(WebSpeakPlayer player) {
        ON_PLAYER_ADDED.invoker().accept(player);
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
        kickScopes(player);
        if (ws != null) {
            ws.closeSession(WsCloseStatus.NORMAL_CLOSURE, "Player removed from server");
            ON_SESSION_DISCONNECTED.invoker().accept(player);
        }
        // rtcManager.kickRTC(player);
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

    /**
     * Called when a web client connects.
     * @param listener Event listener
     */
    public final void onSessionConnected(Consumer<WebSpeakPlayer> listener) {
        ON_SESSION_CONNECTED.addListener(listener);
    }

    /**
     * Called when a web client disconnects.
     * @param listener Event listener
     */
    public final void onSessionDisconnected(Consumer<WebSpeakPlayer> listener) {
        ON_SESSION_DISCONNECTED.addListener(listener);
    }

    /**
     * Called when a player is added to WebSpeak
     * @param listener Event listener
     */
    public final void onPlayerAdded(Consumer<WebSpeakPlayer> listener) {
        ON_PLAYER_ADDED.addListener(listener);
    }

    /**
     * Called when a player is removed from WebSpeak.
     * @param listener Event listener
     */
    public final void onPlayerRemoved(Consumer<WebSpeakPlayer> listener) {
        ON_PLAYER_REMOVED.addListener(listener);
    }

    /**
     * Called when two players join each other's scope.
     * @param listener Event listener
     */
    public final void onJoinScope(BiConsumer<WebSpeakPlayer, WebSpeakPlayer> listener) {
        ON_JOIN_SCOPE.addListener(listener);
    }

    /**
     * Called when two players leave each other's scope.
     * @param listener Event listener
     */
    public final void onLeaveScope(BiConsumer<WebSpeakPlayer, WebSpeakPlayer> listener) {
        ON_LEAVE_SCOPE.addListener(listener);
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

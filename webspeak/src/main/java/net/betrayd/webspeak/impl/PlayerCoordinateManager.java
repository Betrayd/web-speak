package net.betrayd.webspeak.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.util.WebSpeakVector;

public class PlayerCoordinateManager {
    private static record WebSpeakTransform(WebSpeakVector pos, WebSpeakVector rot) {
    };

    private final WebSpeakServer server;
    private final WeakHashMap<WebSpeakPlayer, WebSpeakTransform> prevTransforms = new WeakHashMap<>();

    public PlayerCoordinateManager(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    public void tick() {
        Set<WebSpeakPlayer> players = server.getPlayers();
        Map<WebSpeakPlayer, WebSpeakTransform> newTransforms = new HashMap<>();
        // List<WebSpeakPlayer> dirtyTransforms = new ArrayList<>(players.size());

        // Iterate through players and mark transforms dirty as needed
        for (WebSpeakPlayer player : players) {
            WebSpeakTransform transform = new WebSpeakTransform(player.getLocation(), player.getRotation());
            WebSpeakTransform prevTransform = prevTransforms.get(player);

            if (prevTransform == null || !transform.equals(prevTransform)) {
                newTransforms.put(player, transform);
            }
        }
        prevTransforms.putAll(newTransforms); // Update prev transforms with new transforms
        
        // Actually send transform updates
        for (WebSpeakPlayer player : players) {
            // TODO: Better packet sending system
            WsContext ws = player.getWsContext();
            if (ws == null)
                continue;

            for (var entry : newTransforms.entrySet()) {
                WebSpeakPlayer key = entry.getKey();
                WebSpeakTransform transform = entry.getValue();

                ws.send(String.format(
                        "{\"type\":\"updateTransform\",\"player\":\"%s\",\"pos\":[%f,%f,%f],\"rot\":[%f,%f,%f]}",
                        key.getPlayerId(),
                        transform.pos.x(), transform.pos.y(), transform.pos.z(),
                        transform.rot.x(), transform.rot.y(), transform.rot.z()));
            }
        }
    }
}

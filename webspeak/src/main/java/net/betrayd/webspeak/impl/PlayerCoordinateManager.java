package net.betrayd.webspeak.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.packets.UpdateTransformS2CPacket;
import net.betrayd.webspeak.util.WebSpeakVector;

public class PlayerCoordinateManager {
    private static record WebSpeakTransform(WebSpeakVector pos, WebSpeakVector forward, WebSpeakVector up) {
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
        Collection<WebSpeakPlayer> players = server.getPlayers();
        Map<WebSpeakPlayer, WebSpeakTransform> newTransforms = new HashMap<>();

        // Iterate through players and mark transforms dirty as needed
        for (WebSpeakPlayer player : players) {
            WebSpeakTransform transform = new WebSpeakTransform(player.getLocation(), player.getForward(), player.getUp());
            WebSpeakTransform prevTransform = prevTransforms.get(player);

            if (prevTransform == null || !transform.equals(prevTransform)) {
                newTransforms.put(player, transform);
            }
        }
        prevTransforms.putAll(newTransforms); // Update prev transforms with new transforms
        
        for (var entry : newTransforms.entrySet()) {
            sendPlayerTransform(entry.getKey(), entry.getValue(), players);
        }
    }

    public void sendPlayerTransform(WebSpeakPlayer player, Iterable<? extends WebSpeakPlayer> targets) {
        sendPlayerTransform(player, new WebSpeakTransform(player.getLocation(), player.getForward(), player.getUp()),  targets);
    }

    private void sendPlayerTransform(WebSpeakPlayer player, WebSpeakTransform transform,
            Iterable<? extends WebSpeakPlayer> targets) {

        String packet = WebSpeakNet.writePacket(UpdateTransformS2CPacket.PACKET,
                UpdateTransformS2CPacket.fromPlayer(player));

        for (var target : targets) {
            WsContext ws = target.getWsContext();
            if (ws == null || !target.isInScope(player))
                continue;
            
            ws.send(packet);
        }
    }

    public void onPlayerConnected(WebSpeakPlayer player) {
        Collection<WebSpeakPlayer> collectionWrapper = Collections.singleton(player);
        for (WebSpeakPlayer otherPlayer : getServer().getPlayers()) {
            sendPlayerTransform(otherPlayer, collectionWrapper);
        }
    }
}

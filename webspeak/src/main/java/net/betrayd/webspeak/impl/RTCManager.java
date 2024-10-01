package net.betrayd.webspeak.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakFlags;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.packets.RTCPackets;
import net.betrayd.webspeak.impl.net.packets.RTCPackets.RequestOfferS2CPacket;

public class RTCManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak RTC Manager");

    private final WebSpeakServer server;

    private final RelationGraph<WebSpeakPlayer> connections = new RelationGraph<>();

    public RTCManager(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    public void tickRTC() {

        List<WebSpeakPlayer> connectedPlayers = server.getPlayers()
                .stream().filter(p -> p.getWsContext() != null).toList();
        
        for (var pair : Util.compareAll(connectedPlayers)) {
            if (pair.a().equals(pair.b()))
                continue;
            
            boolean isConnected = connections.containsRelation(pair.a(), pair.b());
            boolean isInScope = pair.a().isInScope(pair.b());

            if (!isConnected && isInScope) {
                connectRTC(pair.a(), pair.b());
                connections.add(pair.a(), pair.b());
            } else if (isConnected && !isInScope) {
                disconnectRTC(pair.a(), pair.b());
                connections.remove(pair.a(), pair.b());
            }
        }
    }
    
    private void connectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS)) {
            LOGGER.info("Requesting player {} to RTC offer to {}", a.getPlayerId(), b.getPlayerId());
        }
        WebSpeakNet.sendPacket(a.getWsContext(), RTCPackets.REQUEST_OFFER_S2C, new RequestOfferS2CPacket(b.getPlayerId()));
        connections.add(a, b);
    }

    private void disconnectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS)) {
            LOGGER.info("Requesting player {} to disconnect RTC with {}", a.getPlayerId(), b.getPlayerId());
        }
        RTCPackets.DISCONNECT_RTC_S2C.send(a.getWsContext(), new RequestOfferS2CPacket(b.getPlayerId()));
        RTCPackets.DISCONNECT_RTC_S2C.send(b.getWsContext(), new RequestOfferS2CPacket(a.getPlayerId()));
        connections.remove(a, b);
    }

    public void kickRTC(WebSpeakPlayer player) {
        for (var other : connections.getRelations(player)) {
            WsContext otherWs = other.getWsContext();
            if (otherWs == null)
                continue;
            WebSpeakNet.sendPacket(otherWs, RTCPackets.DISCONNECT_RTC_S2C, new RequestOfferS2CPacket(player.getPlayerId()));
        }
        connections.removeAll(player);
    }
}

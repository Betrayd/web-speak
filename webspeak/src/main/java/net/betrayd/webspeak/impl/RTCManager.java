package net.betrayd.webspeak.impl;

import java.util.List;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.packets.RTCPackets;
import net.betrayd.webspeak.impl.net.packets.RTCPackets.RequestOfferS2CPacket;

public class RTCManager {
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
        WebSpeakNet.sendPacket(a.getWsContext(), RTCPackets.REQUEST_OFFER_S2C, new RequestOfferS2CPacket(b.getPlayerId()));
        connections.add(a, b);
    }

    private void disconnectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        WebSpeakNet.sendPacket(a.getWsContext(), RTCPackets.DISCONNECT_RTC_S2C, new RequestOfferS2CPacket(b.getPlayerId()));
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

package net.betrayd.webspeak.impl;

import java.util.HashSet;
import java.util.Set;

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

        Set<WebSpeakPlayer> untestedPlayers = new HashSet<>(server.numPlayers());
        for (var player : server.getPlayers()) {
            if (player.getWsContext() != null) {
                untestedPlayers.add(player);
            }
        }

        for (WebSpeakPlayer player1 : server.getPlayers()) {
            WsContext player1Context = player1.getWsContext();
            if (player1Context == null)
                continue;
            
            for (WebSpeakPlayer player2 : untestedPlayers) {
                if (connections.containsRelation(player1, player2) && !player1.isInScope(player2)) {

                }
            }
        }

        // WebSpeakPlayer player1;
        // WebSpeakPlayer player2;

        for (WebSpeakPlayer player1 : server.getPlayers()) {
            untestedPlayers.remove(player1);
            for (WebSpeakPlayer player2 : untestedPlayers) {
                WsContext player1Context = player1.getWsContext();

                if (player1Context == null)
                    continue;
                
                if (connections.containsRelation(player1, player2) && !player1.isInScope(player2)) {
                    // Disconnect
                    player1Context.send("{type:disconnectRequest," + "data:" + player2.getPlayerId() + "}");
                    connections.remove(player1, player2);
                } else if (!connections.containsRelation(player1, player2) && player1.isInScope(player2)) {
                    // Connect
                    WebSpeakNet.sendPacket(player1Context, RTCPackets.REQUEST_OFFER_S2C,
                            new RequestOfferS2CPacket(player2.getPlayerId()));
                    connections.add(player1, player2);
                }
                
            }

        }
    }
    
}

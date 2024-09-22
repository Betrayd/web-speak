package net.betrayd.webspeak.impl;

import java.util.HashSet;
import java.util.Set;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.net.RTCOfferPacket;
import net.betrayd.webspeak.net.WebSpeakNet;

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
        Set<WebSpeakPlayer> untestedPlayers = new HashSet<>(server.getPlayers());

        // WebSpeakPlayer player1;
        // WebSpeakPlayer player2;

        for (WebSpeakPlayer player1 : server.getPlayers()) {
            untestedPlayers.remove(player1);

            for (WebSpeakPlayer player2 : untestedPlayers) {
                WsContext player1Context = player1.getWsContext();

                if (player1Context == null || player2.getWsContext() == null)
                    continue;

                System.out.println("REEEEEEEEEEEEEEEEEEE");
                if (connections.containsRelation(player1, player2) && !player1.isInScope(player2)) {
                    player1Context.send("{type:disconnectRequest," + "data:" + player2.getPlayerId() + "}");
                    connections.remove(player1, player2);
                } else if (!connections.containsRelation(player1, player2) && player1.isInScope(player2)) {
                    player1Context
                            .send(WebSpeakNet.writePacket(RTCOfferPacket.REQUEST_OFFER_PACKET, player2.getPlayerId()));
                    connections.add(player1, player2);
                }

            }
        }
    }

}

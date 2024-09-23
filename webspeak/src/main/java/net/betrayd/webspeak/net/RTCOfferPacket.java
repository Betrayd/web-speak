package net.betrayd.webspeak.net;

import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import net.betrayd.webspeak.net.PacketType.WriteOnlyJsonPacketType;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.net.PacketType.SimpleJsonPacketType;
import net.betrayd.webspeak.net.PacketType.WriteOnlyStringPacketType;

public record RTCOfferPacket(String playerID, JsonElement rtcSessionDescription) {
    public static final PacketType<String> REQUEST_OFFER_PACKET = new WriteOnlyStringPacketType();

    public static final PacketType<RTCOfferPacket> HAND_OFFER_PACKET = new WriteOnlyJsonPacketType<>(
            WebSpeakNet.GSON, RTCOfferPacket.class);

    public static final PacketType<RTCOfferPacket> RETURN_OFFER_PACKET = new SimpleJsonPacketType<>(
        WebSpeakNet.GSON, RTCOfferPacket.class, (player, packet) -> {
                LoggerFactory.getLogger("owo").info("packet: {}", packet);
                for(WebSpeakPlayer p : player.getServer().getPlayers())
                {
                        System.out.println("loopPlayerID: " + p.getPlayerId());
                        if(p.getPlayerId().equals(packet.playerID))
                        {
                                System.out.println("correctID here take relation graph: " + player.getServer().getRtcManager().inRTCAttemptOrCall(player, p));
                        }
                        if(p.getPlayerId().equals(packet.playerID) && player.getServer().getRtcManager().inRTCAttemptOrCall(player, p))
                        {
                                System.out.println("testDebug");
                                p.getWsContext().send(WebSpeakNet.writePacket(RTCOfferPacket.HAND_OFFER_PACKET, new RTCOfferPacket(player.getPlayerId(), packet.rtcSessionDescription)));
                        }
                }
        });

    public static final PacketType<RTCOfferPacket> HAND_ANSWER_PACKET = new WriteOnlyJsonPacketType<>(
            WebSpeakNet.GSON, RTCOfferPacket.class);

    public static final PacketType<RTCOfferPacket> RETURN_ANSWER_PACKET = new SimpleJsonPacketType<>(
        WebSpeakNet.GSON, RTCOfferPacket.class, (player, packet) -> {
                System.out.println("packet data: (sendingPlayerID: " + player.getPlayerId() + ", data: " + packet + ")");
                for(WebSpeakPlayer p : player.getServer().getPlayers())
                {
                        if(p.getPlayerId().equals(packet.playerID) && player.getServer().getRtcManager().inRTCAttemptOrCall(player, p))
                        {
                                System.out.println("testDebug2");
                                p.getWsContext().send(WebSpeakNet.writePacket(RTCOfferPacket.HAND_ANSWER_PACKET, new RTCOfferPacket(player.getPlayerId(), packet.rtcSessionDescription)));
                        }
                }
        });

        public static final PacketType<RTCOfferPacket> HAND_ICE_PACKET = new WriteOnlyJsonPacketType<>(
                WebSpeakNet.GSON, RTCOfferPacket.class);

        public static final PacketType<RTCOfferPacket> RETURN_ICE_PACKET = new SimpleJsonPacketType<>(
                WebSpeakNet.GSON, RTCOfferPacket.class, (player, packet) -> {
                        System.out.println("packet data: (sendingPlayerID: " + player.getPlayerId() + ", data: " + packet + ")");
                        for(WebSpeakPlayer p : player.getServer().getPlayers())
                        {
                                if(p.getPlayerId().equals(packet.playerID) && player.getServer().getRtcManager().inRTCAttemptOrCall(player, p))
                                {
                                        p.getWsContext().send(WebSpeakNet.writePacket(RTCOfferPacket.HAND_ICE_PACKET, new RTCOfferPacket(player.getPlayerId(), packet.rtcSessionDescription)));
                                }
                        }
                });
}

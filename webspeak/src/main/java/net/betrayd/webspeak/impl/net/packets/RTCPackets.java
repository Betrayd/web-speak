package net.betrayd.webspeak.impl.net.packets;

import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.impl.net.C2SPacket;
import net.betrayd.webspeak.impl.net.C2SPacket.JsonC2SPacket;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.impl.net.S2CPacket.JsonS2CPacket;
import net.betrayd.webspeak.impl.net.WebSpeakNet;

public class RTCPackets {
    public static record RTCPacketData(String playerID, JsonElement payload) {};
    public static record RequestOfferS2CPacket(String playerID) {};

    public static final S2CPacket<RequestOfferS2CPacket> REQUEST_OFFER_S2C = new JsonS2CPacket<>("requestOffer");
    public static final S2CPacket<RequestOfferS2CPacket> DISCONNECT_RTC_S2C = new JsonS2CPacket<>("disconnectRTC");
    
    public static final S2CPacket<RTCPacketData> HAND_OFFER_S2C = new JsonS2CPacket<>("handOffer");
    public static final S2CPacket<RTCPacketData> HAND_ANSWER_S2C = new JsonS2CPacket<>("handAnswer");

    public static final S2CPacket<RTCPacketData> HAND_ICE_S2C = new JsonS2CPacket<>("handIce");
    
    public static final C2SPacket<RTCPacketData> RETURN_OFFER_C2S = new JsonC2SPacket<>(
            RTCPacketData.class, RTCPackets::onReturnOffer);

    public static final C2SPacket<RTCPacketData> RETURN_ANSWER_C2S = new JsonC2SPacket<>(
            RTCPacketData.class, RTCPackets::onReturnAnswer);

    public static final C2SPacket<RTCPacketData> RETURN_ICE_C2S = new JsonC2SPacket<>(
            RTCPacketData.class, RTCPackets::onReturnIce);

    private static void onReturnOffer(WebSpeakPlayer player, RTCPacketData data) {
        WebSpeakPlayer targetPlayer = player.getServer().getPlayer(data.playerID);
        // TODO: Do we want to do something other than disconnect the player as a result of the exception?

        if (targetPlayer == null) {
            throw new IllegalArgumentException("Unknown player: " + data.playerID);
        }
        
        WsContext targetWs = targetPlayer.getWsContext();
        if (targetWs == null) {
            LoggerFactory.getLogger(RTCPackets.class).warn("Tried to send rtc offer from {} to disconnected player {}",
                    player.getPlayerId(), targetPlayer.getPlayerId());
            return;
        }

        // Hand offer to other client, giving it the source player's ID.
        WebSpeakNet.sendPacket(targetWs, HAND_OFFER_S2C, new RTCPacketData(player.getPlayerId(), data.payload));
    }

    private static void onReturnAnswer(WebSpeakPlayer player, RTCPacketData data) {
        WebSpeakPlayer targetPlayer = player.getServer().getPlayer(data.playerID);
        
        if (targetPlayer == null) {
            throw new IllegalArgumentException("Unknown player: " + data.playerID);
        }
        
        WsContext targetWs = targetPlayer.getWsContext();
        if (targetWs == null) {
            LoggerFactory.getLogger(RTCPackets.class).warn("Tried to send rtc offer from {} to disconnected player {}",
                    player.getPlayerId(), targetPlayer.getPlayerId());
            return;
        }

        WebSpeakNet.sendPacket(targetWs, HAND_ANSWER_S2C, new RTCPacketData(player.getPlayerId(), data.payload));
    }

    private static void onReturnIce(WebSpeakPlayer player, RTCPacketData data) {
        WebSpeakPlayer targetPlayer = player.getServer().getPlayer(data.playerID);
        
        if (targetPlayer == null) {
            throw new IllegalArgumentException("Unknown player: " + data.playerID);
        }

        WsContext targetWs = targetPlayer.getWsContext();
        if (targetWs == null) {
            LoggerFactory.getLogger(RTCPackets.class).warn("Tried to send ice response from {} to disconnected player {}",
                    player.getPlayerId(), targetPlayer.getPlayerId());
            return;
        }

        WebSpeakNet.sendPacket(targetWs, HAND_ICE_S2C, new RTCPacketData(player.getPlayerId(), data.payload));
    }
}

package net.betrayd.webspeak.net;

import com.google.gson.JsonObject;

import net.betrayd.webspeak.net.PacketType.WriteOnlyJsonPacketType;
import net.betrayd.webspeak.net.PacketType.WriteOnlyStringPacketType;

public record RTCOfferPacket(String stringId, JsonObject rtcSessionDescription) {
    public static final PacketType<String> REQUEST_OFFER_PACKET = new WriteOnlyStringPacketType();

    public static final PacketType<RTCOfferPacket> HAND_OFFER_PACKET = new WriteOnlyJsonPacketType<>(
            WebSpeakNet.GSON, RTCOfferPacket.class);

    public static final PacketType<RTCOfferPacket> HAND_ANSWER_PACKET = new WriteOnlyJsonPacketType<>(
            WebSpeakNet.GSON, RTCOfferPacket.class);
}

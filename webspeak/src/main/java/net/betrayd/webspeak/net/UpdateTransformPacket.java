package net.betrayd.webspeak.net;

import net.betrayd.webspeak.net.PacketType.WriteOnlyJsonPacketType;
import net.betrayd.webspeak.util.WebSpeakVector;

public record UpdateTransformPacket(String playerID, WebSpeakVector pos, WebSpeakVector rot) {
    public static final PacketType<UpdateTransformPacket> TYPE = new WriteOnlyJsonPacketType<>(WebSpeakNet.GSON, UpdateTransformPacket.class);
}

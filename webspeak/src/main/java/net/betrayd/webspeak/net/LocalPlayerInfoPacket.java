package net.betrayd.webspeak.net;

import net.betrayd.webspeak.net.PacketType.WriteOnlyJsonPacketType;

public record LocalPlayerInfoPacket(String playerId) {
    public static final PacketType<LocalPlayerInfoPacket> TYPE = new WriteOnlyJsonPacketType<>(WebSpeakNet.GSON,
            LocalPlayerInfoPacket.class);
}

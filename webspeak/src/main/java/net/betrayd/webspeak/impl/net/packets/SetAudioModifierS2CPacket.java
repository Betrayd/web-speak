package net.betrayd.webspeak.impl.net.packets;

import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.util.AudioModifier;

public record SetAudioModifierS2CPacket(String playerID, AudioModifier audioModifier) {
    public static final S2CPacket<SetAudioModifierS2CPacket> PACKET = new S2CPacket.JsonS2CPacket<>("setAudioModifier");

    public void send(PlayerConnection connection) {
        connection.sendPacket(PACKET, this);
    }
}

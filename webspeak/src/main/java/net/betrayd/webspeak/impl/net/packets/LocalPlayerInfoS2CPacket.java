package net.betrayd.webspeak.impl.net.packets;

import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.impl.net.S2CPacket.JsonS2CPacket;

public record LocalPlayerInfoS2CPacket(String playerID) {
    public static final S2CPacket<LocalPlayerInfoS2CPacket> PACKET = new JsonS2CPacket<>("localPlayerInfo");

    public void send(PlayerConnection connection) {
        connection.sendPacket(PACKET, this);
    }
}

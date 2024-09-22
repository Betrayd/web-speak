package net.betrayd.webspeak.impl.net.packets;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.impl.net.S2CPacket.JsonS2CPacket;
import net.betrayd.webspeak.util.WebSpeakVector;

public record UpdateTransformS2CPacket(String playerId, WebSpeakVector pos, WebSpeakVector rot) {
    public static final S2CPacket<UpdateTransformS2CPacket> PACKET = new JsonS2CPacket<>("updateTransform");

    public static UpdateTransformS2CPacket fromPlayer(WebSpeakPlayer player) {
        return new UpdateTransformS2CPacket(player.getPlayerId(), player.getLocation(), player.getRotation());
    }
}

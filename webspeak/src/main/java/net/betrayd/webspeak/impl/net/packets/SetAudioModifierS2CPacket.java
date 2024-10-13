package net.betrayd.webspeak.impl.net.packets;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.util.AudioModifier;

public record SetAudioModifierS2CPacket(String playerID, AudioModifier audioModifier) {
    public static final S2CPacket<SetAudioModifierS2CPacket> PACKET = new S2CPacket.JsonS2CPacket<>("setAudioModifier");

    public void send(WsContext context) {
        PACKET.send(context, this);
    }
}

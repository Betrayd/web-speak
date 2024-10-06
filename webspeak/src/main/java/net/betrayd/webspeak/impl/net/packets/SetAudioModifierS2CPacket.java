package net.betrayd.webspeak.impl.net.packets;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.util.AudioModifier;

public record SetAudioModifierS2CPacket(String playerID, AudioModifier modifier) {
    public static final S2CPacket<SetAudioModifierS2CPacket> PACKET = S2CPacket.json("setAudioModifier");

    public void send(WsContext ws) {
        PACKET.send(ws, this);
    }
}

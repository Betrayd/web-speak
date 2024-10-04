package net.betrayd.webspeak.impl.net.packets;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.util.PannerOptions;
import net.betrayd.webspeak.impl.net.S2CPacket.JsonS2CPacket;

/**
 * Sets the default panner options to use on the client.
 */
public class SetPannerOptionsC2SPacket {
    public static final S2CPacket<PannerOptions> PACKET = new JsonS2CPacket<>("setPannerOptions");

    public static void send(WsContext ws, PannerOptions payload) {
        WebSpeakNet.sendPacket(ws, PACKET, payload);
    }
}

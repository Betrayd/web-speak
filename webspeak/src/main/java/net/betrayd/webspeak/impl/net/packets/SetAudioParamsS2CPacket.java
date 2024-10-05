package net.betrayd.webspeak.impl.net.packets;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.S2CPacket.JsonS2CPacket;
import net.betrayd.webspeak.util.PannerOptions;

/**
 * Tells the client to override the audio parameters for a given player. Should be
 * sent every time the player enters scope.
 */
public record SetAudioParamsS2CPacket(String playerID, boolean spatialize, boolean mute, boolean overridePanner, PannerOptions.Partial pannerOptions) {
    public static final S2CPacket<SetAudioParamsS2CPacket> PACKET = new JsonS2CPacket<>("setAudioParams");

    public SetAudioParamsS2CPacket(String playerID, PannerOptions.Partial pannerOptions) {
        this(playerID, true, false, true, pannerOptions);
    }

    public SetAudioParamsS2CPacket(String playerID, boolean spatialize, boolean mute) {
        this(playerID, spatialize, mute, false, null);
    }

    public void send(WsContext player) {
        WebSpeakNet.sendPacket(player, PACKET, this);
    }
}

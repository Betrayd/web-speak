package net.betrayd.webspeak.impl.relay.packets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.impl.net.C2SPacket;
import net.betrayd.webspeak.impl.net.C2SPacket.JsonC2SPacket;
import net.betrayd.webspeak.impl.relay.PlayerRelayConnection;

public class RelayC2SPackets {

    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak RelayC2SPackets");

    public static record RelayDisconnectData(int statusCode, String reason) {
    };

    public static final C2SPacket<String> RELAY_CLIENT_CONNECTED_C2S_PACKET = new JsonC2SPacket<>(String.class, RelayC2SPackets::onClientConnected);
    public static final C2SPacket<RelayDisconnectData> RELAY_CLIENT_DISCONNECTED_C2S_PACKET = new JsonC2SPacket<>(RelayDisconnectData.class, RelayC2SPackets::onClientDisconnected);

    private static void onClientConnected(WebSpeakPlayer player, String context) {
        if (player.getConnection() instanceof PlayerRelayConnection relayConnection && !relayConnection.isConnected()) {
            relayConnection.clientConnected(context);
        } else {
            LOGGER.warn("We recived a relay connect packet, but have no relay connection to the player or are already connected!");
        }
    }

    private static void onClientDisconnected(WebSpeakPlayer player, RelayDisconnectData data) {
        if (player.getConnection() instanceof PlayerRelayConnection relayConnection && relayConnection.isConnected()) {
            relayConnection.clientDisconnected(data.statusCode, data.reason);
        } else {
            LOGGER.warn("We recived a relay disconnect packet, but have no relay connection to the player or are already connected!");
        }
    }
}

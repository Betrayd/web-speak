package net.betrayd.webspeak.net;

import com.google.gson.Gson;

import io.javalin.websocket.WsContext;
import net.betrayd.webspeak.WebSpeakPlayer;

public class WebSpeakNet {

    /**
     * A gson instance for convenience.
     */
    public static final Gson GSON = new Gson();

    public static class UnknownPacketException extends RuntimeException {
        private final String packetName;

        public UnknownPacketException(String packetName) {
            super("Unknown packet type: '" + packetName + "'");
            this.packetName = packetName;
        }

        public String getPacketName() {
            return packetName;
        }
    }

    public static void onRecievePacket(WebSpeakPlayer player, String packet) throws IllegalArgumentException, UnknownPacketException {

        int splitIndex = packet.indexOf(';');
        if (splitIndex < 0) {
            throw new IllegalArgumentException("Supplied string was missing ';' to indicate packet type");
        }
        
        String packetName = packet.substring(0, splitIndex);
        PacketType<?> type = PacketTypes.REGISTRY.get(packetName);
        if (type == null) {
            throw new UnknownPacketException(packetName);
        }
        parsePacket(player, type, packet.substring(splitIndex + 1));
    }

    public static <T> T parsePacket(WebSpeakPlayer player, PacketType<T> packetType, String payload) {
        T val = packetType.read(payload);
        packetType.apply(player, val);
        return val;
    }

    public static <T> String writePacket(PacketType<T> packetType, T packet) {
        
        String packetId = PacketTypes.REGISTRY.inverse().get(packetType);
        if (packetId == null) {
            throw new IllegalArgumentException("Packet type %s is not registered!".formatted(packetType));
        }

        String payload = packetType.write(packet);
        return packetId + ";" + payload;
    }

    public static <T> void sendPacket(WebSpeakPlayer player, PacketType<T> packetType, T packet) {
        player.getWsContext().send(writePacket(packetType, packet));
    }

    public static <T> void sendPacket(Iterable<? extends WebSpeakPlayer> players, PacketType<T> packetType, T packet) {
        String serialized = writePacket(packetType, packet);
        for (var player : players) {
            WsContext ws = player.getWsContext();
            if (ws == null)
                continue;
            ws.send(serialized);
        }
    }
}

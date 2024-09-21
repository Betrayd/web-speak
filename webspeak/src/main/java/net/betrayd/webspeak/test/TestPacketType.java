package net.betrayd.webspeak.test;

import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.net.PacketType;

public class TestPacketType implements PacketType<String> {

    @Override
    public String write(String packet) {
        return packet;
    }

    @Override
    public String read(String serialized) {
        return serialized;
    }

    @Override
    public void apply(WebSpeakPlayer player, String packet) {
        LoggerFactory.getLogger(getClass()).info("Recieved packet '{}' from '{}'", packet, player.getPlayerId());
    }
    
}

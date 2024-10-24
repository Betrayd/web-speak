package net.betrayd.webspeak.impl.net.packets;

import java.util.List;
import java.util.Map;

import net.betrayd.webspeak.impl.net.S2CPacket;
import net.betrayd.webspeak.impl.net.S2CPacket.JsonS2CPacket;
import net.betrayd.webspeak.util.WSPlayerListEntry;

public class PlayerListPackets {
    /**
     * Update one or more player list entries on the client.
     */
    public static final S2CPacket<Map<String, WSPlayerListEntry>> SET_PLAYER_ENTRIES_S2C = new JsonS2CPacket<>("setPlayerEntries");

    public static final S2CPacket<List<String>> REMOVE_PLAYER_ENTRIES_S2C = new JsonS2CPacket<>("removePlayerEntries");
}

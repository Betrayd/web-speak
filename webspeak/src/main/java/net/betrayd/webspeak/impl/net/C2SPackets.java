package net.betrayd.webspeak.impl.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.betrayd.webspeak.impl.net.packets.TestC2SPacket;

public class C2SPackets {
    public static final BiMap<String, C2SPacket<?>> REGISTRY = HashBiMap.create();

    static {
        REGISTRY.put("test", TestC2SPacket.PACKET);
    }
    
    public static C2SPacket<?> get(String id) {
        return REGISTRY.get(id);
    }

    public static String getId(C2SPacket<?> packet) {
        return REGISTRY.inverse().get(packet);
    }
}

package net.betrayd.webspeak.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;

import net.betrayd.webspeak.test.TestPacketType;

public class PacketTypes {
    public static final Gson GSON = new Gson();
    public static final BiMap<String, PacketType<?>> REGISTRY = HashBiMap.create();

    static {
        REGISTRY.put("updateTransform", UpdateTransformPacket.TYPE);
        REGISTRY.put("testPacket", new TestPacketType());
    }
}

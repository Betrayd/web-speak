package net.betrayd.webspeak.impl.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.betrayd.webspeak.impl.net.packets.RTCPackets;
import net.betrayd.webspeak.impl.net.packets.TestC2SPacket;

public class C2SPackets {
    public static final BiMap<String, C2SPacket<?>> REGISTRY = HashBiMap.create();

    static {
        REGISTRY.put("test", TestC2SPacket.PACKET);

        REGISTRY.put("returnOffer", RTCPackets.RETURN_OFFER_C2S);
        REGISTRY.put("returnAnswer", RTCPackets.RETURN_ANSWER_C2S);
    }
    
    public static C2SPacket<?> get(String id) {
        return REGISTRY.get(id);
    }

    public static String getId(C2SPacket<?> packet) {
        return REGISTRY.inverse().get(packet);
    }
}

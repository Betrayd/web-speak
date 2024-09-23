package net.betrayd.webspeak.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;

public class PacketTypes {
    public static final Gson GSON = new Gson();
    public static final BiMap<String, PacketType<?>> REGISTRY = HashBiMap.create();

    static {
        REGISTRY.put("updateTransform", UpdateTransformPacket.TYPE);
        REGISTRY.put("requestOffer", RTCOfferPacket.REQUEST_OFFER_PACKET);
        REGISTRY.put("returnOffer", RTCOfferPacket.RETURN_OFFER_PACKET);
        REGISTRY.put("handOffer", RTCOfferPacket.HAND_OFFER_PACKET);
        REGISTRY.put("returnAnswer", RTCOfferPacket.RETURN_ANSWER_PACKET);
        REGISTRY.put("handAnswer", RTCOfferPacket.HAND_ANSWER_PACKET);
        REGISTRY.put("handIce", RTCOfferPacket.HAND_ICE_PACKET);
        REGISTRY.put("returnIce", RTCOfferPacket.RETURN_ICE_PACKET);
        REGISTRY.put("localPlayerInfo", LocalPlayerInfoPacket.TYPE);
    }
}

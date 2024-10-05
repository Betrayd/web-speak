package net.betrayd.webspeak.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.WebSpeakFlags;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.impl.net.WebSpeakNet;
import net.betrayd.webspeak.impl.net.packets.RTCPackets;
import net.betrayd.webspeak.impl.net.packets.RTCPackets.RequestOfferS2CPacket;
import net.betrayd.webspeak.impl.net.packets.UpdateTransformS2CPacket;

public class RTCManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak RTC Manager");

    private final WebSpeakServer server;

    public RTCManager(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    
    public void connectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS)) {
            LOGGER.info("Requesting player {} to RTC offer to {}", a.getPlayerId(), b.getPlayerId());
        }
        WebSpeakNet.sendPacket(a.getWsContext(), RTCPackets.REQUEST_OFFER_S2C, new RequestOfferS2CPacket(b.getPlayerId()));

        // I tried moving this to the player code, but it doesn't seem to work and I don't give a shit.
        UpdateTransformS2CPacket.fromPlayer(a).send(b.getWsContext());
        UpdateTransformS2CPacket.fromPlayer(b).send(a.getWsContext());
    }

    public void disconnectRTC(WebSpeakPlayer a, WebSpeakPlayer b) {
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS)) {
            LOGGER.info("Requesting player {} to disconnect RTC with {}", a.getPlayerId(), b.getPlayerId());
        }
    }
}

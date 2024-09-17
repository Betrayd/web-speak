package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.util.WebSpeakVector;

public class TestWebPlayer extends WebSpeakPlayer {
    
    public TestWebPlayer(WebSpeakServer server, String playerId, String sessionId) {
        super(server, playerId, sessionId);
    }

    @Override
    public WebSpeakVector getLocation() {
        return WebSpeakVector.ZERO;
    }
    
    public WebSpeakVector getRotation() {
        return WebSpeakVector.ZERO;
    };

    @Override
    public boolean isInScope(WebSpeakPlayer player) {
        return true;
    }
    
}

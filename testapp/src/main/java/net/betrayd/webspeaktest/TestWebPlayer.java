package net.betrayd.webspeaktest;

import net.betrayd.webspeak.player.WebSpeakPlayer;
import net.betrayd.webspeak.util.WebSpeakVector;

public class TestWebPlayer implements WebSpeakPlayer {

    @Override
    public WebSpeakVector getWebSpeakLocation() {
        return new WebSpeakVector(0, 0, 0);
    }

    @Override
    public boolean isInScope(WebSpeakPlayer player) {
        return true;
    }
    
}

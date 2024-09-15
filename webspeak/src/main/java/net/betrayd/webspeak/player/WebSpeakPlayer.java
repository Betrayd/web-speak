package net.betrayd.webspeak.player;

import net.betrayd.webspeak.util.WebSpeakVector;

/**
 * An interface allowing WebSpeak to access various info about our game-agnostic player.
 */
public interface WebSpeakPlayer {
    public WebSpeakVector getWebSpeakLocation();
}

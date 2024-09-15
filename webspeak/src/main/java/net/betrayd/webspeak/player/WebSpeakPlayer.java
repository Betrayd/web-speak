package net.betrayd.webspeak.player;

import net.betrayd.webspeak.util.WebSpeakVector;

/**
 * An interface allowing WebSpeak to access various info about our game-agnostic player.
 */
public interface WebSpeakPlayer {
    public WebSpeakVector getWebSpeakLocation();

    /**
     * This has to be bi-dirrectional
     * @param player
     * @return
     */
    public boolean isInScope(WebSpeakPlayer player);
}

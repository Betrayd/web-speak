package net.betrayd.webspeak.player;

/**
 * Contains various info about a player's presence in WebSpeak that isn't game-specific.
 */
public class WebSpeakPlayerData<T extends WebSpeakPlayer> {
    private final T player;
    private final String sessionId;
    private final String playerId;

    /**
     * Create player data
     * @param player Player instance to use
     * @param sessionId The ID used in the URL when a websocket connection is made.
     * @param playerId The public-facing ID of this player.
     */
    public WebSpeakPlayerData(T player, String sessionId, String playerId) {
        this.player = player;
        this.sessionId = sessionId;
        this.playerId = playerId;
    }

    public T getPlayer() {
        return player;
    }

    /**
     * Get the session ID used in the URL when a websocket connection is made.
     * @return Session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get the public-facing ID used for this player.
     * @return Public-facing ID
     */
    public String getPlayerId() {
        return playerId;
    }
}

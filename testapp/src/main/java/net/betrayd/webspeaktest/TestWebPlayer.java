package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.util.WebSpeakVector;
import net.betrayd.webspeaktest.ui.util.URIComponent;

public class TestWebPlayer extends WebSpeakPlayer {

    private final Player player;
    
    public TestWebPlayer(WebSpeakServer server, Player player, String playerId, String sessionId) {
        super(server, playerId, sessionId);
        this.player = player;
    }

    @Override
    public WebSpeakVector getLocation() {
        return player.getLocation();
    }
    
    public WebSpeakVector getRotation() {
        return WebSpeakVector.ZERO;
    };

    @Override
    public boolean isInScope(WebSpeakPlayer player) {
        return true;
    }
    
    public Player getPlayer() {
        return player;
    }

    public String getUsername() {
        return player.getName();
    };

    /**
     * Get a connection URL for this web player when the server and frontend are both running on localhost.
     * @param frontendPort The port the frontend is running on.
     * @return Connection address
     */
    public String getLocalConnectionAddress(int frontendPort) {
        return "http://localhost:" + frontendPort + "?serverAdress=" +
                URIComponent.encode("http://localhost:" + getServer().getPort()) + "&sessionID=" + getSessionId();
    }
}

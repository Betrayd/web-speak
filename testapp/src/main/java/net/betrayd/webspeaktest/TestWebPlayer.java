package net.betrayd.webspeaktest;

import javafx.geometry.Point2D;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.util.WebSpeakVector;
import net.betrayd.webspeaktest.util.MathUtils;
import net.betrayd.webspeaktest.util.URIComponent;

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
    
    @Override
    public WebSpeakVector getForward() {
        Point2D point = MathUtils.rotatePoint(0, 1, Math.toRadians(player.getRotation() + 180d));
        return new WebSpeakVector(point.getX(), 0, point.getY());
    };

    @Override
    public boolean isInScope(WebSpeakPlayer player) {
        double scopeRadius = WebSpeakTestApp.getInstance().getScopeRadius();
        return this.getLocation().squaredDistanceTo(player.getLocation()) <= scopeRadius * scopeRadius;
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
        return "http://localhost:" + frontendPort + "/web-speak?server=" +
                URIComponent.encode("http://localhost:" + getServer().getPort()) + "&id=" + getSessionId();
    }
}

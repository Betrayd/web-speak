package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.util.WebSpeakVector;

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
        double x = this.player.getLocation().x() - player.getLocation().x();
        double y = this.player.getLocation().y() - player.getLocation().y();
        double z = this.player.getLocation().z() - player.getLocation().z();

        if(x*x + y*y + z*z <= 900)
        {
            return true;
        }
        return false;
    }
    
    public Player getPlayer() {
        return player;
    }

    public String getUsername() {
        return player.getName();
    };
}

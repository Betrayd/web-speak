package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeakServer;

public class WebSpeakTest {
    public static void main(String[] args) throws Exception {

        WebSpeakServer server = new WebSpeakServer();
        server.start(9090);

        TestWebPlayer player1 = server.addPlayer(TestWebPlayer::new);
        System.out.println("SessionID for player1 is: " + player1.getSessionId());

        TestWebPlayer player2 = server.addPlayer(TestWebPlayer::new);
        System.out.println("SessionID for player2 is: " + player2.getSessionId());

        while (true) {
            server.tick();
            Thread.sleep(20);
        }
    }
}

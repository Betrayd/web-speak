package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.player.WebSpeakPlayerData;

public class WebSpeakTest {
    public static void main(String[] args) {

        WebSpeakServer<TestWebPlayer> server = new WebSpeakServer<>();
        server.start(9090);

        TestWebPlayer player1 = new TestWebPlayer();
        WebSpeakPlayerData<TestWebPlayer> data1 = server.addPlayer(player1);
        System.out.println("SessionID for player1 is: " + data1.getSessionId());

        TestWebPlayer player2 = new TestWebPlayer();
        WebSpeakPlayerData<TestWebPlayer> data2 = server.addPlayer(player2);
        System.out.println("SessionID for player2 is: " + data2.getSessionId());

        while(true)
        {
            server.tick();
            try{
            Thread.sleep(1000/20);
            }
            catch(InterruptedException e)
            {
                
            }
        }
    }
}

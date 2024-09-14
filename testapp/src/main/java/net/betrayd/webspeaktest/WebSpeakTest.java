package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeak;

public class WebSpeakTest {
    public static void main(String[] args) {

        WebSpeak server = new WebSpeak();

        System.out.println(server.getTestString());
        server.start();
    }
}

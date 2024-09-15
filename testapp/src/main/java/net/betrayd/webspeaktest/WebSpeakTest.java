package net.betrayd.webspeaktest;

import net.betrayd.webspeak.WebSpeakServer;

public class WebSpeakTest {
    public static void main(String[] args) {

        WebSpeakServer<?> server = new WebSpeakServer<>();
        server.start(9090);
    }
}

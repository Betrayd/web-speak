package net.betrayd.webspeak;

public class WebSpeakFlags {
    public final record WebSpeakFlag<T>(String name, T defaultValue) {
    };

    public static final WebSpeakFlag<Boolean> DEBUG_RTC_OFFERS = new WebSpeakFlag<>("debugRTCOffers", false);
    public static final WebSpeakFlag<Boolean> DEBUG_CONNECTION_REQUESTS = new WebSpeakFlag<>("debugConnectionRequests", false);
    public static final WebSpeakFlag<Boolean> DEBUG_KEEPALIVE = new WebSpeakFlag<>("deugKeepAlive", false);
}

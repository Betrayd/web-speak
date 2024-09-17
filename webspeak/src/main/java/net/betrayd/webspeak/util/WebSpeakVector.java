package net.betrayd.webspeak.util;

/**
 * A simple vector class for use in WebSpeak
 */
public record WebSpeakVector(double x, double y, double z) {
    public static WebSpeakVector fromArray(double[] array) {
        return new WebSpeakVector(array[0], array[1], array[2]);
    }

    public static final WebSpeakVector ZERO = new WebSpeakVector(0, 0, 0);
}

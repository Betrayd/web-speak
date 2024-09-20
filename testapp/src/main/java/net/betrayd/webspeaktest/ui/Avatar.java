package net.betrayd.webspeaktest.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import javafx.scene.Node;
import javafx.scene.paint.Color;

public record Avatar(Node node, Color Color) {

    private static final URL AVATAR_URL = Avatar.class.getResource("/ui/avatar.svg");
    private static String avatarSvgContents;

    public static SVGImage loadAvatar(Color color) {
        if (avatarSvgContents == null) {
            try {
                avatarSvgContents = loadSvgContents();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return SVGLoader.load(avatarSvgContents, ".avatar-body {fill:" + colorToCss(color) + ";}");
    }

    private static String loadSvgContents() throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(AVATAR_URL.openStream()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private static String colorToCss(Color color) {
        return String.format("rgba(%d,%d,%d,%d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (color.getOpacity() * 255));
    }

    public static final Avatar create(Color color) {
        return new Avatar(loadAvatar(color), color);
    }
}

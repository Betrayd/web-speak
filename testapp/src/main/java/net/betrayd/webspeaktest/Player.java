package net.betrayd.webspeaktest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import net.betrayd.webspeaktest.ui.Avatar;

public class Player {

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

    public static final Player create(Color color) {
        return new Player(loadAvatar(color), color);
    }

    private final Node node;
    private final Color color;

    private final StringProperty nameProperty = new SimpleStringProperty("player");
    
    public Player(Node node, Color color) {
        this.node = node;
        this.color = color;
    }

    public Node getNode() {
        return node;
    }

    public Color getColor() {
        return color;
    }

    public String getName() {
        return nameProperty.get();
    }

    public void setName(String name) {
        this.nameProperty.set(name);
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    private final ObjectProperty<TestWebPlayer> webPlayerProperty = new SimpleObjectProperty<>();

    public TestWebPlayer getWebPlayer() {
        return webPlayerProperty.get();
    }

    public void setWebPlayer(TestWebPlayer webPlayer) {
        if (webPlayer.getPlayer() != this)
            throw new IllegalArgumentException("Web player must point to this player.");
        webPlayerProperty.set(webPlayer);
    }

    public TestWebPlayer getWebplayer() {
        return webPlayerProperty.get();
    }

    public ReadOnlyObjectProperty<TestWebPlayer> webPlayerProperty() {
        return webPlayerProperty;
    }
}

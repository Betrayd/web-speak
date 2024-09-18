package net.betrayd.webspeaktest.ui;

import java.util.Map;

import com.google.common.collect.MapMaker;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;

public class JavaFXUtils {
    private static Map<Double, Image> gridImages = new MapMaker().weakValues().makeMap();

    public static ImagePattern createGridPattern(double gridSize, double x, double y, Color fillColor, Color lineColor) {
        double w = gridSize;
        double h = gridSize;

        Image image = gridImages.computeIfAbsent(gridSize, s -> {
            Canvas canvas = new Canvas(w, h);
            GraphicsContext gc = canvas.getGraphicsContext2D();
    
            gc.setStroke(Color.BLACK);
            gc.setFill(Color.LIGHTGRAY.deriveColor(1, 1, 1, 0.2));
            gc.fillRect(0, 0, w, h);
            gc.strokeRect(0, 0, w, h);
            return canvas.snapshot(new SnapshotParameters(), null);
        });

        ImagePattern pattern = new ImagePattern(image, x, y, w, h, false);

        return pattern;
    }

    public static ImagePattern createGridPattern(double gridSize, double x, double y) {
        return createGridPattern(gridSize, x, y, Color.LIGHTGRAY, Color.BLACK);
    }
}

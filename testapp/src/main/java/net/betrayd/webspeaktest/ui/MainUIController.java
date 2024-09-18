package net.betrayd.webspeaktest.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public final class MainUIController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainUIController.class);

    @FXML
    private ZoomableGraph zoomGraph;

    @FXML
    private void initialize() {
        zoomGraph.getGraphChildren().add(new Button("Hello World"));
        // Rectangle rect = new Rectangle(512, 256);
        // rect.setFill(Color.GREEN);
        // rect.setStroke(Color.BLACK);
        // rect.setStrokeWidth(2);

        // rect.getTransforms().add(new Translate(512, 64));
        // rect.getTransforms().add(new Scale(.5f, .5f));

        // testPane.getChildren().add(rect);

        // rect.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
        //     LOGGER.info("Clicked rectangle at ({}, {})", e.getX(), e.getY());
        // });
    }
}

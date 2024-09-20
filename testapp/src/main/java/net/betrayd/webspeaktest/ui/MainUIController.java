package net.betrayd.webspeaktest.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.betrayd.webspeaktest.WebSpeakTestApp;
import net.betrayd.webspeaktest.ui.util.ZoomableGraph;

public final class MainUIController {

    private static final Color ON_COLOR = Color.GREEN;
    private static final Color OFF_COLOR = Color.RED;

    private static final String ON_TEXT = "Server Running";
    private static final String OFF_TEXT = "Server Stopped";

    @FXML
    private ZoomableGraph zoomGraph;

    @FXML
    private Shape serverStatusIcon;

    @FXML
    private Label serverStatusText;

    @FXML
    private VBox playerBox;

    @FXML
    private void initialize() {
        playerBox.getChildren().clear();
    }

    public ZoomableGraph getZoomGraph() {
        return zoomGraph;
    }

    public void initApp(WebSpeakTestApp app) {
        zoomGraph.graphScaleProperty().bind(app.graphScaleProperty());

        app.serverProperty().addListener((prop, oldVal, newVal) -> {
            if (newVal != null) 
                onStartServer();
            else
                onStopServer();
        });

        onStopServer();
    }

    private void onStartServer() {
        serverStatusIcon.setFill(ON_COLOR);
        serverStatusText.setText(ON_TEXT);
    }

    private void onStopServer() {
        serverStatusIcon.setFill(OFF_COLOR);
        serverStatusText.setText(OFF_TEXT);
    }
}

package net.betrayd.webspeaktest.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import net.betrayd.webspeaktest.WebSpeakTestApp;
import net.betrayd.webspeaktest.ui.util.ZoomableGraph;

public final class MainUIController {

    @FXML
    private ZoomableGraph zoomGraph;

    @FXML
    private void initialize() {
        zoomGraph.getGraphChildren().add(new Button("Hello World"));
    }

    public ZoomableGraph getZoomGraph() {
        return zoomGraph;
    }

    public void initApp(WebSpeakTestApp app) {
        zoomGraph.graphScaleProperty().bind(app.graphScaleProperty());
    }
}

package net.betrayd.webspeaktest.ui.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class ZoomableGraph extends Region {
    private final Pane pane = new Pane();

    private final Translate translation = new Translate();
    private final Scale scale = new Scale();
    private final Rectangle clipRect = new Rectangle();

    private final Region graph = new Region();

    public ZoomableGraph() {
        pane.getStyleClass().add("grid-background");
        pane.getTransforms().addAll(translation, scale);
        setClip(clipRect);

        getChildren().addAll(graph, pane);

        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);

        translation.xProperty().bindBidirectional(xOffsetProperty);
        translation.yProperty().bindBidirectional(yOffsetProperty);

        zoomAmountProperty.addListener((prop, oldVal, newVal) -> {
            double val = Math.pow(2, newVal.doubleValue());
            scale.setX(val);
            scale.setY(val);
        });

        setOnScroll(e -> {
            setZoomAmount(getZoomAmount() + e.getDeltaY() * 0.005);
        });

        zoomAmountProperty.addListener((prop, oldVal, newVal) -> updateBackground());
        xOffsetProperty.addListener((prop, oldVal, newVal) -> updateBackground());
        yOffsetProperty.addListener((prop, oldVal, newVal) -> updateBackground());
        
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        clipRect.setWidth(getWidth());
        clipRect.setHeight(getHeight());
        graph.resizeRelocate(0, 0, getWidth(), getHeight());
        updateBackground();
    }


    private BooleanProperty allowManualTransform = new SimpleBooleanProperty(true);

    public boolean getAllowManualTransform() {
        return allowManualTransform.get();
    }

    public void setAllowManualTransform(boolean allowManualTransform) {
        this.allowManualTransform.set(allowManualTransform);
    }

    public BooleanProperty allowManualTransformProperty() {
        return allowManualTransform;
    }

    private DoubleProperty xOffsetProperty = new SimpleDoubleProperty();

    public double getXOffset() {
        return xOffsetProperty.get();
    }

    public void setXOffset(double xOffset) {
        xOffsetProperty.set(xOffset);
    }

    public DoubleProperty xOffsetProperty() {
        return xOffsetProperty;
    }

    public DoubleProperty yOffsetProperty = new SimpleDoubleProperty();

    public double getYOffset() {
        return yOffsetProperty.get();
    }

    public void setYOffset(double yOffset) {
        yOffsetProperty.set(yOffset);
    }

    public DoubleProperty yOffsetProperty() {
        return yOffsetProperty;
    }

    private DoubleProperty zoomAmountProperty = new SimpleDoubleProperty(0);

    public double getZoomAmount() {
        return zoomAmountProperty.get();
    }

    public void setZoomAmount(double zoomAmount) {
        zoomAmountProperty.set(zoomAmount);
    }

    public DoubleProperty zoomAmountProperty() {
        return zoomAmountProperty;
    }

    private DoubleProperty graphScaleProperty = new SimpleDoubleProperty(20);

    public double getGraphScale() {
        return graphScaleProperty.get();
    }

    public void setGraphScale(double graphScale) {
        graphScaleProperty.set(graphScale);
    }

    public DoubleProperty graphScaleProperty() {
        return graphScaleProperty;
    }

    public final ObservableList<Node> getGraphChildren() {
        return pane.getChildren();
    }

    private double lastX;
    private double lastY;

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            e.consume();
        }
    }
    
    private void onMouseDragged(MouseEvent e) {
        if (!allowManualTransform.get() || e.getButton() != MouseButton.SECONDARY)
            return;
        double deltaX = e.getSceneX() - lastX;
        double deltaY = e.getSceneY() - lastY;

        lastX = e.getSceneX();
        lastY = e.getSceneY();

        translation.setX(translation.getX() + deltaX);
        translation.setY(translation.getY() + deltaY);
        e.consume();
    }

    private void updateBackground() {
        double size = getGraphScale() * Math.pow(2, getZoomAmount());
        graph.setBackground(Background.fill(JavaFXUtils.createGridPattern(size, getXOffset(), getYOffset())));
    }

}

package net.betrayd.webspeaktest.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class ZoomableGraph extends Region {
    private Pane pane = new Pane();

    private final Translate translation = new Translate();
    private final Scale scale = new Scale();
    private final Rectangle clipRect = new Rectangle();

    public ZoomableGraph() {
        pane.getStyleClass().add("grid-background");
        pane.getTransforms().addAll(translation, scale);
        
        // pane.setClip(clipRect);
        clipRect.setFill(Color.GRAY);
        clipRect.setStroke(Color.RED);
        setClip(clipRect);


        getChildren().addAll(pane);


        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);

        zoomAmountProperty.addListener((prop, oldVal, newVal) -> {
            double val = Math.pow(2, newVal.doubleValue());
            scale.setX(val);
            scale.setY(val);
        });

        setOnScroll(e -> {
            setZoomAmount(getZoomAmount() + e.getDeltaY() * 0.005);
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        clipRect.setWidth(getWidth());
        clipRect.setHeight(getHeight());
        // Point2D globalCoord = localToScene(0, 0);

        // clipRect.setX(globalCoord.getX());
        // clipRect.setY(globalCoord.getY());
    }


    private BooleanProperty allowManualTransform = new SimpleBooleanProperty();

    public boolean getAllowManualTransform() {
        return allowManualTransform.get();
    }

    public void setAllowManualTransform(boolean allowManualTransform) {
        this.allowManualTransform.set(allowManualTransform);
    }

    public BooleanProperty allowManualTransformProperty() {
        return allowManualTransform;
    }

    public Translate getTranslation() {
        return translation;
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

    public final ObservableList<Node> getGraphChildren() {
        return pane.getChildren();
    }

    private double lastX;
    private double lastY;

    private void onMousePressed(MouseEvent e) {
        lastX = e.getSceneX();
        lastY = e.getSceneY();
        e.consume();
    }
    
    private void onMouseDragged(MouseEvent e) {
        double deltaX = e.getSceneX() - lastX;
        double deltaY = e.getSceneY() - lastY;

        lastX = e.getSceneX();
        lastY = e.getSceneY();

        translation.setX(translation.getX() + deltaX);
        translation.setY(translation.getY() + deltaY);
        e.consume();
    }
}

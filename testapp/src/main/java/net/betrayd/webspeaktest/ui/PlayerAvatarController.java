package net.betrayd.webspeaktest.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class PlayerAvatarController {

    @FXML
    private Group root;

    @FXML
    private Circle fillCircle;

    private final BooleanProperty selectedProperty = new SimpleBooleanProperty();

    public boolean isSelected() {
        return selectedProperty.get();
    }

    public void setSelected(boolean selected) {
        selectedProperty.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    public Paint getFill() {
        return fillCircle.getFill();
    }

    public void setFill(Paint fill) {
        fillCircle.setFill(fill);
    }

    public ObjectProperty<Paint> fillProperty() {
        return fillCircle.fillProperty();
    }

    private double oldStrokeWidth;

    @FXML
    public void initialize() {
        // I really should be using CSS for this, but this is a test app so I don't
        // care.
        selectedProperty.addListener((prop, oldVal, newVal) -> {
            if (oldVal.equals(newVal))
                return;
            if (newVal) {
                fillCircle.setStroke(Color.BLUE);
                oldStrokeWidth = fillCircle.getStrokeWidth();
                fillCircle.setStrokeWidth(oldStrokeWidth * 4);
            } else {
                fillCircle.setStroke(Color.BLACK);
                fillCircle.setStrokeWidth(oldStrokeWidth);
            }
        });
    }

    private double mouseAnchorX;
    private double mouseAnchorY;

    @FXML
    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            mouseAnchorX = e.getX();
            mouseAnchorY = e.getY();
            e.consume();
        }
    }

    @FXML
    private void onMouseDragged(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            root.setLayoutX(e.getX() - mouseAnchorX + root.getLayoutX());
            root.setLayoutY(e.getY() - mouseAnchorY + root.getLayoutY());
            e.consume();
        }
    }

    public Group getRoot() {
        return root;
    }
}

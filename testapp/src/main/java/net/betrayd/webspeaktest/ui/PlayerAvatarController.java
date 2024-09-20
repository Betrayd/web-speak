package net.betrayd.webspeaktest.ui;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class PlayerAvatarController {

    @FXML
    private Group root;

    @FXML
    private Circle fillCircle;

    public Paint getFill() {
        return fillCircle.getFill();
    }

    public void setFill(Paint fill) {
        fillCircle.setFill(fill);
    }

    public ObjectProperty<Paint> fillProperty() {
        return fillCircle.fillProperty();
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
}

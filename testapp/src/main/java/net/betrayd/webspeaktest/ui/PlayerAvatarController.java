package net.betrayd.webspeaktest.ui;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class PlayerAvatarController {
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
}

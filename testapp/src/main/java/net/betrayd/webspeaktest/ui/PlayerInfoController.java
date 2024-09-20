package net.betrayd.webspeaktest.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.betrayd.webspeaktest.Player;

public class PlayerInfoController {

    public static PlayerInfoController loadInstance() {
        try {
            FXMLLoader loader = new FXMLLoader(PlayerInfoController.class.getResource("/ui/playerInfo.fxml"));
            loader.load();
            return loader.getController();
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException(e);
            }
        }

    }

    private Player player;

    @FXML
    private TitledPane titledPane;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private TextField nameField;

    @FXML
    private TextField sessionIdField;

    @FXML
    private TextField playerIdField;

    @FXML
    protected void initialize() {
        nameField.focusedProperty().addListener((prop, oldVal, newVal) -> {
            if (!newVal) {
                commitNameChange();
            }
        });
    }

    public void initPlayer(Player player) {
        this.player = player;

        titledPane.textProperty().bind(player.nameProperty());

        colorPicker.valueProperty().bindBidirectional(player.colorProperty());

        // Manually add listener so field can still be updated.
        player.nameProperty().addListener((prop, oldVal, newVal) -> {
            nameField.setText(newVal);
        });

        player.webPlayerProperty().addListener((prop, oldVal, newVal) -> {
            sessionIdField.setText(newVal.getSessionId());
            playerIdField.setText(newVal.getPlayerId());
        });

        player.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                titledPane.requestFocus();
                e.consume();
            }
        });
    }

    @FXML
    protected void onNameFieldCommit(ActionEvent e) {
        commitNameChange();
    }

    protected void commitNameChange() {
        player.setName(nameField.getText());
    }

    public Player getPlayer() {
        return player;
    }

    public TitledPane getTitledPane() {
        return titledPane;
    }
}

package net.betrayd.webspeaktest.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import net.betrayd.webspeaktest.Player;

public class PlayerInfoController {

    private Player player;

    @FXML
    private TitledPane titledPane;

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

        // Manually add listener so field can still be updated.
        player.nameProperty().addListener((prop, oldVal, newVal) -> {
            nameField.setText(newVal);
        });

        player.webPlayerProperty().addListener((prop, oldVal, newVal) -> {
            sessionIdField.setText(newVal.getSessionId());
            playerIdField.setText(newVal.getPlayerId());
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
}

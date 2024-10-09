package net.betrayd.webspeaktest.ui;

import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import net.betrayd.webspeak.WebSpeakChannel;
import net.betrayd.webspeak.util.WebSpeakEvents;
import net.betrayd.webspeak.util.WebSpeakEvents.WebSpeakEvent;
import net.betrayd.webspeaktest.Player;
import net.betrayd.webspeaktest.WebSpeakTestApp;

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

    /**
     * Yeaah, there's gotta be a more "javafx" way to do this, but I can't be bothered to learn right now.
     */
    public final WebSpeakEvent<Consumer<Void>> ON_REQUEST_REMOVE = WebSpeakEvents.createSimple();

    private Player player;

    @FXML
    private TitledPane titledPane;

    @FXML
    private HBox titleBox;

    @FXML
    private GridPane gridPane;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private TextField nameField;

    @FXML
    private TextField sessionIdField;

    @FXML
    private TextField playerIdField;

    @FXML
    private Label connectionText;

    @FXML
    private ChoiceBox<WebSpeakChannel> channelSelector;

    private BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    public boolean isSelected() {
        return selectedProperty.get();
    }

    public void setSelected(boolean selected) {
        selectedProperty.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    @FXML
    protected void initialize() {
        nameField.focusedProperty().addListener((prop, oldVal, newVal) -> {
            if (!newVal) {
                commitNameChange();
            }
        });

        connectionProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal == null) {
                connectionText.setText("Not Connected");
                connectionText.setTextFill(Color.RED);
            } else {
                connectionText.setText(newVal);
                connectionText.setTextFill(Color.GREEN);
            }
        });

        selectedProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal) {
                // I know I *should* be using CSS but I don't care
                titledPane.setBorder(new Border(new BorderStroke(Color.LIGHTBLUE, BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, new BorderWidths(2))));
                titledPane.setExpanded(true);
            } else {
                titledPane.setBorder(Border.EMPTY);
            }
        });

        titleBox.prefWidthProperty().bind(titledPane.widthProperty().subtract(60));

        channelSelector.setConverter(new StringConverter<WebSpeakChannel>() {

            @Override
            public String toString(WebSpeakChannel object) {
                return object != null ? object.getName() : "null";
            }

            @Override
            public WebSpeakChannel fromString(String string) {
                throw new UnsupportedOperationException("Unimplemented method 'fromString'");
            }
            
        });
        
        channelSelector.getSelectionModel().selectedItemProperty().addListener((prop, oldVal, newVal) -> {
            if (!isChannelUpdating && getPlayer() != null) {
                isChannelUpdating = true;
                player.setChannel(newVal);
                isChannelUpdating = false;
            }
        });
        
        var channelList = WebSpeakTestApp.getInstance().getChannels();
        setupChannelList(channelList);
        channelList.addListener(new ListChangeListener<>() {

            @Override
            public void onChanged(Change<? extends WebSpeakChannel> c) {
                setupChannelList(c.getList());
            }
            
        });
        
    }
    
    private boolean isChannelUpdating = false;
    
    private void setupChannelList(List<? extends WebSpeakChannel> channels) {
        var selected = channelSelector.getSelectionModel().getSelectedItem();
        channelSelector.getItems().clear();
        for (var channel : channels) {
            channelSelector.getItems().add(channel);
        }
        channelSelector.getSelectionModel().select(selected);
    }

    private StringProperty connectionProperty = new SimpleStringProperty();

    // Store as a variable so it can be unregistered
    private MapChangeListener<Player, String> mapChangeListener = (change) -> {
        if (change.getKey().equals(player))
            connectionProperty.set(change.getValueAdded());
        
    };

    public void initPlayer(Player player) {
        this.player = player;
        
        // titledPane.textProperty().bind(player.nameProperty());

        colorPicker.valueProperty().bindBidirectional(player.colorProperty());

        // Manually add listener so field can still be updated.
        player.nameProperty().addListener((prop, oldVal, newVal) -> {
            nameField.setText(newVal);
        });

        player.webPlayerProperty().addListener((prop, oldVal, newVal) -> {
            if (newVal != null) {
                sessionIdField.setText(newVal.getSessionId());
                playerIdField.setText(newVal.getPlayerId());
            } else {
                sessionIdField.setText("");
                playerIdField.setText("");
            }
        });

        player.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                titledPane.requestFocus();
                e.consume();
            }
        });

        channelSelector.getSelectionModel().select(player.getChannel());
        channelSelector.getSelectionModel().selectedItemProperty().addListener((prop, oldVal, newVal) -> {
            player.setChannel(newVal);
        });

        WebSpeakTestApp.getInstance().getConnectionIps().addListener(mapChangeListener);
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

    public void onPlayerRemoved() {
        WebSpeakTestApp.getInstance().getConnectionIps().removeListener(mapChangeListener);
    }

    @FXML
    private void removePlayer() {
        ON_REQUEST_REMOVE.invoker().accept(null);
    }
    
}

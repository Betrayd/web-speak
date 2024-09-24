package net.betrayd.webspeaktest.ui;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.betrayd.webspeaktest.Player;
import net.betrayd.webspeaktest.TestWebPlayer;
import net.betrayd.webspeaktest.WebSpeakTestApp;
import net.betrayd.webspeaktest.WebSpeakTestServer;
import net.betrayd.webspeaktest.ui.util.ZoomableGraph;

public final class MainUIController {

    private static final Color ON_COLOR = Color.GREEN;
    private static final Color OFF_COLOR = Color.RED;

    private static final String ON_TEXT = "Server Running";
    private static final String OFF_TEXT = "Server Stopped";

    private WebSpeakTestApp app;

    @FXML
    private ZoomableGraph zoomGraph;

    @FXML
    private VBox playerBox;

    @FXML
    private Shape serverStatusIcon;

    @FXML
    private Label serverStatusText;

    @FXML
    private Label serverAddressText;

    @FXML
    private TextField connectionAddressField;

    @FXML
    private Button startStopButton;

    private final Map<Player, PlayerInfoController> playerInfoControllers = new WeakHashMap<>();

    private final ObjectProperty<Player> selectedPlayerProperty = new SimpleObjectProperty<>();

    public Player getSelectedPlayer() {
        return selectedPlayerProperty.get();
    }

    public void setSelectedPlayer(Player player) {
        selectedPlayerProperty.set(player);
    }

    public ObjectProperty<Player> selectedPlayerProperty() {
        return selectedPlayerProperty;
    }

    private final ObjectProperty<TestWebPlayer> selectedWebPlayerProperty = new SimpleObjectProperty<>();

    public TestWebPlayer getSelectedWebPlayer() {
        return selectedWebPlayerProperty.get();
    }

    public ReadOnlyObjectProperty<TestWebPlayer> selectedWebPlayerProperty() {
        return selectedWebPlayerProperty;
    }

    private final StringProperty serverAddressProperty = new SimpleStringProperty();

    @FXML
    private void initialize() {
        playerBox.getChildren().clear();
        selectedPlayerProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal != null) {
                selectedWebPlayerProperty.bind(newVal.webPlayerProperty());
            } else {
                selectedWebPlayerProperty.unbind();
                selectedWebPlayerProperty.set(null);
            }
        });

        serverAddressText.textProperty().bind(serverAddressProperty);
        
        selectedWebPlayerProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal != null) {
                connectionAddressField.setText(newVal.getLocalConnectionAddress(5173));
            } else {
                connectionAddressField.setText("");
            }
        });
    }

    public ZoomableGraph getZoomGraph() {
        return zoomGraph;
    }

    public void initApp(WebSpeakTestApp app) {
        this.app = app;
        zoomGraph.graphScaleProperty().bind(app.graphScaleProperty());

        app.serverProperty().addListener((prop, oldVal, newVal) -> {
            if (newVal != null) 
                onStartServer(newVal);
            else
                onStopServer();
        });

        onStopServer();
    }

    protected void onStartServer(WebSpeakTestServer server) {
        serverStatusIcon.setFill(ON_COLOR);
        serverStatusText.setText(ON_TEXT);

        startStopButton.setText("Stop Server");
        startStopButton.setDisable(false);

        CompletableFuture.supplyAsync(() -> server.getWsConnectionUrl(), server)
                .thenAcceptAsync(serverAddressProperty::set, Platform::runLater);
    }

    protected void onStopServer() {
        serverStatusIcon.setFill(OFF_COLOR);
        serverStatusText.setText(OFF_TEXT);
        serverAddressProperty.set("");

        startStopButton.setText("Start Server");
        startStopButton.setDisable(false);
    }

    @FXML
    private void pressStartStopButton() {
        if (app.isServerRunning()) {
            startStopButton.setDisable(true);
            app.stopServer();
        } else {
            app.startServer(8080);
        }
    }

    @FXML
    private void addPlayer() {
        app.addPlayer(Player.create(Color.color(Math.random(), Math.random(), Math.random())));
    }

    public void onAddPlayer(Player player) {
        zoomGraph.getGraphChildren().add(player.getNode());

        var infoPanel = PlayerInfoController.loadInstance();
        infoPanel.initPlayer(player);
        infoPanel.getTitledPane().addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            setSelectedPlayer(player);
        });

        player.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2) {
                setSelectedPlayer(player);
                e.consume();
            }
        });

        playerBox.getChildren().add(infoPanel.getTitledPane());

        BooleanBinding selectedBinding = Bindings.createBooleanBinding(
                () -> player.equals(getSelectedPlayer()), selectedPlayerProperty);
        
        infoPanel.selectedProperty().bind(selectedBinding);
        player.getAvatar().selectedProperty().bind(selectedBinding);

        playerInfoControllers.put(player, infoPanel);
    }

    public void onRemovePlayer(Player player) {
        zoomGraph.getGraphChildren().remove(player.getNode());
        PlayerInfoController infoPanel = playerInfoControllers.remove(player);
        if (infoPanel != null) {
            playerBox.getChildren().remove(infoPanel.getTitledPane());
            infoPanel.onPlayerRemoved();
        }
    }
}

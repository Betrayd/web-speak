package net.betrayd.webspeaktest;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.betrayd.webspeaktest.ui.MainUIController;

public class WebSpeakTestApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakTestApp.class);

    private static WebSpeakTestApp instance;

    public static WebSpeakTestApp getInstance() {
        return instance;
    }

    private final ObjectProperty<WebSpeakTestServer> server = new SimpleObjectProperty<>(null);

    public WebSpeakTestServer getServer() {
        return server.get();
    }

    public ReadOnlyObjectProperty<WebSpeakTestServer> serverProperty() {
        return server;
    }

    private final DoubleProperty graphScaleProperty = new SimpleDoubleProperty(32);

    public double getGraphScale() {
        return graphScaleProperty.get();
    }

    public void setGraphScale(double graphScale) {
        graphScaleProperty.set(graphScale);
    }

    public DoubleProperty graphScaleProperty() {
        return graphScaleProperty;
    }

    private MainUIController mainUIController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mainUI.fxml"));
        Parent root = loader.load();
        mainUIController = loader.getController();

        mainUIController.initApp(this);

        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public boolean startServer(int port) {
        LOGGER.info("Starting server");
        if (server.get() != null) {
            LOGGER.error("Server is already running!");
            return false;
        }

        try {
            server.set(new WebSpeakTestServer(port));
            return true;
        } catch (Exception e) {
            LOGGER.error("Exception starting server: ", e);
            return false;
        }
    }

    public CompletableFuture<Void> stopServer(int port) {
        if (!isServerRunning()) {
            LOGGER.error("Server is not running!");
            return CompletableFuture.completedFuture(null);
        }

        return server.get().shutdownAsync().thenRunAsync(() -> {
            server.set(null);
        }, Platform::runLater);
    }

    public boolean isServerRunning() {
        return server.get() != null;
    }

    public int getServerPort() {
        WebSpeakTestServer server = this.server.get();
        if (server == null) {
            throw new IllegalStateException("Server is not running!");
        }
        return server.getWebSpeakServer().getPort();
    }

    @Override
    public void stop() throws Exception {
        if (server.get() != null) {
            try {
                server.get().shutdownAndWait();
            } catch (Exception e) {
                LOGGER.error("Error stopping server", e);
            }
        }

        super.stop();
    }
}

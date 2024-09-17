package net.betrayd.webspeaktest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WebSpeakTest extends Application {

    private WebSpeakTestServer server;

    @Override
    public void start(Stage primaryStage) throws Exception {
        server = new WebSpeakTestServer(9090);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/mainUI.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (server != null)
            server.shutdown();
        super.stop();
    }

    public WebSpeakTestServer getServer() {
        return server;
    }
}

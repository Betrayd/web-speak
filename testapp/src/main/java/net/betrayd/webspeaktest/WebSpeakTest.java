package net.betrayd.webspeaktest;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.betrayd.webspeak.WebSpeakServer;

public class WebSpeakTest extends Application {
    public static void main(String[] args) throws Exception {

        WebSpeakServer server = new WebSpeakServer();
        server.start(9090);

        TestWebPlayer player1 = server.addPlayer(TestWebPlayer::new);
        System.out.println("SessionID for player1 is: " + player1.getSessionId());

        TestWebPlayer player2 = server.addPlayer(TestWebPlayer::new);
        System.out.println("SessionID for player2 is: " + player2.getSessionId());

        while (true) {
            server.tick();
            Thread.sleep(20);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Hello World!");
        Label label = new Label();
        label.setText("Hello World!");;

        StackPane root = new StackPane();
        root.getChildren().add(label);
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.show();
    }
}

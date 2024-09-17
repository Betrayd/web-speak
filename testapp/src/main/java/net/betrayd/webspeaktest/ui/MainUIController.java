package net.betrayd.webspeaktest.ui;

import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;

public class MainUIController {
    @FXML
    private void doHelloWorld() {
        LoggerFactory.getLogger(getClass()).info("Hello World!");
    }
}

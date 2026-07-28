package com.example.eposposlinkexample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class EposPosLinkApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ui/shell/shell-view.fxml"));
        Scene scene = new Scene(loader.load(), 960, 680);
        stage.setTitle("ePOS Sample PosLink");
        stage.setScene(scene);
        stage.show();
    }
}

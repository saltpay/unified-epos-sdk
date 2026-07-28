package com.example.eposposlinkexample.ui.tables;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AddTableDialog extends Dialog<String> {

    public AddTableDialog() {
        setTitle("Add table");

        ButtonType openButtonType = new ButtonType("Open table", ButtonBar.ButtonData.OK_DONE);
        DialogPane pane = getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.CANCEL, openButtonType);

        TextField nameField = new TextField();
        nameField.setPromptText("Table name (e.g. Table 5)");
        VBox content = new VBox(8, new Label("Table name"), nameField);
        content.setPadding(new Insets(10));
        pane.setContent(content);

        Node openButton = pane.lookupButton(openButtonType);
        openButton.setDisable(true);
        nameField.textProperty().addListener((observable, previous, current) -> openButton.setDisable(current.isBlank()));

        setResultConverter(button -> button == openButtonType ? nameField.getText().trim() : null);
        Platform.runLater(nameField::requestFocus);
    }
}

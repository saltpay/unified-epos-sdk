package com.example.eposposlinkexample.ui.shell;

import com.example.eposposlinkexample.teya.TeyaSdkManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class ShellController {

    @FXML
    private ToggleButton saleTab;
    @FXML
    private ToggleButton payAtTableTab;
    @FXML
    private ToggleButton historyTab;
    @FXML
    private MenuButton settingsMenu;
    @FXML
    private Label statusLabel;
    @FXML
    private StackPane contentArea;

    private final TeyaSdkManager sdk = TeyaSdkManager.getInstance();

    private Node saleView;
    private Node payAtTableView;
    private Node historyView;

    @FXML
    private void initialize() throws IOException {
        saleView = FXMLLoader.load(getClass().getResource("/com/example/eposposlinkexample/ui/sale/sale-view.fxml"));
        payAtTableView = FXMLLoader.load(getClass().getResource("/com/example/eposposlinkexample/ui/tables/pay-at-table-view.fxml"));
        historyView = FXMLLoader.load(getClass().getResource("/com/example/eposposlinkexample/ui/history/transaction-history-view.fxml"));
        contentArea.getChildren().addAll(saleView, payAtTableView, historyView);

        ToggleGroup group = new ToggleGroup();
        saleTab.setToggleGroup(group);
        payAtTableTab.setToggleGroup(group);
        historyTab.setToggleGroup(group);
        group.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                previous.setSelected(true);
                return;
            }
            showSelectedView();
        });
        saleTab.setSelected(true);
        showSelectedView();

        statusLabel.visibleProperty().bind(sdk.readyProperty().not());
        statusLabel.managedProperty().bind(sdk.readyProperty().not());
        settingsMenu.disableProperty().bind(sdk.readyProperty().not());

        sdk.setup();
    }

    private void showSelectedView() {
        showView(saleView, saleTab.isSelected());
        showView(payAtTableView, payAtTableTab.isSelected());
        showView(historyView, historyTab.isSelected());
    }

    private void showView(Node view, boolean visible) {
        view.setVisible(visible);
        view.setManaged(visible);
    }

    @FXML
    private void onClearUserAuth() {
        sdk.clearUserAuth();
        sdk.setup();
    }

    @FXML
    private void onClearDeviceLink() {
        sdk.clearDeviceLink();
        sdk.setup();
    }
}

package com.example.eposposlinkexample.ui.tables;

import com.example.eposposlinkexample.models.ProductItem;
import com.example.eposposlinkexample.util.PriceUtils;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ProductCatalogueDialog extends Dialog<Void> {

    public ProductCatalogueDialog(List<ProductItem> items) {
        setTitle("Add items");

        DialogPane pane = getDialogPane();
        pane.getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));

        FlowPane grid = new FlowPane(12, 12);
        grid.setPadding(new Insets(12));
        grid.setPrefWrapLength(520);
        for (ProductItem item : items) {
            grid.getChildren().add(createCard(item));
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(560, 440);
        pane.setContent(scroll);
    }

    private VBox createCard(ProductItem item) {
        Label name = new Label(item.product().name());
        name.setStyle("-fx-font-weight: bold;");
        Label lineTotal = new Label();
        lineTotal.textProperty().bind(Bindings.createStringBinding(
                () -> PriceUtils.formatPrice(item.lineTotal()), item.quantityProperty()));

        Label quantity = new Label();
        quantity.textProperty().bind(Bindings.format("x%d", item.quantityProperty()));
        quantity.setStyle("-fx-text-fill: #1447e6; -fx-font-weight: bold;");
        Hyperlink remove = new Hyperlink("Remove");
        remove.setOnAction(event -> item.remove());
        remove.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox quantityRow = new HBox(6, quantity, remove);
        quantityRow.visibleProperty().bind(item.quantityProperty().greaterThan(0));
        quantityRow.managedProperty().bind(item.quantityProperty().greaterThan(0));

        VBox card = new VBox(4, name, lineTotal, quantityRow);
        card.setPrefWidth(150);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: #dddbd7; -fx-border-radius: 8; -fx-padding: 12;");
        card.setOnMouseClicked(event -> item.add());
        return card;
    }
}

package com.example.eposposlinkexample.ui.sale;

import com.example.eposposlinkexample.models.Product;
import com.example.eposposlinkexample.models.ProductItem;
import com.example.eposposlinkexample.teya.TeyaSdkManager;
import com.example.eposposlinkexample.util.PriceUtils;

import com.teya.unifiedepossdk.PaymentState;
import com.teya.unifiedepossdk.PaymentStateDetails;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SaleController {

    @FXML private FlowPane productGrid;
    @FXML private Label itemCountLabel;
    @FXML private Label subtotalLabel;
    @FXML private TextField tipField;
    @FXML private CheckBox unreferencedRefundToggle;
    @FXML private Button printButton;
    @FXML private Button payButton;

    private final TeyaSdkManager sdk = TeyaSdkManager.getInstance();
    private final List<ProductItem> products = new ArrayList<>();
    private final IntegerProperty itemCount = new SimpleIntegerProperty(0);

    private BigDecimal subtotal = BigDecimal.ZERO;

    @FXML
    private void initialize() {
        for (Product product : Product.getProducts()) {
            ProductItem item = new ProductItem(product);
            item.quantityProperty().addListener((observable, previous, current) -> recompute());
            products.add(item);
            productGrid.getChildren().add(createCard(item));
        }

        tipField.textProperty().addListener((observable, previous, current) -> {
            if (!PriceUtils.isValidTipInput(current)) {
                tipField.setText(previous);
                return;
            }
            recompute();
        });

        unreferencedRefundToggle.selectedProperty().addListener((observable, previous, current) -> recompute());

        BooleanBinding payEnabled = sdk.readyProperty().and(itemCount.greaterThan(0));
        payButton.disableProperty().bind(payEnabled.not());
        printButton.disableProperty().bind(payEnabled.not());

        payButton.setOnAction(event -> pay());
        printButton.setOnAction(event -> sdk.printReceipt(products, PriceUtils.toMinorUnits(tipAmount())));

        recompute();
    }

    private VBox createCard(ProductItem item) {
        Label name = new Label(item.product().name());
        name.setStyle("-fx-font-weight: bold;");
        Label price = new Label(PriceUtils.formatPrice(item.product().price()));
        price.setStyle("-fx-text-fill: #6b6a67;");

        Label quantity = new Label();
        quantity.textProperty().bind(Bindings.format("x%d", item.quantityProperty()));
        quantity.setStyle("-fx-text-fill: #1447e6; -fx-font-weight: bold;");
        Hyperlink remove = new Hyperlink("Remove");
        remove.setOnAction(event -> item.remove());
        remove.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox quantityRow = new HBox(6, quantity, remove);
        quantityRow.visibleProperty().bind(item.quantityProperty().greaterThan(0));
        quantityRow.managedProperty().bind(item.quantityProperty().greaterThan(0));

        VBox card = new VBox(4, name, price, quantityRow);
        card.setPrefWidth(150);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8;"
                + " -fx-border-color: #dddbd7; -fx-border-radius: 8; -fx-padding: 12;");
        card.setOnMouseClicked(event -> item.add());
        return card;
    }

    private void recompute() {
        int count = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (ProductItem item : products) {
            count += item.getQuantity();
            total = total.add(item.lineTotal());
        }
        subtotal = total;
        itemCount.set(count);
        itemCountLabel.setText(count == 1 ? "1 item" : count + " items");
        subtotalLabel.setText("Subtotal: " + PriceUtils.formatPrice(subtotal));
        payButton.setText(unreferencedRefundToggle.isSelected()
                ? "Refund " + PriceUtils.formatPrice(subtotal)
                : "Pay " + PriceUtils.formatPrice(total()));
    }

    private void pay() {
        if (unreferencedRefundToggle.isSelected()) {
            sdk.makeUnreferencedRefund(PriceUtils.toMinorUnits(subtotal), this::clearBasketWhenSuccessful);
            return;
        }
        int totalMinor = PriceUtils.toMinorUnits(total());
        Integer tipMinor = tipAmount().signum() > 0 ? PriceUtils.toMinorUnits(tipAmount()) : null;
        sdk.makePayment(totalMinor, tipMinor, this::clearBasketWhenSuccessful);
    }

    private void clearBasketWhenSuccessful(PaymentStateDetails state) {
        Platform.runLater(() -> {
            if (state.isFinal() && state.getState() == PaymentState.Successful) {
                clearBasket();
            }
        });
    }

    private void clearBasket() {
        for (ProductItem item : products) {
            item.quantityProperty().set(0);
        }
        tipField.clear();
    }

    private BigDecimal tipAmount() {
        String text = tipField.getText();
        if (text == null || text.isBlank() || text.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text);
    }

    private BigDecimal total() {
        return subtotal.add(tipAmount());
    }
}

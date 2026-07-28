package com.example.eposposlinkexample.ui.tables;

import com.example.eposposlinkexample.ui.StatusTags;
import com.example.eposposlinkexample.util.PriceUtils;

import com.teya.unifiedepossdk.poslink.models.tabs.PaymentRequestSummary;
import com.teya.unifiedepossdk.poslink.models.tabs.Tab;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class PaymentsDialog extends Dialog<Void> {

    public PaymentsDialog(Tab tab) {
        setTitle("Payments");

        DialogPane pane = getDialogPane();
        pane.getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.setPrefWidth(420);

        content.getChildren().add(summaryRow("Paid",
                PriceUtils.formatMinor(tab.getTotalPaid() != null ? tab.getTotalPaid() : 0)));
        if (tab.getRemaining() != null) {
            content.getChildren().add(summaryRow("Remaining", PriceUtils.formatMinor(tab.getRemaining())));
        }
        content.getChildren().add(new Separator());

        List<PaymentRequestSummary> payments = tab.getPaymentRequests();
        if (payments == null || payments.isEmpty()) {
            content.getChildren().add(new Label("No payments yet"));
        } else {
            for (PaymentRequestSummary payment : payments) {
                content.getChildren().add(paymentRow(payment));
            }
        }

        pane.setContent(content);
    }

    private HBox summaryRow(String label, String value) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        return new HBox(8, new Label(label), spacer, new Label(value));
    }

    private HBox paymentRow(PaymentRequestSummary payment) {
        Label amount = new Label(PriceUtils.formatMinor(payment.getAmount()));
        amount.setStyle("-fx-font-weight: bold;");

        String method = payment.getMethod() != null ? capitalize(payment.getMethod().name()) : "";
        String type = payment.getType() != null ? capitalize(payment.getType().name()) : "";
        Label descriptor = new Label((method + " · " + type).trim());
        descriptor.setStyle("-fx-text-fill: #6b6a67;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(8, amount, descriptor, spacer, StatusTags.forPayment(payment.getStatus()));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : value.charAt(0) + value.substring(1).toLowerCase();
    }
}

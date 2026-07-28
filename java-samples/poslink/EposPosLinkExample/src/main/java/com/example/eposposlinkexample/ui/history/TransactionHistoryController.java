package com.example.eposposlinkexample.ui.history;

import com.example.eposposlinkexample.models.TransactionRecord;
import com.example.eposposlinkexample.teya.TeyaSdkManager;
import com.example.eposposlinkexample.teya.TransactionStore;
import com.example.eposposlinkexample.ui.StatusTags;
import com.example.eposposlinkexample.util.PriceUtils;

import com.teya.unifiedepossdk.models.TransactionType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TransactionHistoryController {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
            .ofPattern("HH:mm • dd MMM yyyy", Locale.UK)
            .withZone(ZoneId.systemDefault());

    @FXML private ListView<TransactionRecord> transactionList;

    private final TeyaSdkManager sdk = TeyaSdkManager.getInstance();
    private final Set<String> refundingIds = new HashSet<>();

    @FXML
    private void initialize() {
        transactionList.setItems(TransactionStore.getInstance().getTransactions());
        transactionList.setCellFactory(list -> new TransactionCell());
    }

    private void refund(TransactionRecord record) {
        refundingIds.add(record.id());
        transactionList.refresh();
        sdk.refundPayment(record.gatewayPaymentId(), record.amountMinor(), record.currency(),
                () -> Platform.runLater(() -> {
                    refundingIds.remove(record.id());
                    transactionList.refresh();
                }));
    }

    private HBox createRow(TransactionRecord record) {
        Label amount = new Label(PriceUtils.formatMinor(record.amountMinor()));
        amount.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        HBox amountRow = new HBox(8, amount, StatusTags.forTransaction(record));
        amountRow.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(4, amountRow, secondaryLabel(typeLabel(record.type())));
        String source = sourceLabel(record);
        if (source != null) {
            details.getChildren().add(secondaryLabel(source));
        }
        details.getChildren().add(secondaryLabel(TIMESTAMP.format(Instant.ofEpochMilli(record.timestamp()))));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, details, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        if (record.refundable()) {
            boolean refunding = refundingIds.contains(record.id());
            Button refundButton = new Button(refunding ? "Refunding…" : "Refund");
            refundButton.setDisable(refunding);
            refundButton.setOnAction(event -> refund(record));
            row.getChildren().add(refundButton);
        }
        return row;
    }

    private static Label secondaryLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6b6a67;");
        return label;
    }

    private static String typeLabel(TransactionType type) {
        return type == TransactionType.Payment ? "Payment" : "Refund";
    }

    private static String sourceLabel(TransactionRecord record) {
        if (record.type() != TransactionType.Payment) {
            return null;
        }
        if (!record.tabPayment()) {
            return "Card";
        }
        return record.cashPayment() ? "Cash - Table payment" : "Card - Table payment";
    }

    private final class TransactionCell extends ListCell<TransactionRecord> {

        @Override
        protected void updateItem(TransactionRecord record, boolean empty) {
            super.updateItem(record, empty);
            setGraphic(empty || record == null ? null : createRow(record));
        }
    }
}

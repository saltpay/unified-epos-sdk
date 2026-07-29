package com.example.eposposlinkexample.teya;

import com.example.eposposlinkexample.models.TransactionRecord;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * In-memory history of this app session, newest first. Records arrive from SDK callbacks off the
 * JavaFX thread, so every change is applied through {@link Platform#runLater}.
 */
public final class TransactionStore {

    private static final TransactionStore INSTANCE = new TransactionStore();

    private final ObservableList<TransactionRecord> transactions = FXCollections.observableArrayList();

    private TransactionStore() {
    }

    public static TransactionStore getInstance() {
        return INSTANCE;
    }

    public ObservableList<TransactionRecord> getTransactions() {
        return transactions;
    }

    void upsert(TransactionRecord record) {
        Platform.runLater(() -> {
            transactions.removeIf(existing -> existing.id().equals(record.id()));
            transactions.add(0, record);
        });
    }

    void markRefunded(String gatewayPaymentId) {
        Platform.runLater(() -> {
            for (int index = 0; index < transactions.size(); index++) {
                TransactionRecord record = transactions.get(index);
                if (gatewayPaymentId.equals(record.gatewayPaymentId())) {
                    transactions.set(index, record.asRefunded());
                }
            }
        });
    }
}

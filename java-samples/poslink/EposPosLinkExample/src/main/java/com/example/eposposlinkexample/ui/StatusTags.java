package com.example.eposposlinkexample.ui;

import com.example.eposposlinkexample.models.TransactionRecord;

import com.teya.unifiedepossdk.PaymentState;
import com.teya.unifiedepossdk.models.TransactionType;
import com.teya.unifiedepossdk.poslink.models.tabs.TabStatus;

import javafx.scene.control.Label;

public final class StatusTags {

    private StatusTags() {
    }

    public static Label forTab(TabStatus status) {
        return switch (status) {
            case OPEN, COMPLETED -> tag(status.name(), "#f2fae6", "#497d00");
            case PAYING -> tag(status.name(), "#eaf2ff", "#1447e6");
            case PAUSED -> tag(status.name(), "#fff5e6", "#bb4d00");
            default -> tag(status.name(), "#f1f0ee", "#1b1b19");
        };
    }

    public static Label forPayment(PaymentState status) {
        return switch (status) {
            case Successful -> tag("Paid", "#f2fae6", "#497d00");
            case Canceled -> tag("Canceled", "#f1f0ee", "#1b1b19");
            case ProcessingFailed, CommunicationFailed -> tag("Failed", "#feebed", "#e41e2b");
            default -> tag(status.name(), "#eaf2ff", "#1447e6");
        };
    }

    public static Label forTransaction(TransactionRecord record) {
        if (!record.successful()) {
            return tag("Failed", "#feebed", "#e41e2b");
        }
        return record.type() == TransactionType.Refund
                ? tag("Refunded", "#eaf2ff", "#1447e6")
                : tag("Paid", "#f2fae6", "#497d00");
    }

    public static Label billTag(String terminalId) {
        return tag("Bill on " + terminalId, "#eaf2ff", "#1447e6");
    }

    private static Label tag(String text, String background, String foreground) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + foreground + ";"
                + " -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11;");
        return label;
    }
}

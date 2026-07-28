package com.example.eposposlinkexample.models;

import com.teya.unifiedepossdk.models.TransactionType;

public record TransactionRecord(String id, TransactionType type, boolean successful, int amountMinor, String currency,
                                String gatewayPaymentId, long timestamp, boolean refunded, boolean cashPayment,
                                boolean tabPayment) {

    public boolean refundable() {
        return type == TransactionType.Payment && successful && !refunded && gatewayPaymentId != null && !cashPayment;
    }

    public TransactionRecord asRefunded() {
        return new TransactionRecord(id, type, successful, amountMinor, currency, gatewayPaymentId, timestamp,
                true, cashPayment, tabPayment);
    }
}

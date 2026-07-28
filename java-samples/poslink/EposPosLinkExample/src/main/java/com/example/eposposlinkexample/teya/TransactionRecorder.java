package com.example.eposposlinkexample.teya;

import com.example.eposposlinkexample.models.TransactionRecord;

import com.teya.unifiedepossdk.PaymentState;
import com.teya.unifiedepossdk.PaymentStateDetails;
import com.teya.unifiedepossdk.RefundResult;
import com.teya.unifiedepossdk.RefundResultDetails;
import com.teya.unifiedepossdk.models.GatewayPaymentId;
import com.teya.unifiedepossdk.models.GatewayRefundId;
import com.teya.unifiedepossdk.models.TransactionType;

import java.util.UUID;

final class TransactionRecorder {

    private TransactionRecorder() {
    }

    static void recordPaymentIfFinal(PaymentStateDetails state, TransactionType type, boolean tabPayment,
                                     boolean cashPayment) {
        if (!state.isFinal()) {
            return;
        }
        GatewayPaymentId gatewayPaymentId = state.getGatewayPaymentId();
        Long timestamp = state.getTransactionTimestamp();
        TransactionStore.getInstance().upsert(new TransactionRecord(
                state.getEposTransactionId(),
                type,
                state.getState() == PaymentState.Successful,
                state.getAmount(),
                state.getCurrency(),
                gatewayPaymentId != null ? gatewayPaymentId.getId() : null,
                timestamp != null ? timestamp : System.currentTimeMillis(),
                false,
                cashPayment,
                tabPayment));
    }

    static void recordRefund(RefundResultDetails refundResult, String gatewayPaymentId, int amountMinor,
                             String currency) {
        GatewayRefundId gatewayRefundId = refundResult.getGatewayRefundId();
        TransactionStore.getInstance().upsert(new TransactionRecord(
                gatewayRefundId != null ? gatewayRefundId.getId() : UUID.randomUUID().toString(),
                TransactionType.Refund,
                refundResult.getResult() == RefundResult.Success,
                amountMinor,
                currency,
                null,
                System.currentTimeMillis(),
                false,
                false,
                false));

        if (refundResult.getResult() == RefundResult.Success) {
            TransactionStore.getInstance().markRefunded(gatewayPaymentId);
        }
    }
}

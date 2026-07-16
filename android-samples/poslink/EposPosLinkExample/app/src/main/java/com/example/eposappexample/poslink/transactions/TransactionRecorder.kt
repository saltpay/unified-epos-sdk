package com.example.eposappexample.poslink.transactions

import com.teya.unifiedepossdk.PaymentState
import com.teya.unifiedepossdk.PaymentStateDetails
import com.teya.unifiedepossdk.RefundResult
import com.teya.unifiedepossdk.RefundResultDetails
import com.teya.unifiedepossdk.models.TransactionType
import java.util.UUID

object TransactionRecorder {

    fun recordPaymentIfFinal(
        state: PaymentStateDetails,
        type: TransactionType,
        isTab: Boolean = false,
        isCash: Boolean = false,
    ) {
        if (!state.isFinal) return
        TransactionStore.upsert(
            TransactionRecord(
                id = state.eposTransactionId,
                type = type,
                isSuccess = state.state == PaymentState.Successful,
                amountMinor = state.amount,
                currency = state.currency,
                gatewayPaymentId = state.gatewayPaymentId?.id,
                timestamp = state.transactionTimestamp ?: System.currentTimeMillis(),
                isCashPayment = isCash,
                isTabPayment = isTab,
            )
        )
    }

    fun recordRefund(
        refundResult: RefundResultDetails,
        gatewayPaymentId: String,
        amountMinor: Int,
        currency: String,
    ) {
        TransactionStore.upsert(
            TransactionRecord(
                id = refundResult.gatewayRefundId?.id ?: UUID.randomUUID().toString(),
                type = TransactionType.Refund,
                isSuccess = refundResult.result == RefundResult.Success,
                amountMinor = amountMinor,
                currency = currency,
                gatewayPaymentId = null,
                timestamp = System.currentTimeMillis(),
            )
        )
        if (refundResult.result == RefundResult.Success) {
            TransactionStore.markRefunded(gatewayPaymentId)
        }
    }
}
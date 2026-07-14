package com.example.eposappexample.poslink.transactions

import com.teya.unifiedepossdk.models.TransactionType

data class TransactionRecord(
    val id: String,
    val type: TransactionType,
    val isSuccess: Boolean,
    val statusLabel: String,
    val amountMinor: Int,
    val currency: String,
    val gatewayPaymentId: String?,
    val timestamp: Long,
    val isRefunded: Boolean = false,
) {

    // a referenced refund needs the original payment's gatewayPaymentId, so it must be present
    val isRefundable: Boolean
        get() = type == TransactionType.Payment && isSuccess && !isRefunded && gatewayPaymentId != null
}

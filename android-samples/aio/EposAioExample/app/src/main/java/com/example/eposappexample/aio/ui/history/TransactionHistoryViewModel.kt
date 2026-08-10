package com.example.eposappexample.aio.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.eposappexample.aio.TeyaUtils
import com.example.eposappexample.aio.transactions.TransactionRecord
import com.example.eposappexample.aio.transactions.TransactionStore

class TransactionHistoryViewModel : ViewModel() {

    val transactions: List<TransactionRecord> get() = TransactionStore.transactions

    var refundingIds by mutableStateOf<Set<String>>(emptySet())
        private set

    fun refund(record: TransactionRecord) {
        val paymentId = record.gatewayPaymentId ?: return
        if (record.id in refundingIds) return

        refundingIds = refundingIds + record.id
        TeyaUtils.refundPayment(
            gatewayPaymentId = paymentId,
            amountMinor = record.amountMinor,
            currency = record.currency,
        ) {
            refundingIds = refundingIds - record.id
        }
    }
}

package com.example.eposappexample.poslink.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object TransactionStore {

    var transactions by mutableStateOf<List<TransactionRecord>>(emptyList())
        private set

    fun upsert(record: TransactionRecord) {
        transactions = listOf(record) + transactions.filterNot { it.id == record.id }
    }

    fun markRefunded(gatewayPaymentId: String) {
        transactions = transactions.map {
            if (it.gatewayPaymentId == gatewayPaymentId) it.copy(isRefunded = true) else it
        }
    }
}

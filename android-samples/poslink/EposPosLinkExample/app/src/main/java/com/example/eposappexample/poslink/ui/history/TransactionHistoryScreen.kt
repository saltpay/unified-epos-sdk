package com.example.eposappexample.poslink.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eposappexample.poslink.formatMinor
import com.example.eposappexample.poslink.transactions.TransactionRecord
import com.teya.lemonade.Button
import com.teya.lemonade.LemonadeTheme
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Tag
import com.teya.lemonade.Text
import com.teya.lemonade.core.LemonadeButtonSize
import com.teya.lemonade.core.LemonadeButtonType
import com.teya.lemonade.core.LemonadeButtonVariant
import com.teya.lemonade.core.TagVoice
import com.teya.unifiedepossdk.models.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = viewModel()
) {
    val transactions = viewModel.transactions

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    LemonadeUi.Text(
                        "Transaction History",
                        textStyle = LemonadeTheme.typography.headingXSmall
                    )
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LemonadeUi.Text(
                    "No transactions yet. Complete a sale to see it here.",
                    textStyle = LemonadeTheme.typography.bodyLargeRegular
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(transactions, key = { it.id }) { record ->
                    TransactionRow(
                        record = record,
                        refunding = record.id in viewModel.refundingIds,
                        onRefund = { viewModel.refund(record) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    record: TransactionRecord,
    refunding: Boolean,
    onRefund: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LemonadeUi.Text(
                    formatMinor(record.amountMinor),
                    textStyle = LemonadeTheme.typography.headingXSmall
                )
                TransactionStatusTag(record)
            }
            LemonadeUi.Text(
                transactionTypeLabel(record.type),
                textStyle = LemonadeTheme.typography.bodyLargeRegular
            )
            LemonadeUi.Text(
                formatTimestamp(record.timestamp),
                textStyle = LemonadeTheme.typography.bodyLargeRegular
            )
        }

        if (record.isRefundable) {
            LemonadeUi.Button(
                label = if (refunding) "Refunding…" else "Refund",
                onClick = onRefund,
                variant = LemonadeButtonVariant.Secondary,
                type = LemonadeButtonType.Solid,
                size = LemonadeButtonSize.Large,
                enabled = !refunding
            )
        }
    }
}

@Composable
private fun TransactionStatusTag(record: TransactionRecord) {
    val (voice, label) = when {
        record.isSuccess && record.type == TransactionType.Refund -> TagVoice.Info to "Refunded"
        record.isSuccess -> TagVoice.Positive to "Paid"
        else -> TagVoice.Critical to "Failed"
    }
    LemonadeUi.Tag(label = label, voice = voice)
}

private fun transactionTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.Payment -> "Payment"
    TransactionType.Refund -> "Refund"
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yy · HH:mm", Locale.getDefault()).format(Date(timestamp))

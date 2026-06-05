package com.example.eposappexample.poslink.ui.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.eposappexample.poslink.formatMinor
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.ui.components.ProductGrid
import com.teya.lemonade.Button
import com.teya.lemonade.LemonadeTheme
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Tag
import com.teya.lemonade.Text
import com.teya.lemonade.core.LemonadeButtonSize
import com.teya.lemonade.core.LemonadeButtonType
import com.teya.lemonade.core.LemonadeButtonVariant
import com.teya.lemonade.core.TagVoice
import com.teya.unifiedepossdk.PaymentStateSubscription
import com.teya.unifiedepossdk.poslink.models.tabs.PaymentRequestSummary
import com.teya.unifiedepossdk.poslink.models.tabs.Tab
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailsScreen(
    viewModel: TablesViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab = viewModel.selectedTab ?: return
    val tabDetail = viewModel.selectedTabDetail
    val items = viewModel.selectedTabItems

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    LemonadeUi.Text(
                        selectedTab.tabName,
                        textStyle = LemonadeTheme.typography.headingXSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeTableDetails() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to tables"
                        )
                    }
                },
                actions = {
                    StatusTag(status = selectedTab.status)
                    IconButton(onClick = { viewModel.refreshSelectedTabDetail() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh tab details")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            selectedTab.showingBillTerminalId?.let { terminalId ->
                BillShowingBanner(
                    terminalId = terminalId,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            CurrentItems(
                items = items,
                onAddProduct = { viewModel.addProduct(it) },
                onRemoveProduct = { viewModel.removeProduct(it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LemonadeUi.Button(
                    label = "Close table",
                    onClick = { viewModel.closeTab(selectedTab.tabId) },
                    variant = LemonadeButtonVariant.Critical,
                    type = LemonadeButtonType.Subtle,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.padding(end = 16.dp)
                )
                LemonadeUi.Button(
                    label = "Add items",
                    onClick = { viewModel.showProductCatalogue() },
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            HorizontalDivider()

            if (tabDetail != null) {
                PaymentsSummary(
                    tab = tabDetail,
                    onViewPayments = { viewModel.showPaymentsDialog() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LemonadeUi.Text("Total", textStyle = LemonadeTheme.typography.headingXSmall)
                LemonadeUi.Text(
                    formatMinor(viewModel.tabTotalMinor(selectedTab.tabId)),
                    textStyle = LemonadeTheme.typography.headingXSmall,
                    color = LemonadeTheme.colors.content.contentBrand
                )
            }
        }

        if (viewModel.showProductCatalogue) {
            ProductCatalogueDialog(
                basket = items,
                onAdd = { viewModel.addProduct(it) },
                onRemove = { viewModel.removeProduct(it) },
                onDismiss = { viewModel.dismissProductCatalogue() }
            )
        }

        if (viewModel.showPaymentsDialog && tabDetail != null) {
            PaymentsDialog(tab = tabDetail, onDismiss = { viewModel.dismissPaymentsDialog() })
        }
    }
}

@Composable
private fun ProductCatalogueDialog(
    basket: List<Product>,
    onAdd: (Product) -> Unit,
    onRemove: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                LemonadeUi.Text(
                    "Add items",
                    textStyle = LemonadeTheme.typography.headingSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ProductGrid(
                    products = Product.getProducts(),
                    basket = basket,
                    onAdd = onAdd,
                    onRemove = onRemove,
                    modifier = Modifier.heightIn(max = 420.dp)
                )
                LemonadeUi.Button(
                    label = "Done",
                    onClick = onDismiss,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BillShowingBanner(terminalId: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LemonadeUi.Text(
            "Bill showing on terminal ID",
            textStyle = LemonadeTheme.typography.bodyMediumRegular,
            color = LemonadeTheme.colors.content.contentSecondary
        )
        LemonadeUi.Tag(label = terminalId, voice = TagVoice.Info)
    }
}

@Composable
private fun CurrentItems(
    items: List<Product>,
    onAddProduct: (Product) -> Unit,
    onRemoveProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LemonadeUi.Text("Current items", textStyle = LemonadeTheme.typography.headingXSmall)
        if (items.isEmpty()) {
            LemonadeUi.Text(
                "No items yet. Tap \"Add items\" to get started.",
                textStyle = LemonadeTheme.typography.bodyMediumRegular,
                color = LemonadeTheme.colors.content.contentSecondary
            )
            return
        }
        items.forEach { product ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LemonadeUi.Text(
                    "${product.emoji} ${product.name}",
                    textStyle = LemonadeTheme.typography.bodyMediumRegular,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onRemoveProduct(product) }) {
                        LemonadeUi.Text(
                            "−",
                            textStyle = LemonadeTheme.typography.headingSmall,
                            color = LemonadeTheme.colors.content.contentBrand
                        )
                    }
                    LemonadeUi.Text(
                        "${product.quantity}",
                        textStyle = LemonadeTheme.typography.bodyMediumRegular
                    )
                    IconButton(onClick = { onAddProduct(product) }) {
                        LemonadeUi.Text(
                            "+",
                            textStyle = LemonadeTheme.typography.headingSmall,
                            color = LemonadeTheme.colors.content.contentBrand
                        )
                    }
                }
                LemonadeUi.Text(
                    formatPrice(product.price * product.quantity),
                    textStyle = LemonadeTheme.typography.bodyMediumRegular,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentsDialog(tab: Tab, onDismiss: () -> Unit) {
    val payments = tab.paymentRequests.orEmpty()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LemonadeUi.Text("Payments", textStyle = LemonadeTheme.typography.headingSmall)
                AmountRow(label = "Paid", amountMinor = tab.totalPaid ?: 0)
                tab.remaining?.let { AmountRow(label = "Remaining", amountMinor = it) }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    payments.forEach { payment -> PaymentRow(payment) }
                }
                LemonadeUi.Button(
                    label = "Done",
                    onClick = onDismiss,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PaymentsSummary(tab: Tab, onViewPayments: () -> Unit, modifier: Modifier = Modifier) {
    val payments = tab.paymentRequests.orEmpty()
    val ongoing = payments.firstOrNull { !it.status.isFinal }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AmountRow(label = "Paid", amountMinor = tab.totalPaid ?: 0)
        if (ongoing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LemonadeUi.Text(
                    "Payment in progress: ${formatMinor(ongoing.amount)}",
                    textStyle = LemonadeTheme.typography.bodyMediumRegular,
                    color = LemonadeTheme.colors.content.contentInfo
                )
                PaymentStatusTag(state = ongoing.status)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LemonadeUi.Text(
                if (payments.isEmpty()) "No payments yet" else "Payments (${payments.size})",
                textStyle = LemonadeTheme.typography.bodyMediumRegular,
                color = LemonadeTheme.colors.content.contentSecondary
            )
            if (payments.isNotEmpty()) {
                LemonadeUi.Button(
                    label = "View payments",
                    onClick = onViewPayments,
                    variant = LemonadeButtonVariant.Neutral,
                    type = LemonadeButtonType.Ghost,
                    size = LemonadeButtonSize.Small
                )
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, amountMinor: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LemonadeUi.Text(label, textStyle = LemonadeTheme.typography.bodyMediumRegular)
        LemonadeUi.Text(
            formatMinor(amountMinor),
            textStyle = LemonadeTheme.typography.bodyMediumRegular
        )
    }
}


@Composable
private fun PaymentRow(payment: PaymentRequestSummary) {
    val descriptor = listOfNotNull(
        payment.method?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
        payment.type.name.lowercase().replaceFirstChar { it.uppercase() }
    ).joinToString(" · ")
    val secondary = LemonadeTheme.colors.content.contentSecondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LemonadeUi.Text(
                formatMinor(payment.amount),
                textStyle = LemonadeTheme.typography.bodyMediumSemiBold
            )
            if (descriptor.isNotEmpty()) {
                LemonadeUi.Text(
                    descriptor,
                    textStyle = LemonadeTheme.typography.bodySmallRegular,
                    color = secondary
                )
            }
            payment.tip?.takeIf { it > 0 }?.let {
                LemonadeUi.Text(
                    "Tip: ${formatMinor(it)}",
                    textStyle = LemonadeTheme.typography.bodySmallRegular,
                    color = secondary
                )
            }

            val timestamp = if (payment.method == TabPaymentMethod.CARD) {
                payment.transactionTimestampEpochMillis
            } else {
                payment.updatedAtEpochMillis
            }
            timestamp?.let {
                LemonadeUi.Text(
                    formatTimestamp(it),
                    textStyle = LemonadeTheme.typography.bodySmallRegular,
                    color = secondary
                )
            }
        }
        PaymentStatusTag(state = payment.status)
    }
}

private fun formatTimestamp(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yy · HH:mm", Locale.getDefault()).format(Date(epochMillis))

@Composable
private fun PaymentStatusTag(state: PaymentStateSubscription.PaymentState) {
    val (voice, label) = when (state) {
        PaymentStateSubscription.PaymentState.Successful -> TagVoice.Positive to "Paid"
        PaymentStateSubscription.PaymentState.Canceled -> TagVoice.Neutral to "Canceled"
        PaymentStateSubscription.PaymentState.ProcessingFailed -> TagVoice.Critical to "Failed"
        PaymentStateSubscription.PaymentState.CommunicationFailed -> TagVoice.Critical to "Failed"
        else -> TagVoice.Info to state.name
    }
    LemonadeUi.Tag(label = label, voice = voice)
}

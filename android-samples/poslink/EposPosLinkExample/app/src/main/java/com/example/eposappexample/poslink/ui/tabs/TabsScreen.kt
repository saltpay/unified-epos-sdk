package com.example.eposappexample.poslink.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eposappexample.poslink.formatMinor
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.ui.components.ProductGrid
import com.teya.lemonade.Button
import com.teya.lemonade.Card
import com.teya.lemonade.LemonadeTheme
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Switch
import com.teya.lemonade.Tag
import com.teya.lemonade.Text
import com.teya.lemonade.TextField
import com.teya.lemonade.core.LemonadeButtonSize
import com.teya.lemonade.core.LemonadeButtonType
import com.teya.lemonade.core.LemonadeButtonVariant
import com.teya.lemonade.core.LemonadeCardBackground
import com.teya.lemonade.core.LemonadeCardPadding
import com.teya.lemonade.core.TagVoice
import com.teya.unifiedepossdk.PaymentStateSubscription
import com.teya.unifiedepossdk.poslink.models.tabs.PaymentRequestSummary
import com.teya.unifiedepossdk.poslink.models.tabs.Tab
import com.teya.unifiedepossdk.poslink.models.tabs.TabId
import com.teya.unifiedepossdk.poslink.models.tabs.TabStatus
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabsScreen(
    modifier: Modifier = Modifier,
    viewModel: TabsViewModel = viewModel()
) {
    val selectedTab = viewModel.selectedTab
    if (selectedTab != null) {
        TableDetailsScreen(
            modifier = modifier,
            tabName = selectedTab.tabName,
            status = selectedTab.status,
            tabDetail = viewModel.selectedTabDetail,
            items = viewModel.selectedTabItems,
            totalMinor = viewModel.tabTotalMinor(selectedTab.tabId),
            showProductCatalogue = viewModel.showProductCatalogue,
            showPaymentsDialog = viewModel.showPaymentsDialog,
            onAddProduct = { viewModel.addProduct(it) },
            onRemoveProduct = { viewModel.removeProduct(it) },
            onShowCatalogue = { viewModel.showProductCatalogue() },
            onDismissCatalogue = { viewModel.dismissProductCatalogue() },
            onShowPayments = { viewModel.showPaymentsDialog() },
            onDismissPayments = { viewModel.dismissPaymentsDialog() },
            onCloseTab = { viewModel.closeTab(selectedTab.tabId) },
            onRefreshDetail = { viewModel.refreshSelectedTabDetail() },
            onBack = { viewModel.closeTableDetails() }
        )
    } else {
        TablesScreen(modifier = modifier, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TablesScreen(
    modifier: Modifier,
    viewModel: TabsViewModel
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { LemonadeUi.Text("Pay at Table", textStyle = LemonadeTheme.typography.headingXSmall) },
                actions = {
                    IconButton(onClick = { viewModel.refreshTabs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh tabs")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddTableDialog() },
                containerColor = LemonadeTheme.colors.background.bgBrand,
                contentColor = LemonadeTheme.colors.content.contentOnBrandHigh,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add table")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LemonadeUi.Switch(
                checked = viewModel.patEnabled,
                onCheckedChange = { viewModel.setPayAtTableEnabled(it) },
                label = "Pay at Table on store",
                modifier = Modifier.padding(16.dp)
            )

            TablesGrid(
                tabs = viewModel.openTabs,
                totalForTab = { viewModel.tabTotalMinor(it) },
                onOpenDetails = { viewModel.openTableDetails(it) }
            )
        }

        if (viewModel.showAddTableDialog) {
            AddTableDialog(
                tabName = viewModel.tabNameInput,
                canOpenTab = viewModel.canOpenTab,
                onTabNameChange = { viewModel.updateTabName(it) },
                onOpenTab = { viewModel.openTab() },
                onDismiss = { viewModel.dismissAddTableDialog() }
            )
        }
    }
}

@Composable
private fun TablesGrid(
    tabs: List<TabSummary>,
    totalForTab: (TabId) -> Int,
    onOpenDetails: (TabId) -> Unit
) {
    if (tabs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LemonadeUi.Text(
                "No open tables yet.\nTap + to add one.",
                textStyle = LemonadeTheme.typography.bodyMediumRegular,
                color = LemonadeTheme.colors.content.contentSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tabs) { tab ->
            TableTile(
                tab = tab,
                totalMinor = totalForTab(tab.tabId),
                onClick = { onOpenDetails(tab.tabId) }
            )
        }
    }
}

@Composable
private fun TableTile(tab: TabSummary, totalMinor: Int, onClick: () -> Unit) {
    LemonadeUi.Card(
        modifier = Modifier
            .heightIn(min = 160.dp)
            .widthIn(min = 160.dp)
            .clickable(onClick = onClick),
        contentPadding = LemonadeCardPadding.Medium,
        background = LemonadeCardBackground.Elevated
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LemonadeUi.Text(tab.tabName, textStyle = LemonadeTheme.typography.headingXSmall)
            LemonadeUi.Text(
                formatMinor(totalMinor),
                textStyle = LemonadeTheme.typography.bodyMediumRegular,
                color = LemonadeTheme.colors.content.contentBrand
            )
            StatusTag(status = tab.status)
        }
    }
}

@Composable
private fun StatusTag(status: TabStatus) {
    val (voice, label) = when (status) {
        TabStatus.OPEN -> TagVoice.Positive to "Open"
        TabStatus.PAYING -> TagVoice.Info to "Paying"
        TabStatus.PAUSED -> TagVoice.Warning to "Paused"
        TabStatus.COMPLETED -> TagVoice.Positive to "Completed"
        TabStatus.CLOSED -> TagVoice.Neutral to "Closed"
        TabStatus.UNKNOWN -> TagVoice.Neutral to "Unknown"
    }
    LemonadeUi.Tag(label = label, voice = voice)
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
private fun AmountRow(label: String, amountMinor: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LemonadeUi.Text(label, textStyle = LemonadeTheme.typography.bodyMediumRegular)
        LemonadeUi.Text(formatMinor(amountMinor), textStyle = LemonadeTheme.typography.bodyMediumRegular)
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
                LemonadeUi.Text(descriptor, textStyle = LemonadeTheme.typography.bodySmallRegular, color = secondary)
            }
            payment.tip?.takeIf { it > 0 }?.let {
                LemonadeUi.Text("Tip: ${formatMinor(it)}", textStyle = LemonadeTheme.typography.bodySmallRegular, color = secondary)
            }
            payment.transactionTimestampEpochMillis?.let {
                LemonadeUi.Text(formatTimestamp(it), textStyle = LemonadeTheme.typography.bodySmallRegular, color = secondary)
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

@Composable
private fun AddTableDialog(
    tabName: String,
    canOpenTab: Boolean,
    onTabNameChange: (String) -> Unit,
    onOpenTab: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LemonadeUi.Text("Add table", textStyle = LemonadeTheme.typography.headingSmall)
                LemonadeUi.TextField(
                    input = tabName,
                    onInputChanged = onTabNameChange,
                    label = "Table name (e.g. Table 5)",
                    modifier = Modifier.fillMaxWidth()
                )

                LemonadeUi.Button(
                    label = "Open table",
                    onClick = onOpenTab,
                    enabled = canOpenTab,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
                LemonadeUi.Button(
                    label = "Cancel",
                    onClick = onDismiss,
                    variant = LemonadeButtonVariant.Neutral,
                    type = LemonadeButtonType.Ghost,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableDetailsScreen(
    modifier: Modifier,
    tabName: String,
    status: TabStatus,
    tabDetail: Tab?,
    items: List<Product>,
    totalMinor: Int,
    showProductCatalogue: Boolean,
    showPaymentsDialog: Boolean,
    onAddProduct: (Product) -> Unit,
    onRemoveProduct: (Product) -> Unit,
    onShowCatalogue: () -> Unit,
    onDismissCatalogue: () -> Unit,
    onShowPayments: () -> Unit,
    onDismissPayments: () -> Unit,
    onCloseTab: () -> Unit,
    onRefreshDetail: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { LemonadeUi.Text(tabName, textStyle = LemonadeTheme.typography.headingXSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to tables"
                        )
                    }
                },
                actions = {
                    StatusTag(status = status)
                    IconButton(onClick = onRefreshDetail) {
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
            CurrentItems(
                items = items,
                onAddProduct = onAddProduct,
                onRemoveProduct = onRemoveProduct,
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
                    onClick = onCloseTab,
                    variant = LemonadeButtonVariant.Critical,
                    type = LemonadeButtonType.Subtle,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.padding(end = 16.dp)
                )
                LemonadeUi.Button(
                    label = "Add items",
                    onClick = onShowCatalogue,
                    size = LemonadeButtonSize.Large,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            HorizontalDivider()

            if (tabDetail != null) {
                PaymentsSummary(
                    tab = tabDetail,
                    onViewPayments = onShowPayments,
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
                    formatMinor(totalMinor),
                    textStyle = LemonadeTheme.typography.headingXSmall,
                    color = LemonadeTheme.colors.content.contentBrand
                )
            }
        }

        if (showProductCatalogue) {
            ProductCatalogueDialog(
                basket = items,
                onAdd = onAddProduct,
                onRemove = onRemoveProduct,
                onDismiss = onDismissCatalogue
            )
        }

        if (showPaymentsDialog && tabDetail != null) {
            PaymentsDialog(tab = tabDetail, onDismiss = onDismissPayments)
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

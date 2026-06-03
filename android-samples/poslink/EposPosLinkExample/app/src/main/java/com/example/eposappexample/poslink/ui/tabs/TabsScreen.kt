package com.example.eposappexample.poslink.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eposappexample.poslink.formatMinor
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.ui.components.ProductGrid
import com.teya.unifiedepossdk.poslink.models.tabs.TabId
import com.teya.unifiedepossdk.poslink.models.tabs.TabStatus
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary

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
            items = viewModel.selectedTabItems,
            totalMinor = viewModel.tabTotalMinor(selectedTab.tabId),
            showProductCatalogue = viewModel.showProductCatalogue,
            onAddProduct = { viewModel.addProduct(it) },
            onRemoveProduct = { viewModel.removeProduct(it) },
            onShowCatalogue = { viewModel.showProductCatalogue() },
            onDismissCatalogue = { viewModel.dismissProductCatalogue() },
            onCloseTab = { viewModel.closeTab(selectedTab.tabId) },
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
                title = { Text("Pay at Table") },
                actions = {
                    IconButton(onClick = { viewModel.refreshTabs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh tabs")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddTableDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add table")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EnableRow(
                enabled = viewModel.patEnabled,
                onToggle = { viewModel.setPayAtTableEnabled(it) },
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
private fun EnableRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Pay at Table on store",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
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
            Text(
                "No open tables yet.\nTap + to add one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        onClick = onClick, modifier = Modifier
            .heightIn(min = 160.dp)
            .widthIn(min = 160.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(tab.tabName, style = MaterialTheme.typography.titleMedium)
            Text(
                formatMinor(totalMinor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            StatusBadge(status = tab.status)
        }
    }
}

@Composable
private fun StatusBadge(status: TabStatus) {
    val (container, content, label) = when (status) {
        TabStatus.OPEN -> Triple(Color(0xFFDCF5E3), Color(0xFF1B5E20), "Open")
        TabStatus.PAYING -> Triple(Color(0xFFDCE7FB), Color(0xFF0D47A1), "Paying")
        TabStatus.PAUSED -> Triple(Color(0xFFFFF1D6), Color(0xFF8A5A00), "Paused")
        TabStatus.COMPLETED -> Triple(Color(0xFFD7F1EF), Color(0xFF00695C), "Completed")
        TabStatus.CLOSED -> Triple(Color(0xFFE6E6E6), Color(0xFF5F5F5F), "Closed")
        TabStatus.UNKNOWN -> Triple(Color(0xFFE6E6E6), Color(0xFF5F5F5F), "Unknown")
    }
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
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
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add table", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = tabName,
                    onValueChange = onTabNameChange,
                    label = { Text("Table name (e.g. Table 5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onOpenTab,
                    enabled = canOpenTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Open table")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableDetailsScreen(
    modifier: Modifier,
    tabName: String,
    items: List<Product>,
    totalMinor: Int,
    showProductCatalogue: Boolean,
    onAddProduct: (Product) -> Unit,
    onRemoveProduct: (Product) -> Unit,
    onShowCatalogue: () -> Unit,
    onDismissCatalogue: () -> Unit,
    onCloseTab: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(tabName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to tables"
                        )
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
                OutlinedButton(
                    onClick = onCloseTab,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("Close tab", modifier = Modifier.padding(start = 8.dp))
                }

                Button(
                    onClick = onShowCatalogue,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add items", modifier = Modifier.padding(start = 8.dp))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMinor(totalMinor),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
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
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    "Add items",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ProductGrid(
                    products = Product.getProducts(),
                    basket = basket,
                    onAdd = onAdd,
                    onRemove = onRemove,
                    modifier = Modifier.heightIn(max = 420.dp)
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                ) {
                    Text("Done")
                }
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
        Text("Current items", style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) {
            Text(
                "No items yet. Tap \"Add items\" to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        items.forEach { product ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${product.emoji} ${product.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onRemoveProduct(product) }) {
                        Text(
                            "−",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "${product.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onAddProduct(product) }) {
                        Text(
                            "+",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    formatPrice(product.price * product.quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

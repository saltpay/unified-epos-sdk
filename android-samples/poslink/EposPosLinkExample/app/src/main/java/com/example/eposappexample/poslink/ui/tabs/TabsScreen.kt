package com.example.eposappexample.poslink.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eposappexample.poslink.formatMinor
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.ui.components.ProductGrid
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    modifier: Modifier = Modifier,
    viewModel: TabsViewModel = viewModel()
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Pay at Table") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExplainerBanner()

            EnableRow(
                enabled = viewModel.patEnabled,
                connection = viewModel.connectionState.name,
                onToggle = { viewModel.setPayAtTableEnabled(it) }
            )

            SectionTitle("1. Build an order")
            ProductGrid(
                products = Product.getProducts(),
                basket = viewModel.basket,
                onAdd = { viewModel.addProduct(it) },
                onRemove = { viewModel.removeProduct(it) },
                modifier = Modifier.heightIn(max = 360.dp)
            )

            SectionTitle("2. Open a tab")
            OutlinedTextField(
                value = viewModel.tabNameInput,
                onValueChange = { viewModel.updateTabName(it) },
                label = { Text("Table name (e.g. Table 5)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.openTab() },
                enabled = viewModel.canOpenTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Open tab · ${formatMinor(viewModel.basketTotalMinor)}")
            }

            SectionTitle("3. Open tabs")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${viewModel.openTabs.size} open", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { viewModel.refreshTabs() }) { Text("Refresh") }
            }
            if (viewModel.openTabs.isEmpty()) {
                Text("No open tabs yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                viewModel.openTabs.forEach { tab ->
                    OpenTabRow(tab = tab, onClose = { viewModel.closeTab(tab.tabId) })
                }
            }

            SectionTitle("Event log")
            EventLog(entries = viewModel.eventLog)
        }
    }
}

@Composable
private fun ExplainerBanner() {
    Card {
        Text(
            "In Pay at Table the terminal drives the bill and payment. This app registers tabs " +
                "and responds to terminal-initiated events. Watch the event log below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun EnableRow(enabled: Boolean, connection: String, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Pay at Table on store", style = MaterialTheme.typography.titleMedium)
            Text("Stream: $connection", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SectionTitle(text: String) {
    HorizontalDivider()
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun OpenTabRow(tab: TabSummary, onClose: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tab.tabName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${tab.tabId.value} · ${tab.status}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
private fun EventLog(entries: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (entries.isEmpty()) {
                Text("No events yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                entries.forEach { entry ->
                    Text(
                        entry,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

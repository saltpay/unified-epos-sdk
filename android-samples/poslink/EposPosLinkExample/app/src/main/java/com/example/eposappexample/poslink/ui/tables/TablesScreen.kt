package com.example.eposappexample.poslink.ui.tables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.example.eposappexample.poslink.formatMinor
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
import com.teya.unifiedepossdk.poslink.models.tabs.TabId
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    LemonadeUi.Text(
                        "Pay at Table",
                        textStyle = LemonadeTheme.typography.headingXSmall
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.getTabs() }) {
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
            tab.showingBillTerminalId?.let { terminalId ->
                LemonadeUi.Tag(label = "Bill on $terminalId", voice = TagVoice.Info)
            }
        }
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

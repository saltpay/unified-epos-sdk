package com.example.eposappexample.poslink.ui.tables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PayAtTableScreen(
    modifier: Modifier = Modifier,
    viewModel: TablesViewModel = viewModel()
) {
    if (viewModel.selectedTab != null) {
        TableDetailsScreen(viewModel = viewModel, modifier = modifier)
    } else {
        TablesScreen(modifier = modifier, viewModel = viewModel)
    }
}
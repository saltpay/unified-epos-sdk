package com.example.eposappexample.poslink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eposappexample.poslink.teya.TeyaUtils
import com.example.eposappexample.poslink.ui.sale.SaleScreen
import com.example.eposappexample.poslink.ui.tables.PayAtTableScreen
import com.example.eposappexample.poslink.ui.theme.EposAppExampleTheme
import com.teya.lemonade.Icon
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.core.LemonadeIcons

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TeyaUtils.setUp()

        setContent {
            EposAppExampleTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            Column {
                HorizontalDivider()
                NavigationBar {
                    Destination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = dest.icon,
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Sale.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Sale.route) { SaleScreen() }
            composable(Destination.Tabs.route) { PayAtTableScreen() }
        }
    }
}

private enum class Destination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
) {
    Sale(
        "sale",
        "Sale",
        { LemonadeUi.Icon(icon = LemonadeIcons.Basket, contentDescription = null) }),
    Tabs(
        "tabs",
        "Pay at Table",
        { LemonadeUi.Icon(icon = LemonadeIcons.ForkKnife, contentDescription = null) })
}
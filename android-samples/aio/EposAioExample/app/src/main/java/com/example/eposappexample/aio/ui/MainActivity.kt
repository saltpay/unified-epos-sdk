package com.example.eposappexample.aio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eposappexample.aio.TeyaUtils
import com.example.eposappexample.aio.currencySymbol
import com.example.eposappexample.aio.formatPrice
import com.example.eposappexample.aio.isValidTipInput
import com.example.eposappexample.aio.models.Product
import com.example.eposappexample.aio.ui.history.TransactionHistoryScreen
import com.example.eposappexample.aio.ui.theme.EposAppExampleTheme
import com.teya.lemonade.Button
import com.teya.lemonade.Card
import com.teya.lemonade.Icon
import com.teya.lemonade.LemonadeTheme
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Text
import com.teya.lemonade.TextField
import com.teya.lemonade.core.LemonadeButtonSize
import com.teya.lemonade.core.LemonadeButtonType
import com.teya.lemonade.core.LemonadeButtonVariant
import com.teya.lemonade.core.LemonadeCardBackground
import com.teya.lemonade.core.LemonadeCardPadding
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
                            label = { LemonadeUi.Text(dest.label) }
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
            composable(Destination.Sale.route) { MainScreen() }
            composable(Destination.History.route) { TransactionHistoryScreen() }
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
    History(
        "history",
        "History",
        { Icon(Icons.Default.DateRange, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(viewModel: MainViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    LemonadeUi.Text(
                        "ePOS Sample AIO",
                        textStyle = LemonadeTheme.typography.headingXSmall
                    )
                }
            )
        },
        bottomBar = {
            BottomBar(
                itemCount = viewModel.itemCount,
                subtotal = viewModel.subtotal,
                total = viewModel.total,
                tipInput = viewModel.tipInput,
                onTipInputChange = { viewModel.updateTipInput(it) },
                onPay = { viewModel.pay() },
                onPrint = { viewModel.printReceipt() },
                payEnabled = viewModel.payEnabled
            )
        }
    ) { padding ->
        ProductGrid(
            products = Product.getProducts(),
            basket = viewModel.basket,
            onAdd = { viewModel.addProduct(it) },
            onRemove = { viewModel.removeProduct(it) },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun BottomBar(
    itemCount: Int,
    subtotal: Double,
    total: Double,
    tipInput: String,
    onTipInputChange: (String) -> Unit,
    onPay: () -> Unit,
    onPrint: () -> Unit,
    payEnabled: Boolean
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LemonadeUi.Text(
                    "$itemCount item${if (itemCount != 1) "s" else ""}",
                    textStyle = LemonadeTheme.typography.bodyLargeRegular
                )
                LemonadeUi.Text(
                    "Subtotal: ${formatPrice(subtotal)}",
                    textStyle = LemonadeTheme.typography.bodyLargeRegular
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LemonadeUi.TextField(
                input = tipInput,
                onInputChanged = { newValue ->
                    if (isValidTipInput(newValue)) {
                        onTipInputChange(newValue)
                    }
                },
                placeholderText = "Tip (${currencySymbol})",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LemonadeUi.Button(
                    label = "Print",
                    onClick = onPrint,
                    variant = LemonadeButtonVariant.Secondary,
                    type = LemonadeButtonType.Subtle,
                    size = LemonadeButtonSize.Large,
                    enabled = payEnabled,
                    modifier = Modifier.weight(1f)
                )
                LemonadeUi.Button(
                    label = "Pay ${formatPrice(total)}",
                    onClick = onPay,
                    variant = LemonadeButtonVariant.Primary,
                    type = LemonadeButtonType.Solid,
                    size = LemonadeButtonSize.Large,
                    enabled = payEnabled,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    basket: List<Product>,
    onAdd: (Product) -> Unit,
    onRemove: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            val count = basket.find { it.id == product.id }?.quantity ?: 0
            ProductCard(
                product = product,
                count = count,
                onAdd = { onAdd(product) },
                onRemove = { onRemove(product) }
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    count: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    LemonadeUi.Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 125.dp)
            .clickable(onClick = onAdd),
        contentPadding = LemonadeCardPadding.Medium,
        background = LemonadeCardBackground.Elevated
    ) {
        LemonadeUi.Text(
            product.emoji,
            textStyle = LemonadeTheme.typography.headingMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        LemonadeUi.Text(
            product.name,
            textStyle = LemonadeTheme.typography.headingXSmall
        )
        LemonadeUi.Text(
            formatPrice(product.price),
            textStyle = LemonadeTheme.typography.bodyMediumRegular,
            color = LemonadeTheme.colors.content.contentSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (count > 0) {
                LemonadeUi.Text(
                    "x$count",
                    textStyle = LemonadeTheme.typography.bodyMediumSemiBold,
                    color = LemonadeTheme.colors.content.contentBrand
                )
                LemonadeUi.Button(
                    label = "Remove",
                    onClick = onRemove,
                    variant = LemonadeButtonVariant.Neutral,
                    type = LemonadeButtonType.Ghost,
                    size = LemonadeButtonSize.Small
                )
            }
        }
    }
}

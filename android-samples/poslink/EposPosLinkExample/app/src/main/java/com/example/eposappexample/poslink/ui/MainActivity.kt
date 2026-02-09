package com.example.eposappexample.poslink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eposappexample.poslink.TeyaUtils
import com.example.eposappexample.poslink.currencySymbol
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.isValidTipInput
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.ui.theme.EposAppExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TeyaUtils.setUp()

        setContent {
            EposAppExampleTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen(viewModel: MainViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopBar(
                onClearUserAuth = { viewModel.clearUserAuth() },
                onClearDeviceLink = { viewModel.clearDeviceLink() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onClearUserAuth: () -> Unit,
    onClearDeviceLink: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("ePOS Sample Poslink") },
        actions = {
            IconButton(onClick = {
                showMenu = true
            }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                }
            ) {
                DropdownMenuItem(
                    text = { Text("Clear User Auth") },
                    onClick = {
                        showMenu = false
                        onClearUserAuth()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear Device Link") },
                    onClick = {
                        showMenu = false
                        onClearDeviceLink()
                    }
                )
            }
        }
    )
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
                Text(
                    "$itemCount item${if (itemCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Subtotal: ${formatPrice(subtotal)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tipInput,
                onValueChange = { newValue ->
                    if (isValidTipInput(newValue)) {
                        onTipInputChange(newValue)
                    }
                },
                label = { Text("Tip ($currencySymbol)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPrint,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = payEnabled
                ) {
                    Text(
                        "Print",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp),
                    enabled = payEnabled
                ) {
                    Text(
                        "Pay ${formatPrice(total)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
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
    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                formatPrice(product.price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (count > 0) 1f else 0f)
            ) {
                Text(
                    "x$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onRemove, enabled = count > 0) {
                    Text("Remove")
                }
            }
        }
    }
}

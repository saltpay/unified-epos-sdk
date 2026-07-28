package com.example.eposappexample.poslink.ui.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eposappexample.poslink.currencySymbol
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.isValidTipInput
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.ui.components.ProductGrid
import com.teya.lemonade.Button
import com.teya.lemonade.LemonadeTheme
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Text
import com.teya.lemonade.TextField
import com.teya.lemonade.core.LemonadeButtonSize
import com.teya.lemonade.core.LemonadeButtonType
import com.teya.lemonade.core.LemonadeButtonVariant

@Composable
fun SaleScreen(
    modifier: Modifier = Modifier,
    viewModel: SaleViewModel = viewModel()
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SaleTopBar(
                onClearUserAuth = { viewModel.clearUserAuth() },
                onClearDeviceLink = { viewModel.clearDeviceLink() }
            )
        },
        bottomBar = {
            SaleBottomBar(
                itemCount = viewModel.itemCount,
                subtotal = viewModel.subtotal,
                total = viewModel.total,
                tipInput = viewModel.tipInput,
                onTipInputChange = { viewModel.updateTipInput(it) },
                onPay = { viewModel.pay() },
                onPrint = { viewModel.printReceipt() },
                payEnabled = viewModel.payEnabled,
                unreferencedRefund = viewModel.unreferencedRefund,
                onUnreferencedRefundChange = { viewModel.updateUnreferencedRefund(it) }
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
private fun SaleTopBar(
    onClearUserAuth: () -> Unit,
    onClearDeviceLink: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            LemonadeUi.Text(
                "ePOS Sample Poslink",
                textStyle = LemonadeTheme.typography.headingXSmall
            )
        },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = LemonadeTheme.colors.background.bgDefault
            ) {
                DropdownMenuItem(
                    text = { LemonadeUi.Text("Clear User Auth") },
                    onClick = {
                        showMenu = false
                        onClearUserAuth()
                    }
                )
                DropdownMenuItem(
                    text = { LemonadeUi.Text("Clear Device Link") },
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
private fun SaleBottomBar(
    itemCount: Int,
    subtotal: Double,
    total: Double,
    tipInput: String,
    onTipInputChange: (String) -> Unit,
    onPay: () -> Unit,
    onPrint: () -> Unit,
    payEnabled: Boolean,
    unreferencedRefund: Boolean,
    onUnreferencedRefundChange: (Boolean) -> Unit
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUnreferencedRefundChange(!unreferencedRefund) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = unreferencedRefund,
                    onCheckedChange = { onUnreferencedRefundChange(it) }
                )
                LemonadeUi.Text(
                    "Unreferenced refund",
                    textStyle = LemonadeTheme.typography.bodyLargeRegular
                )
            }

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
                    label = if (unreferencedRefund) {
                        "Refund ${formatPrice(subtotal)}"
                    } else {
                        "Pay ${formatPrice(total)}"
                    },
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

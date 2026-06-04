package com.example.eposappexample.poslink.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eposappexample.poslink.formatPrice
import com.example.eposappexample.poslink.models.Product
import com.teya.lemonade.Button
import com.teya.lemonade.Card
import com.teya.lemonade.LemonadeTheme
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Text
import com.teya.lemonade.core.LemonadeButtonSize
import com.teya.lemonade.core.LemonadeButtonType
import com.teya.lemonade.core.LemonadeButtonVariant
import com.teya.lemonade.core.LemonadeCardBackground
import com.teya.lemonade.core.LemonadeCardPadding

@Composable
fun ProductGrid(
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

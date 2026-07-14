package com.example.eposappexample.poslink.ui.sale

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.eposappexample.poslink.teya.TeyaUtils
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.toMinorUnits

class SaleViewModel : ViewModel() {

    var basket by mutableStateOf(listOf<Product>())
        private set

    var tipInput by mutableStateOf("")
        private set

    var unreferencedRefund by mutableStateOf(false)
        private set

    val tipAmount by derivedStateOf { tipInput.toDoubleOrNull() ?: 0.0 }

    val subtotal by derivedStateOf { basket.sumOf { it.price * it.quantity } }

    val total by derivedStateOf { subtotal + tipAmount }

    val itemCount by derivedStateOf { basket.sumOf { it.quantity } }

    val payEnabled by derivedStateOf { basket.isNotEmpty() }

    fun addProduct(product: Product) {
        val existing = basket.find { it.id == product.id }
        basket = if (existing != null) {
            basket.map { if (it.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            basket + product.copy(quantity = 1)
        }
    }

    fun removeProduct(product: Product) {
        val existing = basket.find { it.id == product.id } ?: return
        basket = if (existing.quantity > 1) {
            basket.map { if (it.id == product.id) it.copy(quantity = it.quantity - 1) else it }
        } else {
            basket.filter { it.id != product.id }
        }
    }

    fun updateTipInput(value: String) {
        tipInput = value
    }

    fun updateUnreferencedRefund(value: Boolean) {
        unreferencedRefund = value
    }

    fun pay() {
        if (unreferencedRefund) {
            TeyaUtils.makeUnreferencedRefund(toMinorUnits(total))
        } else {
            TeyaUtils.makePayment(toMinorUnits(total), toMinorUnits(tipAmount))
        }
    }

    fun printReceipt() {
        TeyaUtils.printReceipt(basket, tipAmount)
    }

    fun clearUserAuth() {
        TeyaUtils.clearUserAuth()
        TeyaUtils.setUp()
    }

    fun clearDeviceLink() {
        TeyaUtils.clearDeviceLink()
        TeyaUtils.setUp()
    }
}

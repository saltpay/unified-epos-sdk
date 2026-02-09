package com.example.eposappexample.poslink

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

const val CURRENCY_CODE = "GBP"

val currencySymbol: String =
    Currency.getInstance(CURRENCY_CODE).symbol

fun formatPrice(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.UK)
    format.currency = Currency.getInstance(CURRENCY_CODE)
    return format.format(amount)
}

fun isValidTipInput(input: String): Boolean {
    if (input.isEmpty()) return true
    val dotIndex = input.indexOf('.')
    if (dotIndex != input.lastIndexOf('.')) return false
    for (c in input) {
        if (c != '.' && !c.isDigit()) return false
    }
    return !(dotIndex >= 0 && input.length - dotIndex - 1 > 2)
}
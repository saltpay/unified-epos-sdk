package com.example.eposappexample.poslink

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

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

/** Formats an amount given in minor units (e.g. pence) as a currency string, e.g. 1500 -> "£15.00". */
fun formatMinor(amountMinor: Int): String = formatPrice(amountMinor / 100.0)

/** Converts a major-unit amount (e.g. 1.29) to integer minor units (e.g. 129). */
fun toMinorUnits(amountMajor: Double): Int = (amountMajor * 100).roundToInt()
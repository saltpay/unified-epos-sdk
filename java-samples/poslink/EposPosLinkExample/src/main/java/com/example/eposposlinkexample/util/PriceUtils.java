package com.example.eposposlinkexample.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class PriceUtils {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.UK);

    private PriceUtils() {
    }

    public static String formatPrice(BigDecimal amount) {
        return CURRENCY.format(amount);
    }

    public static String formatMinor(int minorUnits) {
        return formatPrice(BigDecimal.valueOf(minorUnits).movePointLeft(2));
    }

    public static int toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public static boolean isValidTipInput(String text) {
        return text.isEmpty() || text.matches("\\d*(\\.\\d{0,2})?");
    }
}

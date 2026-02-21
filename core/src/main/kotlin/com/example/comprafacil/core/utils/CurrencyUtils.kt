package com.example.comprafacil.core.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)

    fun formatPrice(price: Double): String {
        return currencyFormat.format(price)
    }

    fun formatPriceNoSymbol(price: Double): String {
        return String.format(ptBrLocale, "%.2f", price)
    }
}

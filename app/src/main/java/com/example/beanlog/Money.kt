package com.example.beanlog

import android.content.Context

/** Currencies currently supported in the app. */
val CURRENCIES = listOf("USD", "EUR", "DKK")

fun currencySymbol(currency: String): String = when (currency) {
    "USD" -> "$"
    "EUR" -> "€"
    "DKK" -> "kr"
    else -> currency
}

fun moneyWithCurrency(amount: Double, currency: String, digits: Int = 2): String =
    if (currency == "DKK") "${money(amount, digits)} kr" else "${currencySymbol(currency)}${money(amount, digits)}"

/**
 * Bundled ECB reference rates. These are deliberately updated with app releases,
 * so every installed version uses a consistent, known set of approximate rates.
 * Base currency is EUR. ECB reference rates dated 5 November 2026.
 */
object Rates {
    private val eurRates = mapOf(
        "EUR" to 1.0,
        "USD" to 1.1224,
        "DKK" to 7.4747
    )

    @Volatile var rateDate: String? = "2026-11-05"
        private set

    val loaded: Boolean get() = true
    val approximate: Boolean get() = true

    // Kept suspend/context-shaped to avoid changing existing screen call sites.
    suspend fun load(context: Context): Boolean = true

    fun convert(amount: Double, from: String, to: String): Double? {
        if (from == to) return amount
        val fromRate = eurRates[from] ?: return null
        val toRate = eurRates[to] ?: return null
        // Convert source currency to EUR, then EUR to target currency.
        return amount / fromRate * toRate
    }
}

private fun fmt(pattern: String, utc: Boolean = false) =
    java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
        if (utc) timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

fun todayIso(): String = fmt("yyyy-MM-dd").format(java.util.Date())

fun isoToDisplay(iso: String): String =
    try { fmt("dd/MM/yyyy").format(fmt("yyyy-MM-dd").parse(iso)!!) } catch (e: Exception) { iso }

fun isoToMillis(iso: String): Long =
    try { fmt("yyyy-MM-dd", true).parse(iso)!!.time } catch (e: Exception) { System.currentTimeMillis() }

fun millisToIso(ms: Long): String = fmt("yyyy-MM-dd", true).format(java.util.Date(ms))

fun money(v: Double, digits: Int = 2): String =
    String.format(java.util.Locale.getDefault(), "%.${digits}f", v)

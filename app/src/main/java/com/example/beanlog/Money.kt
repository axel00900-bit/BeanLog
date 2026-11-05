package com.example.beanlog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

/** Live exchange rates from the free Frankfurter API (ECB data). Needs internet. */
object Rates {
    private var eurRates: Map<String, Double>? = null

    val loaded get() = eurRates != null

    suspend fun load(): Boolean {
        if (eurRates != null) return true
        return try {
            val text = withContext(Dispatchers.IO) {
                URL("https://api.frankfurter.dev/v1/latest?base=EUR").readText()
            }
            val json = JSONObject(text).getJSONObject("rates")
            val map = mutableMapOf("EUR" to 1.0)
            json.keys().forEach { map[it] = json.getDouble(it) }
            eurRates = map
            true
        } catch (e: Exception) {
            false
        }
    }

    fun convert(amount: Double, from: String, to: String): Double? {
        if (from == to) return amount
        val r = eurRates ?: return null
        val f = r[from] ?: return null
        val t = r[to] ?: return null
        return amount / f * t
    }
}

private fun fmt(pattern: String, utc: Boolean = false) =
    SimpleDateFormat(pattern, Locale.US).apply { if (utc) timeZone = TimeZone.getTimeZone("UTC") }

fun todayIso(): String = fmt("yyyy-MM-dd").format(Date())

fun isoToDisplay(iso: String): String =
    try { fmt("dd/MM/yyyy").format(fmt("yyyy-MM-dd").parse(iso)!!) } catch (e: Exception) { iso }

fun isoToMillis(iso: String): Long =
    try { fmt("yyyy-MM-dd", true).parse(iso)!!.time } catch (e: Exception) { System.currentTimeMillis() }

fun millisToIso(ms: Long): String = fmt("yyyy-MM-dd", true).format(Date(ms))

fun money(v: Double, digits: Int = 2): String =
    String.format(Locale.getDefault(), "%.${digits}f", v)

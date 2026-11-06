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

/** Approximate ECB reference rates, cached locally and refreshed about weekly. */
object Rates {
    private const val PREFS = "beanlog_exchange_rates"
    private const val KEY_JSON = "eur_rates_json"
    private const val KEY_FETCHED_AT = "fetched_at"
    private const val KEY_RATE_DATE = "rate_date"
    private const val REFRESH_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L

    @Volatile private var eurRates: Map<String, Double>? = null
    @Volatile private var fetchedAt: Long = 0L
    @Volatile var rateDate: String? = null
        private set

    val loaded: Boolean get() = eurRates != null
    val approximate: Boolean get() = loaded

    suspend fun load(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (eurRates == null) {
            val cachedJson = prefs.getString(KEY_JSON, null)
            if (!cachedJson.isNullOrBlank()) {
                runCatching {
                    val json = JSONObject(cachedJson)
                    val ratesJson = json.getJSONObject("rates")
                    val cached = mutableMapOf("EUR" to 1.0)
                    ratesJson.keys().forEach { key -> cached[key] = ratesJson.getDouble(key) }
                    eurRates = cached
                    rateDate = json.optString("date").takeIf { it.isNotBlank() }
                    fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
                }
            }
        }

        val now = System.currentTimeMillis()
        if (eurRates != null && now - fetchedAt < REFRESH_INTERVAL_MS) return true

        return try {
            val text = withContext(Dispatchers.IO) {
                URL("https://api.frankfurter.dev/v1/latest?base=EUR").readText()
            }
            val json = JSONObject(text)
            val ratesJson = json.getJSONObject("rates")
            val map = mutableMapOf("EUR" to 1.0)
            ratesJson.keys().forEach { key -> map[key] = ratesJson.getDouble(key) }
            eurRates = map
            rateDate = json.optString("date").takeIf { it.isNotBlank() }
            fetchedAt = now
            prefs.edit()
                .putString(KEY_JSON, json.toString())
                .putLong(KEY_FETCHED_AT, now)
                .putString(KEY_RATE_DATE, rateDate)
                .apply()
            true
        } catch (_: Exception) {
            eurRates != null
        }
    }

    fun convert(amount: Double, from: String, to: String): Double? {
        if (from == to) return amount
        val rates = eurRates ?: return null
        val fromRate = rates[from] ?: return null
        val toRate = rates[to] ?: return null
        return amount / fromRate * toRate
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

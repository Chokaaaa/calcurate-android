package com.thecalcurate.android.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CryptoRatesCache {
    private const val PREFS_NAME = "crypto_rates_prefs"
    private const val KEY_RATES = "crypto_rates_v1"
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Double>>() {}.type

    fun load(context: Context): Map<String, Double> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RATES, null) ?: return emptyMap()
        return try {
            gson.fromJson<Map<String, Double>>(json, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun save(context: Context, rates: Map<String, Double>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RATES, gson.toJson(rates)).apply()
    }
}

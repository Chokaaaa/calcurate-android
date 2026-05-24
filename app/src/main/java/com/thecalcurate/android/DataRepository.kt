package com.thecalcurate.android

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thecalcurate.android.DataRepository
import com.thecalcurate.android.data.remote.Network
import com.thecalcurate.android.data.remote.NetworkState
import com.thecalcurate.android.model.CryptoItem
import com.thecalcurate.android.model.CurrencyItem
import com.thecalcurate.android.model.CurrencyListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.Response
import java.util.*

/**
 * Repository handling the work with products and comments.
 */
class DataRepository {
//    private val mObservableCurrency: MediatorLiveData<List<Currency>>

    var network = Network

    /**
     * Get the list of products from the database and get notified when the data changes.
     */
//    val currencyList: LiveData<List<Currency>>
//        get() = mObservableCurrency

    companion object {
        private var sInstance: DataRepository? = null
        val instance: DataRepository?
            get() {
                if (sInstance == null) {
                    synchronized(DataRepository::class.java) {
                        if (sInstance == null) {
                            sInstance = DataRepository()
                        }
                    }
                }
                return sInstance
            }
    }

    init {
//        mObservableCurrency = MediatorLiveData()
    }


    fun getCurrencyNames(resources: Resources): List<CurrencyItem> {
        val jsonText = resources.openRawResource(R.raw.currency_list)
            .bufferedReader().use { it.readText() }

        val gson = Gson()
        val itemType = object : TypeToken<List<CurrencyItem>>() {}.type
        val outputList = gson.fromJson<List<CurrencyItem>>(jsonText, itemType)

        return outputList
    }

    fun getCurrencyList(): List<CurrencyItem> {
        return CurrencyItem.getList()
    }

    suspend fun getCurrencyRates(baseUrl: String): Response<CurrencyListResponse> {
        Log.e("DataRepository", "getCurrencyRates baseUrl: ${baseUrl}")

        return network.getCurrencyList(baseUrl)
    }

    /**
     * Fetch crypto rates and normalize to "amount of crypto per 1 baseCurrency".
     * Mirrors iOS FetchData.fetchCryptoRates (FetchData.swift:400).
     *
     * Returns Map<cryptoSymbol, ratePerOneBase> or null on total failure.
     */
    suspend fun fetchCryptoRates(baseCurrency: String): Map<String, Double>? = coroutineScope {
        val cryptos = CryptoItem.codes()
        // 1. Fetch USDT price for each crypto in parallel (USDT itself is 1.0 by definition)
        val deferred = cryptos.map { sym ->
            async(Dispatchers.IO) {
                if (sym == "USDT") return@async sym to 1.0
                try {
                    val resp = network.getBinancePrice("${sym}USDT")
                    val price = resp.body()?.price?.toDoubleOrNull()
                    if (resp.isSuccessful && price != null && price > 0) sym to price else null
                } catch (e: Exception) {
                    Log.e("DataRepository", "Binance fetch failed for $sym: ${e.message}")
                    null
                }
            }
        }
        val collected: Map<String, Double> = deferred.awaitAll().filterNotNull().toMap()
        if (collected.isEmpty()) return@coroutineScope null

        // 2. Compute usdPerBase (how many USD per 1 baseCurrency)
        val isCryptoBase = CryptoItem.isCryptoCode(baseCurrency)
        var usdPerBase = 1.0
        if (isCryptoBase) {
            // baseCurrency is crypto — its USDT price ≈ USD price
            usdPerBase = collected[baseCurrency] ?: 1.0
        } else if (baseCurrency != "USD" && baseCurrency != "USDT") {
            // baseCurrency is fiat (non-USD) — fetch USD-base fiat rates to find usdPerBase
            try {
                val fiatResp = network.getCurrencyList("USD")
                if (fiatResp.isSuccessful) {
                    val baseRate = fiatResp.body()?.rates?.firstOrNull { it.Code == baseCurrency }?.Value ?: 0.0
                    if (baseRate > 0) usdPerBase = 1.0 / baseRate
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Fiat fetch (for usdPerBase) failed: ${e.message}")
            }
        }

        // 3. Build final: ratePerBase = usdPerBase / priceInUSDT
        val finalRates = collected.mapValues { (_, priceInUSDT) -> usdPerBase / priceInUSDT }
        finalRates
    }

    /**
     * Fetch fiat rates when base IS a crypto. Mirrors iOS FetchData.fetchCoinbaseBaseRates (FetchData.swift:588).
     * Returns map "1 cryptoBase = X fiat" for every fiat code (plus USD).
     */
    suspend fun fetchCryptoBaseRates(cryptoBase: String): Map<String, Double>? = coroutineScope {
        // 1. Get the crypto's USDT price (USDT == 1.0)
        val priceInUSDT: Double = if (cryptoBase == "USDT") 1.0 else {
            try {
                val resp = network.getBinancePrice("${cryptoBase}USDT")
                resp.body()?.price?.toDoubleOrNull() ?: return@coroutineScope null
            } catch (e: Exception) {
                Log.e("DataRepository", "Binance base fetch failed: ${e.message}")
                return@coroutineScope null
            }
        }

        // 2. Get USD-base fiat rates
        val fiatResp = try {
            network.getCurrencyList("USD")
        } catch (e: Exception) {
            Log.e("DataRepository", "Fiat fetch failed: ${e.message}")
            return@coroutineScope null
        }
        if (!fiatResp.isSuccessful) return@coroutineScope null
        val fiatRates = fiatResp.body()?.rates ?: return@coroutineScope null

        // 3. For each fiat: cryptoToFiat = priceInUSDT * fiatPerUSD
        val result = mutableMapOf<String, Double>()
        for (rate in fiatRates) {
            result[rate.Code] = priceInUSDT * rate.Value
        }
        result["USD"] = priceInUSDT
        result
    }
}
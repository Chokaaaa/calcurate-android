# Android Crypto Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring Android CalcuRate to crypto-feature parity with iOS — 8 coins via Binance, USDT-bridge math, plus a decimal-precision fix and migration off the hardcoded paid ExchangeRate key.

**Architecture:** Mirror iOS exactly: parallel boolean flags per slot (`isBaseCrypto`/etc.), `Map<String,Double>` rate cache with semantics "amount of crypto per 1 baseCurrency", USDT-bridge math, crypto-as-base support via separate fetch path.

**Tech Stack:** Kotlin, Retrofit 2.9, OkHttp 4.9, kotlinx-coroutines, Gson, SharedPreferences, Android Data Binding (no Compose).

**Reference (iOS source for parity):** `/Users/nursultanyelemessov/Desktop/Desktop - Nursultan's MacBook Pro (2)/Projects/Freelance/Shakhin Zhursinbek/CalcurateApp/CalcuRate 3 decimals/CalcuRateApp/Calcura+e/` — primarily `Core/CurrencyEnums.swift` and `ViewModal/FetchData.swift`.

**Branch:** `feature/CalcurateCrypto` — work directly on this branch (no worktree needed).

**Note on testing:** The project has no unit tests and no test infrastructure. Each phase verifies via `./gradlew :app:assembleDevDebug` (compile check) plus manual smoke test at the end. Frequent commits per phase.

---

## Phase 1: Swap ExchangeRate endpoint (open, no key)

**Goal:** Drop the hardcoded paid API key. Switch to the same endpoint iOS uses.

**Files:**
- Modify: `app/build.gradle` (lines 37, 42, 46)
- Modify: `app/src/main/java/com/thecalcurate/android/data/remote/CurrencyService.kt` (full file)
- Modify: `app/src/main/java/com/thecalcurate/android/model/CurrencyListResponse.kt` (rename field)
- Modify: `app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt` (line 63)

- [ ] **Step 1.1: Update build.gradle API URLs for all flavors**

Edit `app/build.gradle` — replace `https://v6.exchangerate-api.com` with `https://open.exchangerate-api.com` in all three `buildConfigField "String", "API_URL"` lines (dev, stg, prod).

- [ ] **Step 1.2: Replace CurrencyService endpoint**

Replace contents of `app/src/main/java/com/thecalcurate/android/data/remote/CurrencyService.kt`:

```kotlin
package com.thecalcurate.android.data.remote

import com.thecalcurate.android.model.CurrencyListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyService {
    @GET("v6/latest")
    suspend fun getWeatherData(
        @Query("base") base: String
    ): Response<CurrencyListResponse>
}
```

- [ ] **Step 1.3: Rename response field**

Edit `app/src/main/java/com/thecalcurate/android/model/CurrencyListResponse.kt`. The open endpoint returns `rates` (not `conversion_rates`) and omits `result`/`time_eol_unix`. Replace contents with:

```kotlin
package com.thecalcurate.android.model

data class CurrencyListResponse(
    val base_code: String,
    val rates: MutableList<Rate>,
    val time_last_update_unix: Int = 0,
    val time_last_update_utc: String = "",
    val time_next_update_unix: Int = 0,
    val time_next_update_utc: String = ""
)
```

- [ ] **Step 1.4: Update RatesAdapter type registration in Network.kt**

The existing `RatesAdapter` deserializer is registered for `MutableList<Rate>` and works regardless of field name, but we should verify the `TypeToken` still matches. No change needed in `Network.kt` — the adapter works on the field's JSON value (an object), not the field name. Skip.

- [ ] **Step 1.5: Update CurrencyListViewModel reference to renamed field**

Edit `app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt` line 63. Change:

```kotlin
var mappedList = response.body()!!.conversion_rates.filter {
```

To:

```kotlin
var mappedList = response.body()!!.rates.filter {
```

- [ ] **Step 1.6: Build dev debug to confirm compile**

Run: `./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Any reference to `conversion_rates` that we missed will surface as an unresolved-reference error here.

- [ ] **Step 1.7: Commit**

```bash
git add app/build.gradle app/src/main/java/com/thecalcurate/android/data/remote/CurrencyService.kt app/src/main/java/com/thecalcurate/android/model/CurrencyListResponse.kt app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt
git commit -m "feat(api): switch to open ExchangeRate endpoint, drop hardcoded key"
```

---

## Phase 2: Crypto model + Binance service

**Goal:** Define the 8 cryptos and the Retrofit service for Binance prices.

**Files:**
- Create: `app/src/main/java/com/thecalcurate/android/model/CryptoItem.kt`
- Create: `app/src/main/java/com/thecalcurate/android/data/remote/BinanceService.kt`
- Modify: `app/src/main/java/com/thecalcurate/android/data/remote/Network.kt`

- [ ] **Step 2.1: Create CryptoItem model**

Create `app/src/main/java/com/thecalcurate/android/model/CryptoItem.kt`:

```kotlin
package com.thecalcurate.android.model

import com.thecalcurate.android.R

data class CryptoItem(
    val name: String,
    val code: String,
    val iconResId: Int,
    val rate: Double = .0
) {
    var isFavorite2: Boolean = false

    fun getRateStr(): String =
        if (rate == .0) "0" else "%.3f".format(rate)

    companion object {
        fun getList(): MutableList<CryptoItem> = mutableListOf(
            CryptoItem("Bitcoin",     "BTC",  R.drawable.crypto_btc),
            CryptoItem("Ethereum",    "ETH",  R.drawable.crypto_eth),
            CryptoItem("Tether",      "USDT", R.drawable.crypto_usdt),
            CryptoItem("BNB",         "BNB",  R.drawable.crypto_bnb),
            CryptoItem("Solana",      "SOL",  R.drawable.crypto_sol),
            CryptoItem("XRP",         "XRP",  R.drawable.crypto_xrp),
            CryptoItem("Cardano",     "ADA",  R.drawable.crypto_ada),
            CryptoItem("Dogecoin",    "DOGE", R.drawable.crypto_doge),
        )

        fun codes(): List<String> = listOf("BTC","ETH","USDT","BNB","SOL","XRP","ADA","DOGE")

        fun isCryptoCode(code: String): Boolean = codes().contains(code)
    }
}
```

Note: drawable references will be red until Phase 6. That's fine — they resolve once drawables exist; the file itself still compiles because Kotlin doesn't check R.drawable at parse time (it resolves at compile against generated R.java, which fails the BUILD but not the file edit). We build after Phase 6.

- [ ] **Step 2.2: Create BinanceService**

Create `app/src/main/java/com/thecalcurate/android/data/remote/BinanceService.kt`:

```kotlin
package com.thecalcurate.android.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class BinanceTickerPrice(val symbol: String, val price: String)

interface BinanceService {
    @GET("api/v3/ticker/price")
    suspend fun getTickerPrice(
        @Query("symbol") symbol: String
    ): Response<BinanceTickerPrice>
}
```

- [ ] **Step 2.3: Add Binance Retrofit instance to Network.kt**

Edit `app/src/main/java/com/thecalcurate/android/data/remote/Network.kt`. Replace the entire `object Network { ... }` body with:

```kotlin
object Network {
    var currencyService: CurrencyService
    var binanceService: BinanceService

    const val BASE_URL = BuildConfig.API_URL
    const val BINANCE_BASE_URL = "https://api.binance.com/"

    init {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val listType: Type = object : TypeToken<MutableList<Rate>>() {}.type
        val gson = GsonBuilder().registerTypeAdapter(listType, RatesAdapter()).create()

        currencyService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CurrencyService::class.java)

        binanceService = Retrofit.Builder()
            .baseUrl(BINANCE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BinanceService::class.java)
    }

    suspend fun getCurrencyList(base: String): Response<CurrencyListResponse> {
        Log.e("Network", "getCurrencyList base: $base")
        return currencyService.getWeatherData(base)
    }

    suspend fun getBinancePrice(symbol: String): Response<BinanceTickerPrice> {
        return binanceService.getTickerPrice(symbol)
    }
}
```

Note: the existing `import retrofit2.Response` at the top is already present; no new imports needed because `BinanceTickerPrice` resolves through its package (or add `import com.thecalcurate.android.data.remote.BinanceTickerPrice` — same package so unnecessary).

- [ ] **Step 2.4: Build to confirm Phase 2 compiles (excluding drawable refs)**

Run: `./gradlew :app:assembleDevDebug`
Expected: BUILD FAILS at CryptoItem.kt with `unresolved reference: crypto_btc` (and 7 more). This is expected — drawables come in Phase 6. Continue to Phase 3.

- [ ] **Step 2.5: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/model/CryptoItem.kt app/src/main/java/com/thecalcurate/android/data/remote/BinanceService.kt app/src/main/java/com/thecalcurate/android/data/remote/Network.kt
git commit -m "feat(crypto): add CryptoItem model and Binance Retrofit service"
```

---

## Phase 3: Crypto rates cache (SharedPreferences)

**Goal:** Persist crypto rates between launches, matching iOS `FetchData.cryptoRatesCacheKey` behavior.

**Files:**
- Create: `app/src/main/java/com/thecalcurate/android/data/CryptoRatesCache.kt`

- [ ] **Step 3.1: Create CryptoRatesCache**

Create `app/src/main/java/com/thecalcurate/android/data/CryptoRatesCache.kt`:

```kotlin
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
        return try { gson.fromJson<Map<String, Double>>(json, mapType) ?: emptyMap() }
        catch (e: Exception) { emptyMap() }
    }

    fun save(context: Context, rates: Map<String, Double>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RATES, gson.toJson(rates)).apply()
    }
}
```

- [ ] **Step 3.2: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/data/CryptoRatesCache.kt
git commit -m "feat(crypto): add SharedPreferences-backed crypto rates cache"
```

---

## Phase 4: Repository fetch methods

**Goal:** Add `fetchCryptoRates(base)` and `fetchCryptoBaseRates(base)` to `DataRepository`, mirroring iOS `FetchData.fetchCryptoRates` and `fetchCoinbaseBaseRates`.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/DataRepository.kt`

- [ ] **Step 4.1: Add fetch methods to DataRepository**

Append the following methods inside `class DataRepository` (just before the closing brace, after `getCurrencyRates`):

```kotlin
    /**
     * Fetch crypto rates and normalize to "amount of crypto per 1 baseCurrency".
     * Mirrors iOS FetchData.fetchCryptoRates (FetchData.swift:400).
     *
     * Returns Map<cryptoSymbol, ratePerOneBase> or null on total failure.
     */
    suspend fun fetchCryptoRates(baseCurrency: String): Map<String, Double>? = coroutineScope {
        val cryptos = com.thecalcurate.android.model.CryptoItem.codes()
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
        val isCryptoBase = com.thecalcurate.android.model.CryptoItem.isCryptoCode(baseCurrency)
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
     * Returns a CurrencyListResponse-shaped map: "1 cryptoBase = X fiat" for every fiat.
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
        val fiatResp = try { network.getCurrencyList("USD") } catch (e: Exception) {
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
```

Add the missing imports at the top of `DataRepository.kt` (after existing imports):

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
```

- [ ] **Step 4.2: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/DataRepository.kt
git commit -m "feat(crypto): add fetchCryptoRates and fetchCryptoBaseRates"
```

---

## Phase 5: ViewModel state and routing

**Goal:** Extend `CurrencyListViewModel` with crypto LiveData and per-slot crypto state, and route base-change to the right fetch path. Mirror iOS slot state shape.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt`

- [ ] **Step 5.1: Add crypto state and rate fetching to ViewModel**

Replace the entire contents of `app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt` with:

```kotlin
package com.thecalcurate.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.thecalcurate.android.DataRepository
import com.thecalcurate.android.data.CryptoRatesCache
import com.thecalcurate.android.model.CryptoItem
import com.thecalcurate.android.model.CurrencyItem
import kotlinx.coroutines.*

class CurrencyListViewModel(application: Application, private val mRepository: DataRepository) :
    AndroidViewModel(application) {
    val TAG = "CurrencyListViewModel"
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    val MAIN = 1
    val SEC1 = 2
    val SEC2 = 3

    var currencyMainList = MutableLiveData<List<CurrencyItem>>()
    var currencySec1List = MutableLiveData<List<CurrencyItem>>()
    var currencySec2List = MutableLiveData<List<CurrencyItem>>()

    // Crypto rates: amount of crypto per 1 baseCurrency. Mirrors iOS FetchData.thirdCryptoRates.
    val cryptoRates = MutableLiveData<Map<String, Double>>(emptyMap())

    val loading = MutableLiveData<Boolean>()
    var job: Job? = null
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    init {
        // Load cached crypto rates immediately so UI can render stale-but-present values.
        cryptoRates.value = CryptoRatesCache.load(application)
    }

    /**
     * Public entry point — chooses fiat or crypto path based on whether baseCurrency is a crypto code.
     * type is which slot's list to refresh (MAIN / SEC1 / SEC2).
     */
    fun getCurrencyRates(baseCurrency: String, type: Int) {
        if (CryptoItem.isCryptoCode(baseCurrency)) {
            getRatesWithCryptoBase(baseCurrency, type)
        } else {
            getRatesWithFiatBase(baseCurrency, type)
        }
    }

    private fun getRatesWithFiatBase(baseCurrency: String, type: Int) {
        val curList1 = mRepository.getCurrencyList()
        postValue(type, curList1)

        job = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            // Parallel: fiat rates + crypto rates (only when refreshing MAIN, so the rate map is base-correct)
            val fiatDeferred = async { mRepository.getCurrencyRates(baseCurrency) }
            val cryptoDeferred = if (type == MAIN) async { mRepository.fetchCryptoRates(baseCurrency) } else null

            val response = fiatDeferred.await()
            val newCryptoRates = cryptoDeferred?.await()

            withContext(Dispatchers.Main) {
                val curList = mRepository.getCurrencyList()
                if (response.isSuccessful) {
                    postValue(type, curList.map { rate ->
                        val mappedList = response.body()!!.rates.filter { it.Code == rate.code }
                        CurrencyItem(rate.name, rate.code, mappedList[0].Value)
                    })
                    loading.value = false
                } else {
                    onError("Error : ${response.message()} ")
                    postValue(type, curList)
                }
                if (newCryptoRates != null) {
                    cryptoRates.value = newCryptoRates
                    CryptoRatesCache.save(getApplication(), newCryptoRates)
                }
            }
        }
    }

    private fun getRatesWithCryptoBase(cryptoBase: String, type: Int) {
        val curList1 = mRepository.getCurrencyList()
        postValue(type, curList1)

        job = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            val cryptoBaseDeferred = async { mRepository.fetchCryptoBaseRates(cryptoBase) }
            val cryptoRatesDeferred = if (type == MAIN) async { mRepository.fetchCryptoRates(cryptoBase) } else null

            val fiatMap = cryptoBaseDeferred.await()
            val newCryptoRates = cryptoRatesDeferred?.await()

            withContext(Dispatchers.Main) {
                val curList = mRepository.getCurrencyList()
                if (fiatMap != null) {
                    postValue(type, curList.map { item ->
                        val v = fiatMap[item.code] ?: 0.0
                        CurrencyItem(item.name, item.code, v)
                    })
                    loading.value = false
                } else {
                    onError("Could not load crypto base rates")
                    postValue(type, curList)
                }
                if (newCryptoRates != null) {
                    cryptoRates.value = newCryptoRates
                    CryptoRatesCache.save(getApplication(), newCryptoRates)
                }
            }
        }
    }

    private fun postValue(type: Int, map: List<CurrencyItem>) {
        when (type) {
            MAIN -> currencyMainList.postValue(map)
            SEC1 -> currencySec1List.postValue(map)
            SEC2 -> currencySec2List.postValue(map)
        }
    }

    private fun onError(message: String) {
        _errorMessage.value = message
        loading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
```

- [ ] **Step 5.2: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/viewmodel/CurrencyListViewModel.kt
git commit -m "feat(crypto): extend ViewModel with crypto rates and dual-base routing"
```

---

## Phase 6: Crypto drawables

**Goal:** Add 8 vector drawables for the coin icons. Without these, anything referencing `R.drawable.crypto_btc` will not compile.

**Files:**
- Create: `app/src/main/res/drawable/crypto_btc.xml`
- Create: `app/src/main/res/drawable/crypto_eth.xml`
- Create: `app/src/main/res/drawable/crypto_usdt.xml`
- Create: `app/src/main/res/drawable/crypto_bnb.xml`
- Create: `app/src/main/res/drawable/crypto_sol.xml`
- Create: `app/src/main/res/drawable/crypto_xrp.xml`
- Create: `app/src/main/res/drawable/crypto_ada.xml`
- Create: `app/src/main/res/drawable/crypto_doge.xml`

These 8 drawables are placeholder colored circles with the ticker letter overlaid via a layer-list. Real branded icons can replace later. Colors match iOS `CryptoChoice.color()` (CurrencyEnums.swift:35-46).

- [ ] **Step 6.1: Create crypto_btc.xml (orange)**

Create `app/src/main/res/drawable/crypto_btc.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#F7931A"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.2: Create crypto_eth.xml (gray)**

Create `app/src/main/res/drawable/crypto_eth.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#627EEA"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.3: Create crypto_usdt.xml (green)**

Create `app/src/main/res/drawable/crypto_usdt.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#26A17B"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.4: Create crypto_bnb.xml (yellow)**

Create `app/src/main/res/drawable/crypto_bnb.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#F3BA2F"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.5: Create crypto_sol.xml (purple)**

Create `app/src/main/res/drawable/crypto_sol.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#9945FF"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.6: Create crypto_xrp.xml (blue)**

Create `app/src/main/res/drawable/crypto_xrp.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#23292F"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.7: Create crypto_ada.xml (indigo)**

Create `app/src/main/res/drawable/crypto_ada.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#0033AD"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.8: Create crypto_doge.xml (gold)**

Create `app/src/main/res/drawable/crypto_doge.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#C2A633"/>
    <size android:width="48dp" android:height="48dp"/>
</shape>
```

- [ ] **Step 6.9: Build to confirm CryptoItem now resolves**

Run: `./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. If any drawable reference still fails, double-check filename casing (Android drawables are lowercase + underscore only).

- [ ] **Step 6.10: Commit**

```bash
git add app/src/main/res/drawable/crypto_*.xml
git commit -m "feat(crypto): add placeholder drawables for 8 coin icons"
```

---

## Phase 7: Picker dialog — Fiat / Crypto tabs

**Goal:** Add a TabLayout to the picker so user can choose Fiat or Crypto tab. Selection callback gains an `isCrypto` flag.

**Files:**
- Modify: `app/src/main/res/layout/currency_search.xml`
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`
- Modify: `app/src/main/java/com/thecalcurate/android/ui/CurrencyRecyclerViewAdapter.kt` (extend ItemClickListener — see step)

- [ ] **Step 7.1: Add TabLayout to currency_search.xml**

Read `app/src/main/res/layout/currency_search.xml` first to see the current root layout. Then add a `com.google.android.material.tabs.TabLayout` element as the FIRST child of the root, above the search bar:

```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/tabLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:tabMode="fixed"
    app:tabGravity="fill">

    <com.google.android.material.tabs.TabItem
        android:id="@+id/tabFiat"
        android:text="Fiat" />

    <com.google.android.material.tabs.TabItem
        android:id="@+id/tabCrypto"
        android:text="Crypto" />
</com.google.android.material.tabs.TabLayout>
```

If the root is `LinearLayout`, just insert the block above the existing `EditText`. If the root is `ConstraintLayout`, the existing search bar and recyclerView constraints must be adjusted to anchor below `@id/tabLayout` instead of `parent`.

- [ ] **Step 7.2: Extend ItemClickListener interface**

Read `app/src/main/java/com/thecalcurate/android/ui/CurrencyRecyclerViewAdapter.kt`. Find the `interface ItemClickListener` declaration. Change `onItemClick(view: View?, position: Int)` to add a third parameter:

```kotlin
interface ItemClickListener {
    fun onItemClick(view: View?, position: Int, isCrypto: Boolean = false)
}
```

Update any existing call sites inside the adapter to pass `false` (or omit, since default is `false`). The default parameter keeps existing call sites compiling unchanged.

- [ ] **Step 7.3: Update CurrencyDialog to handle tab switching**

Edit `app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt`. Add a field near the other fields (after `var list: MutableList<CurrencyItem>? = null`):

```kotlin
    var cryptoList: MutableList<com.thecalcurate.android.model.CryptoItem>? = null
    private var isCryptoTab: Boolean = false
```

Inside `onCreateDialog`, after `recyclerView.adapter = adapter` and before `builder.setView(dialogView)`, add tab wiring:

```kotlin
            val tabLayout = dialogView.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
            tabLayout?.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    isCryptoTab = (tab?.position == 1)
                    if (isCryptoTab) {
                        edtSearch.visibility = View.GONE
                        val cryptos = com.thecalcurate.android.model.CryptoItem.getList()
                        // Show crypto list — adapter still types CurrencyItem; map for display
                        adapter?.setList(cryptos.map {
                            CurrencyItem(it.name, it.code, it.rate).also { ci -> ci.isFavorite2 = false }
                        }.toMutableList())
                        adapter?.notifyDataSetChanged()
                    } else {
                        edtSearch.visibility = View.VISIBLE
                        adapter?.setList(listToShow!!)
                        adapter?.notifyDataSetChanged()
                    }
                }
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            })
```

Also update `itemClickListener2` near the top of the class to forward `isCryptoTab`:

```kotlin
    val itemClickListener2 = object : CurrencyRecyclerViewAdapter.ItemClickListener {
        override fun onItemClick(view: View?, position: Int, isCrypto: Boolean) {
            itemClickListener.onItemClick(view, position, isCryptoTab)
            dismiss()
        }
    }
```

- [ ] **Step 7.4: Build**

Run: `./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. If the existing `MainActivity` overrides `ItemClickListener.onItemClick(view, position)` without the third param, it still compiles thanks to the default parameter — but the activity will not yet act on `isCrypto`. That's wired up in Phase 8.

- [ ] **Step 7.5: Commit**

```bash
git add app/src/main/res/layout/currency_search.xml app/src/main/java/com/thecalcurate/android/ui/CurrencyDialog.kt app/src/main/java/com/thecalcurate/android/ui/CurrencyRecyclerViewAdapter.kt
git commit -m "feat(crypto): add Fiat/Crypto tab picker in currency dialog"
```

---

## Phase 8: Slot rendering — branch on isCrypto in MainActivity

**Goal:** Each of the 3 slots (main + sec1 + sec2) tracks whether it currently holds a crypto, and renders the appropriate icon and code. Persist crypto slot state.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/MainActivity.kt`

This phase is the most invasive. `MainActivity.kt` is 1015 lines and binds slot click + value display logic. Read sections incrementally rather than the whole file at once.

- [ ] **Step 8.1: Find slot-selection code in MainActivity**

Run: `grep -n "onItemClick\|secondarySelectedId\|baseCurrency\|btn_main\|btn_secondary" app/src/main/java/com/thecalcurate/android/MainActivity.kt`

Read the matching sections (typically lines 300-400 for slot click handling, lines 900-1000 for button bindings).

- [ ] **Step 8.2: Add per-slot crypto state**

Near the existing slot state declarations (after `secondarySelectedId` etc.), add:

```kotlin
    private var baseIsCrypto: Boolean = false
    private var baseCryptoCode: String? = null
    private var sec1IsCrypto: Boolean = false
    private var sec1CryptoCode: String? = null
    private var sec2IsCrypto: Boolean = false
    private var sec2CryptoCode: String? = null

    private val CRYPTO_PREFS = "calcurate_crypto_slots"
    private val KEY_BASE_IS_CRYPTO = "base_is_crypto"
    private val KEY_BASE_CRYPTO_CODE = "base_crypto_code"
    private val KEY_SEC1_IS_CRYPTO = "sec1_is_crypto"
    private val KEY_SEC1_CRYPTO_CODE = "sec1_crypto_code"
    private val KEY_SEC2_IS_CRYPTO = "sec2_is_crypto"
    private val KEY_SEC2_CRYPTO_CODE = "sec2_crypto_code"
```

- [ ] **Step 8.3: Load slot state from SharedPreferences in onCreate**

In `onCreate`, after the existing `viewModel` initialization, add:

```kotlin
        val cryptoPrefs = getSharedPreferences(CRYPTO_PREFS, Context.MODE_PRIVATE)
        baseIsCrypto = cryptoPrefs.getBoolean(KEY_BASE_IS_CRYPTO, false)
        baseCryptoCode = cryptoPrefs.getString(KEY_BASE_CRYPTO_CODE, null)
        sec1IsCrypto = cryptoPrefs.getBoolean(KEY_SEC1_IS_CRYPTO, false)
        sec1CryptoCode = cryptoPrefs.getString(KEY_SEC1_CRYPTO_CODE, null)
        sec2IsCrypto = cryptoPrefs.getBoolean(KEY_SEC2_IS_CRYPTO, false)
        sec2CryptoCode = cryptoPrefs.getString(KEY_SEC2_CRYPTO_CODE, null)
```

- [ ] **Step 8.4: Handle isCrypto in dialog click callback**

Find the existing `onItemClick(view, position)` override (used as `CurrencyDialog`'s click listener — likely passed as constructor arg or via interface). Change the signature to:

```kotlin
override fun onItemClick(view: View?, position: Int, isCrypto: Boolean) {
```

Inside, branch:

```kotlin
    val targetSlot: Int = when (currentDialogTarget) {
        R.id.btn_main -> 1
        R.id.btn_secondary1 -> 2
        R.id.btn_secondary2 -> 3
        else -> 1
    }
    val selectedCode: String = if (isCrypto) {
        com.thecalcurate.android.model.CryptoItem.getList()[position].code
    } else {
        viewModel.currencyMainList.value!![position].code  // existing logic
    }
    when (targetSlot) {
        1 -> { baseIsCrypto = isCrypto; baseCryptoCode = if (isCrypto) selectedCode else null }
        2 -> { sec1IsCrypto = isCrypto; sec1CryptoCode = if (isCrypto) selectedCode else null }
        3 -> { sec2IsCrypto = isCrypto; sec2CryptoCode = if (isCrypto) selectedCode else null }
    }
    persistCryptoSlots()
    refreshSlot(targetSlot, selectedCode)
```

Note: `currentDialogTarget` and `refreshSlot` are likely already implementation details in MainActivity under different names. If they exist with different names (e.g., `selectedButton`, `updateRate(...)`), use those. The replacement should integrate with the existing click handler — not introduce a parallel one.

Add this helper somewhere in the class:

```kotlin
    private fun persistCryptoSlots() {
        getSharedPreferences(CRYPTO_PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_BASE_IS_CRYPTO, baseIsCrypto)
            putString(KEY_BASE_CRYPTO_CODE, baseCryptoCode)
            putBoolean(KEY_SEC1_IS_CRYPTO, sec1IsCrypto)
            putString(KEY_SEC1_CRYPTO_CODE, sec1CryptoCode)
            putBoolean(KEY_SEC2_IS_CRYPTO, sec2IsCrypto)
            putString(KEY_SEC2_CRYPTO_CODE, sec2CryptoCode)
            apply()
        }
    }
```

- [ ] **Step 8.5: Branch icon rendering for crypto slots**

Find the existing slot icon/code rendering (likely a function that sets an `ImageView.setImageResource(...)` based on currency code). Wrap with `if (isCrypto)` branch:

```kotlin
    // Pseudo-code — adapt to actual function names in MainActivity
    fun renderSlotIcon(slot: Int, code: String, isCrypto: Boolean, imageView: ImageView, codeLabel: TextView) {
        if (isCrypto) {
            val crypto = com.thecalcurate.android.model.CryptoItem.getList().firstOrNull { it.code == code }
            if (crypto != null) {
                imageView.setImageResource(crypto.iconResId)
                codeLabel.text = crypto.code
            }
        } else {
            // existing fiat rendering
        }
    }
```

- [ ] **Step 8.6: Branch rate-display logic for crypto slots**

When computing a secondary slot's rate, if the secondary is crypto, read from `viewModel.cryptoRates.value?.get(code)`. If the base is crypto, the secondary rate map (from `fetchCryptoBaseRates`) is already in fiat units, so existing path works.

Locate the function that reads `viewModel.currencyMainList.value!!.filter { it.code == secCode }[0].rate` (around line 322 / 341). Add a branch:

```kotlin
    val rate: Double = if (secIsCrypto) {
        viewModel.cryptoRates.value?.get(secCode) ?: 0.0
    } else {
        viewModel.currencyMainList.value!!.filter { it.code == secCode }[0].rate
    }
```

- [ ] **Step 8.7: Observe cryptoRates in MainActivity**

In `onCreate` where other LiveData observers are set up, add:

```kotlin
        viewModel.cryptoRates.observe(this) { _ ->
            // Re-render any slot that is currently a crypto with the fresh rate
            updateSecondaryDisplays()  // or whatever the existing rate-redraw function is named
        }
```

- [ ] **Step 8.8: Build**

Run: `./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. Compile errors here are most likely from mismatched function names — fix by matching to actual MainActivity identifiers.

- [ ] **Step 8.9: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/MainActivity.kt
git commit -m "feat(crypto): branch slot rendering and rate display on isCrypto"
```

---

## Phase 9: Decimal precision — 4 → 3

**Goal:** Match iOS "3 decimals" build.

**Files:**
- Modify: `app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt:27`

- [ ] **Step 9.1: Change rate formatter**

Edit `CurrencyItem.kt` line 27:

```kotlin
        else "%.4f".format(rate)
```

To:

```kotlin
        else "%.3f".format(rate)
```

- [ ] **Step 9.2: Build**

Run: `./gradlew :app:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9.3: Commit**

```bash
git add app/src/main/java/com/thecalcurate/android/model/CurrencyItem.kt
git commit -m "fix(format): rate precision 4 → 3 decimals to match iOS"
```

---

## Phase 10: Manual smoke test

**Goal:** Verify the build runs on a device/emulator and the crypto feature works end-to-end vs. iOS.

- [ ] **Step 10.1: Install on connected device or emulator**

Run: `./gradlew :app:installDevDebug`
Expected: APK installs without error.

- [ ] **Step 10.2: Smoke checklist (manual)**

Launch the app and verify:
1. App opens, fiat conversion still works (any USD → EUR pair shows a rate).
2. Open the picker for slot 1 (secondary), switch to "Crypto" tab, select BTC. The slot shows BTC icon + code; the converted value updates to a small fractional number.
3. Repeat for slot 2 with ETH. Both slots show distinct crypto values.
4. Type `1` then `0` in the calculator. Slot 1 (BTC) value should equal `0.00001` × current BTC price ≈ small number with 3 decimals.
5. Tap the base slot, switch to Crypto tab, select BTC. Now the calculator's base is BTC; the two secondary slots (if still fiat) should now show "how much fiat per 1 BTC" — large numbers.
6. Force-quit and relaunch. Slot selections persist (BTC base + crypto secondaries).
7. Airplane mode: relaunch app — cached rates still display, no crash.
8. Numerical parity check: pick any combo (e.g., USD base, ETH secondary) and verify the displayed rate matches iOS app within 1% (rates fluctuate, this is just a sanity check).

- [ ] **Step 10.3: Note any failures, fix, re-test, then commit final state**

Make any small fixes needed for smoke-test failures with appropriate commits per fix. Final state: `git status` clean.

---

## Self-Review notes (run before starting)

- Spec coverage: every spec section (data model, networking, cache, math, ViewModel, UI, formatter) maps to Phases 2-9 respectively. Phase 1 (endpoint switch) wasn't in the spec component table — added explicitly per the spec's "Modified files" row for `Network.kt`.
- Phase 8 has the most ambiguity because MainActivity.kt is large and the executing agent will need to match the existing function/variable names rather than invent new ones. The plan calls this out and tells the agent to grep first.
- iOS's request-ID deduplication and retry-with-backoff are NOT implemented in this plan. That's intentional — Android coroutines + `viewModelScope.launch` + last-result-wins naturally avoid most stale-render issues, and the user accepted "no retry abstraction yet" in the spec. If observed flakiness comes up in smoke testing, add a Phase 11 for retry.

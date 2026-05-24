/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thecalcurate.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thecalcurate.android.DataRepository
import com.thecalcurate.android.data.CryptoRatesCache
import com.thecalcurate.android.model.CryptoItem
import com.thecalcurate.android.model.CurrencyItem
import kotlinx.coroutines.*

class CurrencyListViewModel(application: Application, private val mRepository: DataRepository) :
    AndroidViewModel(application) {
    val TAG = "CurrencyListViewModel"
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

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
            // Parallel: fiat rates + crypto rates (only refresh crypto when refreshing MAIN
            // so the rate map is base-correct)
            val fiatDeferred = async { mRepository.getCurrencyRates(baseCurrency) }
            val cryptoDeferred = if (type == MAIN) {
                async { mRepository.fetchCryptoRates(baseCurrency) }
            } else null

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
            val cryptoRatesDeferred = if (type == MAIN) {
                async { mRepository.fetchCryptoRates(cryptoBase) }
            } else null

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

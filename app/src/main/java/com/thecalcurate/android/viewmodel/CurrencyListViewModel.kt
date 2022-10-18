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
import com.thecalcurate.android.DataRepository.Companion.instance
import androidx.lifecycle.AndroidViewModel
import com.thecalcurate.android.DataRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.thecalcurate.android.data.remote.NetworkState
import com.thecalcurate.android.model.Currency
import com.thecalcurate.android.model.CurrencyItem
import com.thecalcurate.android.model.Rate
import com.thecalcurate.android.model.SecondaryRates
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
    val loading = MutableLiveData<Boolean>()
    var job: Job? = null

    fun getCurrencyRates(baseCurrency: String, type: Int) {
        job = CoroutineScope(Dispatchers.IO).launch {
            val response = mRepository.getCurrencyRates(baseCurrency)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val curList = mRepository.getCurrencyList()
                    postValue(type, curList.map { rate ->
                        var mappedList = response.body()!!.conversion_rates.filter {
                            it.Code == rate.code
                        }
                        CurrencyItem(rate.name, rate.code, mappedList[0].Value)
                    })

                    loading.value = false
                } else {
                    onError("Error : ${response.message()} ")
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
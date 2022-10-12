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
import com.thecalcurate.android.model.CurrencyItem
import com.thecalcurate.android.model.CurrencyListResponse
import kotlinx.coroutines.Job
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
}
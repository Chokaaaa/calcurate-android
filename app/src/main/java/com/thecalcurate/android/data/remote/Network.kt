package com.thecalcurate.android.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.thecalcurate.android.BuildConfig
import com.thecalcurate.android.model.CurrencyListResponse
import com.thecalcurate.android.model.Rate
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type


object Network {
    var currencyService: CurrencyService

    const val BASE_URL = BuildConfig.API_URL

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

    }

    suspend fun getCurrencyList(base: String): Response<CurrencyListResponse> {
        Log.e("Network", "getCurrencyList base: ${base}")

        return currencyService.getWeatherData(base)
    }
}
package com.thecalcurate.android.data.remote

import com.thecalcurate.android.BuildConfig
import com.thecalcurate.android.model.CurrencyListResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object Network {
    lateinit var currencyService: CurrencyService

    const val BASE_URL = BuildConfig.API_URL

    init {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        currencyService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyService::class.java)

    }

    suspend fun getCurrencyList(base: String): Response<CurrencyListResponse> {

        return currencyService.getWeatherData(base)
    }
}
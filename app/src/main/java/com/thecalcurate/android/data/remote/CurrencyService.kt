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

package com.thecalcurate.android.data.remote

import com.thecalcurate.android.model.CurrencyListResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path


interface CurrencyService {
    @GET("/v6/d1e0b7e173c11ea479a1fef1/latest/{base}")
    suspend fun getWeatherData(
        @Path("base") base: String
    ): Response<CurrencyListResponse>
}
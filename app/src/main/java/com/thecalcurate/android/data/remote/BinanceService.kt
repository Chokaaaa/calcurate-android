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

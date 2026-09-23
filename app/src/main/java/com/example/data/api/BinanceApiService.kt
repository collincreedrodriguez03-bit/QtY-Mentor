package com.example.data.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String = "BTCUSDT",
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): List<List<Any>>

    @GET("api/v3/ticker/24hr")
    suspend fun getTicker24hr(
        @Query("symbol") symbol: String = "BTCUSDT"
    ): BinanceTickerResponse
}

data class BinanceTickerResponse(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String,
    val volume: String,
    val highPrice: String,
    val lowPrice: String,
    val bidPrice: String?,
    val askPrice: String?
)

object BinanceClient {
    val service: BinanceApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.binance.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(BinanceApiService::class.java)
    }
}

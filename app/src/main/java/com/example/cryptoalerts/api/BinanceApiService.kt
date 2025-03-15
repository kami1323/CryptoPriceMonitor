package com.example.cryptoalerts.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {
    
    @GET("api/v3/ticker/24hr")
    suspend fun get24HrTickerPriceChange(
        @Query("symbol") symbol: String
    ): Response<TickerResponse>
    
    @GET("api/v3/ticker/24hr")
    suspend fun getAllTickers(): Response<List<TickerResponse>>
}

data class TickerResponse(
    val symbol: String,
    val priceChange: String,
    val priceChangePercent: String,
    val weightedAvgPrice: String,
    val prevClosePrice: String,
    val lastPrice: String,
    val lastQty: String,
    val bidPrice: String,
    val bidQty: String,
    val askPrice: String,
    val askQty: String,
    val openPrice: String,
    val highPrice: String,
    val lowPrice: String,
    val volume: String,
    val quoteVolume: String,
    val openTime: Long,
    val closeTime: Long,
    val firstId: Long,
    val lastId: Long,
    val count: Long
)
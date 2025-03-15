package com.example.cryptoalerts.api

import com.example.cryptoalerts.model.Binance24hTickerResponse
import com.example.cryptoalerts.model.BinanceExchangeInfoResponse
import com.example.cryptoalerts.model.BinanceTickerResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {
    @GET("api/v3/ticker/price")
    suspend fun getTickerPrice(@Query("symbol") symbol: String): Response<BinanceTickerResponse>
    
    @GET("api/v3/ticker/24hr")
    suspend fun get24hTicker(@Query("symbol") symbol: String): Response<Binance24hTickerResponse>
    
    @GET("api/v3/ticker/24hr")
    suspend fun getAll24hTickers(): Response<List<Binance24hTickerResponse>>
    
    @GET("api/v3/exchangeInfo")
    suspend fun getExchangeInfo(): Response<BinanceExchangeInfoResponse>

    companion object {
        private const val BASE_URL = "https://api.binance.com/"
        
        fun create(): BinanceApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BinanceApiService::class.java)
        }
    }
}

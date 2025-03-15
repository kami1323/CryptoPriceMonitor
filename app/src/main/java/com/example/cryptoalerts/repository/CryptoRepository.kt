package com.example.cryptoalerts.repository

import com.example.cryptoalerts.api.BinanceApiService
import com.example.cryptoalerts.model.CryptoCurrency
import com.example.cryptoalerts.model.PriceAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CryptoRepository {
    private val binanceApi = BinanceApiService.create()
    
    // In-memory cache of all cryptocurrencies
    private val cryptoCache = ConcurrentHashMap<String, CryptoCurrency>()
    
    // In-memory storage for alerts
    private val alerts = mutableListOf<PriceAlert>()
    
    // Function to search cryptocurrencies by name or symbol
    suspend fun searchCryptoCurrency(query: String): List<CryptoCurrency> = withContext(Dispatchers.IO) {
        try {
            if (cryptoCache.isEmpty()) {
                // If cache is empty, fetch all tickers
                val response = binanceApi.getAll24hTickers()
                if (response.isSuccessful) {
                    response.body()?.forEach { ticker ->
                        if (ticker.symbol.endsWith("USDT")) {
                            // Only add USDT pairs for simplicity
                            val symbol = ticker.symbol.removeSuffix("USDT")
                            val price = ticker.lastPrice.toDoubleOrNull() ?: 0.0
                            val priceChangePercent = ticker.priceChangePercent.toDoubleOrNull() ?: 0.0
                            
                            cryptoCache[ticker.symbol] = CryptoCurrency(
                                symbol = symbol,
                                name = symbol, // Using symbol as name for now
                                price = price,
                                priceChangePercent = priceChangePercent
                            )
                        }
                    }
                }
            }
            
            // Filter cached cryptocurrencies by query
            return@withContext cryptoCache.values.filter { 
                it.symbol.contains(query, ignoreCase = true) || 
                it.name.contains(query, ignoreCase = true)
            }.take(5)  // Limit results to 5
        } catch (e: Exception) {
            // In case of error, return empty list
            return@withContext emptyList<CryptoCurrency>()
        }
    }
    
    // Function to get current price and 24h change for a specific symbol
    suspend fun getCryptoCurrencyDetails(symbol: String): CryptoCurrency? = withContext(Dispatchers.IO) {
        try {
            val fullSymbol = "${symbol}USDT"
            val tickerResponse = binanceApi.get24hTicker(fullSymbol)
            
            if (tickerResponse.isSuccessful) {
                val ticker = tickerResponse.body()!!
                val price = ticker.lastPrice.toDoubleOrNull() ?: 0.0
                val priceChangePercent = ticker.priceChangePercent.toDoubleOrNull() ?: 0.0
                
                val crypto = CryptoCurrency(
                    symbol = symbol,
                    name = symbol, // Using symbol as name
                    price = price,
                    priceChangePercent = priceChangePercent
                )
                
                // Update cache
                cryptoCache[fullSymbol] = crypto
                
                return@withContext crypto
            }
            
            return@withContext null
        } catch (e: Exception) {
            // In case of error, return null
            return@withContext null
        }
    }
    
    // Function to save an alert
    fun saveAlert(alert: PriceAlert) {
        // Only keep 10 most recent alerts
        if (alerts.size >= 10) {
            alerts.removeAt(0)
        }
        alerts.add(alert)
    }
    
    // Function to get all saved alerts
    fun getAlerts(): List<PriceAlert> = alerts.toList()
    
    // Function to update prices for all alerts
    suspend fun updateAlertPrices(): List<PriceAlert> = withContext(Dispatchers.IO) {
        try {
            alerts.forEach { alert ->
                val fullSymbol = "${alert.cryptoSymbol}USDT"
                val tickerResponse = binanceApi.get24hTicker(fullSymbol)
                
                if (tickerResponse.isSuccessful) {
                    val ticker = tickerResponse.body()!!
                    alert.currentPrice = ticker.lastPrice.toDoubleOrNull() ?: alert.currentPrice
                    alert.priceChangePercent = ticker.priceChangePercent.toDoubleOrNull() ?: alert.priceChangePercent
                }
            }
            
            return@withContext alerts.toList()
        } catch (e: Exception) {
            return@withContext alerts.toList()
        }
    }
    
    // Function to check which alerts have been triggered
    suspend fun checkAlertsTriggered(): List<PriceAlert> = withContext(Dispatchers.IO) {
        val triggeredAlerts = mutableListOf<PriceAlert>()
        
        updateAlertPrices()
        
        alerts.forEach { alert ->
            if (!alert.isTriggered && alert.isPriceThresholdCrossed()) {
                alert.isTriggered = true
                triggeredAlerts.add(alert)
            }
        }
        
        return@withContext triggeredAlerts
    }
}

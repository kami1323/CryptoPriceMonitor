package com.example.cryptoalerts.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cryptoalerts.api.BinanceApiService
import com.example.cryptoalerts.model.CryptoCurrency
import com.example.cryptoalerts.model.PriceAlert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CryptoRepository(private val context: Context) {

    private val TAG = "CryptoRepository"
    private val ALERTS_PREF_KEY = "price_alerts"
    private val PREFS_NAME = "crypto_alerts_prefs"
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    private val binanceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.binance.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BinanceApiService::class.java)
    }
    
    suspend fun getCryptoPriceData(symbol: String): CryptoCurrency? {
        return withContext(Dispatchers.IO) {
            try {
                val response = binanceApi.get24HrTickerPriceChange(symbol)
                if (response.isSuccessful) {
                    response.body()?.let {
                        CryptoCurrency(
                            symbol = it.symbol,
                            price = it.lastPrice.toDoubleOrNull() ?: 0.0,
                            priceChangePercent = it.priceChangePercent.toDoubleOrNull() ?: 0.0,
                            volume = it.volume.toDoubleOrNull() ?: 0.0
                        )
                    }
                } else {
                    Log.e(TAG, "Error fetching data: ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception when fetching price data", e)
                null
            }
        }
    }
    
    suspend fun getAllTickers(): List<CryptoCurrency> {
        return withContext(Dispatchers.IO) {
            try {
                val response = binanceApi.getAllTickers()
                if (response.isSuccessful) {
                    response.body()?.map {
                        CryptoCurrency(
                            symbol = it.symbol,
                            price = it.lastPrice.toDoubleOrNull() ?: 0.0,
                            priceChangePercent = it.priceChangePercent.toDoubleOrNull() ?: 0.0,
                            volume = it.volume.toDoubleOrNull() ?: 0.0
                        )
                    } ?: emptyList()
                } else {
                    Log.e(TAG, "Error fetching all tickers: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception when fetching all tickers", e)
                emptyList()
            }
        }
    }
    
    // Price Alert functions
    
    fun saveAlert(alert: PriceAlert) {
        val alerts = getAlerts().toMutableList()
        // Replace if same ID exists, otherwise add
        val index = alerts.indexOfFirst { it.id == alert.id }
        if (index >= 0) {
            alerts[index] = alert
        } else {
            alerts.add(alert)
        }
        saveAlerts(alerts)
    }
    
    fun deleteAlert(alertId: String) {
        val alerts = getAlerts().filterNot { it.id == alertId }
        saveAlerts(alerts)
    }
    
    fun getAlerts(): List<PriceAlert> {
        val alertsJson = sharedPreferences.getString(ALERTS_PREF_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<PriceAlert>>() {}.type
        return try {
            gson.fromJson(alertsJson, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing alerts from SharedPreferences", e)
            emptyList()
        }
    }
    
    private fun saveAlerts(alerts: List<PriceAlert>) {
        val alertsJson = gson.toJson(alerts)
        sharedPreferences.edit().putString(ALERTS_PREF_KEY, alertsJson).apply()
    }
}
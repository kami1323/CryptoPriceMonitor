package com.example.cryptoalerts.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cryptoalerts.model.PriceAlert
import com.example.cryptoalerts.repository.CryptoRepository
import com.example.cryptoalerts.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PriceCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "PriceCheckWorker"
    private val repository = CryptoRepository(appContext)
    private val notificationUtil = NotificationUtil(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting price check worker...")
            
            // Get all active alerts
            val alerts = repository.getAlerts().filter { it.isActive }
            
            if (alerts.isEmpty()) {
                Log.d(TAG, "No active alerts found")
                return@withContext Result.success()
            }
            
            // Group alerts by symbol to minimize API calls
            val alertsBySymbol = alerts.groupBy { it.symbol }
            
            alertsBySymbol.forEach { (symbol, symbolAlerts) ->
                checkAlertsForSymbol(symbol, symbolAlerts)
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in price check worker", e)
            Result.failure()
        }
    }
    
    private suspend fun checkAlertsForSymbol(symbol: String, alerts: List<PriceAlert>) {
        val cryptoData = repository.getCryptoPriceData(symbol)
        
        cryptoData?.let { data ->
            val currentPrice = data.price
            Log.d(TAG, "Current price for $symbol: $currentPrice")
            
            alerts.forEach { alert ->
                checkAlert(alert, currentPrice, data.symbol)
            }
        } ?: Log.e(TAG, "Failed to fetch price data for $symbol")
    }
    
    private fun checkAlert(alert: PriceAlert, currentPrice: Double, symbol: String) {
        val targetPrice = alert.targetPrice
        val alertTriggered = if (alert.isAboveTarget) {
            currentPrice >= targetPrice  // Alert when price goes above target
        } else {
            currentPrice <= targetPrice  // Alert when price goes below target
        }
        
        if (alertTriggered) {
            Log.d(TAG, "Alert triggered for $symbol: target=${alert.targetPrice}, current=$currentPrice")
            
            // Send notification
            val direction = if (alert.isAboveTarget) "above" else "below"
            val message = "$symbol price is now $currentPrice, which is $direction your target of ${alert.targetPrice}"
            
            notificationUtil.showPriceAlertNotification(
                symbol,
                message,
                alert.id.hashCode()
            )
            
            // Optionally, deactivate the alert after it's triggered
            // Uncomment if you want one-time alerts
            // val updatedAlert = alert.copy(isActive = false)
            // repository.saveAlert(updatedAlert)
        }
    }
    
    companion object {
        const val UNIQUE_WORK_NAME = "price_check_work"
    }
}
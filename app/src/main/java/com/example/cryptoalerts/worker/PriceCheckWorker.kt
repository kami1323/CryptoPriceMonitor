package com.example.cryptoalerts.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cryptoalerts.repository.CryptoRepository
import com.example.cryptoalerts.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = CryptoRepository()
    private val notificationUtil = NotificationUtil(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Update prices for all alerts
            repository.updateAlertPrices()
            
            // Check which alerts have been triggered
            val triggeredAlerts = repository.checkAlertsTriggered()
            
            // Show notification for each triggered alert
            triggeredAlerts.forEach { alert ->
                notificationUtil.showAlertNotification(
                    title = "Price Alert: ${alert.cryptoSymbol}",
                    message = "${alert.cryptoSymbol} has reached $${String.format("%.2f", alert.currentPrice)}",
                    alertId = alert.id.hashCode()
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

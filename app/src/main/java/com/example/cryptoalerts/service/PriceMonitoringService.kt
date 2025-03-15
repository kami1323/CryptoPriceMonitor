package com.example.cryptoalerts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cryptoalerts.MainActivity
import com.example.cryptoalerts.R
import com.example.cryptoalerts.worker.PriceCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PriceMonitoringService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val CHANNEL_ID = "price_monitoring_channel"
        private const val NOTIFICATION_ID = 1
        private const val MONITORING_INTERVAL = 60_000L // 1 minute in milliseconds
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as a foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Schedule the WorkManager for periodic checks
        setupWorker()
        
        // Also run a coroutine for more frequent updates (optional)
        serviceScope.launch {
            while (isActive) {
                try {
                    // This is just a heartbeat to keep the service alive
                    // The actual price checking is done by the WorkManager
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }
        
        // If service is killed, restart it
        return START_STICKY
    }
    
    private fun setupWorker() {
        // Create constraints for the worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a periodic work request to check prices every minute
        // Note: Minimum interval in WorkManager is 15 minutes, but we set it to 1 minute
        // The system will adjust to the minimum allowed
        val priceCheckRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(
            1, TimeUnit.MINUTES,
            1, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Enqueue the work request
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PRICE_CHECK_WORK",
            ExistingPeriodicWorkPolicy.REPLACE,
            priceCheckRequest
        )
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Price Monitoring Service",
                NotificationManager.IMPORTANCE_LOW // Low importance for less intrusion
            )
            channel.description = "Monitors cryptocurrency prices in the background"
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Crypto Alerts Active")
            .setContentText("Monitoring cryptocurrency prices")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CryptoAlerts:PriceMonitoringWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes
    }
    
    private fun releaseWakeLock() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock?.release()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
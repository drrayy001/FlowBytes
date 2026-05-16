package com.ray.flowmeter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ray.flowmeter.service.NetworkMonitoringService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        startServiceViaWorkManager(context)
    }
}

class NetworkWakeupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        startServiceViaWorkManager(context)
    }
}

/**
 * Starts the [NetworkMonitoringService] using WorkManager to comply with Android 15
 * foreground service start restrictions from background/boot.
 */
private fun startServiceViaWorkManager(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<ServiceStarterWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
}

class ServiceStarterWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val serviceIntent = Intent(applicationContext, NetworkMonitoringService::class.java)
        try {
            applicationContext.startForegroundService(serviceIntent)
            return Result.success()
        } catch (e: Exception) {
            Log.e("ServiceStarterWorker", "Failed to start service from WorkManager", e)
            return Result.failure()
        }
    }
}

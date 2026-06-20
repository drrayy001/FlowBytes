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

// Broadcast receivers to restart the background monitoring service on system boot
// or network state wakeups, utilizing WorkManager to handle foreground startup constraints.
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

// Bypasses Android's background service limitations (Oreo and later)
// by initiating startup through a WorkManager task execution block.
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

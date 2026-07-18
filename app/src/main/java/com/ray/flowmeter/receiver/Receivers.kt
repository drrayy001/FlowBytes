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
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import android.appwidget.AppWidgetManager
import android.content.ComponentName

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
        val repository = UserPreferencesRepository(applicationContext)
        val isMonitoringEnabled = runBlocking {
            try {
                repository.monitoringEnabled.first()
            } catch (_: Exception) {
                false
            }
        }
        if (!isMonitoringEnabled) {
            return Result.success()
        }

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

// Periodic worker to update all widgets when foreground monitoring is disabled.
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // If the service is running, it updates the widgets in real time.
        // We can skip periodic updates to conserve battery.
        if (NetworkMonitoringService.isRunning) {
            return Result.success()
        }

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        // 1. DailyUsageWidget
        val dailyIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, DailyUsageWidget::class.java))
        for (id in dailyIds) {
            DailyUsageWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 2. MonthlyUsageWidget
        val monthlyIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, MonthlyUsageWidget::class.java))
        for (id in monthlyIds) {
            MonthlyUsageWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 3. TodayDataWidget
        val todayIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, TodayDataWidget::class.java))
        for (id in todayIds) {
            TodayDataWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 4. DailyNetworkLimitWidget
        val dailyLimitIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, DailyNetworkLimitWidget::class.java))
        for (id in dailyLimitIds) {
            DailyNetworkLimitWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 5. MonthlyNetworkLimitWidget
        val monthlyLimitIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, MonthlyNetworkLimitWidget::class.java))
        for (id in monthlyLimitIds) {
            MonthlyNetworkLimitWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 6. CustomNetworkLimitWidget
        val customLimitIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, CustomNetworkLimitWidget::class.java))
        for (id in customLimitIds) {
            CustomNetworkLimitWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        // 7. SpeedMonitorWidget
        val speedMonitorIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, SpeedMonitorWidget::class.java))
        for (id in speedMonitorIds) {
            SpeedMonitorWidget.updateAppWidget(applicationContext, appWidgetManager, id)
        }

        return Result.success()
    }
}

// Helper to manage widget background periodic updates scheduler.
object WidgetUpdateScheduler {
    fun schedule(context: Context, intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(context)
        if (intervalMinutes <= 0) {
            workManager.cancelUniqueWork("WidgetUpdateWorker")
            return
        }

        val widgetWorkRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "WidgetUpdateWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            widgetWorkRequest
        )
    }
}

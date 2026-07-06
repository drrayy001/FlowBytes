package com.ray.flowmeter.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.os.Bundle
import android.widget.RemoteViews
import com.ray.flowmeter.MainActivity
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.utils.SpeedFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.NetworkCapabilities
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.util.Calendar

// AppWidgetProvider that handles rendering and real-time updates of the home screen widget,
// displaying cumulative daily network data usage.
class DailyUsageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyUsageWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val usageBytes = intent.getLongExtra(EXTRA_DAILY_USAGE, -1L)

            for (appWidgetId in appWidgetIds) {
                if (usageBytes >= 0L) {
                    updateWidgetData(context, appWidgetManager, appWidgetId, usageBytes)
                } else {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.ray.flowmeter.ACTION_UPDATE_WIDGET"
        const val EXTRA_DAILY_USAGE = "extra_daily_usage"
        const val EXTRA_MONTHLY_USAGE = "extra_monthly_usage"
        const val EXTRA_RX_SPEED = "extra_rx_speed"
        const val EXTRA_TX_SPEED = "extra_tx_speed"

        // Select either compact or full layout depending on the widget's current vertical span.
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            
            val layoutResId = if (minHeight < 100) {
                R.layout.widget_usage_compact
            } else {
                R.layout.widget_usage
            }
            val views = RemoteViews(context.packageName, layoutResId)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val resetHour = repository.resetTimeHour.first()
                val resetMinute = repository.resetTimeMinute.first()
                val monthlyResetDay = repository.monthlyResetDay.first()

                val wifiUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_WIFI, "daily", resetHour, resetMinute, monthlyResetDay)
                val mobileUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_CELLULAR, "daily", resetHour, resetMinute, monthlyResetDay)
                val totalUsage = wifiUsage + mobileUsage

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_text_usage, SpeedFormatter.formatUsage(totalUsage))
                    views.setTextViewText(R.id.widget_label_usage, context.getString(R.string.label_todays_usage).uppercase())
                    views.setViewVisibility(R.id.widget_speed_layout, View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun updateWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            usageBytes: Long
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            
            val layoutResId = if (minHeight < 100) {
                R.layout.widget_usage_compact
            } else {
                R.layout.widget_usage
            }
            val views = RemoteViews(context.packageName, layoutResId)
            
            views.setTextViewText(R.id.widget_text_usage, SpeedFormatter.formatUsage(usageBytes))
            views.setTextViewText(R.id.widget_label_usage, context.getString(R.string.label_todays_usage).uppercase())

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            views.setViewVisibility(R.id.widget_speed_layout, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

// AppWidgetProvider that handles rendering and real-time updates of the home screen widget,
// displaying cumulative monthly network data usage.
class MonthlyUsageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DailyUsageWidget.ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MonthlyUsageWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val usageBytes = intent.getLongExtra(DailyUsageWidget.EXTRA_MONTHLY_USAGE, -1L)

            for (appWidgetId in appWidgetIds) {
                if (usageBytes >= 0L) {
                    updateWidgetData(context, appWidgetManager, appWidgetId, usageBytes)
                } else {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    companion object {
        // Select either compact or full layout depending on the widget's current vertical span.
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            
            val layoutResId = if (minHeight < 100) {
                R.layout.widget_usage_compact
            } else {
                R.layout.widget_usage
            }
            val views = RemoteViews(context.packageName, layoutResId)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val resetHour = repository.resetTimeHour.first()
                val resetMinute = repository.resetTimeMinute.first()
                val monthlyResetDay = repository.monthlyResetDay.first()

                val wifiUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_WIFI, "monthly", resetHour, resetMinute, monthlyResetDay)
                val mobileUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_CELLULAR, "monthly", resetHour, resetMinute, monthlyResetDay)
                val totalUsage = wifiUsage + mobileUsage

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_text_usage, SpeedFormatter.formatUsage(totalUsage))
                    views.setTextViewText(R.id.widget_label_usage, context.getString(R.string.label_this_month).uppercase())
                    views.setViewVisibility(R.id.widget_speed_layout, View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun updateWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            usageBytes: Long
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            
            val layoutResId = if (minHeight < 100) {
                R.layout.widget_usage_compact
            } else {
                R.layout.widget_usage
            }
            val views = RemoteViews(context.packageName, layoutResId)
            
            views.setTextViewText(R.id.widget_text_usage, SpeedFormatter.formatUsage(usageBytes))
            views.setTextViewText(R.id.widget_label_usage, context.getString(R.string.label_this_month).uppercase())

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            views.setViewVisibility(R.id.widget_speed_layout, View.GONE)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

// Helper to query usage from NetworkStatsManager on demand
object WidgetUsageQuerier {
    fun getUsageBytes(context: Context, transportType: Int, period: String, resetHour: Int, resetMinute: Int, monthlyResetDay: Int): Long {
        val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return 0L
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        
        if (period == "monthly") {
            val clampedDay = monthlyResetDay.coerceAtMost(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar[Calendar.DAY_OF_MONTH] = clampedDay
        }
        calendar[Calendar.HOUR_OF_DAY] = resetHour
        calendar[Calendar.MINUTE] = resetMinute
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0

        var startTime = calendar.timeInMillis
        if (currentTime < startTime) {
            if (period == "monthly") {
                calendar.add(Calendar.MONTH, -1)
                val prevMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar[Calendar.DAY_OF_MONTH] = monthlyResetDay.coerceAtMost(prevMaxDay)
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            startTime = calendar.timeInMillis
        }

        var total = 0L
        try {
            val stats = manager.querySummary(transportType, null, startTime, currentTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                total += bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) {
            // ignore
        }
        return total
    }

    fun formatLimitValues(usage: Long, limit: Long): String {
        val gbUsage = usage / (1024.0 * 1024.0 * 1024.0)
        val gbLimit = limit / (1024.0 * 1024.0 * 1024.0)
        
        val formattedUsage = if (gbUsage % 1.0 == 0.0) String.format("%.0f", gbUsage) else String.format("%.1f", gbUsage)
        val formattedLimit = if (gbLimit % 1.0 == 0.0) String.format("%.0f", gbLimit) else String.format("%.1f", gbLimit)
        
        return "$formattedUsage / $formattedLimit GB"
    }
}

// 2x2 Today's Data Widget
class TodayDataWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DailyUsageWidget.ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodayDataWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_data)

            val mainIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val resetHour = repository.resetTimeHour.first()
                val resetMinute = repository.resetTimeMinute.first()
                val monthlyResetDay = repository.monthlyResetDay.first()
                
                // Get daily limits and usage
                val dailyLimit = repository.dataDailyLimit.first()
                val wifiUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_WIFI, "daily", resetHour, resetMinute, monthlyResetDay)
                val mobileUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_CELLULAR, "daily", resetHour, resetMinute, monthlyResetDay)
                val totalUsage = wifiUsage + mobileUsage

                val formatted = SpeedFormatter.formatUsage(totalUsage)
                val parts = formatted.split(" ")
                val valueText = parts.getOrNull(0) ?: "0"
                val unitText = parts.getOrNull(1) ?: "B"

                val bitmap = drawCircularProgress(totalUsage, dailyLimit)

                withContext(Dispatchers.Main) {
                    views.setImageViewBitmap(R.id.widget_circle_progress, bitmap)
                    views.setTextViewText(R.id.widget_text_usage_value, valueText)
                    views.setTextViewText(R.id.widget_text_usage_unit, unitText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun drawCircularProgress(usage: Long, limit: Long): Bitmap {
            val size = 200
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val strokeWidth = 14f
            
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1C2330")
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
            }
            
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#00daf3")
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
            }
            
            val padding = strokeWidth / 2f + 4f
            val rect = RectF(padding, padding, size - padding, size - padding)
            
            canvas.drawArc(rect, 0f, 360f, false, bgPaint)
            
            val fraction = if (limit > 0L) (usage.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
            val sweepAngle = fraction * 360f
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
            
            return bitmap
        }
    }
}

// 4x2 Daily Network Limit Widget
class DailyNetworkLimitWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DailyUsageWidget.ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyNetworkLimitWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_network_limit)

            val mainIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val resetHour = repository.resetTimeHour.first()
                val resetMinute = repository.resetTimeMinute.first()
                val monthlyResetDay = repository.monthlyResetDay.first()

                val wifiLimit = repository.wifiDailyLimit.first()
                val mobileLimit = repository.dataDailyLimit.first()

                val wifiUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_WIFI, "daily", resetHour, resetMinute, monthlyResetDay)
                val mobileUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_CELLULAR, "daily", resetHour, resetMinute, monthlyResetDay)

                val formattedWifi = WidgetUsageQuerier.formatLimitValues(wifiUsage, wifiLimit)
                val formattedMobile = WidgetUsageQuerier.formatLimitValues(mobileUsage, mobileLimit)

                val wifiProgress = if (wifiLimit > 0L) ((wifiUsage.toDouble() / wifiLimit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
                val mobileProgress = if (mobileLimit > 0L) ((mobileUsage.toDouble() / mobileLimit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_title, "DAILY NETWORK LIMIT")
                    views.setTextViewText(R.id.widget_wifi_values, formattedWifi)
                    views.setTextViewText(R.id.widget_mobile_values, formattedMobile)
                    views.setProgressBar(R.id.widget_wifi_progress, 100, wifiProgress, false)
                    views.setProgressBar(R.id.widget_mobile_progress, 100, mobileProgress, false)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}

// 4x2 Monthly Network Limit Widget
class MonthlyNetworkLimitWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == DailyUsageWidget.ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MonthlyNetworkLimitWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_network_limit)

            val mainIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = UserPreferencesRepository(context)
                val resetHour = repository.resetTimeHour.first()
                val resetMinute = repository.resetTimeMinute.first()
                val monthlyResetDay = repository.monthlyResetDay.first()

                val wifiLimit = repository.wifiMonthlyLimit.first()
                val mobileLimit = repository.dataMonthlyLimit.first()

                val wifiUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_WIFI, "monthly", resetHour, resetMinute, monthlyResetDay)
                val mobileUsage = WidgetUsageQuerier.getUsageBytes(context, NetworkCapabilities.TRANSPORT_CELLULAR, "monthly", resetHour, resetMinute, monthlyResetDay)

                val formattedWifi = WidgetUsageQuerier.formatLimitValues(wifiUsage, wifiLimit)
                val formattedMobile = WidgetUsageQuerier.formatLimitValues(mobileUsage, mobileLimit)

                val wifiProgress = if (wifiLimit > 0L) ((wifiUsage.toDouble() / wifiLimit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
                val mobileProgress = if (mobileLimit > 0L) ((mobileUsage.toDouble() / mobileLimit.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_title, "MONTHLY NETWORK LIMIT")
                    views.setTextViewText(R.id.widget_wifi_values, formattedWifi)
                    views.setTextViewText(R.id.widget_mobile_values, formattedMobile)
                    views.setProgressBar(R.id.widget_wifi_progress, 100, wifiProgress, false)
                    views.setProgressBar(R.id.widget_mobile_progress, 100, mobileProgress, false)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}

package com.ray.flowmeter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.withScale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.ray.flowmeter.MainActivity
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AlertRepository
import com.ray.flowmeter.data.AppAlert
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.receiver.NetworkWakeupReceiver
import com.ray.flowmeter.utils.SpeedFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class NetworkMonitoringService : Service() {

    companion object {
        const val ACTION_IGNORE_APP = "com.ray.flowmeter.IGNORE_APP"
        const val ACTION_SET_MUTE_DURATION = "com.ray.flowmeter.SET_MUTE_DURATION"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_MUTE_DURATION_MS = "extra_mute_duration_ms"
        const val EXTRA_NAVIGATE_TO_ALERTS = "extra_navigate_to_alerts"
        const val EXTRA_NAVIGATE_TO_LIMITS = "extra_navigate_to_limits"
        const val EXTRA_MUTE_APP_NAME = "extra_mute_app_name"
        const val EXTRA_DISMISS_NOTIFICATION_ID = "extra_dismiss_notification_id"
        
        @Volatile
        var isRunning = false

        private const val CHANNEL_ACTIVE = "NetworkMonitoringActive"
        private const val CHANNEL_ALERTS = "NetworkUsageAlerts"

        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 100
        private const val TRAFFIC_ALERT_ID = 500
        private const val SUMMARY_ID = 99

        private const val ALERT_GROUP_KEY = "high_traffic_group"
    }

    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var lastTime: Long = 0
    private var currentRxSpeed: Long = 0
    private var currentTxSpeed: Long = 0
    private var currentTotalSpeed: Long = 0
    private var notificationStartTime: Long = 0

    private var isForeground = false

    private var cachedWifiUsage: Long = 0
    private var cachedMobileUsage: Long = 0
    private var cachedMonthlyWifiUsage: Long = 0
    private var cachedMonthlyMobileUsage: Long = 0
    private var cachedCustomWifiUsage: Long = 0
    private var cachedCustomMobileUsage: Long = 0
    private var lastUsageQueryTime: Long = 0

    private var hasAlertedData = false
    private var hasAlertedWifi = false
    private var hasAlertedMonthlyData = false
    private var hasAlertedMonthlyWifi = false
    private var hasAlertedCustomData = false
    private var hasAlertedCustomWifi = false

    private var iconBitmap: Bitmap? = null
    private var iconCanvas: Canvas? = null
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isFakeBoldText = true
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var monitorJob: Job? = null

    private lateinit var repository: UserPreferencesRepository
    private var showNotificationDetails = true
    private var notificationContentType = "BOTH"
    private var iconScale = 1.28f
    private var highPriority = true
    private var resetHour = 0
    private var resetMinute = 0
    private var monthlyResetDay = 1
    private var showOnlyWhenConnected = false
    private var highTrafficDetectionEnabled = false

    private var trafficTimer: Long = 0
    private var uidSnapshot: Map<Int, Pair<Long, Long>> = emptyMap()
    private var lastTrafficNotificationTime: Long = 0

    private var trafficThresholdSpeed: Long = 1_000_000L
    private var trafficThresholdTime: Long = 60_000L
    private var trafficAlertCooldown: Long = 600_000L
    private var trafficResetBelowThresholdTime: Long = 5_000L
    private var trafficResetSpeed: Long = 200_000L

    private val alertRepository: AlertRepository by lazy { AlertRepository(FlowMeterDatabase.getDatabase(applicationContext).appAlertDao()) }
    private val appLimitRepository: AppLimitRepository by lazy { AppLimitRepository(FlowMeterDatabase.getDatabase(applicationContext).appLimitDao()) }

    private val statsMutex = Mutex()

    private val ignoredApps = mutableMapOf<String, Long>()

    override fun attachBaseContext(newBase: Context) {
        val repository = UserPreferencesRepository(newBase)
        val languageCode = kotlinx.coroutines.runBlocking {
            try {
                repository.language.first()
            } catch (e: Exception) {
                ""
            }
        }
        val context = com.ray.flowmeter.utils.LocaleHelper.applyLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        repository = UserPreferencesRepository(applicationContext)
        
        notificationStartTime = System.currentTimeMillis()
        createNotificationChannel()

        serviceScope.launch {
            var isFirst = true
            repository.showNotification.collect { 
                showNotificationDetails = it
                if (!isFirst) updateStats(force = true)
                isFirst = false
            } 
        }
        serviceScope.launch {
            var isFirst = true
            repository.notificationContentType.collect {
                notificationContentType = it
                if (!isFirst) updateStats(force = true)
                isFirst = false
            }
        }
        serviceScope.launch { 
            var isFirst = true
            repository.notificationIconScale.collect { 
                iconScale = it
                if (!isFirst) updateStats(force = true)
                isFirst = false
            } 
        }
        serviceScope.launch {
            repository.highPriorityNotification.collect { isHigh ->
                if (highPriority != isHigh) {
                    highPriority = isHigh
                    createNotificationChannel()
                    updateStats(force = true)
                }
            }
        }
        serviceScope.launch { repository.resetTimeHour.collect { resetHour = it } }
        serviceScope.launch { repository.resetTimeMinute.collect { resetMinute = it } }
        serviceScope.launch { repository.monthlyResetDay.collect { monthlyResetDay = it } }
        serviceScope.launch { 
            var isFirst = true
            repository.showOnlyWhenConnected.collect { 
                showOnlyWhenConnected = it
                if (!isFirst) updateStats(force = true)
                isFirst = false
            } 
        }
        serviceScope.launch { repository.highTrafficDetectionEnabled.collect { highTrafficDetectionEnabled = it } }
        serviceScope.launch { repository.trafficThresholdSpeed.collect { trafficThresholdSpeed = it } }
        serviceScope.launch { repository.trafficThresholdTime.collect { trafficThresholdTime = it } }
        serviceScope.launch { repository.trafficAlertCooldown.collect { trafficAlertCooldown = it } }
        serviceScope.launch { repository.trafficResetBelowThresholdTime.collect { trafficResetBelowThresholdTime = it } }
        serviceScope.launch { repository.trafficResetSpeed.collect { trafficResetSpeed = it } }

        // Initialize baseline immediately for instant first measurement
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()

        iconBitmap = createBitmap(64, 64)
        iconCanvas = Canvas(iconBitmap!!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_IGNORE_APP) {
            return START_STICKY
        }

        if (intent?.action == ACTION_SET_MUTE_DURATION) {
            val appName = intent.getStringExtra(EXTRA_MUTE_APP_NAME)
            val durationMs = intent.getLongExtra(EXTRA_MUTE_DURATION_MS, 0L)
            if ((appName != null) && (durationMs > 0)) {
                ignoredApps[appName] = System.currentTimeMillis() + durationMs
            }
            return START_STICKY
        }

        if (isForeground) {
            startMonitoring()
            return START_STICKY
        }

        val initialLayout = RemoteViews(packageName, R.layout.notification_compact_speed)
        initialLayout.setTextViewText(R.id.text_down, "0 KB/s")
        initialLayout.setTextViewText(R.id.text_combined, "0 KB/s")
        initialLayout.setTextViewText(R.id.text_up, "0 KB/s")
        initialLayout.setViewVisibility(R.id.layout_usage, View.GONE)

        try {
            // Use "0 KB/s" as initial icon text to avoid "blank space" / transparent icon
            safeStartForeground(createNotification(initialLayout, "0 KB/s"))
            isForeground = true
            
            serviceScope.launch {
                // Initialize usage immediately
                updateDailyUsage()
                
                // Initial delay to get a proper measurement before first UI update
                // Reduced from 600ms to 300ms for faster first update
                delay(300)
                updateStats(force = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cancelNetworkWakeup()
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        
        monitorJob = serviceScope.launch {
            while (isActive) {
                if ((System.currentTimeMillis() - lastUsageQueryTime) > 5000) {
                    updateDailyUsage()
                    checkAppLimits()
                }

                updateStats()
                delay(1000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        monitorJob?.cancel()
        serviceJob.cancel()
        iconBitmap?.recycle()
        iconBitmap = null
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        return cm.activeNetwork != null
    }

    private fun formatSpeed(bytesPerSec: Long): String = SpeedFormatter.formatBytes(bytesPerSec)

    private fun formatDataUsage(bytes: Long): String {
        return when {
            (bytes >= (1024L * 1024L * 1024L)) -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            (bytes >= (1024L * 1024L)) -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
            (bytes >= 1024L) -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private suspend fun updateDailyUsage() = withContext(Dispatchers.IO) {
        try {
            val networkStatsManager = getSystemService(NetworkStatsManager::class.java)
            val calendar = Calendar.getInstance()
            val currentTime = System.currentTimeMillis()

            fun getStartTime(period: String): Long {
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
                return startTime
            }

            fun getSumUsage(transportType: Int, period: String): Long {
                var total = 0L
                val start = getStartTime(period)
                try {
                    val stats = networkStatsManager.querySummary(transportType, null, start, currentTime)
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

            cachedWifiUsage = getSumUsage(NetworkCapabilities.TRANSPORT_WIFI, "daily")
            cachedMobileUsage = getSumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, "daily")
            cachedMonthlyWifiUsage = getSumUsage(NetworkCapabilities.TRANSPORT_WIFI, "monthly")
            cachedMonthlyMobileUsage = getSumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, "monthly")

            val dataCustomStart = repository.dataCustomLimitStart.first()
            val dataCustomEnd = repository.dataCustomLimitEnd.first()
            val wifiCustomStart = repository.wifiCustomLimitStart.first()
            val wifiCustomEnd = repository.wifiCustomLimitEnd.first()

            fun getCustomSumUsage(transportType: Int, start: Long, end: Long): Long {
                var total = 0L
                try {
                    val queryEnd = end.coerceAtMost(currentTime)
                    val queryStart = start.coerceAtMost(queryEnd)
                    val stats = networkStatsManager.querySummary(transportType, null, queryStart, queryEnd)
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

            cachedCustomMobileUsage = getCustomSumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, dataCustomStart, dataCustomEnd)
            cachedCustomWifiUsage = getCustomSumUsage(NetworkCapabilities.TRANSPORT_WIFI, wifiCustomStart, wifiCustomEnd)

            checkLimits()
            lastUsageQueryTime = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun checkLimits() {
        val dataDailyEnabled = repository.dataDailyLimitEnabled.first()
        val dataMonthlyEnabled = repository.dataMonthlyLimitEnabled.first()
        val wifiDailyEnabled = repository.wifiDailyLimitEnabled.first()
        val wifiMonthlyEnabled = repository.wifiMonthlyLimitEnabled.first()

        val dataDailyLimit = repository.dataDailyLimit.first()
        val dataMonthlyLimit = repository.dataMonthlyLimit.first()
        val wifiDailyLimit = repository.wifiDailyLimit.first()
        val wifiMonthlyLimit = repository.wifiMonthlyLimit.first()

        // 1. Check Daily Mobile
        if (dataDailyEnabled && (cachedMobileUsage > dataDailyLimit) && !hasAlertedData) {
            sendLimitAlert(getString(R.string.label_mobile), cachedMobileUsage, "daily", dataDailyLimit)
            hasAlertedData = true
        } else if (cachedMobileUsage < dataDailyLimit) {
            hasAlertedData = false
        }

        // 2. Check Monthly Mobile
        if (dataMonthlyEnabled && (cachedMonthlyMobileUsage > dataMonthlyLimit) && !hasAlertedMonthlyData) {
            sendLimitAlert(getString(R.string.label_mobile), cachedMonthlyMobileUsage, "monthly", dataMonthlyLimit)
            hasAlertedMonthlyData = true
        } else if (cachedMonthlyMobileUsage < dataMonthlyLimit) {
            hasAlertedMonthlyData = false
        }

        // 3. Check Daily Wi-Fi
        if (wifiDailyEnabled && (cachedWifiUsage > wifiDailyLimit) && !hasAlertedWifi) {
            sendLimitAlert(getString(R.string.label_wifi), cachedWifiUsage, "daily", wifiDailyLimit)
            hasAlertedWifi = true
        } else if (cachedWifiUsage < wifiDailyLimit) {
            hasAlertedWifi = false
        }

        // 4. Check Monthly Wi-Fi
        if (wifiMonthlyEnabled && (cachedMonthlyWifiUsage > wifiMonthlyLimit) && !hasAlertedMonthlyWifi) {
            sendLimitAlert(getString(R.string.label_wifi), cachedMonthlyWifiUsage, "monthly", wifiMonthlyLimit)
            hasAlertedMonthlyWifi = true
        } else if (cachedMonthlyWifiUsage < wifiMonthlyLimit) {
            hasAlertedMonthlyWifi = false
        }

        // 5. Check Custom Mobile
        val dataCustomEnabled = repository.dataCustomLimitEnabled.first()
        val dataCustomLimit = repository.dataCustomLimit.first()
        if (dataCustomEnabled && (cachedCustomMobileUsage > dataCustomLimit) && !hasAlertedCustomData) {
            sendLimitAlert(getString(R.string.label_mobile), cachedCustomMobileUsage, "custom", dataCustomLimit)
            hasAlertedCustomData = true
        } else if (cachedCustomMobileUsage < dataCustomLimit) {
            hasAlertedCustomData = false
        }

        // 6. Check Custom Wi-Fi
        val wifiCustomEnabled = repository.wifiCustomLimitEnabled.first()
        val wifiCustomLimit = repository.wifiCustomLimit.first()
        if (wifiCustomEnabled && (cachedCustomWifiUsage > wifiCustomLimit) && !hasAlertedCustomWifi) {
            sendLimitAlert(getString(R.string.label_wifi), cachedCustomWifiUsage, "custom", wifiCustomLimit)
            hasAlertedCustomWifi = true
        } else if (cachedCustomWifiUsage < wifiCustomLimit) {
            hasAlertedCustomWifi = false
        }
    }

    private fun sendLimitAlert(type: String, currentUsage: Long, period: String, limitValue: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        val alertType = when (period) {
            "monthly" -> "MONTHLY_LIMIT"
            "custom" -> "CUSTOM_LIMIT"
            else -> "DAILY_LIMIT"
        }
        
        val displayPeriod = when (period) {
            "daily" -> "Daily"
            "monthly" -> "Monthly"
            else -> "Custom"
        }
        val appNameForAlert = "$displayPeriod $type Limit"

        serviceScope.launch(Dispatchers.IO) {
            alertRepository.insert(
                AppAlert(
                    timestamp = System.currentTimeMillis(),
                    appName = appNameForAlert,
                    packageName = null,
                    rxBytes = currentUsage,
                    txBytes = 0L,
                    speed = 0L,
                    alertType = alertType,
                    limitValue = limitValue,
                ),
            )
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            (type + period).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val periodLabel = when (period) {
            "daily" -> getString(R.string.filter_daily).lowercase()
            "monthly" -> getString(R.string.filter_monthly).lowercase()
            else -> getString(R.string.filter_custom).lowercase()
        }
        val message = getString(R.string.msg_reached_limit, periodLabel, type, formatDataUsage(currentUsage))

        val title = when (period) {
            "daily" -> getString(R.string.label_daily_limit_reached)
            "monthly" -> getString(R.string.label_monthly_limit_reached)
            else -> getString(R.string.label_custom_limit_reached)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(ALERT_GROUP_KEY)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID + (type + period).hashCode(), notification)
        sendSummaryNotification()
    }

    private suspend fun updateStats(force: Boolean = false) {
        statsMutex.withLock {
            val currentTime = System.currentTimeMillis()
            
            // Handle first run initialization
            if (lastTime == 0L) {
                lastRxBytes = TrafficStats.getTotalRxBytes()
                lastTxBytes = TrafficStats.getTotalTxBytes()
                lastTime = currentTime
                return
            }

            val timeDiffMillis = currentTime - lastTime
            
            // If forced, allow slightly more frequent updates for UI responsiveness, 
            // but still maintain a minimum threshold to avoid 0 calculations.
            val minInterval = if (force) 200L else 500L
            
            val customLayout = RemoteViews(packageName, R.layout.notification_compact_speed)

            if (!isNetworkConnected()) {
                if (showOnlyWhenConnected) {
                    if (isForeground) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        isForeground = false
                        scheduleNetworkWakeup()
                    }

                    lastTime = currentTime
                    lastRxBytes = TrafficStats.getTotalRxBytes()
                    lastTxBytes = TrafficStats.getTotalTxBytes()
                    currentRxSpeed = 0
                    currentTxSpeed = 0
                    currentTotalSpeed = 0
                    return
                } else {
                    customLayout.setTextViewText(R.id.text_down, "0 KB/s")
                    customLayout.setTextViewText(R.id.text_combined, "0 KB/s")
                    customLayout.setTextViewText(R.id.text_up, "0 KB/s")

                    if (showNotificationDetails) {
                        when (notificationContentType) {
                            "SPEED" -> {
                                customLayout.setViewVisibility(R.id.layout_speeds, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.layout_usage, View.GONE)
                            }
                            "DAILY" -> {
                                customLayout.setViewVisibility(R.id.layout_speeds, View.GONE)
                                customLayout.setViewVisibility(R.id.layout_usage, View.VISIBLE)
                                
                                customLayout.setViewVisibility(R.id.text_usage_header, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.text_today_label, View.GONE)
                                customLayout.setViewVisibility(R.id.text_wifi_label, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.icon_wifi, View.GONE)
                                customLayout.setViewVisibility(R.id.text_mobile_label, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.icon_mobile, View.GONE)
                                customLayout.setTextViewText(R.id.text_usage_separator, ", ")
                            }
                            else -> {
                                customLayout.setViewVisibility(R.id.layout_speeds, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.layout_usage, View.VISIBLE)

                                customLayout.setViewVisibility(R.id.text_usage_header, View.GONE)
                                customLayout.setViewVisibility(R.id.text_today_label, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.text_wifi_label, View.GONE)
                                customLayout.setViewVisibility(R.id.icon_wifi, View.VISIBLE)
                                customLayout.setViewVisibility(R.id.text_mobile_label, View.GONE)
                                customLayout.setViewVisibility(R.id.icon_mobile, View.VISIBLE)
                                customLayout.setTextViewText(R.id.text_usage_separator, getString(R.string.label_separator))
                            }
                        }
                        customLayout.setTextViewText(R.id.text_mobile_usage, formatDataUsage(cachedMobileUsage))
                        customLayout.setTextViewText(R.id.text_wifi_usage, formatDataUsage(cachedWifiUsage))
                    } else {
                        customLayout.setViewVisibility(R.id.layout_speeds, View.GONE)
                        customLayout.setViewVisibility(R.id.layout_usage, View.GONE)
                    }

                    if (!isForeground) {
                        try {
                            safeStartForeground(createNotification(customLayout, "0 KB/s"))
                            isForeground = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        updateNotification(customLayout, "0 KB/s")
                    }

                    lastTime = currentTime
                    lastRxBytes = TrafficStats.getTotalRxBytes()
                    lastTxBytes = TrafficStats.getTotalTxBytes()
                    currentRxSpeed = 0
                    currentTxSpeed = 0
                    currentTotalSpeed = 0
                    return
                }
            }

            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()

            if (
                (currentRxBytes == TrafficStats.UNSUPPORTED.toLong()) ||
                (currentTxBytes == TrafficStats.UNSUPPORTED.toLong())
            ) {
                if (!isForeground) {
                    try {
                        safeStartForeground(createNotification(customLayout, "0 KB/s"))
                        isForeground = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (isForeground) {
                    customLayout.setTextViewText(R.id.text_down, getString(R.string.status_not_supported))
                    customLayout.setTextViewText(R.id.text_combined, "")
                    customLayout.setTextViewText(R.id.text_up, "")
                    customLayout.setViewVisibility(R.id.layout_usage, View.GONE)
                    updateNotification(customLayout, "0 KB/s")
                }
                return
            }

            // Perform speed calculation only if enough time has passed
            if (timeDiffMillis >= minInterval) {
                val timeDiff = timeDiffMillis / 1000.0

                currentRxSpeed = if (timeDiff > 0) ((currentRxBytes - lastRxBytes) / timeDiff).coerceAtLeast(0.0).toLong() else 0L
                currentTxSpeed = if (timeDiff > 0) ((currentTxBytes - lastTxBytes) / timeDiff).coerceAtLeast(0.0).toLong() else 0L
                currentTotalSpeed = currentRxSpeed + currentTxSpeed

                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTime = currentTime
            }

            if (highTrafficDetectionEnabled && currentTotalSpeed > 0) {
                checkHighTraffic(currentTotalSpeed)
            }

            customLayout.setTextViewText(R.id.text_down, formatSpeed(currentRxSpeed))
            customLayout.setTextViewText(R.id.text_combined, formatSpeed(currentTotalSpeed))
            customLayout.setTextViewText(R.id.text_up, formatSpeed(currentTxSpeed))

            if (showNotificationDetails) {
                when (notificationContentType) {
                    "SPEED" -> {
                        customLayout.setViewVisibility(R.id.layout_speeds, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.layout_usage, View.GONE)
                    }
                    "DAILY" -> {
                        customLayout.setViewVisibility(R.id.layout_speeds, View.GONE)
                        customLayout.setViewVisibility(R.id.layout_usage, View.VISIBLE)

                        customLayout.setViewVisibility(R.id.text_usage_header, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.text_today_label, View.GONE)
                        customLayout.setViewVisibility(R.id.text_wifi_label, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.icon_wifi, View.GONE)
                        customLayout.setViewVisibility(R.id.text_mobile_label, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.icon_mobile, View.GONE)
                        customLayout.setTextViewText(R.id.text_usage_separator, ", ")
                    }
                    else -> {
                        customLayout.setViewVisibility(R.id.layout_speeds, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.layout_usage, View.VISIBLE)

                        customLayout.setViewVisibility(R.id.text_usage_header, View.GONE)
                        customLayout.setViewVisibility(R.id.text_today_label, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.text_wifi_label, View.GONE)
                        customLayout.setViewVisibility(R.id.icon_wifi, View.VISIBLE)
                        customLayout.setViewVisibility(R.id.text_mobile_label, View.GONE)
                        customLayout.setViewVisibility(R.id.icon_mobile, View.VISIBLE)
                        customLayout.setTextViewText(R.id.text_usage_separator, getString(R.string.label_separator))
                    }
                }
                customLayout.setTextViewText(R.id.text_mobile_usage, formatDataUsage(cachedMobileUsage))
                customLayout.setTextViewText(R.id.text_wifi_usage, formatDataUsage(cachedWifiUsage))
            } else {
                customLayout.setViewVisibility(R.id.layout_speeds, View.GONE)
                customLayout.setViewVisibility(R.id.layout_usage, View.GONE)
            }

            if (!isForeground) {
                try {
                    safeStartForeground(createNotification(customLayout, formatSpeed(currentTotalSpeed)))
                    isForeground = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                updateNotification(customLayout, formatSpeed(currentTotalSpeed))
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)

        val currentChannelId = if (highPriority) "SPEED_METER_V7_HIGH" else "SPEED_METER_V7_DEFAULT"

        listOf(
            CHANNEL_ACTIVE, "${CHANNEL_ACTIVE}_HIGH", "${CHANNEL_ACTIVE}_LOW",
            "SPEED_METER_V2_HIGH", "SPEED_METER_V2_DEFAULT",
            "SPEED_METER_V3_HIGH", "SPEED_METER_V3_DEFAULT",
            "SPEED_METER_V4_HIGH", "SPEED_METER_V4_DEFAULT",
            "SPEED_METER_V5_HIGH", "SPEED_METER_V5_DEFAULT",
            "SPEED_METER_V6_HIGH", "SPEED_METER_V6_DEFAULT",
        ).forEach { manager.deleteNotificationChannel(it) }

            val importance = if (highPriority) {
                NotificationManager.IMPORTANCE_MAX
            } else {
                NotificationManager.IMPORTANCE_LOW
            }

            val activeChannel = NotificationChannel(
                currentChannelId,
                getString(R.string.channel_speed_monitor_name),
                importance,
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                setBypassDnd(highPriority)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = getString(R.string.channel_speed_monitor_desc)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.channel_usage_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_usage_alerts_desc)
                enableLights(true)
                lightColor = Color.RED
            }

            manager.createNotificationChannel(activeChannel)
            manager.createNotificationChannel(alertChannel)
    }

    private fun createSpeedIcon(speedText: String): Icon? {
        val bitmap = iconBitmap ?: return null
        val canvas = iconCanvas ?: return null

        bitmap.eraseColor(Color.TRANSPARENT)

        if (speedText.isNotBlank()) {
            val parts = speedText.split(" ")
            val valueStr = parts[0]
            val unitStr = if (parts.size > 1) parts[1] else ""

            canvas.withScale(iconScale, iconScale, canvas.width / 2f, canvas.height / 2f) {
                val xPos = canvas.width / 2f

                textPaint.textSize = when {
                    valueStr.length <= 2 -> 32f
                    valueStr.length == 3 -> 28f
                    else -> 24f
                }
                canvas.drawText(valueStr, xPos, 31f, textPaint)

                textPaint.textSize = 20f
                canvas.drawText(unitStr, xPos, 52f, textPaint)
            }
        }

        return Icon.createWithBitmap(bitmap)
    }

    private fun createNotification(customLayout: RemoteViews, iconText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activeChannelId = if (highPriority) "SPEED_METER_V7_HIGH" else "SPEED_METER_V7_DEFAULT"

        val notificationTime = if (highPriority) notificationStartTime + 10000000000L else notificationStartTime
        val sortOrder = if (highPriority) "\u0001" else null

        val builder = NotificationCompat.Builder(this, activeChannelId)
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setWhen(notificationTime)
            .setOnlyAlertOnce(true)
            .setCategory(if (highPriority) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocalOnly(true)
            .setSortKey(sortOrder)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .setSilent(true)

        val icon = createSpeedIcon(iconText)
        if (icon != null) {
            builder.setSmallIcon(IconCompat.createFromIcon(this, icon))
        } else {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
        }

        return builder.build()
    }

    private fun updateNotification(customLayout: RemoteViews, iconText: String) {
        val notification = createNotification(customLayout, iconText)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun safeStartForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // Monitor for sustained high traffic and alert if needed
    private fun checkHighTraffic(totalSpeed: Long) {
        val currentTime = System.currentTimeMillis()

        if (totalSpeed > trafficThresholdSpeed) {
            if (trafficTimer == 0L) {
                trafficTimer = currentTime
                uidSnapshot = captureUidStats()
            } else if (currentTime - trafficTimer > trafficThresholdTime) {
                if (currentTime - lastTrafficNotificationTime > trafficAlertCooldown) {
                    serviceScope.launch {
                        val trafficInfo = withContext(Dispatchers.IO) {
                            findHighTrafficAppFromSnapshot()
                        } ?: return@launch

                        val appName = trafficInfo.appName
                        val muteExpiry = ignoredApps[appName]

                        if (muteExpiry != null && System.currentTimeMillis() < muteExpiry) {
                            return@launch
                        }

                        if (muteExpiry != null && System.currentTimeMillis() >= muteExpiry) {
                            ignoredApps.remove(appName)
                        }

                        sendTrafficAlert(totalSpeed, trafficInfo)
                    }
                    lastTrafficNotificationTime = currentTime
                }
            }
        } else if (totalSpeed < trafficResetSpeed) {
            if (trafficTimer != 0L &&
                currentTime - lastTrafficNotificationTime > trafficResetBelowThresholdTime
            ) {
                trafficTimer = 0L
                uidSnapshot = emptyMap()
            }
        }
    }

    private fun captureUidStats(): Map<Int, Pair<Long, Long>> {
        val stats = mutableMapOf<Int, Pair<Long, Long>>()
        val networkStatsManager = getSystemService(NetworkStatsManager::class.java)

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24L * 60 * 60 * 1000)

        val networks = listOf(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_WIFI
        )

        try {
            for (transport in networks) {
                val networkStats = networkStatsManager.querySummary(transport, null, startTime, endTime)
                val bucket = NetworkStats.Bucket()
                while (networkStats.hasNextBucket()) {
                    networkStats.getNextBucket(bucket)
                    val uid = bucket.uid

                    if (
                        (uid == Process.SYSTEM_UID) ||
                        (uid == Process.SHELL_UID)
                    ) continue

                    val current = stats.getOrDefault(uid, 0L to 0L)
                    stats[uid] = Pair(
                        current.first + bucket.rxBytes,
                        current.second + bucket.txBytes
                    )
                }
                networkStats.close()
            }
        } catch (_: SecurityException) {
            // Ignore
        } catch (_: Exception) {
            // Ignore
        }
        return stats
    }

    private fun findHighTrafficAppFromSnapshot(): AppTrafficInfo? {
        val currentStats = captureUidStats()
        var maxUsage = 0L
        var topUid = -1
        var topRx = 0L
        var topTx = 0L

        for ((uid, currentUsage) in currentStats) {
            val startUsage = uidSnapshot[uid] ?: Pair(0L, 0L)

            val rxDiff = (currentUsage.first - startUsage.first).coerceAtLeast(0L)
            val txDiff = (currentUsage.second - startUsage.second).coerceAtLeast(0L)
            val totalDiff = rxDiff + txDiff

            if (totalDiff > maxUsage) {
                maxUsage = totalDiff
                topUid = uid
                topRx = rxDiff
                topTx = txDiff
            }
        }

        if (topUid != -1) {
            val pm = packageManager
            val packages = pm.getPackagesForUid(topUid)
            
            if (!packages.isNullOrEmpty()) {
                for (pkg in packages) {
                    try {
                        val info = pm.getApplicationInfo(pkg, 0)
                        // Match the logic in AppLimitsViewModel to only show "selectable" apps
                        val isSelectable = ((info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) || 
                                           ((info.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                        
                        if (isSelectable) {
                            return AppTrafficInfo(
                                appName = pm.getApplicationLabel(info).toString(),
                                packageName = pkg,
                                rxBytes = topRx,
                                txBytes = topTx
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return null
    }

    data class AppTrafficInfo(
        val appName: String,
        val packageName: String,
        val rxBytes: Long,
        val txBytes: Long
    )

    private fun sendTrafficAlert(speed: Long, trafficInfo: AppTrafficInfo) {
        val manager = getSystemService(NotificationManager::class.java)
        
        serviceScope.launch(Dispatchers.IO) {
            alertRepository.insert(
                AppAlert(
                    timestamp = System.currentTimeMillis(),
                    appName = trafficInfo.appName,
                    packageName = trafficInfo.packageName,
                    rxBytes = trafficInfo.rxBytes,
                    txBytes = trafficInfo.txBytes,
                    speed = speed,
                    alertType = "HIGH_TRAFFIC",
                )
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO_ALERTS, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val requestCode = trafficInfo.appName.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = getString(R.string.notification_high_usage_app_msg, trafficInfo.appName, formatSpeed(speed), formatDataUsage(trafficInfo.rxBytes + trafficInfo.txBytes))

        val titleText = getString(R.string.notification_high_usage_title)

        val builder = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setGroup(ALERT_GROUP_KEY)
            .setContentIntent(pendingIntent)

        val notificationId = TRAFFIC_ALERT_ID + trafficInfo.appName.hashCode()
        
        val ignoreIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_IGNORE_APP
            putExtra(EXTRA_APP_NAME, trafficInfo.appName)
            putExtra(EXTRA_MUTE_APP_NAME, trafficInfo.appName)
            putExtra(EXTRA_NAVIGATE_TO_ALERTS, true)
            putExtra(EXTRA_DISMISS_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val ignorePendingIntent = PendingIntent.getActivity(
            this,
            trafficInfo.appName.hashCode(),
            ignoreIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        builder.addAction(
            0,
            getString(R.string.btn_silence),
            ignorePendingIntent
        )

        manager.notify(notificationId, builder.build())
        sendSummaryNotification()
    }

    private fun sendSummaryNotification() {
        val manager = getSystemService(NotificationManager::class.java)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO_ALERTS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            SUMMARY_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summary = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_summary_title))
            .setContentText(getString(R.string.notification_summary_msg))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(getString(R.string.notification_summary_text))
            )
            .setGroup(ALERT_GROUP_KEY)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(SUMMARY_ID, summary)
    }

    private fun scheduleNetworkWakeup() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val intent = Intent(this, NetworkWakeupReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        cm.registerNetworkCallback(request, pendingIntent)
    }

    private fun cancelNetworkWakeup() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val intent = Intent(this, NetworkWakeupReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            cm.unregisterNetworkCallback(pendingIntent)
        } catch (_: Exception) {
        }
    }

    private suspend fun checkAppLimits() {
        val limits = appLimitRepository.getAllAppLimitsList()
        if (limits.isEmpty()) return

        val networkStatsManager = getSystemService(NetworkStatsManager::class.java)
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()

        val pm = packageManager

        for (limit in limits) {
            try {
                    if (!limit.isEnabled) {
                        if (limit.isBlocked || limit.isWifiBlocked || limit.isMobileBlocked) {
                            appLimitRepository.update(
                                limit.copy(
                                    isBlocked = false,
                                    isWifiBlocked = false,
                                    isMobileBlocked = false,
                                ),
                            )
                        }
                        continue
                    }

                calendar.timeInMillis = currentTime
                if (limit.limitType == "monthly") {
                    val clampedDay = monthlyResetDay.coerceAtMost(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calendar[Calendar.DAY_OF_MONTH] = clampedDay
                }
                calendar[Calendar.HOUR_OF_DAY] = resetHour
                calendar[Calendar.MINUTE] = resetMinute
                calendar[Calendar.SECOND] = 0
                calendar[Calendar.MILLISECOND] = 0
                
                var startTime = calendar.timeInMillis
                if (currentTime < startTime) {
                    if (limit.limitType == "monthly") {
                        calendar.add(Calendar.MONTH, -1)
                        val prevMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        calendar[Calendar.DAY_OF_MONTH] = monthlyResetDay.coerceAtMost(prevMaxDay)
                    } else {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    startTime = calendar.timeInMillis
                }

                val info = pm.getApplicationInfo(limit.packageName, 0)
                val uid = info.uid
                
                val wifiUsage = getUidUsageForTransport(networkStatsManager, uid, startTime, currentTime, NetworkCapabilities.TRANSPORT_WIFI)
                val mobileUsage = getUidUsageForTransport(networkStatsManager, uid, startTime, currentTime, NetworkCapabilities.TRANSPORT_CELLULAR)
                
                val currentUsage = when (limit.networkType) {
                    "wifi" -> wifiUsage
                    "mobile" -> mobileUsage
                    else -> wifiUsage + mobileUsage // Includes "both"
                }
                
                if (currentUsage != limit.currentUsage || wifiUsage != limit.currentWifiUsage || mobileUsage != limit.currentMobileUsage) {
                    var updatedLimit = limit.copy(
                        currentUsage = currentUsage,
                        currentWifiUsage = wifiUsage,
                        currentMobileUsage = mobileUsage
                    )
                    
                    if (limit.networkType == "both") {
                        val wifiOver = (wifiUsage >= limit.wifiDataLimit) && (limit.wifiDataLimit > 0)
                        val mobileOver = (mobileUsage >= limit.mobileDataLimit) && (limit.mobileDataLimit > 0)
                        
                        if (wifiOver && !limit.isWifiBlocked) {
                            sendAppLimitAlert(updatedLimit.copy(isWifiBlocked = true, networkType = "wifi", dataLimit = limit.wifiDataLimit))
                        }
                        if (mobileOver && !limit.isMobileBlocked) {
                            sendAppLimitAlert(updatedLimit.copy(isMobileBlocked = true, networkType = "mobile", dataLimit = limit.mobileDataLimit))
                        }
                        
                        updatedLimit = updatedLimit.copy(
                            isWifiBlocked = wifiOver,
                            isMobileBlocked = mobileOver
                        )
                        appLimitRepository.update(updatedLimit)
                    } else {
                        if (currentUsage >= limit.dataLimit && !limit.isBlocked) {
                            sendAppLimitAlert(updatedLimit.copy(isBlocked = true))
                            appLimitRepository.update(updatedLimit.copy(isBlocked = true))
                        } else if (currentUsage < limit.dataLimit && limit.isBlocked) {
                            appLimitRepository.update(updatedLimit.copy(isBlocked = false))
                        } else {
                            appLimitRepository.update(updatedLimit)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    private fun getUidUsageForTransport(nsm: NetworkStatsManager, uid: Int, startTime: Long, endTime: Long, transport: Int): Long {
        var total = 0L
        try {
            val stats = nsm.querySummary(transport, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                if (bucket.uid == uid) {
                    total += bucket.rxBytes + bucket.txBytes
                }
            }
            stats.close()
        } catch (_: Exception) {
            // ignore
        }
        return total
    }

    private fun sendAppLimitAlert(limit: AppLimit) {
        val manager = getSystemService(NotificationManager::class.java)
        val message = getString(R.string.notification_app_limit_msg, limit.appName, formatDataUsage(limit.dataLimit))
        
        val alertType = if (limit.limitType == "monthly") "MONTHLY_LIMIT" else "APP_LIMIT"
        
        serviceScope.launch(Dispatchers.IO) {
            alertRepository.insert(
                AppAlert(
                    timestamp = System.currentTimeMillis(),
                    appName = limit.appName,
                    packageName = limit.packageName,
                    rxBytes = limit.currentUsage,
                    txBytes = 0L,
                    speed = 0L,
                    alertType = alertType,
                    limitValue = limit.dataLimit,
                )
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO_LIMITS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            limit.packageName.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_app_limit_title, limit.appName))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setGroup(ALERT_GROUP_KEY)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID + limit.packageName.hashCode(), notification)
        sendSummaryNotification()
    }
}

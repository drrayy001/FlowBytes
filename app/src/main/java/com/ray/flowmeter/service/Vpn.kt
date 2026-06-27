package com.ray.flowmeter.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.utils.LocaleHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import java.util.Calendar

// VPN Service that intercepts and blocks network traffic for applications
// that have exceeded their configured cellular or Wi-Fi data usage limits.
@SuppressLint("VpnServicePolicy")
class AppBlockVpnService : VpnService() {

    override fun attachBaseContext(newBase: Context) {
        val repository = UserPreferencesRepository(newBase)
        val languageCode = runBlocking {
            try {
                repository.language.first()
            } catch (_: Exception) {
                ""
            }
        }
        val context = LocaleHelper.applyLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var repository: AppLimitRepository
    private lateinit var userPrefs: UserPreferencesRepository

    private var collectionJob: Job? = null
    private val currentNetworkType = MutableStateFlow<Int?>(null)

    // Track active connection type changes (cellular vs Wi-Fi) to apply corresponding block rules.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                else -> null
            }
            type?.let { currentNetworkType.value = it }
        }

        override fun onLost(network: Network) {
            val cm = getSystemService(ConnectivityManager::class.java)
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            if (capabilities == null) {
                currentNetworkType.value = null
            } else {
                val type = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                    else -> null
                }
                currentNetworkType.value = type
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = FlowMeterDatabase.getDatabase(applicationContext)
        repository = AppLimitRepository(database.appLimitDao())
        userPrefs = UserPreferencesRepository(applicationContext)

        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        
        val activeNet = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNet)
        if (caps != null) {
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                else -> null
            }
            currentNetworkType.value = type
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private data class VpnBlockConfig(
        val blockAll: Boolean,
        val blockedApps: List<String>
    )

    private suspend fun isSystemPlanLimitExceeded(networkType: Int?): Boolean {
        if (networkType == null) return false
        
        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return false
        
        val resetHour = userPrefs.resetTimeHour.first()
        val resetMinute = userPrefs.resetTimeMinute.first()
        val monthlyResetDay = userPrefs.monthlyResetDay.first()
        
        val currentTime = System.currentTimeMillis()
        
        fun getStartTime(period: String): Long {
            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = resetHour
            calendar[Calendar.MINUTE] = resetMinute
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0

            val timeNow = System.currentTimeMillis()

            if (period == "monthly") {
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar[Calendar.DAY_OF_MONTH] = monthlyResetDay.coerceAtMost(maxDay)
            }

            var startTime = calendar.timeInMillis

            if (timeNow < startTime) {
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
            } catch (_: Exception) {}
            return total
        }

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
            } catch (_: Exception) {}
            return total
        }

        if (networkType == NetworkCapabilities.TRANSPORT_CELLULAR) {
            val dailyEnabled = userPrefs.dataDailyLimitEnabled.first()
            if (dailyEnabled) {
                val limit = userPrefs.dataDailyLimit.first()
                val usage = getSumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, "daily")
                if (usage >= limit) return true
            }
            
            val monthlyEnabled = userPrefs.dataMonthlyLimitEnabled.first()
            if (monthlyEnabled) {
                val limit = userPrefs.dataMonthlyLimit.first()
                val usage = getSumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, "monthly")
                if (usage >= limit) return true
            }
            
            val customEnabled = userPrefs.dataCustomLimitEnabled.first()
            if (customEnabled) {
                val limit = userPrefs.dataCustomLimit.first()
                val start = userPrefs.dataCustomLimitStart.first()
                val end = userPrefs.dataCustomLimitEnd.first()
                val usage = getCustomSumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, start, end)
                if (usage >= limit) return true
            }
        } else if (networkType == NetworkCapabilities.TRANSPORT_WIFI) {
            val dailyEnabled = userPrefs.wifiDailyLimitEnabled.first()
            if (dailyEnabled) {
                val limit = userPrefs.wifiDailyLimit.first()
                val usage = getSumUsage(NetworkCapabilities.TRANSPORT_WIFI, "daily")
                if (usage >= limit) return true
            }
            
            val monthlyEnabled = userPrefs.wifiMonthlyLimitEnabled.first()
            if (monthlyEnabled) {
                val limit = userPrefs.wifiMonthlyLimit.first()
                val usage = getSumUsage(NetworkCapabilities.TRANSPORT_WIFI, "monthly")
                if (usage >= limit) return true
            }
            
            val customEnabled = userPrefs.wifiCustomLimitEnabled.first()
            if (customEnabled) {
                val limit = userPrefs.wifiCustomLimit.first()
                val start = userPrefs.wifiCustomLimitStart.first()
                val end = userPrefs.wifiCustomLimitEnd.first()
                val usage = getCustomSumUsage(NetworkCapabilities.TRANSPORT_WIFI, start, end)
                if (usage >= limit) return true
            }
        }
        
        return false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        collectionJob?.cancel()
        
        val tickerFlow = flow {
            while (currentCoroutineContext().isActive) {
                emit(System.currentTimeMillis())
                delay(5000)
            }
        }

        collectionJob = serviceScope.launch {
            combine(
                repository.allAppLimits,
                userPrefs.appBlockingMasterEnabled,
                currentNetworkType,
                tickerFlow
            ) { limits, masterEnabled, networkType, _ ->
                if (!masterEnabled) null
                else {
                    val systemLimitExceeded = isSystemPlanLimitExceeded(networkType)
                    if (systemLimitExceeded) {
                        VpnBlockConfig(blockAll = true, blockedApps = emptyList())
                    } else {
                        val blockedApps = limits.filter { limit ->
                            limit.isEnabled && when (limit.networkType) {
                                "wifi" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_WIFI)
                                "mobile" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR)
                                "both" -> {
                                    (limit.isWifiBlocked && (networkType == NetworkCapabilities.TRANSPORT_WIFI)) ||
                                    (limit.isMobileBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR))
                                }
                                else -> limit.isBlocked
                            }
                        }.map { it.packageName }.toList()
                        VpnBlockConfig(blockAll = false, blockedApps = blockedApps)
                    }
                }
            }.distinctUntilChanged().collectLatest { config ->
                if (config == null) {
                    vpnInterface?.close()
                    vpnInterface = null
                    stopSelf()
                } else {
                    updateVpnInterface(config.blockAll, config.blockedApps)
                }
            }
        }
        return START_STICKY
    }

    private fun updateVpnInterface(blockAll: Boolean, blockedApps: List<String>) {
        vpnInterface?.close()
        vpnInterface = null

        if (!blockAll && blockedApps.isEmpty()) {
            Log.d("AppBlockVpnService", "No apps to block. VPN idle.")
            return
        }

        try {
            val builder = Builder()
                .setSession("FlowMeter Block")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)

            if (blockAll) {
                builder.addDisallowedApplication("com.ray.flowmeter")
                Log.d("AppBlockVpnService", "VPN established blocking ALL traffic (system plan limit exceeded)")
            } else {
                for (packageName in blockedApps) {
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (e: Exception) {
                        Log.e("AppBlockVpnService", "Could not add app to VPN: $packageName", e)
                    }
                }
                Log.d("AppBlockVpnService", "VPN established blocking apps: ${blockedApps.joinToString()}")
            }

            vpnInterface = builder.establish()
        } catch (e: Exception) {
            Log.e("AppBlockVpnService", "Failed to establish VPN", e)
        }
    }

    override fun onDestroy() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        serviceJob.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }
}

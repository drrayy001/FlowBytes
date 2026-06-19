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
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@SuppressLint("VpnServicePolicy")
class AppBlockVpnService : VpnService() {

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

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var repository: AppLimitRepository
    private lateinit var userPrefs: UserPreferencesRepository

    private var collectionJob: Job? = null
    private val currentNetworkType = MutableStateFlow<Int?>(null)

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
            // Check if there's any other active network before setting to null
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
        
        // Initial state
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        collectionJob?.cancel()
        collectionJob = serviceScope.launch {
            combine(
                repository.allAppLimits,
                userPrefs.appBlockingMasterEnabled,
                currentNetworkType,
            ) { limits, masterEnabled, networkType ->
                if (!masterEnabled) null
                else {
                    limits.asSequence().filter { limit ->
                        limit.isEnabled && when (limit.networkType) {
                            "wifi" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_WIFI)
                            "mobile" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR)
                            "both" -> {
                                (limit.isWifiBlocked && (networkType == NetworkCapabilities.TRANSPORT_WIFI)) ||
                                (limit.isMobileBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR))
                            }
                            else -> limit.isBlocked // fallback
                        }
                    }.map { it.packageName }.toList()
                }
            }.distinctUntilChanged().collectLatest { blockedApps ->
                if (blockedApps == null) {
                    vpnInterface?.close()
                    vpnInterface = null
                    stopSelf()
                } else {
                    updateVpnInterface(blockedApps)
                }
            }
        }
        return START_STICKY
    }

    private fun updateVpnInterface(blockedApps: List<String>) {
        vpnInterface?.close()
        vpnInterface = null

        if (blockedApps.isEmpty()) {
            Log.d("AppBlockVpnService", "No apps to block. VPN idle.")
            return
        }

        try {
            val builder = Builder()
                .setSession("FlowMeter Block")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)

            for (packageName in blockedApps) {
                try {
                    builder.addAllowedApplication(packageName)
                } catch (e: Exception) {
                    Log.e("AppBlockVpnService", "Could not add app to VPN: $packageName", e)
                }
            }

            vpnInterface = builder.establish()
            Log.d("AppBlockVpnService", "VPN established blocking: ${blockedApps.joinToString()}")
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

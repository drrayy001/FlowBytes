package com.ray.flowmeter.ui.viewmodels

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AppLimitsViewModel(
    private val repository: AppLimitRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val applicationContext: Context,
) : ViewModel() {

    val appLimits: StateFlow<List<AppLimit>> = repository.allAppLimits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dataDailyLimitConfigured: StateFlow<Boolean> = preferencesRepository.dataDailyLimitConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val dataMonthlyLimitConfigured: StateFlow<Boolean> = preferencesRepository.dataMonthlyLimitConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val wifiDailyLimitConfigured: StateFlow<Boolean> = preferencesRepository.wifiDailyLimitConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val wifiMonthlyLimitConfigured: StateFlow<Boolean> = preferencesRepository.wifiMonthlyLimitConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val dataDailyLimitEnabled: StateFlow<Boolean> = preferencesRepository.dataDailyLimitEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val dataMonthlyLimitEnabled: StateFlow<Boolean> = preferencesRepository.dataMonthlyLimitEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val wifiDailyLimitEnabled: StateFlow<Boolean> = preferencesRepository.wifiDailyLimitEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val wifiMonthlyLimitEnabled: StateFlow<Boolean> = preferencesRepository.wifiMonthlyLimitEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val dataDailyLimit: StateFlow<Long> = preferencesRepository.dataDailyLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2_147_483_648L)

    val wifiDailyLimit: StateFlow<Long> = preferencesRepository.wifiDailyLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5_368_709_120L)

    val dataMonthlyLimit: StateFlow<Long> = preferencesRepository.dataMonthlyLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 53_687_091_200L)

    val wifiMonthlyLimit: StateFlow<Long> = preferencesRepository.wifiMonthlyLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 107_374_182_400L)

    val appBlockingMasterEnabled: StateFlow<Boolean> = preferencesRepository.appBlockingMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    private val _currentMobileUsage = MutableStateFlow(0L)
    val currentMobileUsage: StateFlow<Long> = _currentMobileUsage.asStateFlow()

    private val _currentWifiUsage = MutableStateFlow(0L)
    val currentWifiUsage: StateFlow<Long> = _currentWifiUsage.asStateFlow()

    private val _currentMonthlyMobileUsage = MutableStateFlow(0L)
    val currentMonthlyMobileUsage: StateFlow<Long> = _currentMonthlyMobileUsage.asStateFlow()

    private val _currentMonthlyWifiUsage = MutableStateFlow(0L)
    val currentMonthlyWifiUsage: StateFlow<Long> = _currentMonthlyWifiUsage.asStateFlow()

    private var usageJob: Job? = null

    private var monthlyResetDay = 1

    init {
        viewModelScope.launch {
            preferencesRepository.monthlyResetDay.collect {
                monthlyResetDay = it
                updateUsage()
            }
        }
        startUsageTracking()
    }

    private fun startUsageTracking() {
        usageJob?.cancel()
        usageJob = viewModelScope.launch {
            while (true) {
                updateUsage()
                delay(3000) // Update every 3 seconds
            }
        }
    }

    private suspend fun updateUsage() {
        val resetHour = preferencesRepository.resetTimeHour.first()
        val resetMinute = preferencesRepository.resetTimeMinute.first()
        
        val usage = withContext(Dispatchers.IO) {
            getDeviceUsage(resetHour, resetMinute)
        }
        
        _currentMobileUsage.value = usage.dailyMobile
        _currentWifiUsage.value = usage.dailyWifi
        _currentMonthlyMobileUsage.value = usage.monthlyMobile
        _currentMonthlyWifiUsage.value = usage.monthlyWifi
    }

    data class DeviceUsage(val dailyMobile: Long, val dailyWifi: Long, val monthlyMobile: Long, val monthlyWifi: Long)

    private fun getDeviceUsage(resetHour: Int, resetMinute: Int): DeviceUsage {
        val nsm = applicationContext.getSystemService(NetworkStatsManager::class.java)
        val calendar = Calendar.getInstance()
        val endTime = System.currentTimeMillis()

        fun getStartTime(period: String): Long {
            calendar.timeInMillis = endTime
            if (period == "monthly") {
                val clampedDay = monthlyResetDay.coerceAtMost(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar[Calendar.DAY_OF_MONTH] = clampedDay
            }
            calendar[Calendar.HOUR_OF_DAY] = resetHour
            calendar[Calendar.MINUTE] = resetMinute
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0

            if (endTime < calendar.timeInMillis) {
                if (period == "monthly") {
                    calendar.add(Calendar.MONTH, -1)
                    val prevMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar[Calendar.DAY_OF_MONTH] = monthlyResetDay.coerceAtMost(prevMaxDay)
                } else {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
            return calendar.timeInMillis
        }

        fun sumUsage(transport: Int, period: String): Long {
            val start = getStartTime(period)
            return try {
                val stats = nsm.querySummaryForDevice(transport, null, start, endTime)
                stats.rxBytes + stats.txBytes
            } catch (_: Exception) {
                0L
            }
        }

        return DeviceUsage(
            dailyMobile = sumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, "daily"),
            dailyWifi = sumUsage(NetworkCapabilities.TRANSPORT_WIFI, "daily"),
            monthlyMobile = sumUsage(NetworkCapabilities.TRANSPORT_CELLULAR, "monthly"),
            monthlyWifi = sumUsage(NetworkCapabilities.TRANSPORT_WIFI, "monthly"),
        )
    }

    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var isLoadingApps by mutableStateOf(value = false)
        private set

    var searchQuery by mutableStateOf("")

    var isPickerOpen by mutableStateOf(false)
    var editingLimit by mutableStateOf<AppLimit?>(null)
    var isGeneralLimitOpen by mutableStateOf(false)

    val isSubViewOpen: Boolean
        get() = isPickerOpen || (editingLimit != null) || isGeneralLimitOpen

    val filteredApps: List<AppInfo>
        get() = if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }

    fun loadInstalledApps() {
        if (installedApps.isNotEmpty()) return

        viewModelScope.launch {
            isLoadingApps = true
            installedApps = withContext(Dispatchers.IO) {
                val pm = applicationContext.packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                apps.asSequence()
                    .filter { ((it.flags and ApplicationInfo.FLAG_SYSTEM) == 0) || ((it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) }
                    .map { info ->
                        AppInfo(
                            packageName = info.packageName,
                            name = pm.getApplicationLabel(info).toString(),
                        )
                    }
                    .sortedBy { it.name.lowercase() }
                    .toList()
            }
            isLoadingApps = false
        }
    }

    fun addAppLimit(
        packageName: String, 
        appName: String, 
        limitBytes: Long,
        limitType: String = "daily",
        networkType: String = "both",
        wifiLimitBytes: Long = 0L,
        mobileLimitBytes: Long = 0L,
    ) {
        viewModelScope.launch {
            val existing = repository.getAppLimit(packageName)
            if (existing == null) {
                repository.insert(
                    AppLimit(
                        packageName = packageName,
                        appName = appName,
                        dataLimit = limitBytes,
                        limitType = limitType,
                        networkType = networkType,
                        wifiDataLimit = wifiLimitBytes,
                        mobileDataLimit = mobileLimitBytes,
                    ),
                )
            } else {
                repository.update(
                    existing.copy(
                        dataLimit = limitBytes,
                        limitType = limitType,
                        networkType = networkType,
                        wifiDataLimit = wifiLimitBytes,
                        mobileDataLimit = mobileLimitBytes,
                        isBlocked = false,
                        isWifiBlocked = false,
                        isMobileBlocked = false,
                    ),
                )
            }
        }
    }
    
    fun updateAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            repository.update(appLimit)
        }
    }

    fun removeAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            repository.delete(appLimit)
        }
    }

    fun setDataDailyLimitConfigured(configured: Boolean) {
        viewModelScope.launch { preferencesRepository.setDataDailyLimitConfigured(configured) }
    }

    fun setDataMonthlyLimitConfigured(configured: Boolean) {
        viewModelScope.launch { preferencesRepository.setDataMonthlyLimitConfigured(configured) }
    }

    fun setWifiDailyLimitConfigured(configured: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiDailyLimitConfigured(configured) }
    }

    fun setWifiMonthlyLimitConfigured(configured: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiMonthlyLimitConfigured(configured) }
    }

    fun setDataDailyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDataDailyLimitEnabled(enabled) }
    }

    fun setDataMonthlyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDataMonthlyLimitEnabled(enabled) }
    }

    fun setWifiDailyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiDailyLimitEnabled(enabled) }
    }

    fun setWifiMonthlyLimitEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiMonthlyLimitEnabled(enabled) }
    }

    fun setDataDailyLimit(limitBytes: Long) {
        viewModelScope.launch { preferencesRepository.setDataDailyLimit(limitBytes) }
    }

    fun setWifiDailyLimit(limitBytes: Long) {
        viewModelScope.launch { preferencesRepository.setWifiDailyLimit(limitBytes) }
    }

    fun setDataMonthlyLimit(limitBytes: Long) {
        viewModelScope.launch { preferencesRepository.setDataMonthlyLimit(limitBytes) }
    }

    fun setWifiMonthlyLimit(limitBytes: Long) {
        viewModelScope.launch { preferencesRepository.setWifiMonthlyLimit(limitBytes) }
    }

    data class AppInfo(
        val packageName: String,
        val name: String,
    )
}

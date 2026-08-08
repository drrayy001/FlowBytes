package com.ray.flowmeter.ui.viewmodels

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.milliseconds
import java.util.Calendar
import java.util.Locale

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap?,
    val totalUsage: Long,
    val downUsage: Long,
    val upUsage: Long,
    val wifiUsage: Long,
    val cellUsage: Long,
    val wifiDown: Long,
    val wifiUp: Long,
    val cellDown: Long,
    val cellUp: Long,
    val isSystemGroup: Boolean = false,
)

// ViewModel that processes and caches per-app traffic data and structures app categories.
class AppUsageViewModel(
    private val repository: UserPreferencesRepository,
    private val applicationContext: Context,
) : ViewModel() {

    private val _appUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    private var monthlyResetDay = 1

    private val _systemAppUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())

    val timeFilter: StateFlow<String> = repository.usageTimeFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "day")

    val networkFilter: StateFlow<String> = repository.usageNetworkFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all")

    val filteredAppUsageList: StateFlow<List<AppUsageInfo>> = combine(
        _appUsageList,
        networkFilter,
    ) { list, filter ->
        filterAndSort(list, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSystemAppUsageList: StateFlow<List<AppUsageInfo>> = combine(
        _systemAppUsageList,
        networkFilter,
    ) { list, filter ->
        filterAndSort(list, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sorts the app list based on selected network transport filter and total usage.
    private fun filterAndSort(list: List<AppUsageInfo>, filter: String): List<AppUsageInfo> {
        return list.asSequence().filter { app ->
            when (filter) {
                "mobile" -> app.cellUsage > 0
                "wifi" -> app.wifiUsage > 0
                else -> true
            }
        }.sortedByDescending { app ->
            when (filter) {
                "mobile" -> app.cellUsage
                "wifi" -> app.wifiUsage
                else -> app.totalUsage
            }
        }.toList()
    }

    private val iconCache = mutableMapOf<String, ImageBitmap?>()

    var globalWifiDown by mutableLongStateOf(0L)
    var globalWifiUp by mutableLongStateOf(0L)
    var globalCellDown by mutableLongStateOf(0L)
    var globalCellUp by mutableLongStateOf(0L)

    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var selectedDateString by mutableStateOf("")
    var currentViewDate: Calendar by mutableStateOf(Calendar.getInstance())

    var isViewingSystemApps by mutableStateOf(false)

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repository.resetTimeHour,
                repository.resetTimeMinute,
                repository.usageTimeFilter,
                repository.monthlyResetDay,
                repository.language
            ) { h, m, f, r, lang -> Triple(h, m, f) to r }.collect { (triple, resetDay) ->
                val (resetHour, resetMinute, savedTime) = triple
                monthlyResetDay = resetDay
                val start: Long
                val end: Long
                val now = System.currentTimeMillis()

                when (savedTime) {
                    "month" -> {
                        val cal = Calendar.getInstance()
                        val clampedDay = monthlyResetDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, clampedDay)
                        cal.set(Calendar.HOUR_OF_DAY, resetHour)
                        cal.set(Calendar.MINUTE, resetMinute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        
                        if (now < cal.timeInMillis) {
                            cal.add(Calendar.MONTH, -1)
                            val prevMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            cal.set(Calendar.DAY_OF_MONTH, monthlyResetDay.coerceAtMost(prevMaxDay))
                        }
                        
                        start = cal.timeInMillis
                        end = now
                        currentViewDate = (cal.clone() as Calendar)
                        selectedDateString = ""
                    }
                    "custom" -> {
                        start = repository.usageCustomStart.first() ?: (now - 86400000L)
                        val rawEnd = repository.usageCustomEnd.first() ?: now
                        
                        val endCal = Calendar.getInstance().apply { timeInMillis = rawEnd }
                        endCal.set(Calendar.HOUR_OF_DAY, 23)
                        endCal.set(Calendar.MINUTE, 59)
                        endCal.set(Calendar.SECOND, 59)
                        endCal.set(Calendar.MILLISECOND, 999)
                        end = if (endCal.timeInMillis > now) now else endCal.timeInMillis
                        
                        selectedDateString = formatRange(start, rawEnd)
                    }
                    else -> {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, resetHour)
                        cal.set(Calendar.MINUTE, resetMinute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        
                        if (now < cal.timeInMillis) {
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                        }
                        
                        start = cal.timeInMillis
                        end = now
                        currentViewDate = (cal.clone() as Calendar)
                        selectedDateString = ""
                    }
                }
                loadDataInternal(start, end)
            }
        }
    }

    private fun loadDataInternal(start: Long, end: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            _appUsageList.value = getAppUsageList(start, end)
            isLoading = false
        }
    }

    fun setTimeFilter(filter: String) {
        viewModelScope.launch {
            repository.saveUsageTimeFilter(filter)
        }
    }

    fun setNetworkFilter(filter: String) {
        viewModelScope.launch {
            repository.saveUsageNetworkFilter(filter)
        }
    }

    fun refreshData(isManual: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (isManual) isRefreshing = true
            else if (_appUsageList.value.isEmpty()) isLoading = true
            
            val resetHour = repository.resetTimeHour.first()
            val resetMinute = repository.resetTimeMinute.first()
            
            val filter = repository.usageTimeFilter.first()
            val now = System.currentTimeMillis()
            val start: Long
            val end: Long

            when (filter) {
                "month" -> {
                    val cal = (currentViewDate.clone() as Calendar)
                    val clampedDay = monthlyResetDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, clampedDay)
                    cal.set(Calendar.HOUR_OF_DAY, resetHour)
                    cal.set(Calendar.MINUTE, resetMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    
                    if (now < cal.timeInMillis) {
                        cal.add(Calendar.MONTH, -1)
                        val prevMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, monthlyResetDay.coerceAtMost(prevMaxDay))
                    }
                    start = cal.timeInMillis
                    
                    cal.add(Calendar.MONTH, 1)
                    val nextMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, monthlyResetDay.coerceAtMost(nextMaxDay))
                    cal.set(Calendar.MILLISECOND, -1) // End of month
                    end = if (cal.timeInMillis > now) now else cal.timeInMillis
                }
                "custom" -> {
                    start = repository.usageCustomStart.first() ?: (now - 86400000L)
                    val rawEnd = repository.usageCustomEnd.first() ?: now
                    
                    // Inclusive end of day for custom range
                    val endCal = Calendar.getInstance().apply { timeInMillis = rawEnd }
                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59)
                    endCal.set(Calendar.MILLISECOND, 999)
                    end = if (endCal.timeInMillis > now) now else endCal.timeInMillis
                }
                else -> {
                    val cal = (currentViewDate.clone() as Calendar)
                    cal.set(Calendar.HOUR_OF_DAY, resetHour)
                    cal.set(Calendar.MINUTE, resetMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    start = cal.timeInMillis
                    
                    val endCal = (cal.clone() as Calendar)
                    endCal.add(Calendar.DAY_OF_YEAR, 1)
                    endCal.set(Calendar.MILLISECOND, -1) // End of day
                    end = if (endCal.timeInMillis > now) now else endCal.timeInMillis
                }
            }
            _appUsageList.value = getAppUsageList(start, end)
            if (isManual) {
                delay(500.milliseconds)
                isRefreshing = false
            } else {
                isLoading = false
            }
        }
    }

    private fun loadAppUsageForDateInternal(millis: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            val now = System.currentTimeMillis()
            
            val resetHour = repository.resetTimeHour.first()
            val resetMinute = repository.resetTimeMinute.first()
            
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, resetHour)
            cal.set(Calendar.MINUTE, resetMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            currentViewDate = (cal.clone() as Calendar)

            val todayStart = Calendar.getInstance()
            todayStart.set(Calendar.HOUR_OF_DAY, resetHour)
            todayStart.set(Calendar.MINUTE, resetMinute)
            todayStart.set(Calendar.SECOND, 0)
            todayStart.set(Calendar.MILLISECOND, 0)
            if (now < todayStart.timeInMillis) {
                todayStart.add(Calendar.DAY_OF_YEAR, -1)
            }

            selectedDateString = if (
                cal[Calendar.YEAR] == todayStart[Calendar.YEAR] &&
                cal[Calendar.DAY_OF_YEAR] == todayStart[Calendar.DAY_OF_YEAR]
            ) {
                ""
            } else {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(cal.time)
            }

            val start = cal.timeInMillis
            val endCal = (cal.clone() as Calendar)
            endCal.add(Calendar.DAY_OF_YEAR, 1)
            endCal.set(Calendar.MILLISECOND, -1)
            val end = if (endCal.timeInMillis > now) now else endCal.timeInMillis

            _appUsageList.value = getAppUsageList(start, end)
            isLoading = false
        }
    }

    private fun updateCustomRangeFilterInternal(startMillis: Long, endMillis: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            val now = System.currentTimeMillis()
            
            val resetHour = repository.resetTimeHour.first()
            val resetMinute = repository.resetTimeMinute.first()
            
            repository.saveUsageCustomRange(startMillis, endMillis)
            selectedDateString = formatRange(startMillis, endMillis)
            
            val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
            startCal.set(Calendar.HOUR_OF_DAY, resetHour)
            startCal.set(Calendar.MINUTE, resetMinute)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            
            val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
            endCal.set(Calendar.HOUR_OF_DAY, resetHour)
            endCal.set(Calendar.MINUTE, resetMinute)
            endCal.set(Calendar.SECOND, 0)
            endCal.set(Calendar.MILLISECOND, 0)
            endCal.add(Calendar.DAY_OF_YEAR, 1)
            endCal.add(Calendar.MILLISECOND, -1)
            
            val finalEnd = if (endCal.timeInMillis > now) now else endCal.timeInMillis
            
            _appUsageList.value = getAppUsageList(startCal.timeInMillis, finalEnd)
            isLoading = false
        }
    }

    fun updateToThisMonth() {
        viewModelScope.launch {
            val resetHour = repository.resetTimeHour.first()
            val resetMinute = repository.resetTimeMinute.first()
            val now = System.currentTimeMillis()

            val cal = Calendar.getInstance()
            val clampedDay = monthlyResetDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.DAY_OF_MONTH, clampedDay)
            cal.set(Calendar.HOUR_OF_DAY, resetHour)
            cal.set(Calendar.MINUTE, resetMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            if (now < cal.timeInMillis) {
                cal.add(Calendar.MONTH, -1)
                val prevMaxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, monthlyResetDay.coerceAtMost(prevMaxDay))
            }
            
            currentViewDate = cal
            selectedDateString = ""
            
            repository.saveUsageTimeFilter("month")
            refreshData(isManual = false)
        }
    }

    fun updateMonthFilter(year: Int, month: Int) {
        viewModelScope.launch {
            val resetHour = repository.resetTimeHour.first()
            val resetMinute = repository.resetTimeMinute.first()
            val now = System.currentTimeMillis()

            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            val clampedDay = monthlyResetDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.DAY_OF_MONTH, clampedDay)
            cal.set(Calendar.HOUR_OF_DAY, resetHour)
            cal.set(Calendar.MINUTE, resetMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            currentViewDate = (cal.clone() as Calendar)

            val thisMonthStart = Calendar.getInstance()
            val clampedDayThis = monthlyResetDay.coerceAtMost(thisMonthStart.getActualMaximum(Calendar.DAY_OF_MONTH))
            thisMonthStart.set(Calendar.DAY_OF_MONTH, clampedDayThis)
            thisMonthStart.set(Calendar.HOUR_OF_DAY, resetHour)
            thisMonthStart.set(Calendar.MINUTE, resetMinute)
            thisMonthStart.set(Calendar.SECOND, 0)
            thisMonthStart.set(Calendar.MILLISECOND, 0)
            if (now < thisMonthStart.timeInMillis) {
                thisMonthStart.add(Calendar.MONTH, -1)
                val prevMaxDay = thisMonthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                thisMonthStart.set(Calendar.DAY_OF_MONTH, monthlyResetDay.coerceAtMost(prevMaxDay))
            }

            selectedDateString = if (
                cal[Calendar.YEAR] == thisMonthStart[Calendar.YEAR] && 
                cal[Calendar.MONTH] == thisMonthStart[Calendar.MONTH]
            ) {
                ""
            } else {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }

            repository.saveUsageTimeFilter("month")
            refreshData(isManual = false)
        }
    }

    fun updateCustomRangeFilter(start: Long, end: Long) {
        viewModelScope.launch {
            repository.saveUsageTimeFilter("custom")
            updateCustomRangeFilterInternal(start, end)
        }
    }

    fun loadAppUsageForDate(millis: Long) {
        viewModelScope.launch {
            repository.saveUsageTimeFilter("day")
            loadAppUsageForDateInternal(millis)
        }
    }

    private fun formatRange(start: Long, end: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return "${sdf.format(start)} - ${sdf.format(end)}"
    }

    fun moveDate(backwards: Boolean) {
        val filter = timeFilter.value
        val amount = if (backwards) -1 else 1
        if (filter == "month") {
            currentViewDate.add(Calendar.MONTH, amount)
            updateMonthFilter(currentViewDate[Calendar.YEAR], currentViewDate[Calendar.MONTH])
        } else {
            currentViewDate.add(Calendar.DAY_OF_YEAR, amount)
            loadAppUsageForDate(currentViewDate.timeInMillis)
        }
    }

    private fun cleanAppName(name: String): String {
        return name.replace(Regex("\\s+"), " ").trim()
    }

    private data class ResolvedInfo(
        val packageName: String,
        val appName: String,
        val icon: ImageBitmap?,
        val isSystem: Boolean,
        val isProcess: Boolean = false,
    )

    private fun resolveUidToInfo(uid: Int, packageManager: PackageManager, systemIcon: ImageBitmap?): ResolvedInfo {
        when (uid) {
            -2, -4 -> return ResolvedInfo("removed_$uid", applicationContext.getString(R.string.label_removed_apps), null, isSystem = true, isProcess = true)
            -3, -5 -> return ResolvedInfo("tethering_$uid", applicationContext.getString(R.string.label_tethering), null, isSystem = true, isProcess = true)
            0 -> return ResolvedInfo("root_0", applicationContext.getString(R.string.label_root), systemIcon, isSystem = true, isProcess = true)
            1000 -> return ResolvedInfo("android.system_$uid", applicationContext.getString(R.string.label_android_system), systemIcon, isSystem = true, isProcess = true)
            1051, 1052 -> return ResolvedInfo("android.dns_$uid", applicationContext.getString(R.string.label_dns_resolver), systemIcon, isSystem = true, isProcess = true)
            1020 -> return ResolvedInfo("android.mdns_$uid", applicationContext.getString(R.string.label_mdns_responder), systemIcon, isSystem = true, isProcess = true)
            1013 -> return ResolvedInfo("android.media_$uid", applicationContext.getString(R.string.label_media_service), systemIcon, isSystem = true, isProcess = true)
            1061, 2904 -> return ResolvedInfo("android.ota_$uid", applicationContext.getString(R.string.label_system_update), systemIcon, isSystem = true, isProcess = true)
        }

        val packages = try {
            packageManager.getPackagesForUid(uid)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }

        if (packages.isNullOrEmpty()) {
            val systemName = try {
                packageManager.getNameForUid(uid)
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            } ?: (applicationContext.getString(R.string.label_system_processes) + " ($uid)")
            val icon = if (uid < 10000) systemIcon else null
            return ResolvedInfo("uid_$uid", systemName, icon, isSystem = true, isProcess = true)
        }

        var isSystem = uid < 10000
        if (!isSystem) {
            var isUserApp = false
            for (pkg in packages) {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val isSystemPkg = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val hasLaunchIntent = packageManager.getLaunchIntentForPackage(pkg) != null

                    if (!isSystemPkg || isUpdatedSystem || hasLaunchIntent) {
                        isUserApp = true
                        break
                    }
                } catch (_: Exception) {}
            }
            isSystem = !isUserApp
        }

        for (pkg in packages) {
            val info = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                
                val icon = synchronized(iconCache) {
                    iconCache.getOrPut(pkg) {
                        packageManager.getApplicationIcon(appInfo).toBitmap(width = 96, height = 96).asImageBitmap()
                    }
                }

                ResolvedInfo("${pkg}_$uid", name, icon, isSystem)
            } catch (_: Exception) {
                null
            }
            info?.let { return it }
        }

        return ResolvedInfo("${packages[0]}_$uid", packages[0], if (uid < 10000) systemIcon else null, isSystem)
    }

    private suspend fun getAppUsageList(startTime: Long, endTime: Long): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val networkStatsManager = applicationContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val packageManager = applicationContext.packageManager

        val wifiDownMap = mutableMapOf<Int, Long>()
        val wifiUpMap = mutableMapOf<Int, Long>()
        val cellDownMap = mutableMapOf<Int, Long>()
        val cellUpMap = mutableMapOf<Int, Long>()

        coroutineScope {
            val wifiJob = async {
                queryDetailedUsage(networkStatsManager, NetworkCapabilities.TRANSPORT_WIFI, startTime, endTime, wifiDownMap, wifiUpMap)
            }
            val cellJob = async {
                queryDetailedUsage(networkStatsManager, NetworkCapabilities.TRANSPORT_CELLULAR, startTime, endTime, cellDownMap, cellUpMap)
            }
            wifiJob.await()
            cellJob.await()
        }

        val allUids = (wifiDownMap.keys + wifiUpMap.keys + cellDownMap.keys + cellUpMap.keys).toSet()

        val systemIcon = try {
            val appInfo = packageManager.getApplicationInfo("android", 0)
            synchronized(iconCache) {
                iconCache.getOrPut("android") {
                    packageManager.getApplicationIcon(appInfo).toBitmap(width = 96, height = 96).asImageBitmap()
                }
            }
        } catch (_: Exception) {
            null
        }

        val resolvedInfos = coroutineScope {
            allUids.map { uid ->
                async { uid to resolveUidToInfo(uid, packageManager, systemIcon) }
            }.awaitAll().toMap()
        }

        val systemApps = mutableListOf<AppUsageInfo>()
        val systemProcesses = mutableListOf<AppUsageInfo>()
        val userList = mutableListOf<AppUsageInfo>()

        allUids.forEach { uid ->
            val wifiDown = wifiDownMap[uid] ?: 0L
            val wifiUp = wifiUpMap[uid] ?: 0L
            val cellDown = cellDownMap[uid] ?: 0L
            val cellUp = cellUpMap[uid] ?: 0L

            val totalDownForApp = wifiDown + cellDown
            val totalUpForApp = wifiUp + cellUp

            val totalWifi = wifiDown + wifiUp
            val totalCell = cellDown + cellUp
            val absoluteTotal = totalWifi + totalCell

            if (absoluteTotal > 0) {
                val info = resolvedInfos[uid] ?: return@forEach

                val appUsage = AppUsageInfo(
                    packageName = info.packageName,
                    appName = cleanAppName(info.appName),
                    iconBitmap = info.icon,
                    totalUsage = absoluteTotal,
                    downUsage = totalDownForApp,
                    upUsage = totalUpForApp,
                    wifiUsage = totalWifi,
                    cellUsage = totalCell,
                    wifiDown = wifiDown,
                    wifiUp = wifiUp,
                    cellDown = cellDown,
                    cellUp = cellUp,
                    isSystemGroup = false,
                )
                
                if (info.isSystem) {
                    if (info.isProcess) {
                        systemProcesses.add(appUsage)
                    } else {
                        systemApps.add(appUsage)
                    }
                } else {
                    userList.add(appUsage)
                }
            }
        }

        val finalSystemList = mutableListOf<AppUsageInfo>()
        finalSystemList.addAll(systemApps)

        if (systemProcesses.isNotEmpty()) {
            val totalProcessUsage = systemProcesses.sumOf { it.totalUsage }
            if (totalProcessUsage > 0) {
                val systemIcon = try {
                    val appInfo = packageManager.getApplicationInfo("android", 0)
                    packageManager.getApplicationIcon(appInfo).toBitmap(width = 96, height = 96).asImageBitmap()
                } catch (_: Exception) { null }

                val processGroup = AppUsageInfo(
                    packageName = "system_processes_group",
                    appName = applicationContext.getString(R.string.label_system_processes),
                    iconBitmap = systemIcon,
                    totalUsage = totalProcessUsage,
                    downUsage = systemProcesses.sumOf { it.downUsage },
                    upUsage = systemProcesses.sumOf { it.upUsage },
                    wifiUsage = systemProcesses.sumOf { it.wifiUsage },
                    cellUsage = systemProcesses.sumOf { it.cellUsage },
                    wifiDown = systemProcesses.sumOf { it.wifiDown },
                    wifiUp = systemProcesses.sumOf { it.wifiUp },
                    cellDown = systemProcesses.sumOf { it.cellDown },
                    cellUp = systemProcesses.sumOf { it.cellUp },
                    isSystemGroup = false,
                )
                finalSystemList.add(processGroup)
            }
        }

        _systemAppUsageList.value = finalSystemList

        val totalSystemUsage = finalSystemList.sumOf { it.totalUsage }
        if (totalSystemUsage > 0) {
            val systemIcon = try {
                val appInfo = packageManager.getApplicationInfo("android", 0)
                packageManager.getApplicationIcon(appInfo).toBitmap(width = 96, height = 96).asImageBitmap()
            } catch (_: Exception) { null }

            val systemGroup = AppUsageInfo(
                packageName = "system_group",
                appName = applicationContext.getString(R.string.label_system_apps),
                iconBitmap = systemIcon,
                totalUsage = totalSystemUsage,
                downUsage = finalSystemList.sumOf { it.downUsage },
                upUsage = finalSystemList.sumOf { it.upUsage },
                wifiUsage = finalSystemList.sumOf { it.wifiUsage },
                cellUsage = finalSystemList.sumOf { it.cellUsage },
                wifiDown = finalSystemList.sumOf { it.wifiDown },
                wifiUp = finalSystemList.sumOf { it.wifiUp },
                cellDown = finalSystemList.sumOf { it.cellDown },
                cellUp = finalSystemList.sumOf { it.cellUp },
                isSystemGroup = true,
            )
            userList.add(systemGroup)
        }

        val sumWifiDown = wifiDownMap.values.sum()
        val sumWifiUp = wifiUpMap.values.sum()
        val sumCellDown = cellDownMap.values.sum()
        val sumCellUp = cellUpMap.values.sum()

        withContext(Dispatchers.Main) {
            globalWifiDown = sumWifiDown
            globalWifiUp = sumWifiUp
            globalCellDown = sumCellDown
            globalCellUp = sumCellUp
        }

        return@withContext userList
    }

    private fun queryDetailedUsage(
        manager: NetworkStatsManager,
        transportType: Int,
        startTime: Long,
        endTime: Long,
        downMap: MutableMap<Int, Long>,
        upMap: MutableMap<Int, Long>,
    ) {
        try {
            val stats = manager.querySummary(transportType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                downMap[uid] = (downMap[uid] ?: 0L) + bucket.rxBytes
                upMap[uid] = (upMap[uid] ?: 0L) + bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) {
        }
    }
}

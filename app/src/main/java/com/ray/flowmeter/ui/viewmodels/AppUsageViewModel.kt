package com.ray.flowmeter.ui.viewmodels

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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

class AppUsageViewModel(
    private val repository: UserPreferencesRepository,
    private val applicationContext: Context,
) : ViewModel() {

    private val _appUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())

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

    var globalWifiDown by mutableStateOf(0L)
    var globalWifiUp by mutableStateOf(0L)
    var globalCellDown by mutableStateOf(0L)
    var globalCellUp by mutableStateOf(0L)

    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var selectedDateString by mutableStateOf("")
    var currentViewDate: Calendar by mutableStateOf(Calendar.getInstance())

    var isViewingSystemApps by mutableStateOf(false)

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val savedTime = repository.usageTimeFilter.first()
            val start: Long
            val end: Long
            val now = System.currentTimeMillis()

            when (savedTime) {
                "month" -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    start = cal.timeInMillis
                    end = now
                    currentViewDate = cal
                    selectedDateString = ""
                }
                "custom" -> {
                    start = repository.usageCustomStart.first() ?: (now - 86400000L)
                    end = repository.usageCustomEnd.first() ?: now
                    selectedDateString = formatRange(start, end)
                }
                else -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    start = cal.timeInMillis
                    end = now
                    currentViewDate = cal
                    selectedDateString = ""
                }
            }
            loadDataInternal(start, end)
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
        viewModelScope.launch {
            if (isManual) isRefreshing = true
            val filter = timeFilter.value
            val now = System.currentTimeMillis()
            val start: Long
            val end: Long

            when (filter) {
                "month" -> {
                    val cal = (currentViewDate.clone() as Calendar)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    start = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    end = if (cal.timeInMillis > now) now else cal.timeInMillis
                }
                "custom" -> {
                    start = repository.usageCustomStart.first() ?: (now - 86400000L)
                    end = repository.usageCustomEnd.first() ?: now
                }
                else -> {
                    val cal = (currentViewDate.clone() as Calendar)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    start = cal.timeInMillis
                    val endCal = (cal.clone() as Calendar)
                    endCal.add(Calendar.DAY_OF_YEAR, 1)
                    end = if (endCal.timeInMillis > now) now else endCal.timeInMillis
                }
            }
            _appUsageList.value = getAppUsageList(start, end)
            if (isManual) {
                delay(500)
                isRefreshing = false
            }
        }
    }

    private fun loadAppUsageForDateInternal(millis: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            currentViewDate = cal

            val today = Calendar.getInstance()
            selectedDateString = if (
                cal[Calendar.YEAR] == today[Calendar.YEAR] &&
                cal[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]
            ) {
                ""
            } else {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(cal.time)
            }

            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val end = if (cal.timeInMillis > System.currentTimeMillis()) System.currentTimeMillis() else cal.timeInMillis

            _appUsageList.value = getAppUsageList(start, end)
            isLoading = false
        }
    }

    private fun updateCustomRangeFilterInternal(start: Long, end: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            repository.saveUsageCustomRange(start, end)
            selectedDateString = formatRange(start, end)
            _appUsageList.value = getAppUsageList(start, end)
            isLoading = false
        }
    }

    fun updateDateFilter(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        loadAppUsageForDateInternal(cal.timeInMillis)
    }

    fun updateToThisMonth() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            currentViewDate = cal
            selectedDateString = ""
            refreshData(isManual = false)
        }
    }

    fun updateMonthFilter(year: Int, month: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(year, month, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            currentViewDate = cal

            val now = Calendar.getInstance()
            selectedDateString = if (year == now[Calendar.YEAR] && month == now[Calendar.MONTH]) {
                ""
            } else {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }

            setTimeFilter("month")
            refreshData(isManual = false)
        }
    }

    fun updateCustomRangeFilter(start: Long, end: Long) {
        viewModelScope.launch {
            setTimeFilter("custom")
            updateCustomRangeFilterInternal(start, end)
        }
    }

    fun loadAppUsageForDate(millis: Long) {
        viewModelScope.launch {
            setTimeFilter("day")
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

    private fun resolveUidToInfo(uid: Int, packageManager: PackageManager): ResolvedInfo {
        val systemIcon = try {
            val appInfo = packageManager.getApplicationInfo("android", 0)
            iconCache.getOrPut("android") {
                packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }

        when (uid) {
            -2, -4 -> return ResolvedInfo("removed_$uid", "Removed Apps", null, true, true)
            -3, -5 -> return ResolvedInfo("tethering_$uid", "Tethering", null, true, true)
            0 -> return ResolvedInfo("root_0", "Root", systemIcon, true, true)
            1000 -> return ResolvedInfo("android.system_$uid", "Android System", systemIcon, true, true)
            1051, 1052 -> return ResolvedInfo("android.dns_$uid", "DNS Resolver", systemIcon, true, true)
            1020 -> return ResolvedInfo("android.mdns_$uid", "mDNS Responder", systemIcon, true, true)
            1013 -> return ResolvedInfo("android.media_$uid", "Media Service", systemIcon, true, true)
            1061, 2904 -> return ResolvedInfo("android.ota_$uid", "System Update", systemIcon, true, true)
        }

        val packages = packageManager.getPackagesForUid(uid)
        if (packages.isNullOrEmpty()) {
            val systemName = packageManager.getNameForUid(uid) ?: "System process ($uid)"
            val icon = if (uid < 10000) systemIcon else null
            return ResolvedInfo("uid_$uid", systemName, icon, true, true)
        }

        var isSystem = uid < 10000
        // Check if any package is a system app
        for (pkg in packages) {
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    isSystem = true
                    break
                }
            } catch (_: Exception) {}
        }

        for (pkg in packages) {
            val info = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val icon = iconCache.getOrPut(pkg) {
                    packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
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
                val info = resolveUidToInfo(uid, packageManager)

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
                    packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
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
                    isSystemGroup = false, // Individual item in the system list, not a nested group
                )
                finalSystemList.add(processGroup)
            }
        }

        _systemAppUsageList.value = finalSystemList

        val totalSystemUsage = finalSystemList.sumOf { it.totalUsage }
        if (totalSystemUsage > 0) {
            val systemIcon = try {
                val appInfo = packageManager.getApplicationInfo("android", 0)
                packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
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

    // Query detailed usage per UID and update maps
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
            // Ignore
        }
    }
}

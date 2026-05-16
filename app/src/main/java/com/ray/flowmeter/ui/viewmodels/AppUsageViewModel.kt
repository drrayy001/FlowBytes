package com.ray.flowmeter.ui.viewmodels

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
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
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
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
)

// ViewModel for managing and loading per-app network usage statistics
class AppUsageViewModel(private val repository: UserPreferencesRepository, private val applicationContext: Context) : ViewModel() {

    private val _appUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageInfo>> = _appUsageList.asStateFlow()

    val timeFilter: StateFlow<String> = repository.usageTimeFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "day")

    val networkFilter: StateFlow<String> = repository.usageNetworkFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all")

    val filteredAppUsageList: StateFlow<List<AppUsageInfo>> = combine(
        _appUsageList,
        networkFilter,
    ) { list, filter ->
        list.asSequence().filter { app ->
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val iconCache = mutableMapOf<String, ImageBitmap?>()

    var globalWifiDown by mutableLongStateOf(0L)
    var globalWifiUp by mutableLongStateOf(0L)
    var globalCellDown by mutableLongStateOf(0L)
    var globalCellUp by mutableLongStateOf(0L)

    var isLoading by mutableStateOf(value = true)
    var isRefreshing by mutableStateOf(value = false)
    var selectedDateString by mutableStateOf("Today")
    var currentViewDate: Calendar by mutableStateOf(Calendar.getInstance())

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.usageTimeFilter.collectLatest { filterType ->
                when (filterType) {
                    "month" -> {
                        // If it's the first load or if we are not in month mode yet, reset to this month
                        // Otherwise, refreshData will handle the currentViewDate correctly
                        if (selectedDateString == "Today" || selectedDateString.isBlank()) {
                            updateToThisMonth()
                        } else {
                            refreshData(isManual = false)
                        }
                    }
                    "day" -> {
                        if (selectedDateString == "This Month" || selectedDateString.isBlank() || selectedDateString.contains("-")) {
                            loadAppUsageForDate(System.currentTimeMillis())
                        } else {
                            refreshData(isManual = false)
                        }
                    }
                    "custom" -> {
                        val start = repository.usageCustomStart.first()
                        val end = repository.usageCustomEnd.first()
                        if ((start != null) && (end != null)) {
                            updateCustomRangeFilter(start, end)
                        } else {
                            loadAppUsageForDate(System.currentTimeMillis())
                        }
                    }
                    else -> {
                        if (appUsageList.value.isEmpty()) {
                            loadAppUsageForDate(System.currentTimeMillis())
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadDataInternal(startTime: Long, endTime: Long, dateString: String) {
        isLoading = true
        selectedDateString = dateString
        val result = getAppUsageList(startTime, endTime)
        _appUsageList.value = result
        isLoading = false
    }

    private fun loadData(startTime: Long, endTime: Long, dateString: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadDataInternal(startTime, endTime, dateString)
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
            if (isManual) {
                isRefreshing = true
                delay(500)
            }

            val filter = repository.usageTimeFilter.first()
            when (filter) {
                "month" -> {
                    updateMonthFilter(
                        currentViewDate[Calendar.YEAR],
                        currentViewDate[Calendar.MONTH]
                    )
                }
                "day" -> {
                    loadAppUsageForDate(currentViewDate.timeInMillis)
                }
                "custom" -> {
                    val start = repository.usageCustomStart.first()
                    val end = repository.usageCustomEnd.first()
                    if ((start != null) && (end != null)) {
                        updateCustomRangeFilterInternal(start, end)
                    } else {
                        loadAppUsageForDateInternal(System.currentTimeMillis())
                    }
                }
                else -> {
                    if (appUsageList.value.isEmpty()) {
                        loadAppUsageForDateInternal(System.currentTimeMillis())
                    }
                }
            }
            if (isManual) {
                isRefreshing = false
            }
        }
    }

    private suspend fun loadAppUsageForDateInternal(timeInMillis: Long) {
        val resetHour = repository.resetTimeHour.first()
        val resetMinute = repository.resetTimeMinute.first()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis

        calendar[Calendar.HOUR_OF_DAY] = resetHour
        calendar[Calendar.MINUTE] = resetMinute
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        val startTime = calendar.timeInMillis

        val endTime = startTime + (24 * 60 * 60 * 1000L) - 1
        val now = System.currentTimeMillis()
        val actualEndTime = if (endTime > now) now else endTime

        val dateStr = if ((now in startTime..endTime)) {
            "Today"
        } else {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(calendar.time)
        }

        loadDataInternal(startTime, actualEndTime, dateStr)
    }

    private suspend fun updateCustomRangeFilterInternal(startMillis: Long, endMillis: Long) {
        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        var actualEndTime = endCalendar.timeInMillis
        val now = System.currentTimeMillis()
        if (actualEndTime > now) {
            actualEndTime = now
        }

        val dateStr = "${formatter.format(startMillis)} - ${formatter.format(endMillis)}"
        loadDataInternal(startMillis, actualEndTime, dateStr)
    }

    fun updateDateFilter(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar[year, month] = dayOfMonth
        currentViewDate = calendar
        loadAppUsageForDate(calendar.timeInMillis)
    }

    fun updateToThisMonth() {
        val calendar = Calendar.getInstance()
        currentViewDate = calendar
        val now = System.currentTimeMillis()

        calendar[Calendar.DAY_OF_MONTH] = 1
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        val startTime = calendar.timeInMillis

        loadData(startTime, now, "This Month")
    }

    fun updateMonthFilter(year: Int, month: Int) {
        val calendar = Calendar.getInstance()
        calendar[year, month, 1, 0, 0] = 0
        calendar[Calendar.MILLISECOND] = 0
        currentViewDate = calendar
        val startTime = calendar.timeInMillis

        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.MONTH, 1)
        endCalendar.add(Calendar.MILLISECOND, -1)
        var endTime = endCalendar.timeInMillis

        val now = System.currentTimeMillis()
        if (endTime > now) {
            endTime = now
        }

        val nowCal = Calendar.getInstance()
        val dateStr = if (year == nowCal[Calendar.YEAR] && month == nowCal[Calendar.MONTH]) {
            "This Month"
        } else {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(startTime)
        }
        loadData(startTime, endTime, dateStr)
    }

    fun updateCustomRangeFilter(startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            repository.saveUsageCustomRange(startMillis, endMillis)

            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())

            val endCalendar = Calendar.getInstance().apply {
                timeInMillis = endMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            var actualEndTime = endCalendar.timeInMillis
            val now = System.currentTimeMillis()
            if (actualEndTime > now) {
                actualEndTime = now
            }

            val dateStr = "${formatter.format(startMillis)} - ${formatter.format(endMillis)}"
            loadData(startMillis, actualEndTime, dateStr)
        }
    }

    fun loadAppUsageForDate(timeInMillis: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val resetHour = repository.resetTimeHour.first()
            val resetMinute = repository.resetTimeMinute.first()

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeInMillis
            currentViewDate = calendar

            calendar[Calendar.HOUR_OF_DAY] = resetHour
            calendar[Calendar.MINUTE] = resetMinute
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            val startTime = calendar.timeInMillis

            val endTime = startTime + (24 * 60 * 60 * 1000L) - 1
            val now = System.currentTimeMillis()
            val actualEndTime = if (endTime > now) now else endTime

            val dateStr = if ((now in startTime..endTime)) {
                "Today"
            } else {
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                formatter.format(calendar.time)
            }

            loadDataInternal(startTime, actualEndTime, dateStr)
        }
    }

    fun moveDate(backwards: Boolean) {
        viewModelScope.launch {
            val filter = repository.usageTimeFilter.first()
            val newCal = currentViewDate.clone() as Calendar
            if (filter == "month") {
                newCal.add(Calendar.MONTH, if (backwards) -1 else 1)
                updateMonthFilter(newCal[Calendar.YEAR], newCal[Calendar.MONTH])
            } else {
                newCal.add(Calendar.DAY_OF_YEAR, if (backwards) -1 else 1)
                loadAppUsageForDate(newCal.timeInMillis)
            }
        }
    }

    // Clean up app name for display
    private fun cleanAppName(rawName: String): String {
        if (!rawName.contains(".")) return rawName
        val lastPart = rawName.substringAfterLast('.')
        return lastPart.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    private data class ResolvedInfo(val packageName: String, val appName: String, val icon: ImageBitmap?)

    private fun resolveUidToInfo(uid: Int, packageManager: android.content.pm.PackageManager): ResolvedInfo {
        val systemIcon = try {
            val appInfo = packageManager.getApplicationInfo("android", 0)
            iconCache.getOrPut("android") {
                packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }

        when (uid) {
            -2, -4 -> return ResolvedInfo("removed_$uid", "Removed Apps", null)
            -3, -5 -> return ResolvedInfo("tethering_$uid", "Tethering", null)
            0 -> return ResolvedInfo("root_0", "Root", systemIcon)
            1000 -> return ResolvedInfo("android.system_$uid", "Android System", systemIcon)
            1051, 1052 -> return ResolvedInfo("android.dns_$uid", "DNS Resolver", systemIcon)
            1020 -> return ResolvedInfo("android.mdns_$uid", "mDNS Responder", systemIcon)
            1013 -> return ResolvedInfo("android.media_$uid", "Media Service", systemIcon)
            1061, 2904 -> return ResolvedInfo("android.ota_$uid", "System Update", systemIcon)
        }

        val packages = packageManager.getPackagesForUid(uid)
        if (packages.isNullOrEmpty()) {
            val systemName = packageManager.getNameForUid(uid) ?: "System process ($uid)"
            val icon = if (uid < 10000) systemIcon else null
            return ResolvedInfo("uid_$uid", systemName, icon)
        }

        for (pkg in packages) {
            val info = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val icon = iconCache.getOrPut(pkg) {
                    packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
                }
                ResolvedInfo("${pkg}_$uid", name, icon)
            } catch (_: Exception) {
                null
            }
            info?.let { return it }
        }

        return ResolvedInfo("${packages[0]}_$uid", packages[0], if (uid < 10000) systemIcon else null)
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

        val resultList = allUids.asSequence().mapNotNull { uid ->
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

                AppUsageInfo(
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
                )
            } else {
                null
            }
        }.sortedByDescending { it.totalUsage }.toList()

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

        return@withContext resultList
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
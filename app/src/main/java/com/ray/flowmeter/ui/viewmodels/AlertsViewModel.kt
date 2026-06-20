package com.ray.flowmeter.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ray.flowmeter.data.AlertRepository
import com.ray.flowmeter.data.AppAlert
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.service.NetworkMonitoringService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// ViewModel managing user alerts, filter configurations, and custom muting actions.
class AlertsViewModel(
    private val repository: AlertRepository,
    private val userPrefsRepository: UserPreferencesRepository,
) : ViewModel() {

    val selectedCategory: StateFlow<String> = userPrefsRepository.alertsCategory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ALL")

    private val _isRefreshing = mutableStateOf(value = false)
    val isRefreshing: Boolean get() = _isRefreshing.value

    val alerts: StateFlow<List<AppAlert>> = combine(
        repository.allAlerts,
        selectedCategory,
    ) { allAlerts, category ->
        if (category == "ALL") allAlerts
        else allAlerts.filter { it.alertType == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshData(isManual: Boolean = true) {
        if (!isManual) return
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500.milliseconds)
            _isRefreshing.value = false
        }
    }

    fun setSelectedCategory(category: String) {
        viewModelScope.launch {
            userPrefsRepository.saveAlertsCategory(category)
        }
    }

    var muteRequestAppName by mutableStateOf<String?>(null)
        private set

    fun onMuteRequested(appName: String) {
        muteRequestAppName = appName
    }

    fun clearMuteRequest() {
        muteRequestAppName = null
    }

    // Commits mute status to local database and broadcasts it to NetworkMonitoringService.
    fun muteApp(context: Context, appName: String, durationMs: Long) {
        viewModelScope.launch {
            repository.markLastAlertAsMuted(appName)
        }
        val intent = Intent(context, NetworkMonitoringService::class.java).apply {
            action = NetworkMonitoringService.ACTION_SET_MUTE_DURATION
            putExtra(NetworkMonitoringService.EXTRA_MUTE_APP_NAME, appName)
            putExtra(NetworkMonitoringService.EXTRA_MUTE_DURATION_MS, durationMs)
        }
        context.startService(intent)
        clearMuteRequest()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

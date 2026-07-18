package com.ray.flowmeter.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.utils.BillingEvent
import com.ray.flowmeter.utils.BillingManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object ThemeMode {
    const val SYSTEM = "System"
    const val LIGHT = "Light"
    const val DARK = "Dark"
}

// ViewModel that exposes user settings and coordinates configuration modifications (Theme, Language, Billing).
class SettingsViewModel(
    private val repository: UserPreferencesRepository,
    initialTheme: String = ThemeMode.SYSTEM,
    initialMaterialYou: Boolean = true,
    initialAmoled: Boolean = false,
    initialAccent: Long? = null
) : ViewModel() {

    init {
        // Migrate legacy configurations to modern options format where applicable.
        viewModelScope.launch {
            when (repository.themeMode.first()) {
                "Material" -> {
                    repository.saveThemeMode(ThemeMode.SYSTEM)
                    repository.setUseMaterialYou(enabled = true)
                }
                "Amoled" -> {
                    repository.saveThemeMode(ThemeMode.DARK)
                    repository.setUseAmoled(enabled = true)
                    repository.setUseMaterialYou(enabled = false)
                }
                "White" -> {
                    repository.saveThemeMode(ThemeMode.LIGHT)
                    repository.setUseMaterialYou(enabled = false)
                }
            }
        }
    }

    val monitoringEnabled: StateFlow<Boolean?> = repository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accentColor: StateFlow<Long?> = repository.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialAccent)

    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialTheme)

    val useMaterialYou: StateFlow<Boolean> = repository.useMaterialYou
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = initialMaterialYou)

    val useAmoled: StateFlow<Boolean> = repository.useAmoled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = initialAmoled)

    val showNotification: StateFlow<Boolean> = repository.showNotification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val notificationContentType: StateFlow<String> = repository.notificationContentType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BOTH")

    val notificationIconScale: StateFlow<Float> = repository.notificationIconScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.28f)

    val highPriorityNotification: StateFlow<Boolean> = repository.highPriorityNotification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val resetTimeHour: StateFlow<Int> = repository.resetTimeHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val resetTimeMinute: StateFlow<Int> = repository.resetTimeMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyResetDay: StateFlow<Int> = repository.monthlyResetDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val showOnlyWhenConnected: StateFlow<Boolean> = repository.showOnlyWhenConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val highTrafficDetectionEnabled: StateFlow<Boolean> = repository.highTrafficDetectionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val trafficThresholdSpeed: StateFlow<Long> = repository.trafficThresholdSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1_000_000L)

    val trafficThresholdTime: StateFlow<Long> = repository.trafficThresholdTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60_000L)

    val trafficAlertCooldown: StateFlow<Long> = repository.trafficAlertCooldown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 600_000L)

    val trafficResetBelowThresholdTime: StateFlow<Long> = repository.trafficResetBelowThresholdTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5_000L)

    val trafficResetSpeed: StateFlow<Long> = repository.trafficResetSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200_000L)

    val appBlockingMasterEnabled: StateFlow<Boolean?> = repository.appBlockingMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val vpnDisclosureAccepted: StateFlow<Boolean> = repository.vpnDisclosureAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val widgetShowSpeed: StateFlow<Boolean> = repository.widgetShowSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val widgetUsageType: StateFlow<String> = repository.widgetUsageType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "DAILY")

    val widgetUpdateInterval: StateFlow<Int> = repository.widgetUpdateInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 30)

    fun toggleMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonitoringEnabled(enabled)
        }
    }

    fun setAccentColor(color: Long?) {
        viewModelScope.launch {
            repository.setAccentColor(color)
        }
    }

    fun toggleNotification(show: Boolean) {
        viewModelScope.launch {
            repository.setShowNotification(show)
        }
    }

    fun setNotificationContentType(type: String) {
        viewModelScope.launch {
            repository.setNotificationContentType(type)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.saveThemeMode(mode)
        }
    }

    fun setUseMaterialYou(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseMaterialYou(enabled)
        }
    }

    fun setUseAmoled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseAmoled(enabled)
        }
    }

    fun setNotificationIconScale(scale: Float) {
        viewModelScope.launch { repository.setNotificationIconScale(scale) }
    }

    fun setHighPriorityNotification(enabled: Boolean) {
        viewModelScope.launch { repository.setHighPriorityNotification(enabled) }
    }

    fun setResetTime(hour: Int, minute: Int) {
        viewModelScope.launch { repository.setResetTime(hour, minute) }
    }

    fun setMonthlyResetDay(day: Int) {
        viewModelScope.launch { repository.setMonthlyResetDay(day) }
    }

    fun setShowOnlyWhenConnected(enabled: Boolean) {
        viewModelScope.launch { repository.setShowOnlyWhenConnected(enabled) }
    }

    fun setHighTrafficDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHighTrafficDetectionEnabled(enabled)
        }
    }

    fun saveTrafficDetectionSettings(
        thresholdSpeed: Long,
        thresholdTime: Long,
        alertCooldown: Long,
        resetBelowThresholdTime: Long,
        resetSpeed: Long,
    ) {
        viewModelScope.launch {
            repository.saveTrafficDetectionSettings(
                thresholdSpeed = thresholdSpeed,
                thresholdTime = thresholdTime,
                alertCooldown = alertCooldown,
                resetBelowThresholdTime = resetBelowThresholdTime,
                resetSpeed = resetSpeed,
            )
        }
    }

    fun toggleAppBlockingMaster(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppBlockingMasterEnabled(enabled)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.setLanguage(languageCode)
        }
    }

    fun setVpnDisclosureAccepted(accepted: Boolean) {
        viewModelScope.launch {
            repository.setVpnDisclosureAccepted(accepted)
        }
    }

    private var billingManager: BillingManager? = null
    private val _billingEvents = MutableSharedFlow<BillingEvent>()
    val billingEvents = _billingEvents.asSharedFlow()

    // Initializes Play Billing client connection to handle voluntary user support donations.
    fun initBilling(context: Context) {
        if (billingManager == null) {
            val manager = BillingManager(context.applicationContext, viewModelScope)
            billingManager = manager
            viewModelScope.launch {
                manager.events.collect { event ->
                    _billingEvents.emit(event)
                }
            }
        }
    }

    fun makeDonation(activity: android.app.Activity, amount: String) {
        val productId = when (amount) {
            "1.00" -> "donate_1"
            "2.50" -> "donate_2_5"
            "6.00" -> "donate_6"
            else -> return
        }
        billingManager?.makePurchase(activity, productId)
    }

    fun markAsReviewed() {
        viewModelScope.launch {
            repository.setUserReviewedRated(true)
        }
    }

    fun setWidgetShowSpeed(show: Boolean) {
        viewModelScope.launch { repository.setWidgetShowSpeed(show) }
    }

    fun setWidgetUsageType(type: String) {
        viewModelScope.launch { repository.setWidgetUsageType(type) }
    }

    fun setWidgetUpdateInterval(interval: Int) {
        viewModelScope.launch { repository.setWidgetUpdateInterval(interval) }
    }

    val supportBannerDismissed: StateFlow<Boolean> = repository.supportBannerDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appLaunchCount: StateFlow<Int> = repository.appLaunchCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val firstInstallTime: StateFlow<Long> = repository.firstInstallTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val checkUpdatesAutomatically: StateFlow<Boolean> = repository.checkUpdatesAutomatically
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lastUpdateCheckTime: StateFlow<Long> = repository.lastUpdateCheckTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val ignoredUpdateVersion: StateFlow<String> = repository.ignoredUpdateVersion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun dismissSupportBanner() {
        viewModelScope.launch {
            repository.setSupportBannerDismissed(true)
        }
    }

    fun setCheckUpdatesAutomatically(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCheckUpdatesAutomatically(enabled)
        }
    }

    fun setLastUpdateCheckTime(time: Long) {
        viewModelScope.launch {
            repository.setLastUpdateCheckTime(time)
        }
    }

    fun setIgnoredUpdateVersion(version: String) {
        viewModelScope.launch {
            repository.setIgnoredUpdateVersion(version)
        }
    }
}

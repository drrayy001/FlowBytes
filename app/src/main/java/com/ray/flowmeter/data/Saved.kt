package com.ray.flowmeter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

// --- Database Entities & DAOs ---

@Entity(tableName = "app_alerts")
data class AppAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val appName: String?,
    val packageName: String?,
    val rxBytes: Long,
    val txBytes: Long,
    val speed: Long,
    val isMuted: Boolean = false,
    val alertType: String = "HIGH_TRAFFIC", // "HIGH_TRAFFIC", "APP_LIMIT", "DAILY_LIMIT"
    val limitValue: Long = 0L, // Used for limit alerts
)

@Dao
interface AppAlertDao {
    @Insert
    suspend fun insert(alert: AppAlert)

    @Query("SELECT * FROM app_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AppAlert>>

    @Query("UPDATE app_alerts SET isMuted = :isMuted WHERE id = (SELECT id FROM app_alerts WHERE appName = :appName ORDER BY timestamp DESC LIMIT 1)")
    suspend fun updateLastAlertMutedForApp(appName: String, isMuted: Boolean)

    @Query("DELETE FROM app_alerts")
    suspend fun deleteAll()
}

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dataLimit: Long,
    val limitType: String = "daily",
    val networkType: String = "both",
    val currentUsage: Long = 0L,
    val currentWifiUsage: Long = 0L,
    val currentMobileUsage: Long = 0L,
    val isBlocked: Boolean = false,
    val lastResetTime: Long = System.currentTimeMillis(),
    val wifiDataLimit: Long = 0L,
    val mobileDataLimit: Long = 0L,
    val isWifiBlocked: Boolean = false,
    val isMobileBlocked: Boolean = false,
    val isEnabled: Boolean = true,
)

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits")
    fun getAllAppLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAllAppLimitsList(): List<AppLimit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appLimit: AppLimit)

    @Update
    suspend fun update(appLimit: AppLimit)

    @Delete
    suspend fun delete(appLimit: AppLimit)

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getAppLimit(packageName: String): AppLimit?
}

@Database(entities = [AppAlert::class, AppLimit::class], version = 7, exportSchema = false)
abstract class FlowMeterDatabase : RoomDatabase() {
    abstract fun appAlertDao(): AppAlertDao
    abstract fun appLimitDao(): AppLimitDao

    companion object {
        @Volatile
        private var INSTANCE: FlowMeterDatabase? = null

        fun getDatabase(context: Context): FlowMeterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlowMeterDatabase::class.java,
                    "flowmeter_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository Layer ---

class AlertRepository(private val appAlertDao: AppAlertDao) {
    val allAlerts: Flow<List<AppAlert>> = appAlertDao.getAllAlerts()

    suspend fun insert(alert: AppAlert) {
        appAlertDao.insert(alert)
    }

    suspend fun markLastAlertAsMuted(appName: String) {
        appAlertDao.updateLastAlertMutedForApp(appName, isMuted = true)
    }

    suspend fun clearHistory() {
        appAlertDao.deleteAll()
    }
}

class AppLimitRepository(private val appLimitDao: AppLimitDao) {
    val allAppLimits: Flow<List<AppLimit>> = appLimitDao.getAllAppLimits()

    suspend fun insert(appLimit: AppLimit) {
        appLimitDao.insert(appLimit)
    }

    suspend fun update(appLimit: AppLimit) {
        appLimitDao.update(appLimit)
    }

    suspend fun delete(appLimit: AppLimit) {
        appLimitDao.delete(appLimit)
    }

    suspend fun getAppLimit(packageName: String): AppLimit? {
        return appLimitDao.getAppLimit(packageName)
    }

    suspend fun getAllAppLimitsList(): List<AppLimit> {
        return appLimitDao.getAllAppLimitsList()
    }
}

// --- User Preferences Storage (DataStore) ---

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val USE_AMOLED = booleanPreferencesKey("use_amoled")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val NOTIFICATION_CONTENT_TYPE = stringPreferencesKey("notification_content_type")
        val DATA_DAILY_LIMIT_CONFIGURED = booleanPreferencesKey("data_daily_limit_configured")
        val DATA_MONTHLY_LIMIT_CONFIGURED = booleanPreferencesKey("data_monthly_limit_configured")
        val WIFI_DAILY_LIMIT_CONFIGURED = booleanPreferencesKey("wifi_daily_limit_configured")
        val WIFI_MONTHLY_LIMIT_CONFIGURED = booleanPreferencesKey("wifi_monthly_limit_configured")

        val DATA_DAILY_LIMIT_ENABLED = booleanPreferencesKey("data_daily_limit_enabled")
        val DATA_MONTHLY_LIMIT_ENABLED = booleanPreferencesKey("data_monthly_limit_enabled")
        val WIFI_DAILY_LIMIT_ENABLED = booleanPreferencesKey("wifi_daily_limit_enabled")
        val WIFI_MONTHLY_LIMIT_ENABLED = booleanPreferencesKey("wifi_monthly_limit_enabled")
        val DATA_DAILY_LIMIT = longPreferencesKey("data_daily_limit")
        val WIFI_DAILY_LIMIT = longPreferencesKey("wifi_daily_limit")
        val DATA_MONTHLY_LIMIT = longPreferencesKey("data_monthly_limit")
        val WIFI_MONTHLY_LIMIT = longPreferencesKey("wifi_monthly_limit")
        val DATA_CUSTOM_LIMIT_CONFIGURED = booleanPreferencesKey("data_custom_limit_configured")
        val WIFI_CUSTOM_LIMIT_CONFIGURED = booleanPreferencesKey("wifi_custom_limit_configured")
        val DATA_CUSTOM_LIMIT_ENABLED = booleanPreferencesKey("data_custom_limit_enabled")
        val WIFI_CUSTOM_LIMIT_ENABLED = booleanPreferencesKey("wifi_custom_limit_enabled")
        val DATA_CUSTOM_LIMIT = longPreferencesKey("data_custom_limit")
        val WIFI_CUSTOM_LIMIT = longPreferencesKey("wifi_custom_limit")
        val DATA_CUSTOM_LIMIT_START = longPreferencesKey("data_custom_limit_start")
        val DATA_CUSTOM_LIMIT_END = longPreferencesKey("data_custom_limit_end")
        val WIFI_CUSTOM_LIMIT_START = longPreferencesKey("wifi_custom_limit_start")
        val WIFI_CUSTOM_LIMIT_END = longPreferencesKey("wifi_custom_limit_end")
        val NOTIFICATION_ICON_SCALE = floatPreferencesKey("notification_icon_scale")
        val HIGH_PRIORITY_NOTIFICATION = booleanPreferencesKey("high_priority_notification")
        val RESET_TIME_HOUR = intPreferencesKey("reset_time_hour")
        val RESET_TIME_MINUTE = intPreferencesKey("reset_time_minute")
        val SHOW_ONLY_WHEN_CONNECTED = booleanPreferencesKey("show_only_when_connected")
        val HIGH_TRAFFIC_DETECTION_ENABLED = booleanPreferencesKey("high_traffic_detection_enabled")

        val TRAFFIC_THRESHOLD_SPEED = longPreferencesKey("traffic_threshold_speed")
        val TRAFFIC_THRESHOLD_TIME = longPreferencesKey("traffic_threshold_time")
        val TRAFFIC_ALERT_COOLDOWN = longPreferencesKey("traffic_alert_cooldown")
        val TRAFFIC_RESET_BELOW_THRESHOLD_TIME = longPreferencesKey("traffic_reset_below_threshold_time")
        val TRAFFIC_RESET_SPEED = longPreferencesKey("traffic_reset_speed")

        val USAGE_TIME_FILTER = stringPreferencesKey("usage_time_filter")
        val USAGE_NETWORK_FILTER = stringPreferencesKey("usage_network_filter")
        val USAGE_CUSTOM_START = longPreferencesKey("usage_custom_start")
        val USAGE_CUSTOM_END = longPreferencesKey("usage_custom_end")

        val LAST_VERSION_CODE = intPreferencesKey("last_version_code")
        val APP_BLOCKING_MASTER_ENABLED = booleanPreferencesKey("app_blocking_master_enabled")
        val VPN_DISCLOSURE_ACCEPTED = booleanPreferencesKey("vpn_disclosure_accepted")
        val USAGE_CHART_TYPE = stringPreferencesKey("usage_chart_type")
        val ALERTS_CATEGORY = stringPreferencesKey("alerts_category")
        val MONTHLY_RESET_DAY = intPreferencesKey("monthly_reset_day")
        val LANGUAGE = stringPreferencesKey("language")

        val APP_LAUNCH_COUNT = intPreferencesKey("app_launch_count")
        val FIRST_INSTALL_TIME = longPreferencesKey("first_install_time")
        val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
        val USER_REVIEWED_RATED = booleanPreferencesKey("user_reviewed_rated")

        val WIDGET_SHOW_SPEED = booleanPreferencesKey("widget_show_speed")
        val WIDGET_USAGE_TYPE = stringPreferencesKey("widget_usage_type") // "DAILY", "MONTHLY"
        val SUPPORT_BANNER_DISMISSED = booleanPreferencesKey("support_banner_dismissed")
    }

    private val preferencesFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // --- Preferences Read Streams (Flows) ---

    val onboardingCompleted: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }.distinctUntilChanged()

    val supportBannerDismissed: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.SUPPORT_BANNER_DISMISSED] ?: false
        }.distinctUntilChanged()

    val monitoringEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.MONITORING_ENABLED] ?: false
        }.distinctUntilChanged()

    val themeMode: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: "System"
        }.distinctUntilChanged()

    val useMaterialYou: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USE_MATERIAL_YOU] ?: true
        }.distinctUntilChanged()

    val useAmoled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USE_AMOLED] ?: false
        }.distinctUntilChanged()

    val accentColor: Flow<Long?> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR]
        }.distinctUntilChanged()

    val showNotification: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_NOTIFICATION] ?: true
        }.distinctUntilChanged()

    val notificationContentType: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_CONTENT_TYPE] ?: "BOTH"
        }.distinctUntilChanged()

    val dataDailyLimitConfigured: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_DAILY_LIMIT_CONFIGURED] ?: false
        }.distinctUntilChanged()

    val dataMonthlyLimitConfigured: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_MONTHLY_LIMIT_CONFIGURED] ?: false
        }.distinctUntilChanged()

    val wifiDailyLimitConfigured: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_DAILY_LIMIT_CONFIGURED] ?: false
        }.distinctUntilChanged()

    val wifiMonthlyLimitConfigured: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_MONTHLY_LIMIT_CONFIGURED] ?: false
        }.distinctUntilChanged()

    val dataDailyLimitEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_DAILY_LIMIT_ENABLED] ?: false
        }.distinctUntilChanged()

    val dataMonthlyLimitEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_MONTHLY_LIMIT_ENABLED] ?: false
        }.distinctUntilChanged()

    val wifiDailyLimitEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_DAILY_LIMIT_ENABLED] ?: false
        }.distinctUntilChanged()

    val wifiMonthlyLimitEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_MONTHLY_LIMIT_ENABLED] ?: false
        }.distinctUntilChanged()

    val dataCustomLimitConfigured: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_CONFIGURED] ?: false
        }.distinctUntilChanged()

    val wifiCustomLimitConfigured: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_CONFIGURED] ?: false
        }.distinctUntilChanged()

    val dataCustomLimitEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_ENABLED] ?: false
        }.distinctUntilChanged()

    val wifiCustomLimitEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_ENABLED] ?: false
        }.distinctUntilChanged()

    val dataDailyLimit: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_DAILY_LIMIT] ?: 2_147_483_648L
        }.distinctUntilChanged()

    val wifiDailyLimit: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_DAILY_LIMIT] ?: 5_368_709_120L
        }.distinctUntilChanged()

    val dataMonthlyLimit: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_MONTHLY_LIMIT] ?: 53_687_091_200L // 50 GB default
        }.distinctUntilChanged()

    val wifiMonthlyLimit: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_MONTHLY_LIMIT] ?: 107_374_182_400L // 100 GB default
        }.distinctUntilChanged()

    val dataCustomLimit: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT] ?: 0L
        }.distinctUntilChanged()

    val wifiCustomLimit: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT] ?: 0L
        }.distinctUntilChanged()

    val dataCustomLimitStart: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_START] ?: 0L
        }.distinctUntilChanged()

    val dataCustomLimitEnd: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_END] ?: 0L
        }.distinctUntilChanged()

    val wifiCustomLimitStart: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_START] ?: 0L
        }.distinctUntilChanged()

    val wifiCustomLimitEnd: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_END] ?: 0L
        }.distinctUntilChanged()

    val notificationIconScale: Flow<Float> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ICON_SCALE] ?: 1.28f
        }.distinctUntilChanged()

    val highPriorityNotification: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.HIGH_PRIORITY_NOTIFICATION] ?: true
        }.distinctUntilChanged()

    val resetTimeHour: Flow<Int> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.RESET_TIME_HOUR] ?: 0
        }.distinctUntilChanged()

    val resetTimeMinute: Flow<Int> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.RESET_TIME_MINUTE] ?: 0
        }.distinctUntilChanged()

    val monthlyResetDay: Flow<Int> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.MONTHLY_RESET_DAY] ?: 1
        }.distinctUntilChanged()

    val showOnlyWhenConnected: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_ONLY_WHEN_CONNECTED] ?: false
        }.distinctUntilChanged()

    val highTrafficDetectionEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.HIGH_TRAFFIC_DETECTION_ENABLED] ?: false
        }.distinctUntilChanged()

    val trafficThresholdSpeed: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.TRAFFIC_THRESHOLD_SPEED] ?: 1_000_000L
        }.distinctUntilChanged()

    val trafficThresholdTime: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.TRAFFIC_THRESHOLD_TIME] ?: 60_000L
        }.distinctUntilChanged()

    val trafficAlertCooldown: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.TRAFFIC_ALERT_COOLDOWN] ?: 600_000L
        }.distinctUntilChanged()

    val trafficResetBelowThresholdTime: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.TRAFFIC_RESET_BELOW_THRESHOLD_TIME] ?: 5_000L
        }.distinctUntilChanged()

    val trafficResetSpeed: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.TRAFFIC_RESET_SPEED] ?: 200_000L
        }.distinctUntilChanged()

    val usageTimeFilter: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USAGE_TIME_FILTER] ?: "day"
        }.distinctUntilChanged()

    val usageNetworkFilter: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USAGE_NETWORK_FILTER] ?: "all"
        }.distinctUntilChanged()

    val usageCustomStart: Flow<Long?> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USAGE_CUSTOM_START]
        }.distinctUntilChanged()

    val usageCustomEnd: Flow<Long?> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USAGE_CUSTOM_END]
        }.distinctUntilChanged()

    val lastVersionCode: Flow<Int> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.LAST_VERSION_CODE] ?: 0
        }.distinctUntilChanged()

    val appBlockingMasterEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.APP_BLOCKING_MASTER_ENABLED] ?: false
        }.distinctUntilChanged()

    val vpnDisclosureAccepted: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.VPN_DISCLOSURE_ACCEPTED] ?: false
        }.distinctUntilChanged()

    val usageChartType: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USAGE_CHART_TYPE] ?: "COMBINED"
        }.distinctUntilChanged()

    val alertsCategory: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.ALERTS_CATEGORY] ?: "ALL"
        }.distinctUntilChanged()

    val language: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: ""
        }.distinctUntilChanged()

    val appLaunchCount: Flow<Int> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.APP_LAUNCH_COUNT] ?: 0
        }.distinctUntilChanged()

    val firstInstallTime: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.FIRST_INSTALL_TIME] ?: 0L
        }.distinctUntilChanged()

    val lastReviewPromptTime: Flow<Long> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.LAST_REVIEW_PROMPT_TIME] ?: 0L
        }.distinctUntilChanged()

    val userReviewedRated: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.USER_REVIEWED_RATED] ?: false
        }.distinctUntilChanged()

    val widgetShowSpeed: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIDGET_SHOW_SPEED] ?: true
        }.distinctUntilChanged()

    val widgetUsageType: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.WIDGET_USAGE_TYPE] ?: "DAILY"
        }.distinctUntilChanged()

    // --- Preferences Write Operations ---

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONITORING_ENABLED] = enabled
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setUseMaterialYou(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MATERIAL_YOU] = enabled
        }
    }

    suspend fun setUseAmoled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_AMOLED] = enabled
        }
    }

    suspend fun setAccentColor(color: Long?) {
        context.dataStore.edit { preferences ->
            if (color == null) {
                preferences.remove(PreferencesKeys.ACCENT_COLOR)
            } else {
                preferences[PreferencesKeys.ACCENT_COLOR] = color
            }
        }
    }

    suspend fun setShowNotification(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_NOTIFICATION] = show
        }
    }

    suspend fun setNotificationContentType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_CONTENT_TYPE] = type
        }
    }

    suspend fun setDataDailyLimitConfigured(configured: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_DAILY_LIMIT_CONFIGURED] = configured
        }
    }

    suspend fun setDataMonthlyLimitConfigured(configured: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_MONTHLY_LIMIT_CONFIGURED] = configured
        }
    }

    suspend fun setWifiDailyLimitConfigured(configured: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_DAILY_LIMIT_CONFIGURED] = configured
        }
    }

    suspend fun setWifiMonthlyLimitConfigured(configured: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_MONTHLY_LIMIT_CONFIGURED] = configured
        }
    }

    suspend fun setDataDailyLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_DAILY_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun setDataMonthlyLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_MONTHLY_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun setWifiDailyLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_DAILY_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun setWifiMonthlyLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_MONTHLY_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun setDataCustomLimitConfigured(configured: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_CONFIGURED] = configured
        }
    }

    suspend fun setWifiCustomLimitConfigured(configured: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_CONFIGURED] = configured
        }
    }

    suspend fun setDataCustomLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun setWifiCustomLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun setDataDailyLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_DAILY_LIMIT] = limitBytes
        }
    }

    suspend fun setWifiDailyLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_DAILY_LIMIT] = limitBytes
        }
    }

    suspend fun setDataMonthlyLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_MONTHLY_LIMIT] = limitBytes
        }
    }

    suspend fun setWifiMonthlyLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_MONTHLY_LIMIT] = limitBytes
        }
    }

    suspend fun setDataCustomLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT] = limitBytes
        }
    }

    suspend fun setWifiCustomLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT] = limitBytes
        }
    }

    suspend fun setDataCustomLimitRange(start: Long, end: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_START] = start
            preferences[PreferencesKeys.DATA_CUSTOM_LIMIT_END] = end
        }
    }

    suspend fun setWifiCustomLimitRange(start: Long, end: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_START] = start
            preferences[PreferencesKeys.WIFI_CUSTOM_LIMIT_END] = end
        }
    }

    suspend fun setNotificationIconScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ICON_SCALE] = scale
        }
    }

    suspend fun setHighPriorityNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_PRIORITY_NOTIFICATION] = enabled
        }
    }

    suspend fun setResetTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESET_TIME_HOUR] = hour
            preferences[PreferencesKeys.RESET_TIME_MINUTE] = minute
        }
    }

    suspend fun setMonthlyResetDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_RESET_DAY] = day.coerceIn(1, 31)
        }
    }

    suspend fun setShowOnlyWhenConnected(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ONLY_WHEN_CONNECTED] = enabled
        }
    }

    suspend fun setHighTrafficDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_TRAFFIC_DETECTION_ENABLED] = enabled
        }
    }

    suspend fun saveTrafficDetectionSettings(
        thresholdSpeed: Long,
        thresholdTime: Long,
        alertCooldown: Long,
        resetBelowThresholdTime: Long,
        resetSpeed: Long
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRAFFIC_THRESHOLD_SPEED] = thresholdSpeed
            preferences[PreferencesKeys.TRAFFIC_THRESHOLD_TIME] = thresholdTime
            preferences[PreferencesKeys.TRAFFIC_ALERT_COOLDOWN] = alertCooldown
            preferences[PreferencesKeys.TRAFFIC_RESET_BELOW_THRESHOLD_TIME] = resetBelowThresholdTime
            preferences[PreferencesKeys.TRAFFIC_RESET_SPEED] = resetSpeed
        }
    }

    suspend fun saveUsageTimeFilter(filter: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_TIME_FILTER] = filter
        }
    }

    suspend fun saveUsageNetworkFilter(filter: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_NETWORK_FILTER] = filter
        }
    }

    suspend fun saveUsageCustomRange(start: Long, end: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_CUSTOM_START] = start
            preferences[PreferencesKeys.USAGE_CUSTOM_END] = end
        }
    }

    suspend fun updateLastVersionCode(versionCode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_VERSION_CODE] = versionCode
        }
    }

    suspend fun setAppBlockingMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_BLOCKING_MASTER_ENABLED] = enabled
        }
    }

    suspend fun setVpnDisclosureAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VPN_DISCLOSURE_ACCEPTED] = accepted
        }
    }

    suspend fun saveUsageChartType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_CHART_TYPE] = type
        }
    }

    suspend fun saveAlertsCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERTS_CATEGORY] = category
        }
    }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = languageCode
        }
    }

    suspend fun incrementLaunchCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.APP_LAUNCH_COUNT] ?: 0
            preferences[PreferencesKeys.APP_LAUNCH_COUNT] = current + 1
            if (preferences[PreferencesKeys.FIRST_INSTALL_TIME] == null || preferences[PreferencesKeys.FIRST_INSTALL_TIME] == 0L) {
                preferences[PreferencesKeys.FIRST_INSTALL_TIME] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setUserReviewedRated(reviewed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_REVIEWED_RATED] = reviewed
        }
    }

    suspend fun setLastReviewPromptTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_REVIEW_PROMPT_TIME] = time
        }
    }

    suspend fun setWidgetShowSpeed(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_SHOW_SPEED] = show
        }
    }

    suspend fun setWidgetUsageType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_USAGE_TYPE] = type
        }
    }

    suspend fun setSupportBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPPORT_BANNER_DISMISSED] = dismissed
        }
    }
}

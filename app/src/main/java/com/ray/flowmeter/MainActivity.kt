package com.ray.flowmeter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ray.flowmeter.data.AlertRepository
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.service.AppBlockVpnService
import com.ray.flowmeter.service.NetworkMonitoringService
import com.ray.flowmeter.receiver.WidgetUpdateScheduler
import com.ray.flowmeter.ui.dialogs.ChangelogDialog
import android.net.Uri
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import android.util.Log
import com.ray.flowmeter.utils.AppUpdateHelper
import com.ray.flowmeter.utils.UpdateResult
import com.ray.flowmeter.ui.dialogs.UpdateDialog
import com.ray.flowmeter.ui.screens.Destination
import com.ray.flowmeter.ui.screens.MainScreen
import com.ray.flowmeter.ui.screens.OnboardingScreen
import com.ray.flowmeter.ui.theme.FlowMeterTheme
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.OnboardingViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import com.ray.flowmeter.utils.LocaleHelper
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.net.VpnService
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

// Main entry activity. Handles app startup, database/repository initialization,
// Compose UI hosting, and orchestration of background monitoring and VPN blocking services.
class MainActivity : ComponentActivity() {

    private var currentAppliedLanguage: String = ""
    private lateinit var appUpdateHelper: AppUpdateHelper
    private var appUpdateManager: AppUpdateManager? = null

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateCompletedToast()
        }
    }

    private fun showUpdateCompletedToast() {
        Toast.makeText(
            this,
            "An update has been downloaded. Restarting app in 3 seconds to complete install...",
            Toast.LENGTH_LONG
        ).show()
        lifecycleScope.launch {
            delay(3000)
            appUpdateManager?.completeUpdate()
        }
    }

    // --- VPN Permission & Startup Orchestration ---
    
    // Result launcher to handle the user's response to the system VPN permission dialog.
    private val vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            // Revert the setting if permission was denied by the user.
            val repository = UserPreferencesRepository(applicationContext)
            kotlinx.coroutines.MainScope().launch {
                repository.setAppBlockingMasterEnabled(false)
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, AppBlockVpnService::class.java)
        startService(intent)
    }

    // Requests system VPN permission if needed, otherwise starts the VPN directly.
    private fun prepareVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnRequestLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lay out UI components edge-to-edge behind system status/navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val repository = UserPreferencesRepository(applicationContext)
        appUpdateHelper = AppUpdateHelper(this, repository)
        if (appUpdateHelper.getInstallerPackageName(this) == "com.android.vending") {
            val manager = AppUpdateManagerFactory.create(this)
            appUpdateManager = manager
            manager.registerListener(installStateUpdatedListener)
        }

        // Initialize language configuration before setting up the Compose layout.
        lifecycleScope.launch {
            val languageCode = try {
                repository.language.first()
            } catch (_: Exception) {
                ""
            }
            if (languageCode != currentAppliedLanguage) {
                currentAppliedLanguage = languageCode
                LocaleHelper.applyLocale(this@MainActivity, languageCode)
            }
        }

        val database = FlowMeterDatabase.getDatabase(applicationContext)
        val alertRepository = AlertRepository(database.appAlertDao())
        val appLimitRepository = AppLimitRepository(database.appLimitDao())

        // Track launches to evaluate when to prompt the user for an app review/rating.
        lifecycleScope.launch {
            repository.incrementLaunchCount()
        }

        setContent {
            val (gitHubUpdate, setGitHubUpdate) = remember { mutableStateOf<UpdateResult.GitHubUpdateAvailable?>(null) }
            // Load user theme preferences asynchronously before rendering the app theme.
            val themeSettingsState = produceState<ThemeSettings?>(initialValue = null) {
                val themeMode = repository.themeMode.first()
                val useMaterialYou = repository.useMaterialYou.first()
                val useAmoled = repository.useAmoled.first()
                val accentColor = repository.accentColor.first()
                value = ThemeSettings(themeMode, useMaterialYou, useAmoled, accentColor)
            }

            val settings = themeSettingsState.value
            if (settings == null) {
                // Show a matching blank background during the brief preference loading phase to prevent screen flash.
                val isDark = isSystemInDarkTheme()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF1A1B1E) else Color(0xFFFDFBFF))
                )
            } else {
                // --- ViewModel Provisioning ---
                // ViewModels are created with custom factories to inject repositories and application contexts.
                
                val onboardingViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return OnboardingViewModel(repository) as T
                            }
                        },
                    )[OnboardingViewModel::class.java]
                }

                val settingsViewModel = remember(settings) {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return SettingsViewModel(
                                    repository = repository,
                                    initialTheme = settings.themeMode,
                                    initialMaterialYou = settings.useMaterialYou,
                                    initialAmoled = settings.useAmoled,
                                    initialAccent = settings.accentColor
                                ) as T
                            }
                        },
                    )[SettingsViewModel::class.java]
                }

                val homeViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return HomeViewModel(applicationContext, repository) as T
                            }
                        },
                    )[HomeViewModel::class.java]
                }

                val appUsageViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AppUsageViewModel(repository, applicationContext) as T
                            }
                        },
                    )[AppUsageViewModel::class.java]
                }

                val alertsViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AlertsViewModel(alertRepository, repository) as T
                            }
                        },
                    )[AlertsViewModel::class.java]
                }

                val appLimitsViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AppLimitsViewModel(appLimitRepository, repository, applicationContext) as T
                            }
                        },
                    )[AppLimitsViewModel::class.java]
                }

                val themeMode by settingsViewModel.themeMode.collectAsState()
                val languageCode by settingsViewModel.language.collectAsState()
                val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
                val useAmoled by settingsViewModel.useAmoled.collectAsState()
                val accentColor by settingsViewModel.accentColor.collectAsState()
                val onboardingCompleted by repository.onboardingCompleted.collectAsState(null)

                CompositionLocalProvider(
                    LocalContext provides LocaleHelper.applyLocale(LocalContext.current, languageCode)
                ) {
                    var isInitialCollect by remember { mutableStateOf(true) }
                    // Recreate activity to apply runtime language changes if the language changed.
                    LaunchedEffect(languageCode) {
                        if (isInitialCollect) {
                            isInitialCollect = false
                        } else if (languageCode != currentAppliedLanguage) {
                            recreate()
                        }
                    }

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    val (showChangelog, setShowChangelog) = remember { mutableStateOf(false) }

                    val appLaunchCount by repository.appLaunchCount.collectAsState(0)
                    val firstInstallTime by repository.firstInstallTime.collectAsState(0L)
                    val lastReviewPromptTime by repository.lastReviewPromptTime.collectAsState(0L)
                    val userReviewedRated by repository.userReviewedRated.collectAsState(false)
                    val lastVersionCode by repository.lastVersionCode.collectAsState(-1)

                    val checkUpdatesAutomatically by repository.checkUpdatesAutomatically.collectAsState(true)
                    val lastUpdateCheckTime by repository.lastUpdateCheckTime.collectAsState(0L)

                    LaunchedEffect(onboardingCompleted) {
                        if (onboardingCompleted == true) {
                            val now = System.currentTimeMillis()
                            val oneDay = 24 * 60 * 60 * 1000L
                            if (checkUpdatesAutomatically && (now - lastUpdateCheckTime >= oneDay)) {
                                repository.setLastUpdateCheckTime(now)
                                appUpdateHelper.checkForUpdates { result ->
                                    when (result) {
                                        is UpdateResult.PlayStoreUpdateAvailable -> {
                                            @Suppress("DEPRECATION")
                                            appUpdateManager?.startUpdateFlowForResult(
                                                result.appUpdateInfo,
                                                AppUpdateType.FLEXIBLE,
                                                this@MainActivity,
                                                UPDATE_REQUEST_CODE
                                            )
                                        }
                                        is UpdateResult.GitHubUpdateAvailable -> {
                                            setGitHubUpdate(result)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }

                    // Prompt user to rate the app after sufficient launches and time have elapsed.
                    val context = LocalContext.current
                    LaunchedEffect(onboardingCompleted, showChangelog, appLaunchCount, firstInstallTime, lastReviewPromptTime, userReviewedRated) {
                        if (onboardingCompleted == true && !showChangelog && !userReviewedRated) {
                            val now = System.currentTimeMillis()
                            val threeDays = 3 * 24 * 60 * 60 * 1000L
                            val sevenDays = 7 * 24 * 60 * 60 * 1000L

                            val isTimeSinceInstallOk = (now - firstInstallTime) >= threeDays
                            val isTimeSinceLastPromptOk = (now - lastReviewPromptTime) >= sevenDays
                            val isLaunchCountOk = appLaunchCount >= 3

                            if (isTimeSinceInstallOk && isTimeSinceLastPromptOk && isLaunchCountOk) {
                                delay(2000.milliseconds)
                                try {
                                    val manager = ReviewManagerFactory.create(context)
                                    Log.d("MainActivity", "Requesting Play In-App Review Flow")
                                    manager.requestReviewFlow().addOnCompleteListener { requestTask ->
                                        if (requestTask.isSuccessful) {
                                            val reviewInfo = requestTask.result
                                            Log.d("MainActivity", "Launching Play In-App Review Flow")
                                            manager.launchReviewFlow(this@MainActivity, reviewInfo).addOnCompleteListener {
                                                Log.d("MainActivity", "Play In-App Review completed")
                                                lifecycleScope.launch {
                                                    repository.setUserReviewedRated(true)
                                                }
                                            }
                                        } else {
                                            val exception = requestTask.exception
                                            Log.e("MainActivity", "Play In-App Review request failed", exception)
                                            lifecycleScope.launch {
                                                repository.setLastReviewPromptTime(System.currentTimeMillis())
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Play In-App Review wrapper failed", e)
                                    repository.setLastReviewPromptTime(System.currentTimeMillis())
                                }
                            }
                        }
                    }

                    // Check for version code changes to determine if we should update settings or trigger release changes.
                    LaunchedEffect(onboardingCompleted, lastVersionCode) {
                        if ((onboardingCompleted == true) && (lastVersionCode != -1)) {
                            if (lastVersionCode < currentVersionCode) {
                                delay(1000.milliseconds)
                                repository.updateLastVersionCode(currentVersionCode)
                            }
                        }
                    }

                    if (onboardingCompleted != null) {
                        val monitoringEnabled by settingsViewModel.monitoringEnabled.collectAsState()
                        val appBlockingMasterEnabled by settingsViewModel.appBlockingMasterEnabled.collectAsState()

                        // Monitor the foreground tracking service lifecycle based on user settings.
                        LaunchedEffect(monitoringEnabled) {
                            if ((onboardingCompleted == true) && (monitoringEnabled != null)) {
                                val serviceIntent = Intent(this@MainActivity, NetworkMonitoringService::class.java)
                                if (monitoringEnabled == true) {
                                    if (!NetworkMonitoringService.isRunning) {
                                        startForegroundService(serviceIntent)
                                    }
                                } else {
                                    stopService(serviceIntent)
                                    stopService(Intent(this@MainActivity, AppBlockVpnService::class.java))
                                }
                            }
                        }

                        val widgetUpdateInterval by settingsViewModel.widgetUpdateInterval.collectAsState()

                        LaunchedEffect(widgetUpdateInterval) {
                            if (onboardingCompleted == true) {
                                WidgetUpdateScheduler.schedule(applicationContext, widgetUpdateInterval)
                            }
                        }

                        // Automatically start VPN blocking service if master controls are toggled on.
                        LaunchedEffect(monitoringEnabled, appBlockingMasterEnabled) {
                            if ((onboardingCompleted == true) && (monitoringEnabled == true) && (appBlockingMasterEnabled == true)) {
                                prepareVpn()
                            }
                        }

                        FlowMeterTheme(
                            themeMode = themeMode,
                            useMaterialYou = useMaterialYou,
                            useAmoled = useAmoled,
                            accentColor = accentColor,
                        ) {
                            androidx.compose.material3.Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.background
                            ) {
                                if (onboardingCompleted == true) {
                                    val (currentIntent, setCurrentIntent) = remember { mutableStateOf(intent) }

                                    // Listen for resume lifecycle events to update current intent (e.g. user clicked notification while app is running).
                                    val lifecycleOwner = LocalLifecycleOwner.current
                                    DisposableEffect(lifecycleOwner) {
                                        val observer = LifecycleEventObserver { _, event ->
                                            if (event == Lifecycle.Event.ON_RESUME) {
                                                if (currentIntent != intent) {
                                                    setCurrentIntent(intent)
                                                }
                                            }
                                        }
                                        lifecycleOwner.lifecycle.addObserver(observer)
                                        onDispose {
                                            lifecycleOwner.lifecycle.removeObserver(observer)
                                        }
                                    }

                                    // --- Notification Extra / Deep Link Handling ---
                                    val navigateToAlerts = currentIntent?.getBooleanExtra(NetworkMonitoringService.EXTRA_NAVIGATE_TO_ALERTS, false) ?: false
                                    val navigateToLimits = currentIntent?.getBooleanExtra(NetworkMonitoringService.EXTRA_NAVIGATE_TO_LIMITS, false) ?: false

                                    val initialDestination = when {
                                        navigateToLimits -> Destination.Limits
                                        navigateToAlerts -> Destination.Alerts
                                        else -> Destination.Home
                                    }

                                    val muteAppName = currentIntent?.getStringExtra(NetworkMonitoringService.EXTRA_MUTE_APP_NAME)
                                    val dismissNotificationId = currentIntent?.getIntExtra(NetworkMonitoringService.EXTRA_DISMISS_NOTIFICATION_ID, -1) ?: -1
                                    val isIgnoreAction = currentIntent?.action == NetworkMonitoringService.ACTION_IGNORE_APP

                                    // Process incoming intent actions (such as clicking "Ignore App" directly from an alert notification).
                                    LaunchedEffect(currentIntent) {
                                        if (muteAppName != null) {
                                            if (isIgnoreAction) {
                                                if (dismissNotificationId != -1) {
                                                    try {
                                                        val manager = getSystemService(android.app.NotificationManager::class.java)
                                                        manager?.cancel(dismissNotificationId)
                                                    } catch (e: Exception) {
                                                        Log.e("MainActivity", "Failed to cancel notification", e)
                                                    }
                                                }
                                            }
                                            alertsViewModel.onMuteRequested(muteAppName)

                                            // Clear extras to avoid re-triggering the action if the activity is recreated.
                                            intent.removeExtra(NetworkMonitoringService.EXTRA_MUTE_APP_NAME)
                                            intent.removeExtra(NetworkMonitoringService.EXTRA_DISMISS_NOTIFICATION_ID)
                                            if (intent.action == NetworkMonitoringService.ACTION_IGNORE_APP) {
                                                intent.action = null
                                            }

                                            setCurrentIntent(null)
                                        }
                                    }

                                    MainScreen(
                                        homeViewModel = homeViewModel,
                                        appUsageViewModel = appUsageViewModel,
                                        alertsViewModel = alertsViewModel,
                                        appLimitsViewModel = appLimitsViewModel,
                                        settingsViewModel = settingsViewModel,
                                        initialDestination = initialDestination,
                                        onCheckForUpdates = {
                                            Toast.makeText(context, R.string.toast_checking_updates, Toast.LENGTH_SHORT).show()
                                            appUpdateHelper.checkForUpdates { result ->
                                                when (result) {
                                                    is UpdateResult.PlayStoreUpdateAvailable -> {
                                                        @Suppress("DEPRECATION")
                                                        appUpdateManager?.startUpdateFlowForResult(
                                                            result.appUpdateInfo,
                                                            AppUpdateType.FLEXIBLE,
                                                            this@MainActivity,
                                                            UPDATE_REQUEST_CODE
                                                        )
                                                    }
                                                    is UpdateResult.GitHubUpdateAvailable -> {
                                                        setGitHubUpdate(result)
                                                    }
                                                    is UpdateResult.NoUpdateAvailable -> {
                                                        Toast.makeText(context, R.string.toast_app_up_to_date, Toast.LENGTH_SHORT).show()
                                                    }
                                                    is UpdateResult.Error -> {
                                                        Toast.makeText(context, R.string.toast_update_check_failed, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    if (showChangelog) {
                                        ChangelogDialog { setShowChangelog(false) }
                                    }

                                    if (gitHubUpdate != null) {
                                        UpdateDialog(
                                            tagName = gitHubUpdate.tag,
                                            releaseNotes = gitHubUpdate.releaseNotes,
                                            onDismiss = { setGitHubUpdate(null) },
                                            onIgnore = {
                                                lifecycleScope.launch {
                                                    repository.setIgnoredUpdateVersion(gitHubUpdate.tag)
                                                }
                                                setGitHubUpdate(null)
                                            },
                                            onUpdate = {
                                                val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubUpdate.downloadUrl))
                                                try {
                                                    startActivity(updateIntent)
                                                } catch (_: Exception) {}
                                                setGitHubUpdate(null)
                                            }
                                        )
                                    }

                                } else {
                                    OnboardingScreen(
                                        onComplete = {
                                            onboardingViewModel.completeOnboarding()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateCompletedToast()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager?.unregisterListener(installStateUpdatedListener)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.e("MainActivity", "Update flow failed! Result code: $resultCode")
            }
        }
    }
}

private const val UPDATE_REQUEST_CODE = 9999

private data class ThemeSettings(
    val themeMode: String,
    val useMaterialYou: Boolean,
    val useAmoled: Boolean,
    val accentColor: Long?
)

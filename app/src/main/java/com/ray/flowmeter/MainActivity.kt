package com.ray.flowmeter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import android.net.VpnService
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.rememberCoroutineScope
import com.ray.flowmeter.data.AlertRepository
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.service.AppBlockVpnService
import com.ray.flowmeter.service.NetworkMonitoringService
import com.ray.flowmeter.ui.dialogs.ChangelogDialog
import com.ray.flowmeter.ui.dialogs.ReviewDialog
import com.ray.flowmeter.ui.screens.MainScreen
import com.ray.flowmeter.ui.screens.OnboardingScreen
import com.ray.flowmeter.ui.screens.Destination
import com.ray.flowmeter.ui.theme.FlowMeterTheme
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.OnboardingViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            // Permission denied, reset the toggle in preferences
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
        enableEdgeToEdge()

        val repository = UserPreferencesRepository(applicationContext)
        val database = FlowMeterDatabase.getDatabase(applicationContext)
        val alertRepository = AlertRepository(database.appAlertDao())
        val appLimitRepository = AppLimitRepository(database.appLimitDao())

        // Increment launch count for review dialog
        lifecycleScope.launch {
            repository.incrementLaunchCount()
        }

        // Pre-load theme settings to avoid dark theme flash on startup
        val initialTheme = runBlocking { repository.themeMode.first() }
        val initialMaterialYou = runBlocking { repository.useMaterialYou.first() }
        val initialAmoled = runBlocking { repository.useAmoled.first() }
        val initialAccent = runBlocking { repository.accentColor.first() }

        val onboardingViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return OnboardingViewModel(repository) as T
                }
            },
        )[OnboardingViewModel::class.java]

        val settingsViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(
                        repository = repository,
                        initialTheme = initialTheme,
                        initialMaterialYou = initialMaterialYou,
                        initialAmoled = initialAmoled,
                        initialAccent = initialAccent
                    ) as T
                }
            },
        )[SettingsViewModel::class.java]

        val homeViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(applicationContext, repository) as T
                }
            },
        )[HomeViewModel::class.java]

        val appUsageViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AppUsageViewModel(repository, applicationContext) as T
                }
            },
        )[AppUsageViewModel::class.java]

        val alertsViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AlertsViewModel(alertRepository, repository) as T
                }
            },
        )[AlertsViewModel::class.java]

        val appLimitsViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AppLimitsViewModel(appLimitRepository, repository, applicationContext) as T
                }
            },
        )[AppLimitsViewModel::class.java]

        setContent {
            val onboardingCompleted by repository.onboardingCompleted.collectAsState(null)
            val lastVersionCode by repository.lastVersionCode.collectAsState(-1)
            
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val useAmoled by settingsViewModel.useAmoled.collectAsState()
            val accentColor by settingsViewModel.accentColor.collectAsState()

            val currentVersionCode = BuildConfig.VERSION_CODE
            val (showChangelog, setShowChangelog) = remember { mutableStateOf(false) }

            val (showReviewDialog, setShowReviewDialog) = remember { mutableStateOf(false) }
            val appLaunchCount by repository.appLaunchCount.collectAsState(0)
            val firstInstallTime by repository.firstInstallTime.collectAsState(0L)
            val lastReviewPromptTime by repository.lastReviewPromptTime.collectAsState(0L)
            val userReviewedRated by repository.userReviewedRated.collectAsState(false)

            LaunchedEffect(onboardingCompleted, showChangelog, appLaunchCount, firstInstallTime, lastReviewPromptTime, userReviewedRated) {
                if (onboardingCompleted == true && !showChangelog && !userReviewedRated) {
                    val now = System.currentTimeMillis()
                    val threeDays = 3 * 24 * 60 * 60 * 1000L
                    val sevenDays = 7 * 24 * 60 * 60 * 1000L

                    val isTimeSinceInstallOk = (now - firstInstallTime) >= threeDays
                    val isTimeSinceLastPromptOk = (now - lastReviewPromptTime) >= sevenDays
                    val isLaunchCountOk = appLaunchCount >= 3

                    if (isTimeSinceInstallOk && isTimeSinceLastPromptOk && isLaunchCountOk) {
                        delay(2000)
                        setShowReviewDialog(true)
                    }
                }
            }

            LaunchedEffect(onboardingCompleted, lastVersionCode) {
                if ((onboardingCompleted == true) && (lastVersionCode != -1)) {
                    if (lastVersionCode < currentVersionCode) {
                        delay(1000)
                        setShowChangelog(true)
                        repository.updateLastVersionCode(currentVersionCode)
                    }
                }
            }

            if (onboardingCompleted != null) {
                val monitoringEnabled by settingsViewModel.monitoringEnabled.collectAsState()
                val appBlockingMasterEnabled by settingsViewModel.appBlockingMasterEnabled.collectAsState()
                
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
                    if (onboardingCompleted == true) {
                        val (currentIntent, setCurrentIntent) = remember { mutableStateOf(intent) }
                        
                        LaunchedEffect(Unit) {
                        }
                        
                        // Handler for incoming intents
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

                        LaunchedEffect(currentIntent) {
                            if (muteAppName != null) {
                                if (isIgnoreAction) {
                                    if (dismissNotificationId != -1) {
                                        val manager = getSystemService(android.app.NotificationManager::class.java)
                                        manager.cancel(dismissNotificationId)
                                    }
                                }
                                alertsViewModel.onMuteRequested(muteAppName)
                                
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
                        )

                        if (showChangelog) {
                            ChangelogDialog { setShowChangelog(false) }
                        }

                        val coroutineScope = rememberCoroutineScope()
                        if (showReviewDialog) {
                            ReviewDialog(
                                onDismiss = { setShowReviewDialog(false) },
                                onNeverShowAgain = {
                                    coroutineScope.launch {
                                        repository.setUserReviewedRated(true)
                                    }
                                    setShowReviewDialog(false)
                                },
                                onLater = {
                                    coroutineScope.launch {
                                        repository.setLastReviewPromptTime(System.currentTimeMillis())
                                    }
                                    setShowReviewDialog(false)
                                },
                                onReviewCompleted = {
                                    coroutineScope.launch {
                                        repository.setUserReviewedRated(true)
                                    }
                                    setShowReviewDialog(false)
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

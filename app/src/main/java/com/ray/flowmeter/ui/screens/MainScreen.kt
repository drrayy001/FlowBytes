package com.ray.flowmeter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.serialization.Serializable
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.premiumSpring
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import com.ray.flowmeter.ui.dialogs.MuteAppDialog
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination
    @Serializable
    data object Usage : Destination
    @Serializable
    data object Alerts : Destination
    @Serializable
    data object Limits : Destination
    @Serializable
    data object Settings : Destination
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    appUsageViewModel: AppUsageViewModel,
    alertsViewModel: AlertsViewModel,
    appLimitsViewModel: AppLimitsViewModel,
    settingsViewModel: SettingsViewModel,
    initialDestination: Destination = Destination.Home,
) {
    val backStack = remember { mutableStateListOf<Destination>(initialDestination) }
    val currentDestination = backStack.last()

    val homeTopBarState = rememberTopAppBarState()
    val usageTopBarState = rememberTopAppBarState()
    val alertsTopBarState = rememberTopAppBarState()
    val limitsTopBarState = rememberTopAppBarState()
    val settingsTopBarState = rememberTopAppBarState()
    val homeScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(homeTopBarState)
    val usageScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(usageTopBarState)
    val alertsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(alertsTopBarState)
    val limitsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(limitsTopBarState)
    val settingsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(settingsTopBarState)

    val context = LocalContext.current
    val muteRequestAppName = alertsViewModel.muteRequestAppName

    muteRequestAppName?.let {
        MuteAppDialog(
            appName = it,
            onDismiss = { alertsViewModel.clearMuteRequest() },
        ) { durationMs ->
            alertsViewModel.muteApp(context, it, durationMs)
        }
    }

    LaunchedEffect(currentDestination) {
        homeTopBarState.heightOffset = 0f
        homeTopBarState.contentOffset = 0f
        usageTopBarState.heightOffset = 0f
        usageTopBarState.contentOffset = 0f
        alertsTopBarState.heightOffset = 0f
        alertsTopBarState.contentOffset = 0f
        limitsTopBarState.heightOffset = 0f
        limitsTopBarState.contentOffset = 0f
        settingsTopBarState.heightOffset = 0f
        settingsTopBarState.contentOffset = 0f
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.updateTotalUsage()
                appUsageViewModel.refreshData(isManual = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(15000)
                homeViewModel.updateTotalUsage()
            }
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.updateTotalUsage()
    }

    BackHandler(enabled = (currentDestination != Destination.Home)) {
        backStack.clear()
        backStack.add(Destination.Home)
    }

    val scrollBehavior = when (currentDestination) {
        Destination.Home -> homeScrollBehavior
        Destination.Usage -> usageScrollBehavior
        Destination.Alerts -> alertsScrollBehavior
        Destination.Limits -> limitsScrollBehavior
        else -> settingsScrollBehavior
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val (limitsTab, setLimitsTab) = remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                val containerColor by animateColorAsState(
                    targetValue = if (scrollBehavior.state.contentOffset < -1f) {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    animationSpec = tween(durationMillis = 250),
                    label = "TopBarColorAnimation",
                )

                Surface(
                    color = containerColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        key(currentDestination) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = when (currentDestination) {
                                        Destination.Home -> stringResource(R.string.app_name)
                                        Destination.Usage -> stringResource(R.string.title_app_usage)
                                        Destination.Alerts -> stringResource(R.string.title_alerts)
                                        Destination.Limits -> stringResource(R.string.title_limits)
                                        else -> stringResource(R.string.title_settings)
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentDestination == Destination.Alerts) {
                    val alerts by alertsViewModel.alerts.collectAsState()
                    if (alerts.isNotEmpty()) {
                        val (showClearDialog, setShowClearDialog) = remember { mutableStateOf(value = false) }

                        FloatingActionButton(
                            onClick = { setShowClearDialog(true) },
                            modifier = Modifier.offset(x = (-16).dp, y = (-16).dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.btn_clear_history),
                            )
                        }

                        if (showClearDialog) {
                            AlertDialog(
                                onDismissRequest = { setShowClearDialog(false) },
                                title = { Text(stringResource(R.string.btn_clear_history)) },
                                text = { Text(stringResource(R.string.msg_confirm_clear_history)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            alertsViewModel.clearHistory()
                                            setShowClearDialog(false)
                                        }
                                    ) {
                                        Text(stringResource(R.string.btn_ok))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { setShowClearDialog(false) }) {
                                        Text(stringResource(R.string.btn_cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.title_home)
                            )
                        },
                        label = { Text(stringResource(R.string.title_home)) },
                        selected = currentDestination == Destination.Home,
                        onClick = {
                            if (currentDestination != Destination.Home) {
                                backStack.clear()
                                backStack.add(Destination.Home)
                                homeViewModel.updateTotalUsage()
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Usage) Icons.Filled.Leaderboard else Icons.Outlined.Leaderboard,
                                contentDescription = stringResource(R.string.label_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage)) },
                        selected = currentDestination == Destination.Usage,
                        onClick = {
                            if (currentDestination != Destination.Usage) {
                                backStack.clear()
                                backStack.add(Destination.Usage)
                                appUsageViewModel.refreshData(isManual = false)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Alerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.label_alerts)
                            )
                        },
                        label = { Text(stringResource(R.string.label_alerts)) },
                        selected = currentDestination == Destination.Alerts,
                        onClick = {
                            if (currentDestination != Destination.Alerts) {
                                backStack.clear()
                                backStack.add(Destination.Alerts)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Limits) Icons.Filled.WatchLater else Icons.Outlined.Alarm,
                                contentDescription = stringResource(R.string.label_plans)
                            )
                        },
                        label = { Text(stringResource(R.string.label_plans)) },
                        selected = currentDestination == Destination.Limits,
                        onClick = {
                            if (currentDestination != Destination.Limits) {
                                backStack.clear()
                                backStack.add(Destination.Limits)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.title_settings)) },
                        selected = currentDestination == Destination.Settings,
                        onClick = {
                            if (currentDestination != Destination.Settings) {
                                backStack.clear()
                                backStack.add(Destination.Settings)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->

            val windowAdaptiveInfo = currentWindowAdaptiveInfo()
            val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
            val listDetailStrategy = rememberListDetailSceneStrategy<Destination>(directive = directive)

            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(listDetailStrategy),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                entryProvider = { key ->
                    when (key) {
                        Destination.Home -> NavEntry(key) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToUsage = { date ->
                                    backStack.clear()
                                    backStack.add(Destination.Usage)
                                    appUsageViewModel.updateDateFilter(date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
                                },
                                onNavigateToTodayUsage = {
                                    backStack.clear()
                                    backStack.add(Destination.Usage)
                                    appUsageViewModel.setTimeFilter("day")
                                    appUsageViewModel.refreshData(isManual = false)
                                },
                                onNavigateToMonthUsage = {
                                    backStack.clear()
                                    backStack.add(Destination.Usage)
                                    appUsageViewModel.setTimeFilter("month")
                                    appUsageViewModel.updateToThisMonth()
                                },
                                modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(homeScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Usage -> NavEntry(key) {
                            AppUsageScreen(
                                viewModel = appUsageViewModel,
                                modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(usageScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Alerts -> NavEntry(key) {
                            AlertsScreen(
                                viewModel = alertsViewModel,
                                modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(alertsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Limits -> NavEntry(key) {
                            AppLimitsScreen(
                                viewModel = appLimitsViewModel,
                                currentTab = limitsTab,
                                onTabChange = setLimitsTab,
                                modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(limitsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Settings -> NavEntry(key) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(settingsScrollBehavior.nestedScrollConnection)
                            )
                        }
                    }
                }
            )
        }

        AppLimitsOverlay(appLimitsViewModel)
    }
}
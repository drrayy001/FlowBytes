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
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.AppTransitions
import androidx.compose.ui.platform.LocalContext
import com.ray.flowmeter.ui.dialogs.MuteAppDialog
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    appUsageViewModel: AppUsageViewModel,
    alertsViewModel: AlertsViewModel,
    appLimitsViewModel: AppLimitsViewModel,
    settingsViewModel: SettingsViewModel,
    initialTab: Int = 0,
) {
    val (currentTab, setCurrentTab) = remember { mutableIntStateOf(initialTab) }

    // Check if Limits tab has a sub-view open (Picker or Edit)
    val isLimitsSubViewOpen = (currentTab == 3) && appLimitsViewModel.isSubViewOpen

    // Each tab gets its own scroll state so the top bar doesn't carry over between tabs
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

    // Reset the top bar offset when switching tabs so the color resets immediately
    LaunchedEffect(currentTab) {
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

    // Lifecycle observer for data refresh
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

    // Periodic refresh while the app is in foreground
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(15000) // Refresh every 15 seconds
                homeViewModel.updateTotalUsage()
            }
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.updateTotalUsage()
    }

    // Back press returns to Home tab if on another tab
    BackHandler(enabled = (currentTab != 0)) {
        setCurrentTab(0)
    }

    val scrollBehavior = when (currentTab) {
        0 -> homeScrollBehavior
        1 -> usageScrollBehavior
        2 -> alertsScrollBehavior
        3 -> limitsScrollBehavior
        else -> settingsScrollBehavior
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val (limitsTab, setLimitsTab) = remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isLimitsSubViewOpen) {
                // Calculate the scrolled container color based on scroll behavior
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
                        // Keyed so the title text updates immediately on tab switch
                        key(currentTab) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = when (currentTab) {
                                        0 -> stringResource(R.string.app_name)
                                        1 -> stringResource(R.string.title_app_usage)
                                        2 -> stringResource(R.string.title_alerts)
                                        3 -> stringResource(R.string.title_limits)
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
            }
        },
        floatingActionButton = {
            if (currentTab == 2) {
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
            if (!isLimitsSubViewOpen) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.title_home)
                            )
                        },
                        label = { Text(stringResource(R.string.title_home)) },
                        selected = currentTab == 0,
                        onClick = {
                            if (currentTab != 0) {
                                setCurrentTab(0)
                                homeViewModel.updateTotalUsage()
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Filled.Leaderboard else Icons.Outlined.Leaderboard,
                                contentDescription = stringResource(R.string.label_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage)) },
                        selected = currentTab == 1,
                        onClick = {
                            // Only reload if we're not already on this tab
                            if (currentTab != 1) {
                                setCurrentTab(1)
                                appUsageViewModel.refreshData(isManual = false)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.label_alerts)
                            )
                        },
                        label = { Text(stringResource(R.string.label_alerts)) },
                        selected = currentTab == 2,
                        onClick = { setCurrentTab(2) }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Filled.WatchLater else Icons.Outlined.Alarm,
                                contentDescription = stringResource(R.string.label_plans)
                            )
                        },
                        label = { Text(stringResource(R.string.label_plans)) },
                        selected = currentTab == 3,
                        onClick = { setCurrentTab(3) }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.title_settings)) },
                        selected = currentTab == 4,
                        onClick = { setCurrentTab(4) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentPadding = if (isLimitsSubViewOpen) {
            PaddingValues(bottom = innerPadding.calculateBottomPadding())
        } else {
            innerPadding
        }

        AnimatedContent(
            targetState = currentTab,
            modifier = Modifier
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background),
            transitionSpec = {
                if (targetState > initialState) {
                    AppTransitions.SlideForwardEnter togetherWith AppTransitions.SlideForwardExit
                } else {
                    AppTransitions.SlideBackwardEnter togetherWith AppTransitions.SlideBackwardExit
                }
            },
            label = "TabTransition"
        ) { targetTab ->
            when (targetTab) {
                0 -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToUsage = { date ->
                        setCurrentTab(1)
                        appUsageViewModel.updateDateFilter(
                            year = date[Calendar.YEAR],
                            month = date[Calendar.MONTH],
                            dayOfMonth = date[Calendar.DAY_OF_MONTH],
                        )
                    },
                    onNavigateToTodayUsage = {
                        setCurrentTab(1)
                        appUsageViewModel.setTimeFilter("day")
                        appUsageViewModel.refreshData(isManual = false)
                    },
                    onNavigateToMonthUsage = {
                        setCurrentTab(1)
                        appUsageViewModel.setTimeFilter("month")
                        appUsageViewModel.updateToThisMonth()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(homeScrollBehavior.nestedScrollConnection)
                )

                1 -> AppUsageScreen(
                    viewModel = appUsageViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(usageScrollBehavior.nestedScrollConnection)
                )

                2 -> AlertsScreen(
                    viewModel = alertsViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(alertsScrollBehavior.nestedScrollConnection)
                )

                3 -> AppLimitsScreen(
                    viewModel = appLimitsViewModel,
                    currentTab = limitsTab,
                    onTabChange = setLimitsTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(limitsScrollBehavior.nestedScrollConnection),
                )

                4 -> SettingsScreen(
                    viewModel = settingsViewModel,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(settingsScrollBehavior.nestedScrollConnection)
                )
            }
        }
    }
}

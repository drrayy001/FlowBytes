package com.ray.flowmeter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.serialization.Serializable
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.StaggeredEntrance
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val homeScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val usageScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val alertsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val limitsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val settingsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val backStack = remember { mutableStateListOf(initialDestination) }
    val currentDestination = backStack.last()

    BackHandler(enabled = (currentDestination != Destination.Home)) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
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
                    modifier = Modifier.fillMaxWidth(),
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
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Usage) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                contentDescription = stringResource(R.string.title_app_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage)) },
                        selected = currentDestination == Destination.Usage,
                        onClick = {
                            if (currentDestination != Destination.Usage) {
                                backStack.clear()
                                backStack.add(Destination.Usage)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == Destination.Alerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.title_alerts)
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
                                imageVector = if (currentDestination == Destination.Limits) Icons.Filled.Timer else Icons.Outlined.Timer,
                                contentDescription = stringResource(R.string.title_limits)
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

        val isViewingSystemApps = appUsageViewModel.isViewingSystemApps
        val filteredSystemAppList by appUsageViewModel.filteredSystemAppUsageList.collectAsState()
        val systemListState = rememberLazyListState()
        val locale = LocalConfiguration.current.locales[0]

        AnimatedVisibility(
            visible = isViewingSystemApps,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            BackHandler {
                appUsageViewModel.isViewingSystemApps = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.title_system_data_usage),
                                    fontWeight = FontWeight.Black
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { appUsageViewModel.isViewingSystemApps = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { padding ->
                    val networkFilter by appUsageViewModel.networkFilter.collectAsState()

                    val maxUsageBytes = if (filteredSystemAppList.isNotEmpty()) {
                        filteredSystemAppList.maxOf {
                            when (networkFilter) {
                                "mobile" -> it.cellUsage
                                "wifi" -> it.wifiUsage
                                else -> it.totalUsage
                            }
                        }.coerceAtLeast(1L)
                    } else {
                        1L
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background),
                        state = systemListState,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (filteredSystemAppList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.msg_no_usage_data),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = filteredSystemAppList,
                                key = { _, it -> it.packageName }
                            ) { _, appUsage ->
                                val displayUsage = when (networkFilter) {
                                    "mobile" -> appUsage.cellUsage
                                    "wifi" -> appUsage.wifiUsage
                                    else -> appUsage.totalUsage
                                }

                                StaggeredEntrance {
                                    AppUsageItem(
                                        appUsage = appUsage,
                                        displayUsage = displayUsage,
                                        maxUsageBytes = maxUsageBytes,
                                        locale = locale,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

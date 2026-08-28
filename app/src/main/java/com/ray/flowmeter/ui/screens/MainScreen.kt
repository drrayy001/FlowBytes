// Scaffold container coordinating bottom navigation bar destination switching
// and binding activity-level lifecycle events to screen-level parameters.
package com.ray.flowmeter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.ui.screens.WidgetsScreen
import com.ray.flowmeter.ui.theme.LocalThemeTransition
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.ThemeTransitionKind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import com.ray.flowmeter.ui.dialogs.MuteAppDialog
import androidx.compose.ui.platform.LocalContext
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.ray.flowmeter.ui.dialogs.DonateDialog
import com.ray.flowmeter.utils.BillingEvent
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

    @Serializable
    data object AppPicker : Destination

    @Serializable
    data object Widgets : Destination
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
    onCheckForUpdates: () -> Unit = {},
) {
    val homeScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val usageScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val alertsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val limitsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val settingsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val systemAppsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val backStack = remember { mutableStateListOf(initialDestination) }
    val currentDestination = backStack.last()
    val themeTransition = LocalThemeTransition.current

    fun navigateTo(destination: Destination, origin: Offset? = null, onBeforeNav: (() -> Unit)? = null) {
        if (currentDestination == destination) return
        themeTransition.startTransition(
            origin = origin,
            kind = ThemeTransitionKind.FADE,
            durationMs = 350
        ) {
            onBeforeNav?.invoke()
            backStack.clear()
            backStack.add(destination)
        }
    }

    var activeLayoutDestination by remember { mutableStateOf(initialDestination) }
    LaunchedEffect(currentDestination) {
        if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) {
            delay(500)
            activeLayoutDestination = currentDestination
        } else {
            activeLayoutDestination = currentDestination
        }
    }

    val currentScrollBehavior = when (currentDestination) {
        Destination.Home -> homeScrollBehavior
        Destination.Usage -> usageScrollBehavior
        Destination.Alerts -> alertsScrollBehavior
        Destination.Limits -> limitsScrollBehavior
        Destination.Settings -> settingsScrollBehavior
        Destination.AppPicker -> limitsScrollBehavior
        Destination.Widgets -> settingsScrollBehavior
    }

    BackHandler(enabled = (currentDestination != Destination.Home)) {
        if (currentDestination == Destination.AppPicker) {
            appLimitsViewModel.isPickerOpen = false
        } else if (currentDestination == Destination.Widgets) {
            homeViewModel.isWidgetsOpen = false
        } else {
            backStack.clear()
            backStack.add(Destination.Home)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val (limitsTab, setLimitsTab) = remember { mutableIntStateOf(0) }
    var showUsageFilters by remember { mutableStateOf(false) }
    var showAlertsFilters by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val repository = remember { UserPreferencesRepository(context.applicationContext) }
    val locale = LocalConfiguration.current.locales[0]
    var showDonateDialog by remember { mutableStateOf(false) }
    var isDonationSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.initBilling(context)
    }

    val donationCancelledMessage = stringResource(R.string.msg_donation_cancelled)
    val donationFailedMessage = stringResource(R.string.msg_donation_failed)

    LaunchedEffect(Unit) {
        settingsViewModel.billingEvents.collect { event ->
            when (event) {
                is BillingEvent.Success -> {
                    isDonationSuccess = true
                    showDonateDialog = true
                }
                is BillingEvent.Cancelled -> {
                    snackbarHostState.showSnackbar(donationCancelledMessage)
                }
                is BillingEvent.Error -> {
                    snackbarHostState.showSnackbar(String.format(locale, donationFailedMessage, event.message))
                }
            }
        }
    }

    // Milestone-based support prompt: show a snackbar after sufficient usage.
    val supportBannerDismissed by settingsViewModel.supportBannerDismissed.collectAsState()
    val appLaunchCount by settingsViewModel.appLaunchCount.collectAsState()
    val firstInstallTime by settingsViewModel.firstInstallTime.collectAsState()
    val supportPromptMessage = stringResource(R.string.snackbar_support_prompt)
    val supportPromptAction = stringResource(R.string.snackbar_support_action)

    LaunchedEffect(supportBannerDismissed, appLaunchCount, firstInstallTime) {
        if (!supportBannerDismissed && appLaunchCount >= 5 && firstInstallTime > 0L) {
            val threeDays = 3.days
            if ((System.currentTimeMillis() - firstInstallTime).milliseconds >= threeDays) {
                delay(3.seconds)
                settingsViewModel.dismissSupportBanner()
                val result = snackbarHostState.showSnackbar(
                    message = supportPromptMessage,
                    actionLabel = supportPromptAction,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    showDonateDialog = true
                }
            }
        }
    }

    // Update data when switching tabs
    LaunchedEffect(currentDestination) {
        when (currentDestination) {
            Destination.Home -> homeViewModel.updateTotalUsage()
            Destination.Usage -> appUsageViewModel.refreshData(isManual = false)
            Destination.Alerts -> alertsViewModel.refreshData(isManual = false)
            else -> {}
        }
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val isWideScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(600)

    LaunchedEffect(appLimitsViewModel.isPickerOpen) {
        if (appLimitsViewModel.isPickerOpen) {
            if (currentDestination != Destination.AppPicker) {
                themeTransition.startTransition(kind = ThemeTransitionKind.FADE, durationMs = 350) {
                    backStack.add(Destination.AppPicker)
                }
            }
        } else {
            if (currentDestination == Destination.AppPicker) {
                themeTransition.startTransition(kind = ThemeTransitionKind.FADE, durationMs = 350) {
                    backStack.removeLastOrNull()
                }
            }
        }
    }

    LaunchedEffect(homeViewModel.isWidgetsOpen) {
        if (homeViewModel.isWidgetsOpen) {
            if (currentDestination != Destination.Widgets) {
                themeTransition.startTransition(kind = ThemeTransitionKind.FADE, durationMs = 350) {
                    backStack.add(Destination.Widgets)
                }
            }
        } else {
            if (currentDestination == Destination.Widgets) {
                themeTransition.startTransition(kind = ThemeTransitionKind.FADE, durationMs = 350) {
                    backStack.removeLastOrNull()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            val showGlobalNavRail = isWideScreen && activeLayoutDestination != Destination.AppPicker && activeLayoutDestination != Destination.Widgets
            if (showGlobalNavRail) {
                val navRailAlpha by animateFloatAsState(
                    targetValue = if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) 0f else 1f,
                    animationSpec = tween(300),
                    label = "NavRailAlpha"
                )

                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.graphicsLayer { alpha = navRailAlpha }
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    var railHomeCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationRailItem(
                        modifier = Modifier.onGloballyPositioned { railHomeCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.title_home)
                            )
                        },
                        label = { Text(stringResource(R.string.title_home), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Home,
                        onClick = { navigateTo(Destination.Home, railHomeCenter) }
                    )
                    var railUsageCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationRailItem(
                        modifier = Modifier.onGloballyPositioned { railUsageCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Usage) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = stringResource(R.string.title_app_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Usage,
                        onClick = { navigateTo(Destination.Usage, railUsageCenter) }
                    )
                    var railAlertsCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationRailItem(
                        modifier = Modifier.onGloballyPositioned { railAlertsCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Alerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.title_alerts)
                            )
                        },
                        label = { Text(stringResource(R.string.label_alerts), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Alerts,
                        onClick = { navigateTo(Destination.Alerts, railAlertsCenter) }
                    )
                    var railLimitsCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationRailItem(
                        modifier = Modifier.onGloballyPositioned { railLimitsCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Limits) Icons.Filled.Security else Icons.Outlined.Security,
                                contentDescription = stringResource(R.string.title_limits)
                            )
                        },
                        label = { Text(stringResource(R.string.title_limits), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Limits,
                        onClick = { navigateTo(Destination.Limits, railLimitsCenter) }
                    )
                    var railSettingsCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationRailItem(
                        modifier = Modifier.onGloballyPositioned { railSettingsCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.title_settings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Settings,
                        onClick = { navigateTo(Destination.Settings, railSettingsCenter) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                val showGlobalTopBar = activeLayoutDestination != Destination.AppPicker && activeLayoutDestination != Destination.Widgets
                if (showGlobalTopBar) {
                    val containerColor by animateColorAsState(
                        targetValue = if (currentScrollBehavior.state.contentOffset < -1f) {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        animationSpec = tween(durationMillis = 250),
                        label = "TopBarColorAnimation",
                    )

                    val topBarAlpha by animateFloatAsState(
                        targetValue = if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) 0f else 1f,
                        animationSpec = tween(300),
                        label = "TopBarAlpha"
                    )

                    Surface(
                        color = containerColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = topBarAlpha },
                    ) {
                        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                            key(activeLayoutDestination) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {


                                    Text(
                                        text = when (activeLayoutDestination) {
                                            Destination.Home -> stringResource(R.string.app_name)
                                            Destination.Usage -> stringResource(R.string.title_app_usage)
                                            Destination.Alerts -> stringResource(R.string.title_alerts)
                                            Destination.Limits -> stringResource(R.string.title_limits)
                                            Destination.Settings -> stringResource(R.string.title_settings)
                                            else -> ""
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = if (LocalConfiguration.current.locales[0].language == "ar") 0.sp else (-0.5).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (activeLayoutDestination == Destination.Home) {
                                        var widgetsBtnCenter by remember { mutableStateOf(Offset.Zero) }
                                        IconButton(
                                            modifier = Modifier.onGloballyPositioned { widgetsBtnCenter = it.boundsInRoot().center },
                                            onClick = {
                                                themeTransition.startTransition(
                                                    origin = widgetsBtnCenter,
                                                    kind = ThemeTransitionKind.FADE,
                                                    durationMs = 350
                                                ) {
                                                    homeViewModel.isWidgetsOpen = true
                                                }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Widgets,
                                                contentDescription = stringResource(R.string.cd_manage_widgets),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }



                                    if (activeLayoutDestination == Destination.Usage) {
                                        IconButton(
                                            onClick = { showUsageFilters = !showUsageFilters },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = com.ray.flowmeter.ui.components.AppIcons.Filter,
                                                contentDescription = stringResource(R.string.cd_toggle_filters),
                                                tint = if (showUsageFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (activeLayoutDestination == Destination.Alerts) {
                                        IconButton(
                                            onClick = { showAlertsFilters = !showAlertsFilters },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = com.ray.flowmeter.ui.components.AppIcons.Filter,
                                                contentDescription = stringResource(R.string.cd_toggle_filters),
                                                tint = if (showAlertsFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (activeLayoutDestination == Destination.Limits) {
                                        val appBlockingMasterEnabled by appLimitsViewModel.appBlockingMasterEnabled.collectAsState()
                                        val scope = rememberCoroutineScope()
                                        val firewallEnabledMsg = stringResource(R.string.msg_firewall_enabled)
                                        val firewallDisabledMsg = stringResource(R.string.msg_firewall_disabled)
                                        
                                        val buttonBgColor by animateColorAsState(
                                            targetValue = if (appBlockingMasterEnabled) MaterialTheme.colorScheme.primaryContainer
                                                          else Color.Transparent,
                                            animationSpec = tween(300),
                                            label = "FirewallBgColor"
                                        )
                                        val buttonContentColor by animateColorAsState(
                                            targetValue = if (appBlockingMasterEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                                            animationSpec = tween(300),
                                            label = "FirewallContentColor"
                                        )
                                        
                                        IconButton(
                                            onClick = {
                                                val targetState = !appBlockingMasterEnabled
                                                appLimitsViewModel.setAppBlockingMasterEnabled(targetState)
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    val msg = if (targetState) firewallEnabledMsg else firewallDisabledMsg
                                                    val job = launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = msg,
                                                            duration = SnackbarDuration.Indefinite
                                                        )
                                                    }
                                                    delay(800.milliseconds)
                                                    job.cancel()
                                                }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = buttonBgColor,
                                                contentColor = buttonContentColor
                                            ),
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                        ) {
                                            Crossfade(
                                                targetState = appBlockingMasterEnabled,
                                                animationSpec = tween(200),
                                                label = "FirewallIconTransition"
                                            ) { enabled ->
                                                Icon(
                                                    imageVector = if (enabled) Icons.Rounded.Security else Icons.Rounded.Shield,
                                                    contentDescription = stringResource(R.string.label_block_apps),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
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
                            modifier = Modifier.padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(20.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
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
                val showGlobalBottomBar = !isWideScreen && activeLayoutDestination != Destination.AppPicker && activeLayoutDestination != Destination.Widgets
                if (showGlobalBottomBar) {
                    val bottomBarAlpha by animateFloatAsState(
                        targetValue = if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) 0f else 1f,
                        animationSpec = tween(300),
                        label = "BottomBarAlpha"
                    )

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.graphicsLayer { alpha = bottomBarAlpha }
                    ) {
                    var navHomeCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationBarItem(
                        modifier = Modifier.onGloballyPositioned { navHomeCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.title_home)
                            )
                        },
                        label = { Text(stringResource(R.string.title_home), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Home,
                        onClick = { navigateTo(Destination.Home, navHomeCenter) }
                    )
                    var navUsageCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationBarItem(
                        modifier = Modifier.onGloballyPositioned { navUsageCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Usage) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = stringResource(R.string.title_app_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Usage,
                        onClick = { navigateTo(Destination.Usage, navUsageCenter) }
                    )
                    var navLimitsCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationBarItem(
                        modifier = Modifier.onGloballyPositioned { navLimitsCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Limits) Icons.Filled.Timer else Icons.Outlined.Timer,
                                contentDescription = stringResource(R.string.title_limits)
                            )
                        },
                        label = { Text(stringResource(R.string.label_plans), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Limits,
                        onClick = { navigateTo(Destination.Limits, navLimitsCenter) }
                    )
                    var navAlertsCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationBarItem(
                        modifier = Modifier.onGloballyPositioned { navAlertsCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Alerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.title_alerts)
                            )
                        },
                        label = { Text(stringResource(R.string.label_alerts), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Alerts,
                        onClick = { navigateTo(Destination.Alerts, navAlertsCenter) }
                    )
                    var navSettingsCenter by remember { mutableStateOf(Offset.Zero) }
                    NavigationBarItem(
                        modifier = Modifier.onGloballyPositioned { navSettingsCenter = it.boundsInRoot().center },
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.title_settings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Settings,
                        onClick = { navigateTo(Destination.Settings, navSettingsCenter) }
                    )
                }
                }
            }
        ) { innerPadding ->

            var lastStablePadding by remember { mutableStateOf(PaddingValues()) }
            val inFullscreenTransition = (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets)
                    && (activeLayoutDestination != currentDestination)

            LaunchedEffect(innerPadding, inFullscreenTransition) {
                if (!inFullscreenTransition) {
                    lastStablePadding = innerPadding
                }
            }

            val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
            val listDetailStrategy = rememberListDetailSceneStrategy<Destination>(directive = directive)

            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(listDetailStrategy),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                transitionSpec = {
                    val initialDest = initialState.key as? Destination
                    val targetDest = targetState.key as? Destination

                    when {
                        targetDest == Destination.Widgets || targetDest == Destination.AppPicker -> {
                            (scaleIn(initialScale = 0.85f, animationSpec = tween(450, easing = EaseOutCubic)) +
                             fadeIn(animationSpec = tween(350)) +
                             slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(450, easing = EaseOutCubic))
                            ).togetherWith(
                                scaleOut(targetScale = 0.95f, animationSpec = tween(350)) +
                                fadeOut(animationSpec = tween(300))
                            )
                        }
                        initialDest == Destination.Widgets || initialDest == Destination.AppPicker -> {
                            (fadeIn(animationSpec = tween(350)) +
                             scaleIn(initialScale = 0.95f, animationSpec = tween(350))
                            ).togetherWith(
                                scaleOut(targetScale = 0.85f, animationSpec = tween(400, easing = EaseOutCubic)) +
                                fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(targetOffsetY = { it / 6 }, animationSpec = tween(400, easing = EaseOutCubic))
                            )
                        }
                        else -> {
                            val initIdx = when (initialDest) {
                                Destination.Home -> 0
                                Destination.Usage -> 1
                                Destination.Limits -> 2
                                Destination.Alerts -> 3
                                Destination.Settings -> 4
                                else -> 0
                            }
                            val targetIdx = when (targetDest) {
                                Destination.Home -> 0
                                Destination.Usage -> 1
                                Destination.Limits -> 2
                                Destination.Alerts -> 3
                                Destination.Settings -> 4
                                else -> 0
                            }
                            val isForward = targetIdx >= initIdx
                            val sign = if (isForward) 1 else -1

                            (slideInHorizontally(
                                initialOffsetX = { sign * it / 5 },
                                animationSpec = tween(350, easing = EaseOutCubic)
                            ) + fadeIn(animationSpec = tween(280)) +
                             scaleIn(initialScale = 0.96f, animationSpec = tween(350, easing = EaseOutCubic))
                            ).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { -sign * it / 5 },
                                    animationSpec = tween(350, easing = EaseOutCubic)
                                ) + fadeOut(animationSpec = tween(250)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(350, easing = EaseOutCubic))
                            )
                        }
                    }
                },
                entryProvider = { key ->
                    when (key) {
                        Destination.Home -> NavEntry(key) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToUsage = { date, origin ->
                                    navigateTo(Destination.Usage, origin) {
                                        appUsageViewModel.isViewingSystemApps = false
                                        appUsageViewModel.loadAppUsageForDate(date.timeInMillis)
                                    }
                                },
                                onNavigateToTodayUsage = { origin ->
                                    navigateTo(Destination.Usage, origin) {
                                        appUsageViewModel.isViewingSystemApps = false
                                        appUsageViewModel.loadAppUsageForDate(System.currentTimeMillis())
                                    }
                                },
                                onNavigateToMonthUsage = { origin ->
                                    navigateTo(Destination.Usage, origin) {
                                        appUsageViewModel.isViewingSystemApps = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(homeScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Usage -> NavEntry(key) {
                            AppUsageScreen(
                                viewModel = appUsageViewModel,
                                showFilters = showUsageFilters,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(usageScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Alerts -> NavEntry(key) {
                            AlertsScreen(
                                viewModel = alertsViewModel,
                                showFilters = showAlertsFilters,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(alertsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Limits -> NavEntry(key) {
                            AppLimitsScreen(
                                viewModel = appLimitsViewModel,
                                currentTab = limitsTab,
                                onTabChange = setLimitsTab,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(limitsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Settings -> NavEntry(key) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onDonateClick = { showDonateDialog = true },
                                onCheckForUpdates = onCheckForUpdates,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(settingsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.AppPicker -> NavEntry(key) {
                            AppPickerScreen(
                                viewModel = appLimitsViewModel,
                                onBack = {
                                    appLimitsViewModel.isPickerOpen = false
                                }
                            ) { limits ->
                                appLimitsViewModel.addAppLimits(limits)
                                appLimitsViewModel.isPickerOpen = false
                            }
                        }

                        Destination.Widgets -> NavEntry(key) {
                            WidgetsScreen(
                                context = context,
                                repository = repository,
                                onBack = {
                                    homeViewModel.isWidgetsOpen = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            )
        }

        AppLimitsOverlay(appLimitsViewModel)

        val context = LocalContext.current
        val muteAppName = alertsViewModel.muteRequestAppName
        if (muteAppName != null) {
            MuteAppDialog(
                appName = muteAppName,
                onDismiss = { alertsViewModel.clearMuteRequest() },
                onConfirm = { durationMs ->
                    alertsViewModel.muteApp(context, muteAppName, durationMs)
                }
            )
        }

        val isViewingSystemApps = appUsageViewModel.isViewingSystemApps
        val filteredSystemAppList by appUsageViewModel.filteredSystemAppUsageList.collectAsState()
        val systemListState = rememberLazyListState()

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
                    .nestedScroll(systemAppsScrollBehavior.nestedScrollConnection)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.title_system_data_usage),
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                            scrollBehavior = systemAppsScrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        if (showDonateDialog) {
            DonateDialog(
                isSuccess = isDonationSuccess,
                onDismiss = {
                    showDonateDialog = false
                    isDonationSuccess = false
                },
            ) { amount ->
                val activity = context.findActivity()
                activity?.let {
                    settingsViewModel.makeDonation(it, amount)
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

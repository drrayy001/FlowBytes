package com.ray.flowmeter.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel

@Composable
fun AppLimitsScreen(
    viewModel: AppLimitsViewModel,
    modifier: Modifier = Modifier,
    currentTab: Int = 0,
    onTabChange: (Int) -> Unit = {}
) {
    var limitToDelete by remember { mutableStateOf<AppLimit?>(null) }
    val (showTypeSelector, setShowTypeSelector) = remember { mutableStateOf(value = false) }

    val dataDailyLimitConfigured by viewModel.dataDailyLimitConfigured.collectAsState()
    val dataMonthlyLimitConfigured by viewModel.dataMonthlyLimitConfigured.collectAsState()
    val wifiDailyLimitConfigured by viewModel.wifiDailyLimitConfigured.collectAsState()
    val wifiMonthlyLimitConfigured by viewModel.wifiMonthlyLimitConfigured.collectAsState()

    val (internalTab, setInternalTab) = remember { mutableIntStateOf(currentTab) }
    
    // Sync internal state with external state when navigating back
    LaunchedEffect(currentTab) {
        setInternalTab(currentTab)
    }

    val selectedTab = internalTab
    val updateTab = { index: Int ->
        onTabChange(index)
        setInternalTab(index)
    }

    val allGeneralLimitsConfigured = dataDailyLimitConfigured && dataMonthlyLimitConfigured &&
            wifiDailyLimitConfigured && wifiMonthlyLimitConfigured

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MinimalPillsRow(
                selectedTab = selectedTab,
                onTabChange = updateTab,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None using SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "SubTabTransition"
            ) { page ->
                when (page) {
                    0 -> GeneralLimitsList(
                        viewModel = viewModel,
                        onEdit = { viewModel.isGeneralLimitOpen = true },
                        onDelete = { limitToDelete = it }
                    )
                    1 -> AppLimitsList(
                        viewModel = viewModel,
                        onEdit = { viewModel.editingLimit = it },
                        onDelete = { limitToDelete = it }
                    )
                }
            }
        }

        val showFab = if (selectedTab == 0) !allGeneralLimitsConfigured else true

        if (showFab) {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 1) {
                        viewModel.loadInstalledApps()
                        viewModel.isPickerOpen = true
                    } else {
                        viewModel.isGeneralLimitOpen = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_add_app_limit))
            }
        }
    }

    if (showTypeSelector) {
        LimitTypeSelectionDialog(
            onDismiss = { setShowTypeSelector(false) },
            onAppLimitSelected = {
                viewModel.loadInstalledApps()
                viewModel.isPickerOpen = true
                setShowTypeSelector(false)
            },
            onGeneralLimitSelected = {
                viewModel.isGeneralLimitOpen = true
                setShowTypeSelector(false)
            }
        )
    }

    if (limitToDelete != null) {
        AlertDialog(
            onDismissRequest = { limitToDelete = null },
            title = { Text(stringResource(R.string.title_delete_limit)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_limit, limitToDelete?.appName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        limitToDelete?.let {
                            when(it.packageName) {
                                "system.wifi.daily" -> {
                                    viewModel.setWifiDailyLimitEnabled(false)
                                    viewModel.setWifiDailyLimitConfigured(false)
                                }
                                "system.mobile.daily" -> {
                                    viewModel.setDataDailyLimitEnabled(false)
                                    viewModel.setDataDailyLimitConfigured(false)
                                }
                                "system.wifi.monthly" -> {
                                    viewModel.setWifiMonthlyLimitEnabled(false)
                                    viewModel.setWifiMonthlyLimitConfigured(false)
                                }
                                "system.mobile.monthly" -> {
                                    viewModel.setDataMonthlyLimitEnabled(false)
                                    viewModel.setDataMonthlyLimitConfigured(false)
                                }
                                else -> viewModel.removeAppLimit(it)
                            }
                        }
                        limitToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { limitToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun AppLimitsOverlay(viewModel: AppLimitsViewModel) {
    val currentView = when {
        viewModel.isPickerOpen -> "picker"
        viewModel.editingLimit != null -> "edit"
        viewModel.isGeneralLimitOpen -> "general"
        else -> "list"
    }

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()

    AnimatedVisibility(
        visible = currentView != "list",
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) using SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "OverlayContent"
            ) { viewState ->
                when (viewState) {
                    "picker" -> {
                        AppPickerScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.isPickerOpen = false },
                        ) { appInfo, limitBytes, limitType, networkType, wifiLimitBytes, mobileLimitBytes ->
                            viewModel.addAppLimit(
                                packageName = appInfo.packageName,
                                appName = appInfo.name,
                                limitBytes = limitBytes,
                                limitType = limitType,
                                networkType = networkType,
                                wifiLimitBytes = wifiLimitBytes,
                                mobileLimitBytes = mobileLimitBytes,
                            )
                            viewModel.isPickerOpen = false
                        }
                    }
                    "edit" -> {
                        if (viewModel.editingLimit != null) {
                            AppLimitEditScreen(
                                limit = viewModel.editingLimit!!,
                                onBack = { viewModel.editingLimit = null },
                            ) { updatedLimit ->
                                viewModel.updateAppLimit(updatedLimit)
                                viewModel.editingLimit = null
                            }
                        }
                    }
                    "general" -> {
                        GeneralLimitConfigScreen(
                            initialDataDailyLimit = dataDailyLimit,
                            initialWifiDailyLimit = wifiDailyLimit,
                            initialDataMonthlyLimit = dataMonthlyLimit,
                            initialWifiMonthlyLimit = wifiMonthlyLimit,
                            onBack = { viewModel.isGeneralLimitOpen = false },
                            onConfirm = { network, period, dataDaily, wifiDaily, dataMonthly, wifiMonthly ->
                                when(network) {
                                    "mobile" -> {
                                        when (period) {
                                            "daily" -> {
                                                viewModel.setDataDailyLimit(dataDaily)
                                                viewModel.setDataDailyLimitEnabled(enabled = true)
                                                viewModel.setDataDailyLimitConfigured(configured = true)
                                            }
                                            "monthly" -> {
                                                viewModel.setDataMonthlyLimit(dataMonthly)
                                                viewModel.setDataMonthlyLimitEnabled(enabled = true)
                                                viewModel.setDataMonthlyLimitConfigured(configured = true)
                                            }
                                            else -> {
                                                viewModel.setDataDailyLimit(dataDaily)
                                                viewModel.setDataDailyLimitEnabled(enabled = true)
                                                viewModel.setDataDailyLimitConfigured(configured = true)
                                                viewModel.setDataMonthlyLimit(dataMonthly)
                                                viewModel.setDataMonthlyLimitEnabled(enabled = true)
                                                viewModel.setDataMonthlyLimitConfigured(configured = true)
                                            }
                                        }
                                    }
                                    "wifi" -> {
                                        when (period) {
                                            "daily" -> {
                                                viewModel.setWifiDailyLimit(wifiDaily)
                                                viewModel.setWifiDailyLimitEnabled(enabled = true)
                                                viewModel.setWifiDailyLimitConfigured(configured = true)
                                            }
                                            "monthly" -> {
                                                viewModel.setWifiMonthlyLimit(wifiMonthly)
                                                viewModel.setWifiMonthlyLimitEnabled(enabled = true)
                                                viewModel.setWifiMonthlyLimitConfigured(configured = true)
                                            }
                                            else -> {
                                                viewModel.setWifiDailyLimit(wifiDaily)
                                                viewModel.setWifiDailyLimitEnabled(enabled = true)
                                                viewModel.setWifiDailyLimitConfigured(configured = true)
                                                viewModel.setWifiMonthlyLimit(wifiMonthly)
                                                viewModel.setWifiMonthlyLimitEnabled(enabled = true)
                                                viewModel.setWifiMonthlyLimitConfigured(configured = true)
                                            }
                                        }
                                    }
                                    "both" -> {
                                        when (period) {
                                            "daily" -> {
                                                viewModel.setDataDailyLimit(dataDaily)
                                                viewModel.setDataDailyLimitEnabled(enabled = true)
                                                viewModel.setDataDailyLimitConfigured(configured = true)
                                                viewModel.setWifiDailyLimit(wifiDaily)
                                                viewModel.setWifiDailyLimitEnabled(enabled = true)
                                                viewModel.setWifiDailyLimitConfigured(configured = true)
                                            }
                                            "monthly" -> {
                                                viewModel.setDataMonthlyLimit(dataMonthly)
                                                viewModel.setDataMonthlyLimitEnabled(enabled = true)
                                                viewModel.setDataMonthlyLimitConfigured(configured = true)
                                                viewModel.setWifiMonthlyLimit(wifiMonthly)
                                                viewModel.setWifiMonthlyLimitEnabled(enabled = true)
                                                viewModel.setWifiMonthlyLimitConfigured(configured = true)
                                            }
                                            else -> {
                                                viewModel.setDataDailyLimit(dataDaily)
                                                viewModel.setDataDailyLimitEnabled(enabled = true)
                                                viewModel.setDataDailyLimitConfigured(configured = true)
                                                viewModel.setDataMonthlyLimit(dataMonthly)
                                                viewModel.setDataMonthlyLimitEnabled(enabled = true)
                                                viewModel.setDataMonthlyLimitConfigured(configured = true)
                                                viewModel.setWifiDailyLimit(wifiDaily)
                                                viewModel.setWifiDailyLimitEnabled(enabled = true)
                                                viewModel.setWifiDailyLimitConfigured(configured = true)
                                                viewModel.setWifiMonthlyLimit(wifiMonthly)
                                                viewModel.setWifiMonthlyLimitEnabled(enabled = true)
                                                viewModel.setWifiMonthlyLimitConfigured(configured = true)
                                            }
                                        }
                                    }
                                }
                                viewModel.isGeneralLimitOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalPillsRow(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            stringResource(R.string.title_general_limits),
            stringResource(R.string.label_app_limits)
        )

        tabs.forEachIndexed { index, label ->
            val selected = selectedTab == index
            val contentColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "TabContentColor"
            )
            val containerColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "TabContainerColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
                    .clickable {
                        onTabChange(index)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun GeneralLimitsList(
    viewModel: AppLimitsViewModel,
    onEdit: () -> Unit,
    onDelete: (AppLimit) -> Unit
) {
    val dataDailyLimitEnabled by viewModel.dataDailyLimitEnabled.collectAsState()
    val dataMonthlyLimitEnabled by viewModel.dataMonthlyLimitEnabled.collectAsState()
    val wifiDailyLimitEnabled by viewModel.wifiDailyLimitEnabled.collectAsState()
    val wifiMonthlyLimitEnabled by viewModel.wifiMonthlyLimitEnabled.collectAsState()

    val dataDailyLimitConfigured by viewModel.dataDailyLimitConfigured.collectAsState()
    val dataMonthlyLimitConfigured by viewModel.dataMonthlyLimitConfigured.collectAsState()
    val wifiDailyLimitConfigured by viewModel.wifiDailyLimitConfigured.collectAsState()
    val wifiMonthlyLimitConfigured by viewModel.wifiMonthlyLimitConfigured.collectAsState()

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()

    val currentMobileUsage by viewModel.currentMobileUsage.collectAsState()
    val currentWifiUsage by viewModel.currentWifiUsage.collectAsState()
    val currentMonthlyMobileUsage by viewModel.currentMonthlyMobileUsage.collectAsState()
    val currentMonthlyWifiUsage by viewModel.currentMonthlyWifiUsage.collectAsState()

    val anyLimitConfigured = dataDailyLimitConfigured || dataMonthlyLimitConfigured ||
            wifiDailyLimitConfigured || wifiMonthlyLimitConfigured

    Box(modifier = Modifier.fillMaxSize()) {
        if (!anyLimitConfigured) {
            EmptyLimitsPlaceholder(
                title = stringResource(R.string.msg_no_general_limits),
                subtitle = stringResource(R.string.desc_add_general_limit_subtitle)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (wifiDailyLimitConfigured) {
                    item {
                        GeneralLimitItem(
                            title = "Daily Wi-Fi",
                            icon = Icons.Rounded.Wifi,
                            currentUsage = currentWifiUsage,
                            limit = wifiDailyLimit,
                            onToggle = { viewModel.setWifiDailyLimitEnabled(it) },
                            onEdit = onEdit,
                            onDelete = { onDelete(AppLimit(packageName = "system.wifi.daily", appName = "Daily Wi-Fi Limit", dataLimit = wifiDailyLimit, limitType = "daily", networkType = "wifi")) },
                            enabled = wifiDailyLimitEnabled,
                        )
                    }
                }
                if (dataDailyLimitConfigured) {
                    item {
                        GeneralLimitItem(
                            title = "Daily Mobile",
                            icon = Icons.Rounded.SignalCellularAlt,
                            currentUsage = currentMobileUsage,
                            limit = dataDailyLimit,
                            onToggle = { viewModel.setDataDailyLimitEnabled(it) },
                            onEdit = onEdit,
                            onDelete = { onDelete(AppLimit(packageName = "system.mobile.daily", appName = "Daily Mobile Limit", dataLimit = dataDailyLimit, limitType = "daily", networkType = "mobile")) },
                            enabled = dataDailyLimitEnabled,
                        )
                    }
                }
                if (wifiMonthlyLimitConfigured) {
                    item {
                        GeneralLimitItem(
                            title = "Monthly Wi-Fi",
                            icon = Icons.Rounded.Wifi,
                            currentUsage = currentMonthlyWifiUsage,
                            limit = wifiMonthlyLimit,
                            onToggle = { viewModel.setWifiMonthlyLimitEnabled(it) },
                            onEdit = onEdit,
                            onDelete = { onDelete(AppLimit(packageName = "system.wifi.monthly", appName = "Monthly Wi-Fi Limit", dataLimit = wifiMonthlyLimit, limitType = "monthly", networkType = "wifi")) },
                            enabled = wifiMonthlyLimitEnabled,
                        )
                    }
                }
                if (dataMonthlyLimitConfigured) {
                    item {
                        GeneralLimitItem(
                            title = "Monthly Mobile",
                            icon = Icons.Rounded.SignalCellularAlt,
                            currentUsage = currentMonthlyMobileUsage,
                            limit = dataMonthlyLimit,
                            onToggle = { viewModel.setDataMonthlyLimitEnabled(it) },
                            onEdit = onEdit,
                            onDelete = { onDelete(AppLimit(packageName = "system.mobile.monthly", appName = "Monthly Mobile Limit", dataLimit = dataMonthlyLimit, limitType = "monthly", networkType = "mobile")) },
                            enabled = dataMonthlyLimitEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitsList(
    viewModel: AppLimitsViewModel,
    onEdit: (AppLimit) -> Unit,
    onDelete: (AppLimit) -> Unit
) {
    val appLimits by viewModel.appLimits.collectAsState()
    val appBlockingMasterEnabled by viewModel.appBlockingMasterEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (appLimits.isEmpty()) {
            EmptyLimitsPlaceholder(
                stringResource(R.string.msg_no_app_limits),
                stringResource(R.string.desc_restrict_app_subtitle)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(appLimits.asReversed()) { limit ->
                    StaggeredEntrance {
                        AppLimitItem(
                            limit = limit,
                            onToggle = { enabled -> viewModel.updateAppLimit(limit.copy(isEnabled = enabled)) },
                            onDelete = { onDelete(limit) },
                            onEdit = { onEdit(limit) },
                            appBlockingMasterEnabled = appBlockingMasterEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralLimitItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentUsage: Long,
    limit: Long,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = if (limit > 0) (currentUsage.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val isOverLimitValue = enabled && (limit > 0) && (currentUsage >= limit)
    
    val accentColor = when {
        title.contains("Wi-Fi", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
        title.contains("Mobile", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .bounceClick { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = accentColor.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isOverLimitValue) {
                            Spacer(Modifier.width(8.dp))
                            StatusIcon(
                                icon = Icons.Rounded.Error,
                                color = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.badge_limit_reached)
                            )
                        }
                    }
                    
                    if (enabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(if (isOverLimitValue) MaterialTheme.colorScheme.error else accentColor)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(0.95f).padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (isOverLimitValue) MaterialTheme.colorScheme.error else accentColor
                            )
                            Text(
                                text = "${formatUsage(currentUsage)} / ${formatUsage(limit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.7f).padding(top = 2.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onEdit,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_edit), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_delete), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitItem(
    limit: AppLimit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    appBlockingMasterEnabled: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appIcon = remember(limit.packageName) {
        try {
            context.packageManager.getApplicationIcon(limit.packageName)
        } catch (_: Exception) {
            null
        }
    }

    val wifiProgress = if (limit.wifiDataLimit > 0) (limit.currentWifiUsage.toFloat() / limit.wifiDataLimit).coerceIn(0f, 1f) else 0f
    val mobileProgress = if (limit.mobileDataLimit > 0) (limit.currentMobileUsage.toFloat() / limit.mobileDataLimit).coerceIn(0f, 1f) else 0f

    val wifiColor = MaterialTheme.colorScheme.secondary
    val mobileColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .bounceClick { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (limit.isEnabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        val isWifiOver = limit.isEnabled && limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit
        val isMobileOver = limit.isEnabled && limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit
        val isBlocked = appBlockingMasterEnabled && limit.isEnabled && (limit.isBlocked || limit.isWifiBlocked || limit.isMobileBlocked)

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        alpha = if (limit.isEnabled) 1f else 0.5f,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Apps, null,
                        tint = if (limit.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = limit.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (limit.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isWifiOver || isMobileOver) {
                            Spacer(Modifier.width(8.dp))
                            StatusIcon(
                                icon = Icons.Rounded.Error,
                                color = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.badge_limit_reached)
                            )
                        }
                        if (isBlocked) {
                            Spacer(Modifier.width(6.dp))
                            StatusIcon(
                                icon = Icons.Rounded.Block,
                                color = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.badge_blocked)
                            )
                        }
                    }
                    
                    if (limit.isEnabled) {
                        if ((limit.networkType == "both") || (limit.networkType == "wifi")) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(wifiColor.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(wifiProgress)
                                        .background(if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.error else wifiColor)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(0.95f).padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${(wifiProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = wifiColor, fontWeight = FontWeight.Black)
                                Text("${formatUsage(limit.currentWifiUsage)} / ${formatUsage(limit.wifiDataLimit)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }

                        if ((limit.networkType == "both") || (limit.networkType == "mobile")) {
                            Spacer(modifier = Modifier.height(if (limit.networkType == "both") 16.dp else 10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(mobileColor.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(mobileProgress)
                                        .background(if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.error else mobileColor)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(0.95f).padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${(mobileProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = mobileColor, fontWeight = FontWeight.Black)
                                Text("${formatUsage(limit.currentMobileUsage)} / ${formatUsage(limit.mobileDataLimit)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Switch(
                    checked = limit.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.7f).padding(top = 2.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onEdit,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_edit), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_delete), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitTypeSelectionDialog(
    onDismiss: () -> Unit,
    onAppLimitSelected: () -> Unit,
    onGeneralLimitSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_choose_limit_type), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = onGeneralLimitSelected,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Public, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.label_general_limit_selection), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.desc_general_limit_selection), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Surface(
                    onClick = onAppLimitSelected,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Apps, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.label_app_limit_selection), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.desc_app_limit_selection), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

// Configuration screen for general limits (daily/monthly limits, Wi-Fi vs mobile,
// and custom billing period schedules).
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.Dp
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.theme.premiumSpring
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.drawBehind
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@Composable
fun AppLimitsScreen(
    viewModel: AppLimitsViewModel,
    modifier: Modifier = Modifier,
    currentTab: Int = 0,
    onTabChange: (Int) -> Unit = {}
) {
    var limitToDelete by remember { mutableStateOf<AppLimit?>(null) }
    var showAddGeneralLimitDialog by remember { mutableStateOf(false) }

    val dataDailyLimitConfigured by viewModel.dataDailyLimitConfigured.collectAsState()
    val dataMonthlyLimitConfigured by viewModel.dataMonthlyLimitConfigured.collectAsState()
    val wifiDailyLimitConfigured by viewModel.wifiDailyLimitConfigured.collectAsState()
    val wifiMonthlyLimitConfigured by viewModel.wifiMonthlyLimitConfigured.collectAsState()
    val dataCustomLimitConfigured by viewModel.dataCustomLimitConfigured.collectAsState()
    val wifiCustomLimitConfigured by viewModel.wifiCustomLimitConfigured.collectAsState()

    val dataDailyLimitEnabled by viewModel.dataDailyLimitEnabled.collectAsState()
    val dataMonthlyLimitEnabled by viewModel.dataMonthlyLimitEnabled.collectAsState()
    val wifiDailyLimitEnabled by viewModel.wifiDailyLimitEnabled.collectAsState()
    val wifiMonthlyLimitEnabled by viewModel.wifiMonthlyLimitEnabled.collectAsState()
    val dataCustomLimitEnabled by viewModel.dataCustomLimitEnabled.collectAsState()
    val wifiCustomLimitEnabled by viewModel.wifiCustomLimitEnabled.collectAsState()

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()
    val dataCustomLimit by viewModel.dataCustomLimit.collectAsState()
    val wifiCustomLimit by viewModel.wifiCustomLimit.collectAsState()

    val dataCustomLimitStart by viewModel.dataCustomLimitStart.collectAsState()
    val dataCustomLimitEnd by viewModel.dataCustomLimitEnd.collectAsState()
    val wifiCustomLimitStart by viewModel.wifiCustomLimitStart.collectAsState()
    val wifiCustomLimitEnd by viewModel.wifiCustomLimitEnd.collectAsState()

    val currentMobileUsage by viewModel.currentMobileUsage.collectAsState()
    val currentWifiUsage by viewModel.currentWifiUsage.collectAsState()
    val currentMonthlyMobileUsage by viewModel.currentMonthlyMobileUsage.collectAsState()
    val currentMonthlyWifiUsage by viewModel.currentMonthlyWifiUsage.collectAsState()
    val currentCustomMobileUsage by viewModel.currentCustomMobileUsage.collectAsState()
    val currentCustomWifiUsage by viewModel.currentCustomWifiUsage.collectAsState()

    val appLimits by viewModel.appLimits.collectAsState()
    val appBlockingMasterEnabled by viewModel.appBlockingMasterEnabled.collectAsState()

    val unconfiguredPlans = remember(dataDailyLimitConfigured, dataMonthlyLimitConfigured, wifiDailyLimitConfigured, wifiMonthlyLimitConfigured, dataCustomLimitConfigured, wifiCustomLimitConfigured) {
        buildList {
            if (!wifiDailyLimitConfigured) add("daily_wifi")
            if (!dataDailyLimitConfigured) add("daily_mobile")
            if (!wifiMonthlyLimitConfigured) add("monthly_wifi")
            if (!dataMonthlyLimitConfigured) add("monthly_mobile")
            if (!wifiCustomLimitConfigured) add("custom_wifi")
            if (!dataCustomLimitConfigured) add("custom_mobile")
        }
    }

    val allGeneralLimitsConfigured = unconfiguredPlans.isEmpty()

    if (showAddGeneralLimitDialog) {
        AddGeneralLimitDialog(
            unconfiguredPlans = unconfiguredPlans,
            onDismiss = { showAddGeneralLimitDialog = false },
            onPlanSelected = { planType ->
                viewModel.configuringGeneralLimitType = planType
                showAddGeneralLimitDialog = false
            }
        )
    }

    val filteredAppLimits = remember(appLimits, viewModel.searchQuery) {
        if (viewModel.searchQuery.isBlank()) {
            appLimits.asReversed()
        } else {
            appLimits.asReversed().filter {
                it.appName.contains(viewModel.searchQuery, ignoreCase = true) ||
                it.packageName.contains(viewModel.searchQuery, ignoreCase = true)
            }
        }
    }

    fun formatRange(start: Long, end: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
    }

    data class SystemPlanItem(
        val type: String,
        val title: String,
        val limit: Long,
        val usage: Long,
        val enabled: Boolean,
        val isWifi: Boolean,
        val subtitle: String? = null,
        val onToggle: (Boolean) -> Unit,
    )

    val dailyWifiTitle = stringResource(R.string.label_daily_wifi)
    val dailyMobileTitle = stringResource(R.string.label_daily_mobile)
    val monthlyWifiTitle = stringResource(R.string.label_monthly_wifi)
    val monthlyMobileTitle = stringResource(R.string.label_monthly_mobile)
    val customWifiTitle = stringResource(R.string.label_custom_wifi)
    val customMobileTitle = stringResource(R.string.label_custom_mobile)

    val activePlansList = remember(
        dataDailyLimitConfigured, dataMonthlyLimitConfigured, wifiDailyLimitConfigured, wifiMonthlyLimitConfigured, dataCustomLimitConfigured, wifiCustomLimitConfigured,
        dataDailyLimitEnabled, dataMonthlyLimitEnabled, wifiDailyLimitEnabled, wifiMonthlyLimitEnabled, dataCustomLimitEnabled, wifiCustomLimitEnabled,
        dataDailyLimit, wifiDailyLimit, dataMonthlyLimit, wifiMonthlyLimit, dataCustomLimit, wifiCustomLimit,
        currentMobileUsage, currentWifiUsage, currentMonthlyMobileUsage, currentMonthlyWifiUsage, currentCustomMobileUsage, currentCustomWifiUsage,
        wifiCustomLimitStart, wifiCustomLimitEnd, dataCustomLimitStart, dataCustomLimitEnd,
        dailyWifiTitle, dailyMobileTitle, monthlyWifiTitle, monthlyMobileTitle, customWifiTitle, customMobileTitle
    ) {
        buildList<SystemPlanItem> {
            if (wifiDailyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "daily_wifi",
                        title = dailyWifiTitle,
                        limit = wifiDailyLimit,
                        usage = currentWifiUsage,
                        enabled = wifiDailyLimitEnabled,
                        isWifi = true,
                        onToggle = { viewModel.setWifiDailyLimitEnabled(it) }
                    )
                )
            }
            if (dataDailyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "daily_mobile",
                        title = dailyMobileTitle,
                        limit = dataDailyLimit,
                        usage = currentMobileUsage,
                        enabled = dataDailyLimitEnabled,
                        isWifi = false,
                        onToggle = { viewModel.setDataDailyLimitEnabled(it) }
                    )
                )
            }
            if (wifiMonthlyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "monthly_wifi",
                        title = monthlyWifiTitle,
                        limit = wifiMonthlyLimit,
                        usage = currentMonthlyWifiUsage,
                        enabled = wifiMonthlyLimitEnabled,
                        isWifi = true,
                        onToggle = { viewModel.setWifiMonthlyLimitEnabled(it) }
                    )
                )
            }
            if (dataMonthlyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "monthly_mobile",
                        title = monthlyMobileTitle,
                        limit = dataMonthlyLimit,
                        usage = currentMonthlyMobileUsage,
                        enabled = dataMonthlyLimitEnabled,
                        isWifi = false,
                        onToggle = { viewModel.setDataMonthlyLimitEnabled(it) }
                    )
                )
            }
            if (wifiCustomLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "custom_wifi",
                        title = customWifiTitle,
                        limit = wifiCustomLimit,
                        usage = currentCustomWifiUsage,
                        enabled = wifiCustomLimitEnabled,
                        isWifi = true,
                        subtitle = formatRange(wifiCustomLimitStart, wifiCustomLimitEnd),
                        onToggle = { viewModel.setWifiCustomLimitEnabled(it) }
                    )
                )
            }
            if (dataCustomLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "custom_mobile",
                        title = customMobileTitle,
                        limit = dataCustomLimit,
                        usage = currentCustomMobileUsage,
                        enabled = dataCustomLimitEnabled,
                        isWifi = false,
                        subtitle = formatRange(dataCustomLimitStart, dataCustomLimitEnd),
                        onToggle = { viewModel.setDataCustomLimitEnabled(it) }
                    )
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.title_general_limits),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (!allGeneralLimitsConfigured) {
                        TextButton(
                            onClick = {
                                if (unconfiguredPlans.size == 1) {
                                    viewModel.configuringGeneralLimitType = unconfiguredPlans.first()
                                } else {
                                    showAddGeneralLimitDialog = true
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_add_plan), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (activePlansList.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SetupPlanPlaceholderTile(
                            onClick = { showAddGeneralLimitDialog = true }
                        )
                    }
                }
            } else {
                itemsIndexed(activePlansList) { index, plan ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        StaggeredEntrance(index = index) {
                            SystemPlanCard(
                                title = plan.title,
                                usage = plan.usage,
                                limit = plan.limit,
                                enabled = plan.enabled,
                                isWifi = plan.isWifi,
                                subtitle = plan.subtitle,
                                onToggle = plan.onToggle,
                                onCardClick = { viewModel.configuringGeneralLimitType = plan.type },
                                onDelete = {
                                    val title = plan.title
                                    val pType = plan.type
                                    val limitVal = plan.limit
                                    val netType = if (plan.isWifi) "wifi" else "mobile"
                                    val periodType = if (pType.startsWith("daily")) "daily" else if (pType.startsWith("monthly")) "monthly" else "custom"
                                    limitToDelete = AppLimit(
                                        packageName = "system.${netType}.${periodType}",
                                        appName = title,
                                        dataLimit = limitVal,
                                        limitType = periodType,
                                        networkType = netType
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_app_limits),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TextButton(
                        onClick = {
                            viewModel.loadInstalledApps()
                            viewModel.isPickerOpen = true
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_add_app_limit), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (appLimits.isEmpty()) {
                item {
                    EmptyAppLimitsPlaceholder(
                        onAddClick = {
                            viewModel.loadInstalledApps()
                            viewModel.isPickerOpen = true
                        }
                    )
                }
            } else {
                itemsIndexed(appLimits.asReversed()) { index, limit ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        StaggeredEntrance(index = activePlansList.size + index) {
                            AppLimitItem(
                                limit = limit,
                                onToggle = { enabled -> viewModel.updateAppLimit(limit.copy(isEnabled = enabled)) },
                                onDelete = { limitToDelete = limit },
                                onEdit = { viewModel.editingLimit = limit },
                                appBlockingMasterEnabled = appBlockingMasterEnabled
                            )
                        }
                    }
                }
            }
        }

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
                                "system.wifi.custom" -> {
                                    viewModel.setWifiCustomLimitEnabled(false)
                                    viewModel.setWifiCustomLimitConfigured(false)
                                }
                                "system.mobile.custom" -> {
                                    viewModel.setDataCustomLimitEnabled(false)
                                    viewModel.setDataCustomLimitConfigured(false)
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
    val showPicker = viewModel.isPickerOpen
    val editingLimit = viewModel.editingLimit
    val configuringGeneralLimitType = viewModel.configuringGeneralLimitType

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()
    val dataCustomLimit by viewModel.dataCustomLimit.collectAsState()
    val wifiCustomLimit by viewModel.wifiCustomLimit.collectAsState()
    val dataCustomLimitStart by viewModel.dataCustomLimitStart.collectAsState()
    val dataCustomLimitEnd by viewModel.dataCustomLimitEnd.collectAsState()
    val wifiCustomLimitStart by viewModel.wifiCustomLimitStart.collectAsState()
    val wifiCustomLimitEnd by viewModel.wifiCustomLimitEnd.collectAsState()



    if (editingLimit != null) {
        AppLimitEditScreen(
            limit = editingLimit,
            onBack = { viewModel.editingLimit = null },
        ) { updatedLimit ->
            viewModel.updateAppLimit(updatedLimit)
            viewModel.editingLimit = null
        }
    }

    if (configuringGeneralLimitType != null) {
        val planType = configuringGeneralLimitType
        val initialLimit = when (planType) {
            "daily_wifi" -> wifiDailyLimit
            "daily_mobile" -> dataDailyLimit
            "monthly_wifi" -> wifiMonthlyLimit
            "monthly_mobile" -> dataMonthlyLimit
            "custom_wifi" -> wifiCustomLimit
            "custom_mobile" -> dataCustomLimit
            else -> 0L
        }
        val initialStart = when (planType) {
            "custom_wifi" -> wifiCustomLimitStart
            "custom_mobile" -> dataCustomLimitStart
            else -> 0L
        }
        val initialEnd = when (planType) {
            "custom_wifi" -> wifiCustomLimitEnd
            "custom_mobile" -> dataCustomLimitEnd
            else -> 0L
        }

        GeneralLimitConfigScreen(
            planType = planType,
            initialLimit = initialLimit,
            initialStart = initialStart,
            initialEnd = initialEnd,
            onBack = { viewModel.configuringGeneralLimitType = null },
            onConfirm = { limitBytes, start, end ->
                when (planType) {
                    "daily_wifi" -> {
                        viewModel.setWifiDailyLimit(limitBytes)
                        viewModel.setWifiDailyLimitEnabled(enabled = true)
                        viewModel.setWifiDailyLimitConfigured(configured = true)
                    }
                    "daily_mobile" -> {
                        viewModel.setDataDailyLimit(limitBytes)
                        viewModel.setDataDailyLimitEnabled(enabled = true)
                        viewModel.setDataDailyLimitConfigured(configured = true)
                    }
                    "monthly_wifi" -> {
                        viewModel.setWifiMonthlyLimit(limitBytes)
                        viewModel.setWifiMonthlyLimitEnabled(enabled = true)
                        viewModel.setWifiMonthlyLimitConfigured(configured = true)
                    }
                    "monthly_mobile" -> {
                        viewModel.setDataMonthlyLimit(limitBytes)
                        viewModel.setDataMonthlyLimitEnabled(enabled = true)
                        viewModel.setDataMonthlyLimitConfigured(configured = true)
                    }
                    "custom_wifi" -> {
                        viewModel.setWifiCustomLimit(limitBytes)
                        viewModel.setWifiCustomLimitRange(start, end)
                        viewModel.setWifiCustomLimitEnabled(enabled = true)
                        viewModel.setWifiCustomLimitConfigured(configured = true)
                    }
                    "custom_mobile" -> {
                        viewModel.setDataCustomLimit(limitBytes)
                        viewModel.setDataCustomLimitRange(start, end)
                        viewModel.setDataCustomLimitEnabled(enabled = true)
                        viewModel.setDataCustomLimitConfigured(configured = true)
                    }
                }
                viewModel.configuringGeneralLimitType = null
            }
        )
    }
}

@Composable
fun MinimalPillsRow(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(R.string.title_general_limits),
        stringResource(R.string.label_app_limits)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        val width = maxWidth
        val indicatorWidth = width / 2
        val targetOffset = if (selectedTab == 0) 0.dp else indicatorWidth
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = premiumSpring<Dp>(),
            label = "TabIndicatorOffset"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(indicatorWidth)
                .offset(x = animatedOffset)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = premiumSpring(),
                    label = "TabTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabChange(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralLimitsList(
    viewModel: AppLimitsViewModel,
    onEdit: (String) -> Unit,
    onDelete: (AppLimit) -> Unit,
    onAddPlanClick: () -> Unit
) {
    val dataDailyLimitEnabled by viewModel.dataDailyLimitEnabled.collectAsState()
    val dataMonthlyLimitEnabled by viewModel.dataMonthlyLimitEnabled.collectAsState()
    val wifiDailyLimitEnabled by viewModel.wifiDailyLimitEnabled.collectAsState()
    val wifiMonthlyLimitEnabled by viewModel.wifiMonthlyLimitEnabled.collectAsState()
    val dataCustomLimitEnabled by viewModel.dataCustomLimitEnabled.collectAsState()
    val wifiCustomLimitEnabled by viewModel.wifiCustomLimitEnabled.collectAsState()

    val dataDailyLimitConfigured by viewModel.dataDailyLimitConfigured.collectAsState()
    val dataMonthlyLimitConfigured by viewModel.dataMonthlyLimitConfigured.collectAsState()
    val wifiDailyLimitConfigured by viewModel.wifiDailyLimitConfigured.collectAsState()
    val wifiMonthlyLimitConfigured by viewModel.wifiMonthlyLimitConfigured.collectAsState()
    val dataCustomLimitConfigured by viewModel.dataCustomLimitConfigured.collectAsState()
    val wifiCustomLimitConfigured by viewModel.wifiCustomLimitConfigured.collectAsState()

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()
    val dataCustomLimit by viewModel.dataCustomLimit.collectAsState()
    val wifiCustomLimit by viewModel.wifiCustomLimit.collectAsState()

    val dataCustomLimitStart by viewModel.dataCustomLimitStart.collectAsState()
    val dataCustomLimitEnd by viewModel.dataCustomLimitEnd.collectAsState()
    val wifiCustomLimitStart by viewModel.wifiCustomLimitStart.collectAsState()
    val wifiCustomLimitEnd by viewModel.wifiCustomLimitEnd.collectAsState()

    val currentMobileUsage by viewModel.currentMobileUsage.collectAsState()
    val currentWifiUsage by viewModel.currentWifiUsage.collectAsState()
    val currentMonthlyMobileUsage by viewModel.currentMonthlyMobileUsage.collectAsState()
    val currentMonthlyWifiUsage by viewModel.currentMonthlyWifiUsage.collectAsState()
    val currentCustomMobileUsage by viewModel.currentCustomMobileUsage.collectAsState()
    val currentCustomWifiUsage by viewModel.currentCustomWifiUsage.collectAsState()

    val anyLimitConfigured = dataDailyLimitConfigured || dataMonthlyLimitConfigured ||
            wifiDailyLimitConfigured || wifiMonthlyLimitConfigured ||
            dataCustomLimitConfigured || wifiCustomLimitConfigured

    fun formatRange(start: Long, end: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!anyLimitConfigured) {
            EmptyLimitsPlaceholder(
                title = stringResource(R.string.msg_no_general_limits),
                subtitle = stringResource(R.string.desc_add_general_limit_subtitle),
                actionLabel = stringResource(R.string.label_general_limit_selection),
                onActionClick = onAddPlanClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (wifiDailyLimitConfigured) {
                    item {
                        val title = stringResource(R.string.label_daily_wifi)
                        GeneralLimitItem(
                            title = title,
                            icon = Icons.Rounded.Wifi,
                            currentUsage = currentWifiUsage,
                            limit = wifiDailyLimit,
                            onToggle = { viewModel.setWifiDailyLimitEnabled(it) },
                            onEdit = { onEdit("daily_wifi") },
                            onDelete = { onDelete(AppLimit(packageName = "system.wifi.daily", appName = title, dataLimit = wifiDailyLimit, limitType = "daily", networkType = "wifi")) },
                            enabled = wifiDailyLimitEnabled,
                        )
                    }
                }
                if (dataDailyLimitConfigured) {
                    item {
                        val title = stringResource(R.string.label_daily_mobile)
                        GeneralLimitItem(
                            title = title,
                            icon = Icons.Rounded.SignalCellularAlt,
                            currentUsage = currentMobileUsage,
                            limit = dataDailyLimit,
                            onToggle = { viewModel.setDataDailyLimitEnabled(it) },
                            onEdit = { onEdit("daily_mobile") },
                            onDelete = { onDelete(AppLimit(packageName = "system.mobile.daily", appName = title, dataLimit = dataDailyLimit, limitType = "daily", networkType = "mobile")) },
                            enabled = dataDailyLimitEnabled,
                        )
                    }
                }
                if (wifiMonthlyLimitConfigured) {
                    item {
                        val title = stringResource(R.string.label_monthly_wifi)
                        GeneralLimitItem(
                            title = title,
                            icon = Icons.Rounded.Wifi,
                            currentUsage = currentMonthlyWifiUsage,
                            limit = wifiMonthlyLimit,
                            onToggle = { viewModel.setWifiMonthlyLimitEnabled(it) },
                            onEdit = { onEdit("monthly_wifi") },
                            onDelete = { onDelete(AppLimit(packageName = "system.wifi.monthly", appName = title, dataLimit = wifiMonthlyLimit, limitType = "monthly", networkType = "wifi")) },
                            enabled = wifiMonthlyLimitEnabled,
                        )
                    }
                }
                if (dataMonthlyLimitConfigured) {
                    item {
                        val title = stringResource(R.string.label_monthly_mobile)
                        GeneralLimitItem(
                            title = title,
                            icon = Icons.Rounded.SignalCellularAlt,
                            currentUsage = currentMonthlyMobileUsage,
                            limit = dataMonthlyLimit,
                            onToggle = { viewModel.setDataMonthlyLimitEnabled(it) },
                            onEdit = { onEdit("monthly_mobile") },
                            onDelete = { onDelete(AppLimit(packageName = "system.mobile.monthly", appName = title, dataLimit = dataMonthlyLimit, limitType = "monthly", networkType = "mobile")) },
                            enabled = dataMonthlyLimitEnabled,
                        )
                    }
                }
                if (wifiCustomLimitConfigured) {
                    item {
                        val title = stringResource(R.string.label_custom_wifi)
                        GeneralLimitItem(
                            title = title,
                            icon = Icons.Rounded.Wifi,
                            currentUsage = currentCustomWifiUsage,
                            limit = wifiCustomLimit,
                            onToggle = { viewModel.setWifiCustomLimitEnabled(it) },
                            onEdit = { onEdit("custom_wifi") },
                            onDelete = { onDelete(AppLimit(packageName = "system.wifi.custom", appName = title, dataLimit = wifiCustomLimit, limitType = "custom", networkType = "wifi")) },
                            enabled = wifiCustomLimitEnabled,
                            subtitle = formatRange(wifiCustomLimitStart, wifiCustomLimitEnd)
                        )
                    }
                }
                if (dataCustomLimitConfigured) {
                    item {
                        val title = stringResource(R.string.label_custom_mobile)
                        GeneralLimitItem(
                            title = title,
                            icon = Icons.Rounded.SignalCellularAlt,
                            currentUsage = currentCustomMobileUsage,
                            limit = dataCustomLimit,
                            onToggle = { viewModel.setDataCustomLimitEnabled(it) },
                            onEdit = { onEdit("custom_mobile") },
                            onDelete = { onDelete(AppLimit(packageName = "system.mobile.custom", appName = title, dataLimit = dataCustomLimit, limitType = "custom", networkType = "mobile")) },
                            enabled = dataCustomLimitEnabled,
                            subtitle = formatRange(dataCustomLimitStart, dataCustomLimitEnd)
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
    onDelete: (AppLimit) -> Unit,
    onAddLimitClick: () -> Unit
) {
    val appLimits by viewModel.appLimits.collectAsState()
    val appBlockingMasterEnabled by viewModel.appBlockingMasterEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (appLimits.isEmpty()) {
            EmptyLimitsPlaceholder(
                title = stringResource(R.string.msg_no_app_limits),
                subtitle = stringResource(R.string.desc_restrict_app_subtitle),
                actionLabel = stringResource(R.string.cd_add_app_limit),
                onActionClick = onAddLimitClick
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
    subtitle: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = if (limit > 0) (currentUsage.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val isOverLimitValue = enabled && (limit > 0) && (currentUsage >= limit)
    
    val accentColor = when (icon) {
        Icons.Rounded.Wifi -> MaterialTheme.colorScheme.secondary
        Icons.Rounded.SignalCellularAlt -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .bounceClick { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (enabled) 1.5.dp else 1.dp,
            color = if (enabled) accentColor.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (enabled) accentColor.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                color = if (isOverLimitValue) MaterialTheme.colorScheme.error else accentColor,
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isOverLimitValue) MaterialTheme.colorScheme.errorContainer
                                        else accentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverLimitValue) MaterialTheme.colorScheme.onErrorContainer else accentColor
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatUsage(currentUsage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " / ${formatUsage(limit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.btn_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
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

    val isWifiOver = limit.isEnabled && limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit
    val isMobileOver = limit.isEnabled && limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit
    val isBlocked = appBlockingMasterEnabled && limit.isEnabled && (limit.isBlocked || limit.isWifiBlocked || limit.isMobileBlocked)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .bounceClick { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (limit.isEnabled) MaterialTheme.colorScheme.surfaceContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (limit.isEnabled) 1.5.dp else 1.dp,
            color = if (limit.isEnabled) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap(width = 96, height = 96).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                        alpha = if (limit.isEnabled) 1f else 0.5f,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = if (limit.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
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
                    }
                    
                    if (limit.isEnabled && (isWifiOver || isMobileOver || isBlocked)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isWifiOver || isMobileOver) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.badge_limit_reached),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            if (isBlocked) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.badge_blocked),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Switch(
                    checked = limit.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            if (limit.isEnabled) {
                if ((limit.networkType == "both") || (limit.networkType == "wifi")) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = wifiColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.label_wifi),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = wifiColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(wifiColor.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(wifiProgress)
                                .background(
                                    color = if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.error else wifiColor,
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.errorContainer
                                            else wifiColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(wifiProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.onErrorContainer else wifiColor
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatUsage(limit.currentWifiUsage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " / ${formatUsage(limit.wifiDataLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }

                if ((limit.networkType == "both") || (limit.networkType == "mobile")) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            tint = mobileColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.label_mobile),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = mobileColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(mobileColor.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(mobileProgress)
                                .background(
                                    color = if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.error else mobileColor,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.errorContainer
                                            else mobileColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(mobileProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.onErrorContainer else mobileColor
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatUsage(limit.currentMobileUsage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " / ${formatUsage(limit.mobileDataLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.btn_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
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

@Composable
fun AddGeneralLimitDialog(
    unconfiguredPlans: List<String>,
    onDismiss: () -> Unit,
    onPlanSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.title_select_plan_type),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                unconfiguredPlans.forEach { planType ->
                    val periodText = when {
                        planType.startsWith("daily") -> stringResource(R.string.filter_daily)
                        planType.startsWith("monthly") -> stringResource(R.string.filter_monthly)
                        else -> stringResource(R.string.filter_custom)
                    }
                    val networkText = when {
                        planType.endsWith("wifi") -> stringResource(R.string.label_wifi)
                        else -> stringResource(R.string.label_mobile)
                    }
                    val icon = when {
                        planType.endsWith("wifi") -> Icons.Rounded.Wifi
                        else -> Icons.Rounded.SignalCellularAlt
                    }
                    val accentColor = when {
                        planType.endsWith("wifi") -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    
                    Surface(
                        onClick = { onPlanSelected(planType) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = accentColor.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$periodText $networkText",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SystemPlanCard(
    title: String,
    usage: Long,
    limit: Long,
    enabled: Boolean,
    isWifi: Boolean,
    subtitle: String? = null,
    onToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (limit > 0) (usage.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val isOverLimit = enabled && (limit > 0) && (usage >= limit)
    
    val accentColor = MaterialTheme.colorScheme.secondary
    
    val backgroundBrush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.secondaryContainer
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .bounceClick { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) accentColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(20.dp)
        ) {
            if (enabled) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(x = 180.dp, y = (-40).dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(x = 210.dp, y = 60.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                )
            }

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (enabled) Color.White.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWifi) Icons.Rounded.Wifi else Icons.Rounded.SignalCellularAlt,
                                contentDescription = null,
                                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (enabled) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (enabled) Color.White else MaterialTheme.colorScheme.outline,
                            checkedTrackColor = if (enabled) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.scale(0.7f)
                    )
                }

                if (enabled) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatUsage(usage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = " / ${formatUsage(limit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverLimit) MaterialTheme.colorScheme.error else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(
                                        color = if (isOverLimit) MaterialTheme.colorScheme.error else Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_inactive),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.cd_delete_plan),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPlanPlaceholderTile(
    onClick: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 145.dp)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 145.dp)
                .drawBehind {
                    val stroke = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawRoundRect(
                        color = outlineColor,
                        style = stroke,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.label_configure_network_plan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.desc_prevent_overages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun VpnMasterCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_block_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.desc_block_apps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Composable
fun EmptyAppLimitsPlaceholder(
    onAddClick: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 145.dp)
            .padding(horizontal = 20.dp)
            .bounceClick { onAddClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 145.dp)
                .drawBehind {
                    val stroke = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawRoundRect(
                        color = outlineColor,
                        style = stroke,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.msg_no_restricted_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.desc_restrict_app_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

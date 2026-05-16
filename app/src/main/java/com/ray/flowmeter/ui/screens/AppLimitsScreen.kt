package com.ray.flowmeter.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.res.stringResource
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.ui.theme.AppTransitions
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitsScreen(
    viewModel: AppLimitsViewModel,
    modifier: Modifier = Modifier,
    externalPagerState: PagerState? = null,
) {
    val (isPickerOpen, setIsPickerOpen) = remember { mutableStateOf(value = false) }
    var editingLimit by remember { mutableStateOf<AppLimit?>(null) }
    var limitToDelete by remember { mutableStateOf<AppLimit?>(null) }

    val dataDailyLimitConfigured by viewModel.dataDailyLimitConfigured.collectAsState()
    val dataMonthlyLimitConfigured by viewModel.dataMonthlyLimitConfigured.collectAsState()
    val wifiDailyLimitConfigured by viewModel.wifiDailyLimitConfigured.collectAsState()
    val wifiMonthlyLimitConfigured by viewModel.wifiMonthlyLimitConfigured.collectAsState()

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()
    
    val (showTypeSelector, setShowTypeSelector) = remember { mutableStateOf(value = false) }
    val (isGeneralLimitOpen, setIsGeneralLimitOpen) = remember { mutableStateOf(value = false) }

    LaunchedEffect(isPickerOpen, editingLimit, isGeneralLimitOpen) {
        viewModel.setSubViewOpenStatus(isPickerOpen || (editingLimit != null) || isGeneralLimitOpen)
    }

    val currentView = when {
        isPickerOpen -> "picker"
        editingLimit != null -> "edit"
        isGeneralLimitOpen -> "general"
        else -> "list"
    }

    AnimatedContent(
        targetState = currentView,
        modifier = modifier,
        transitionSpec = {
            if (targetState != "list") {
                AppTransitions.ScreenEnter togetherWith AppTransitions.ScreenExit
            } else {
                AppTransitions.ScreenPopEnter togetherWith AppTransitions.ScreenPopExit
            }
        },
        label = "AppLimitsNavigation",
    ) { viewState ->
        when (viewState) {
            "picker" -> {
                AppPickerScreen(
                    viewModel = viewModel,
                    onBack = { setIsPickerOpen(false) },
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
                    setIsPickerOpen(false)
                }
            }
            "edit" -> {
                if (editingLimit != null) {
                    AppLimitEditScreen(
                        limit = editingLimit!!,
                        onBack = { editingLimit = null },
                    ) { updatedLimit ->
                        viewModel.updateAppLimit(updatedLimit)
                        editingLimit = null
                    }
                }
            }
            "general" -> {
                GeneralLimitConfigScreen(
                    initialDataDailyLimit = dataDailyLimit,
                    initialWifiDailyLimit = wifiDailyLimit,
                    initialDataMonthlyLimit = dataMonthlyLimit,
                    initialWifiMonthlyLimit = wifiMonthlyLimit,
                    onBack = { setIsGeneralLimitOpen(false) },
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
                                            else -> { // both
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
                                            else -> { // both
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
                                            else -> { // both
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
                        setIsGeneralLimitOpen(false)
                    }
                )
            }
            else -> {
                val internalPagerState = rememberPagerState(pageCount = { 2 })
                val pagerState = externalPagerState ?: internalPagerState
                val coroutineScope = rememberCoroutineScope()

                val allGeneralLimitsConfigured = dataDailyLimitConfigured && dataMonthlyLimitConfigured && 
                                                 wifiDailyLimitConfigured && wifiMonthlyLimitConfigured

                Box(modifier = modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MinimalPillsRow(
                            pagerState = pagerState,
                            coroutineScope = coroutineScope,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            when (page) {
                                0 -> GeneralLimitsList(
                                    viewModel = viewModel,
                                    onEdit = { setIsGeneralLimitOpen(true) },
                                    onDelete = { limitToDelete = it }
                                )
                                1 -> AppLimitsList(
                                    viewModel = viewModel, 
                                    onEdit = { editingLimit = it }, 
                                    onDelete = { limitToDelete = it }
                                )
                            }
                        }
                    }
                    
                    val showFab = if (pagerState.currentPage == 0) !allGeneralLimitsConfigured else true
                    
                    if (showFab) {
                        FloatingActionButton(
                            onClick = {
                                if (pagerState.currentPage == 1) {
                                    viewModel.loadInstalledApps()
                                    setIsPickerOpen(true)
                                } else {
                                    setIsGeneralLimitOpen(true)
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
                            setIsPickerOpen(true)
                            setShowTypeSelector(false)
                        },
                        onGeneralLimitSelected = {
                            setIsGeneralLimitOpen(true)
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalPillsRow(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
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
            val selected = pagerState.currentPage == index
            val contentColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "TabContentColor"
            )
            val containerColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) 
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "TabContainerColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
                    .clickable { 
                        coroutineScope.launch { pagerState.animateScrollToPage(index) } 
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
                    StaggeredEntrance(index = appLimits.indexOf(limit)) {
                        AppLimitItem(
                            limit = limit,
                            onToggle = { enabled -> viewModel.updateAppLimit(limit.copy(isEnabled = enabled)) },
                            onDelete = { onDelete(limit) },
                            onEdit = { onEdit(limit) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLimitsPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(120.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Block,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
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
    val progress = if (limit > 0) (currentUsage.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val isOverLimitValue = enabled && (limit > 0) && (currentUsage >= limit)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null, 
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.weight(1f),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Switch(checked = enabled, onCheckedChange = onToggle, modifier = Modifier.scale(0.7f))
            }
            
            if (enabled) {
                Spacer(Modifier.height(16.dp))
                ModernProgressIndicator(
                    current = currentUsage,
                    limit = limit,
                    progress = progress,
                    isOverLimit = isOverLimitValue,
                )
            }

            Spacer(Modifier.height(12.dp))
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

@Composable
fun AppLimitItem(
    limit: AppLimit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (limit.isEnabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                appIcon?.let {
                    Image(
                        bitmap = it.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        alpha = if (limit.isEnabled) 1f else 0.5f,
                    )
                } ?: Icon(
                    Icons.Rounded.Apps, null, 
                    tint = if (limit.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                    modifier = Modifier.size(36.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (limit.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Switch(
                    checked = limit.isEnabled, 
                    onCheckedChange = onToggle, 
                    modifier = Modifier.scale(0.7f)
                )
            }

            if (limit.isEnabled) {
                if ((limit.networkType == "both") || (limit.networkType == "wifi")) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernProgressIndicator(
                        label = "Wi-Fi",
                        current = limit.currentWifiUsage,
                        limit = limit.wifiDataLimit,
                        progress = wifiProgress,
                        isOverLimit = (limit.wifiDataLimit > 0) && (limit.currentWifiUsage >= limit.wifiDataLimit)
                    )
                }
                
                if ((limit.networkType == "both") || (limit.networkType == "mobile")) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernProgressIndicator(
                        label = "Mobile",
                        current = limit.currentMobileUsage,
                        limit = limit.mobileDataLimit,
                        progress = mobileProgress,
                        isOverLimit = (limit.mobileDataLimit > 0) && (limit.currentMobileUsage >= limit.mobileDataLimit)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
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

@Composable
fun ModernProgressIndicator(
    label: String? = null,
    current: Long,
    limit: Long,
    progress: Float,
    isOverLimit: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${formatUsage(current)} / ${formatUsage(limit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        val barColor = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)) {
            // Track
            drawLine(
                color = trackColor,
                start = Offset(0f, this.size.height / 2),
                end = Offset(this.size.width, this.size.height / 2),
                strokeWidth = this.size.height,
                cap = StrokeCap.Round
            )
            // Progress
            if (progress > 0f) {
                val progressWidth = this.size.width * progress
                drawLine(
                    color = barColor,
                    start = Offset(0f, this.size.height / 2),
                    end = Offset(progressWidth, this.size.height / 2),
                    strokeWidth = this.size.height,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    viewModel: AppLimitsViewModel,
    onBack: () -> Unit,
    onAppSelected: (
        AppLimitsViewModel.AppInfo,
        Long,
        String,
        String,
        Long,
        Long
    ) -> Unit
) {
    var isPickerOpen by remember { mutableStateOf<AppLimitsViewModel.AppInfo?>(null) }
    var limitInput by remember { mutableStateOf("100") }
    var limitUnit by remember { mutableStateOf("MB") }
    var limitType by remember { mutableStateOf("daily") }
    var networkType by remember { mutableStateOf("both") }

    var wifiLimitInput by remember { mutableStateOf("100") }
    var wifiLimitUnit by remember { mutableStateOf("MB") }
    var mobileLimitInput by remember { mutableStateOf("50") }
    var mobileLimitUnit by remember { mutableStateOf("MB") }

    BackHandler {
        if (isPickerOpen != null) {
            isPickerOpen = null
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isPickerOpen == null) {
                            stringResource(R.string.title_select_application)
                        } else {
                            stringResource(R.string.title_configure_limit)
                        },
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isPickerOpen != null) {
                                isPickerOpen = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isPickerOpen == null) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                    placeholder = {
                        Text(stringResource(R.string.placeholder_search_apps))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.searchQuery = "" }
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (viewModel.isLoadingApps) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    }
                } else {
                    val filtered = viewModel.filteredApps

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.msg_no_apps_found),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filtered, key = { it.packageName }) { app ->
                                val context = LocalContext.current
                                val appIcon by produceState<Drawable?>(
                                    initialValue = null,
                                    key1 = app.packageName
                                ) {
                                    value = withContext(Dispatchers.IO) {
                                        try {
                                            context.packageManager.getApplicationIcon(app.packageName)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }

                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = app.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingContent = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(modifier = Modifier.padding(8.dp)) {
                                                if (appIcon != null) {
                                                    Image(
                                                        bitmap = appIcon!!.toBitmap().asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Rounded.Apps,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        isPickerOpen = app
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                ConfigurationContent(
                    selectedApp = isPickerOpen,
                    limitInput = limitInput,
                    onLimitInputChange = { limitInput = it },
                    limitUnit = limitUnit,
                    onLimitUnitChange = { limitUnit = it },
                    limitType = limitType,
                    onLimitTypeChange = { limitType = it },
                    networkType = networkType,
                    onNetworkTypeChange = { networkType = it },
                    wifiLimitInput = wifiLimitInput,
                    onWifiLimitInputChange = { wifiLimitInput = it },
                    wifiLimitUnit = wifiLimitUnit,
                    onWifiLimitUnitChange = { wifiLimitUnit = it },
                    mobileLimitInput = mobileLimitInput,
                    onMobileLimitInputChange = { mobileLimitInput = it },
                    mobileLimitUnit = mobileLimitUnit,
                    onMobileLimitUnitChange = { mobileLimitUnit = it },
                    onConfirm = {
                        val value = limitInput.toLongOrNull() ?: 0L
                        val multiplier =
                            if (limitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                        val wifiValue = wifiLimitInput.toLongOrNull() ?: 0L
                        val wifiMultiplier =
                            if (wifiLimitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                        val mobileValue = mobileLimitInput.toLongOrNull() ?: 0L
                        val mobileMultiplier =
                            if (mobileLimitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                        onAppSelected(
                            isPickerOpen!!,
                            value * multiplier,
                            limitType,
                            networkType,
                            wifiValue * wifiMultiplier,
                            mobileValue * mobileMultiplier
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitEditScreen(
    limit: AppLimit,
    onBack: () -> Unit,
    onConfirm: (AppLimit) -> Unit
) {
    val (limitInput, setLimitInput) = remember(limit) { 
        val mb = limit.dataLimit / (1024 * 1024)
        mutableStateOf(if ((limit.dataLimit % (1024 * 1024 * 1024) == 0L) && (limit.dataLimit > 0)) (limit.dataLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (limitUnit, setLimitUnit) = remember(limit) { 
        mutableStateOf(if (limit.dataLimit >= 1024 * 1024 * 1024 && limit.dataLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }
    val (limitType, setLimitType) = remember(limit) { mutableStateOf(limit.limitType) }
    val (networkType, setNetworkType) = remember(limit) { mutableStateOf(limit.networkType) }

    val (wifiLimitInput, setWifiLimitInput) = remember(limit) {
        val mb = limit.wifiDataLimit / (1024 * 1024)
        mutableStateOf(if (limit.wifiDataLimit % (1024 * 1024 * 1024) == 0L && limit.wifiDataLimit > 0) (limit.wifiDataLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (wifiLimitUnit, setWifiLimitUnit) = remember(limit) {
        mutableStateOf(if (limit.wifiDataLimit >= 1024 * 1024 * 1024 && limit.wifiDataLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val (mobileLimitInput, setMobileLimitInput) = remember(limit) {
        val mb = limit.mobileDataLimit / (1024 * 1024)
        mutableStateOf(if (limit.mobileDataLimit % (1024 * 1024 * 1024) == 0L && limit.mobileDataLimit > 0) (limit.mobileDataLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (mobileLimitUnit, setMobileLimitUnit) = remember(limit) {
        mutableStateOf(if (limit.mobileDataLimit >= 1024 * 1024 * 1024 && limit.mobileDataLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val context = LocalContext.current
    val appIcon = remember(limit.packageName) {
        try {
            context.packageManager.getApplicationIcon(limit.packageName)
        } catch (_: Exception) {
            null
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_edit_app_limit), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        // Reusing the same UI structure for configuration
        Column(modifier = Modifier.padding(padding)) {
            ConfigurationContent(
                selectedAppHeader = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(Modifier.padding(12.dp)) {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(limit.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            Text(limit.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                limitInput = limitInput,
                onLimitInputChange = setLimitInput,
                limitUnit = limitUnit,
                onLimitUnitChange = setLimitUnit,
                limitType = limitType,
                onLimitTypeChange = setLimitType,
                networkType = networkType,
                onNetworkTypeChange = setNetworkType,
                wifiLimitInput = wifiLimitInput,
                onWifiLimitInputChange = setWifiLimitInput,
                wifiLimitUnit = wifiLimitUnit,
                onWifiLimitUnitChange = setWifiLimitUnit,
                mobileLimitInput = mobileLimitInput,
                onMobileLimitInputChange = setMobileLimitInput,
                mobileLimitUnit = mobileLimitUnit,
                onMobileLimitUnitChange = setMobileLimitUnit,
                onConfirm = {
                    val value = limitInput.toLongOrNull() ?: 0L
                    val multiplier = if (limitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L
                    
                    val wifiValue = wifiLimitInput.toLongOrNull() ?: 0L
                    val wifiMultiplier = if (wifiLimitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L
                    
                    val mobileValue = mobileLimitInput.toLongOrNull() ?: 0L
                    val mobileMultiplier = if (mobileLimitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                    onConfirm(limit.copy(
                        dataLimit = value * multiplier,
                        limitType = limitType,
                        networkType = networkType,
                        wifiDataLimit = wifiValue * wifiMultiplier,
                        mobileDataLimit = mobileValue * mobileMultiplier,
                        isBlocked = false,
                        isWifiBlocked = false,
                        isMobileBlocked = false
                    ))
                }
            )
        }
    }
}

@Composable
fun ConfigurationContent(
    selectedApp: AppLimitsViewModel.AppInfo? = null,
    selectedAppHeader: @Composable (() -> Unit)? = null,
    limitInput: String,
    onLimitInputChange: (String) -> Unit,
    limitUnit: String,
    onLimitUnitChange: (String) -> Unit,
    limitType: String,
    onLimitTypeChange: (String) -> Unit,
    networkType: String,
    onNetworkTypeChange: (String) -> Unit,
    wifiLimitInput: String = "100",
    onWifiLimitInputChange: (String) -> Unit = {},
    wifiLimitUnit: String = "MB",
    onWifiLimitUnitChange: (String) -> Unit = {},
    mobileLimitInput: String = "50",
    onMobileLimitInputChange: (String) -> Unit = {},
    mobileLimitUnit: String = "MB",
    onMobileLimitUnitChange: (String) -> Unit = {},
    confirmButtonText: String = stringResource(R.string.btn_create_app_limit),
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (selectedAppHeader != null) {
            selectedAppHeader()
        } else if (selectedApp != null) {
            val context = LocalContext.current
            val appIcon = remember(selectedApp.packageName) {
                try {
                    context.packageManager.getApplicationIcon(selectedApp.packageName)
                } catch (_: Exception) {
                    null
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(Modifier.padding(12.dp)) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(selectedApp.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(selectedApp.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LimitConfigurationContent(
            limitInput = limitInput,
            onLimitInputChange = { onLimitInputChange(it) },
            limitUnit = limitUnit,
            onLimitUnitChange = { onLimitUnitChange(it) },
            limitType = limitType,
            onLimitTypeChange = { onLimitTypeChange(it) },
            networkType = networkType,
            onNetworkTypeChange = { onNetworkTypeChange(it) },
            wifiLimitInput = wifiLimitInput,
            onWifiLimitInputChange = onWifiLimitInputChange,
            wifiLimitUnit = wifiLimitUnit,
            onWifiLimitUnitChange = onWifiLimitUnitChange,
            mobileLimitInput = mobileLimitInput,
            onMobileLimitInputChange = onMobileLimitInputChange,
            mobileLimitUnit = mobileLimitUnit,
            onMobileLimitUnitChange = onMobileLimitUnitChange
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(confirmButtonText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LimitConfigurationContent(
    limitInput: String,
    onLimitInputChange: (String) -> Unit,
    limitUnit: String,
    onLimitUnitChange: (String) -> Unit,
    limitType: String,
    onLimitTypeChange: (String) -> Unit,
    networkType: String,
    onNetworkTypeChange: (String) -> Unit,
    wifiLimitInput: String = "100",
    onWifiLimitInputChange: (String) -> Unit = {},
    wifiLimitUnit: String = "MB",
    onWifiLimitUnitChange: (String) -> Unit = {},
    mobileLimitInput: String = "50",
    onMobileLimitInputChange: (String) -> Unit = {},
    mobileLimitUnit: String = "MB",
    onMobileLimitUnitChange: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Network Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkChip(selected = networkType == "both", onClick = { onNetworkTypeChange("both") }, label = stringResource(R.string.label_both), icon = Icons.Rounded.Language)
                NetworkChip(selected = networkType == "wifi", onClick = { onNetworkTypeChange("wifi") }, label = stringResource(R.string.label_wifi), icon = Icons.Rounded.Wifi)
                NetworkChip(selected = networkType == "mobile", onClick = { onNetworkTypeChange("mobile") }, label = stringResource(R.string.label_mobile), icon = Icons.Rounded.SignalCellularAlt)
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        if (networkType == "both") {
            Text("Wi-Fi Data Limit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LimitInputRow(
                value = wifiLimitInput,
                onValueChange = onWifiLimitInputChange,
                unit = wifiLimitUnit,
                onUnitChange = onWifiLimitUnitChange
            )
            
            Spacer(modifier = Modifier.height(28.dp))

            Text("Mobile Data Limit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LimitInputRow(
                value = mobileLimitInput,
                onValueChange = onMobileLimitInputChange,
                unit = mobileLimitUnit,
                onUnitChange = onMobileLimitUnitChange
            )
        } else {
            Text("${if (networkType == "wifi") "Wi-Fi" else "Mobile"} Data Limit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LimitInputRow(
                value = limitInput,
                onValueChange = onLimitInputChange,
                unit = limitUnit,
                onUnitChange = onLimitUnitChange
            )
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text("Limit Period", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PeriodChip(selected = limitType == "daily", onClick = { onLimitTypeChange("daily") }, label = stringResource(R.string.filter_daily))
            PeriodChip(selected = limitType == "monthly", onClick = { onLimitTypeChange("monthly") }, label = stringResource(R.string.filter_monthly))
        }
    }
}

@Composable
fun LimitInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                var expanded by remember { mutableStateOf(value = false) }
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(unit, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        DropdownMenuItem(
                            text = { Text("MB", fontWeight = FontWeight.Bold) },
                            onClick = { onUnitChange("MB"); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("GB", fontWeight = FontWeight.Bold) },
                            onClick = { onUnitChange("GB"); expanded = false }
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        shape = RoundedCornerShape(12.dp),
        leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkChip(selected: Boolean, onClick: () -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun LimitConfigurationContentPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            LimitConfigurationContent(
                limitInput = "100",
                onLimitInputChange = {},
                limitUnit = "MB",
                onLimitUnitChange = {},
                limitType = "daily",
                onLimitTypeChange = {},
                networkType = "both",
                onNetworkTypeChange = {}
            )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeneralLimitConfigScreen(
    initialDataDailyLimit: Long,
    initialWifiDailyLimit: Long,
    initialDataMonthlyLimit: Long,
    initialWifiMonthlyLimit: Long,
    onBack: () -> Unit,
    onConfirm: (String, String, Long, Long, Long, Long) -> Unit
) {
    val (networkType, setNetworkType) = remember { mutableStateOf("both") }
    val (limitPeriod, setLimitPeriod) = remember { mutableStateOf("both") }

    val (dataDailyInput, setDataDailyInput) = remember {
        val mb = initialDataDailyLimit / (1024 * 1024)
        mutableStateOf(if (initialDataDailyLimit % (1024 * 1024 * 1024) == 0L && initialDataDailyLimit > 0) (initialDataDailyLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (dataDailyUnit, setDataDailyUnit) = remember {
        mutableStateOf(if (initialDataDailyLimit >= 1024 * 1024 * 1024 && initialDataDailyLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val (wifiDailyInput, setWifiDailyInput) = remember {
        val mb = initialWifiDailyLimit / (1024 * 1024)
        mutableStateOf(if (initialWifiDailyLimit % (1024 * 1024 * 1024) == 0L && initialWifiDailyLimit > 0) (initialWifiDailyLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (wifiDailyUnit, setWifiDailyUnit) = remember {
        mutableStateOf(if (initialWifiDailyLimit >= 1024 * 1024 * 1024 && initialWifiDailyLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val (dataMonthlyInput, setDataMonthlyInput) = remember {
        val mb = initialDataMonthlyLimit / (1024 * 1024)
        mutableStateOf(if (initialDataMonthlyLimit % (1024 * 1024 * 1024) == 0L && initialDataMonthlyLimit > 0) (initialDataMonthlyLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (dataMonthlyUnit, setDataMonthlyUnit) = remember {
        mutableStateOf(if (initialDataMonthlyLimit >= 1024 * 1024 * 1024 && initialDataMonthlyLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val (wifiMonthlyInput, setWifiMonthlyInput) = remember {
        val mb = initialWifiMonthlyLimit / (1024 * 1024)
        mutableStateOf(if (initialWifiMonthlyLimit % (1024 * 1024 * 1024) == 0L && initialWifiMonthlyLimit > 0) (initialWifiMonthlyLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (wifiMonthlyUnit, setWifiMonthlyUnit) = remember {
        mutableStateOf(if (initialWifiMonthlyLimit >= 1024 * 1024 * 1024 && initialWifiMonthlyLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_general_limits), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Set your device-wide data plans.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(32.dp))

            Text("Network Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkChip(selected = networkType == "both", onClick = { setNetworkType("both") }, label = stringResource(R.string.label_both), icon = Icons.Rounded.Language)
                NetworkChip(selected = networkType == "wifi", onClick = { setNetworkType("wifi") }, label = stringResource(R.string.label_wifi), icon = Icons.Rounded.Wifi)
                NetworkChip(selected = networkType == "mobile", onClick = { setNetworkType("mobile") }, label = stringResource(R.string.label_mobile), icon = Icons.Rounded.SignalCellularAlt)
            }
            
            Spacer(Modifier.height(28.dp))

            Text("Limit Period", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PeriodChip(selected = limitPeriod == "both", onClick = { setLimitPeriod("both") }, label = stringResource(R.string.label_both))
                PeriodChip(selected = limitPeriod == "daily", onClick = { setLimitPeriod("daily") }, label = stringResource(R.string.filter_daily))
                PeriodChip(selected = limitPeriod == "monthly", onClick = { setLimitPeriod("monthly") }, label = stringResource(R.string.filter_monthly))
            }

            Spacer(Modifier.height(32.dp))

            if (limitPeriod == "daily" || limitPeriod == "both") {
                if (networkType == "wifi" || networkType == "both") {
                    Text("Daily Wi-Fi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    LimitInputRow(value = wifiDailyInput, onValueChange = setWifiDailyInput, unit = wifiDailyUnit, onUnitChange = setWifiDailyUnit)
                    Spacer(Modifier.height(16.dp))
                }

                if (networkType == "mobile" || networkType == "both") {
                    Text("Daily Mobile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    LimitInputRow(value = dataDailyInput, onValueChange = setDataDailyInput, unit = dataDailyUnit, onUnitChange = setDataDailyUnit)
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (limitPeriod == "monthly" || limitPeriod == "both") {
                if (networkType == "wifi" || networkType == "both") {
                    Text("Monthly Wi-Fi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    LimitInputRow(value = wifiMonthlyInput, onValueChange = setWifiMonthlyInput, unit = wifiMonthlyUnit, onUnitChange = setWifiMonthlyUnit)
                    Spacer(Modifier.height(16.dp))
                }

                if (networkType == "mobile" || networkType == "both") {
                    Text("Monthly Mobile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    LimitInputRow(value = dataMonthlyInput, onValueChange = setDataMonthlyInput, unit = dataMonthlyUnit, onUnitChange = setDataMonthlyUnit)
                    Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    fun parse(input: String, unit: String): Long {
                        val v = input.toLongOrNull() ?: 0L
                        val multiplier = if (unit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L
                        return v * multiplier
                    }
                    onConfirm(
                        networkType,
                        limitPeriod,
                        parse(dataDailyInput, dataDailyUnit),
                        parse(wifiDailyInput, wifiDailyUnit),
                        parse(dataMonthlyInput, dataMonthlyUnit),
                        parse(wifiMonthlyInput, wifiMonthlyUnit)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(stringResource(R.string.btn_save_config), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


private fun formatUsage(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.1f KB", kb)
    }
}

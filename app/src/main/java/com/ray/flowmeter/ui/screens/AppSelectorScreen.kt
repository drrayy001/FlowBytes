// Application selection screen used when configuring new individual app limits.
// Lists installed packages with filtering and search capabilities.
package com.ray.flowmeter.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import androidx.compose.foundation.BorderStroke
import com.ray.flowmeter.data.AppLimit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    viewModel: AppLimitsViewModel,
    onBack: () -> Unit,
    onAppsSelected: (List<AppLimit>) -> Unit
) {
    val selectedApps = remember { mutableStateListOf<AppLimitsViewModel.AppInfo>() }
    var isConfigSheetOpen by remember { mutableStateOf(false) }

    BackHandler {
        if (isConfigSheetOpen) {
            isConfigSheetOpen = false
        } else {
            onBack()
        }
    }

    if (isConfigSheetOpen && selectedApps.isNotEmpty()) {
        BatchConfigurationScreen(
            selectedApps = selectedApps,
            onBack = { isConfigSheetOpen = false },
            onConfirm = { limits ->
                onAppsSelected(limits)
                isConfigSheetOpen = false
                selectedApps.clear()
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_select_application),
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().consumeWindowInsets(padding)) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
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
                                        contentDescription = stringResource(R.string.cd_clear_search)
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
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                itemsIndexed(filtered, key = { _, it -> it.packageName }) { index, app ->
                                    val appIcon by produceState<Drawable?>(
                                        initialValue = null,
                                        key1 = app.packageName
                                    ) {
                                        value = viewModel.getAppIcon(app.packageName)
                                    }
                                    val isSelected = selectedApps.contains(app)

                                    StaggeredEntrance(index = index) {
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
                                            trailingContent = {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            selectedApps.add(app)
                                                        } else {
                                                            selectedApps.remove(app)
                                                        }
                                                    }
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                if (isSelected) {
                                                    selectedApps.remove(app)
                                                } else {
                                                    selectedApps.add(app)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Modern Floating Dock
                if (selectedApps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        StaggeredEntrance {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = { selectedApps.clear() },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.cd_clear_selection),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.label_selected_count, selectedApps.size),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.label_app_limits_pending),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { isConfigSheetOpen = true },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                        modifier = Modifier.wrapContentWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.btn_configure),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchConfigurationScreen(
    selectedApps: List<AppLimitsViewModel.AppInfo>,
    onBack: () -> Unit,
    onConfirm: (List<AppLimit>) -> Unit
) {
    var triggerConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_configure_limits),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                BatchConfigurationContent(
                    selectedApps = selectedApps,
                    onCancel = onBack,
                    onConfirm = onConfirm,
                    onRegisterConfirmTrigger = { triggerConfirm = it }
                )
            }

            // Modern Floating Bottom Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onBack,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.btn_cancel),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { triggerConfirm?.invoke() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.btn_create_app_limit),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BatchConfigurationContent(
    selectedApps: List<AppLimitsViewModel.AppInfo>,
    onCancel: () -> Unit,
    onConfirm: (List<AppLimit>) -> Unit,
    onRegisterConfirmTrigger: (() -> Unit) -> Unit = {}
) {
    var networkType by remember { mutableStateOf("both") }
    var limitType by remember { mutableStateOf("daily") }

    // Defaults
    var defaultLimitInput by remember { mutableStateOf("100") }
    var defaultLimitUnit by remember { mutableStateOf("MB") }
    var defaultWifiLimitInput by remember { mutableStateOf("100") }
    var defaultWifiLimitUnit by remember { mutableStateOf("MB") }
    var defaultMobileLimitInput by remember { mutableStateOf("50") }
    var defaultMobileLimitUnit by remember { mutableStateOf("MB") }

    // Map-based states for per-app overrides
    val appNetworkTypes = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "both" }
        }
    }
    val appLimitTypes = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "daily" }
        }
    }
    val appLimitsInput = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "100" }
        }
    }
    val appLimitsUnit = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "MB" }
        }
    }
    val appWifiLimitsInput = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "100" }
        }
    }
    val appWifiLimitsUnit = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "MB" }
        }
    }
    val appMobileLimitsInput = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "50" }
        }
    }
    val appMobileLimitsUnit = remember(selectedApps) {
        mutableStateMapOf<String, String>().apply {
            selectedApps.forEach { this[it.packageName] = "MB" }
        }
    }

    val currentConfirmTrigger = remember {
        {
            val limitsList = selectedApps.map { app ->
                val appNetType = appNetworkTypes[app.packageName] ?: "both"
                val appLimType = appLimitTypes[app.packageName] ?: "daily"

                val wifiVal = appWifiLimitsInput[app.packageName]?.toLongOrNull() ?: 0L
                val wifiMult = if (appWifiLimitsUnit[app.packageName] == "GB") 1024L * 1024L * 1024L else 1024L * 1024L
                
                val mobileVal = appMobileLimitsInput[app.packageName]?.toLongOrNull() ?: 0L
                val mobileMult = if (appMobileLimitsUnit[app.packageName] == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                val singleVal = appLimitsInput[app.packageName]?.toLongOrNull() ?: 0L
                val singleMult = if (appLimitsUnit[app.packageName] == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                AppLimit(
                    packageName = app.packageName,
                    appName = app.name,
                    dataLimit = if (appNetType != "both") singleVal * singleMult else 0L,
                    limitType = appLimType,
                    networkType = appNetType,
                    wifiDataLimit = if (appNetType == "both") wifiVal * wifiMult else if (appNetType == "wifi") singleVal * singleMult else 0L,
                    mobileDataLimit = if (appNetType == "both") mobileVal * mobileMult else if (appNetType == "mobile") singleVal * singleMult else 0L
                )
            }
            onConfirm(limitsList)
        }
    }
    
    SideEffect {
        onRegisterConfirmTrigger(currentConfirmTrigger)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.desc_configure_limits_count, selectedApps.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Global Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.title_global_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.desc_global_settings_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(stringResource(R.string.label_network_type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NetworkChip(
                        selected = networkType == "both",
                        onClick = {
                            networkType = "both"
                            selectedApps.forEach { appNetworkTypes[it.packageName] = "both" }
                        },
                        label = stringResource(R.string.label_both),
                        icon = Icons.Rounded.Language
                    )
                    NetworkChip(
                        selected = networkType == "wifi",
                        onClick = {
                            networkType = "wifi"
                            selectedApps.forEach { appNetworkTypes[it.packageName] = "wifi" }
                        },
                        label = stringResource(R.string.label_wifi),
                        icon = Icons.Rounded.Wifi
                    )
                    NetworkChip(
                        selected = networkType == "mobile",
                        onClick = {
                            networkType = "mobile"
                            selectedApps.forEach { appNetworkTypes[it.packageName] = "mobile" }
                        },
                        label = stringResource(R.string.label_mobile),
                        icon = Icons.Rounded.SignalCellularAlt
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.label_limit_period), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodChip(
                        selected = limitType == "daily",
                        onClick = {
                            limitType = "daily"
                            selectedApps.forEach { appLimitTypes[it.packageName] = "daily" }
                        },
                        label = stringResource(R.string.filter_daily)
                    )
                    PeriodChip(
                        selected = limitType == "monthly",
                        onClick = {
                            limitType = "monthly"
                            selectedApps.forEach { appLimitTypes[it.packageName] = "monthly" }
                        },
                        label = stringResource(R.string.filter_monthly)
                    )
                }
            }
        }

        // Default Limits quick-fill editor Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.title_default_limits),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.desc_default_limits_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (networkType == "both") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_wifi_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LimitInputRow(
                                value = defaultWifiLimitInput,
                                onValueChange = { valVal ->
                                    defaultWifiLimitInput = valVal
                                    selectedApps.forEach { appWifiLimitsInput[it.packageName] = valVal }
                                },
                                unit = defaultWifiLimitUnit,
                                onUnitChange = { unitVal ->
                                    defaultWifiLimitUnit = unitVal
                                    selectedApps.forEach { appWifiLimitsUnit[it.packageName] = unitVal }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_mobile_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LimitInputRow(
                                value = defaultMobileLimitInput,
                                onValueChange = { valVal ->
                                    defaultMobileLimitInput = valVal
                                    selectedApps.forEach { appMobileLimitsInput[it.packageName] = valVal }
                                },
                                unit = defaultMobileLimitUnit,
                                onUnitChange = { unitVal ->
                                    defaultMobileLimitUnit = unitVal
                                    selectedApps.forEach { appMobileLimitsUnit[it.packageName] = unitVal }
                                }
                            )
                        }
                    }
                } else {
                    val dynamicLimitLabel = if (networkType == "wifi") stringResource(R.string.settings_wifi_limit) else stringResource(R.string.settings_mobile_limit)
                    Text(dynamicLimitLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LimitInputRow(
                        value = defaultLimitInput,
                        onValueChange = { valVal ->
                            defaultLimitInput = valVal
                            selectedApps.forEach { appLimitsInput[it.packageName] = valVal }
                        },
                        unit = defaultLimitUnit,
                        onUnitChange = { unitVal ->
                            defaultLimitUnit = unitVal
                            selectedApps.forEach { appLimitsUnit[it.packageName] = unitVal }
                        }
                    )
                }
            }
        }

        // Individual app override list heading
        Text(
            text = stringResource(R.string.title_individual_app_limits),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.desc_individual_app_limits_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        selectedApps.forEach { app ->
            val context = LocalContext.current
            val appIcon = remember(app.packageName) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (_: Exception) {
                    null
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(Modifier.padding(6.dp)) {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val currentAppNetworkType = appNetworkTypes[app.packageName] ?: "both"
                    val currentAppLimitType = appLimitTypes[app.packageName] ?: "daily"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(stringResource(R.string.label_network_type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                MiniChip(
                                    selected = currentAppNetworkType == "both",
                                    onClick = { appNetworkTypes[app.packageName] = "both" },
                                    label = stringResource(R.string.label_both)
                                )
                                MiniChip(
                                    selected = currentAppNetworkType == "wifi",
                                    onClick = { appNetworkTypes[app.packageName] = "wifi" },
                                    label = stringResource(R.string.label_wifi)
                                )
                                MiniChip(
                                    selected = currentAppNetworkType == "mobile",
                                    onClick = { appNetworkTypes[app.packageName] = "mobile" },
                                    label = stringResource(R.string.label_mobile)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(stringResource(R.string.label_limit_period), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                MiniChip(
                                    selected = currentAppLimitType == "daily",
                                    onClick = { appLimitTypes[app.packageName] = "daily" },
                                    label = stringResource(R.string.filter_daily)
                                )
                                MiniChip(
                                    selected = currentAppLimitType == "monthly",
                                    onClick = { appLimitTypes[app.packageName] = "monthly" },
                                    label = stringResource(R.string.filter_monthly)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (currentAppNetworkType == "both") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_wifi_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                LimitInputRow(
                                    value = appWifiLimitsInput[app.packageName] ?: "100",
                                    onValueChange = { appWifiLimitsInput[app.packageName] = it },
                                    unit = appWifiLimitsUnit[app.packageName] ?: "MB",
                                    onUnitChange = { appWifiLimitsUnit[app.packageName] = it }
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_mobile_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                LimitInputRow(
                                    value = appMobileLimitsInput[app.packageName] ?: "50",
                                    onValueChange = { appMobileLimitsInput[app.packageName] = it },
                                    unit = appMobileLimitsUnit[app.packageName] ?: "MB",
                                    onUnitChange = { appMobileLimitsUnit[app.packageName] = it }
                                )
                            }
                        }
                    } else {
                        val dynamicLabel = if (currentAppNetworkType == "wifi") stringResource(R.string.settings_wifi_limit) else stringResource(R.string.settings_mobile_limit)
                        Text(dynamicLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LimitInputRow(
                            value = appLimitsInput[app.packageName] ?: "100",
                            onValueChange = { appLimitsInput[app.packageName] = it },
                            unit = appLimitsUnit[app.packageName] ?: "MB",
                            onUnitChange = { appLimitsUnit[app.packageName] = it }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun MiniChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.height(28.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
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
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(Modifier.padding(10.dp)) {
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
                    Text(selectedApp.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = confirmButtonText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
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
        Text(stringResource(R.string.label_network_type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkChip(selected = networkType == "both", onClick = { onNetworkTypeChange("both") }, label = stringResource(R.string.label_both), icon = Icons.Rounded.Language)
                NetworkChip(selected = networkType == "wifi", onClick = { onNetworkTypeChange("wifi") }, label = stringResource(R.string.label_wifi), icon = Icons.Rounded.Wifi)
                NetworkChip(selected = networkType == "mobile", onClick = { onNetworkTypeChange("mobile") }, label = stringResource(R.string.label_mobile), icon = Icons.Rounded.SignalCellularAlt)
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (networkType == "both") {
            Text(stringResource(R.string.settings_wifi_limit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LimitInputRow(
                value = wifiLimitInput,
                onValueChange = onWifiLimitInputChange,
                unit = wifiLimitUnit,
                onUnitChange = onWifiLimitUnitChange
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(stringResource(R.string.settings_mobile_limit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LimitInputRow(
                value = mobileLimitInput,
                onValueChange = onMobileLimitInputChange,
                unit = mobileLimitUnit,
                onUnitChange = onMobileLimitUnitChange
            )
        } else {
            val dynamicLimitLabel = if (networkType == "wifi") stringResource(R.string.settings_wifi_limit) else stringResource(R.string.settings_mobile_limit)
            Text(dynamicLimitLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LimitInputRow(
                value = limitInput,
                onValueChange = onLimitInputChange,
                unit = limitUnit,
                onUnitChange = onLimitUnitChange
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(stringResource(R.string.label_limit_period), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PeriodChip(selected = limitType == "daily", onClick = { onLimitTypeChange("daily") }, label = stringResource(R.string.filter_daily))
            PeriodChip(selected = limitType == "monthly", onClick = { onLimitTypeChange("monthly") }, label = stringResource(R.string.filter_monthly))
        }
    }
}

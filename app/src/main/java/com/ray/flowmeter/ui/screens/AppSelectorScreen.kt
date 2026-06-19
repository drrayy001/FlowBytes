package com.ray.flowmeter.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                                    value = viewModel.getAppIcon(app.packageName)
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

package com.ray.flowmeter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AppLimit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitEditScreen(
    limit: AppLimit,
    onBack: () -> Unit,
    onConfirm: (AppLimit) -> Unit,
) {
    val (limitInput, setLimitInput) = remember(limit) {
        val mb = limit.dataLimit / (1024 * 1024)
        mutableStateOf(if (((limit.dataLimit % (1024 * 1024 * 1024)) == 0L) && (limit.dataLimit > 0)) (limit.dataLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (limitUnit, setLimitUnit) = remember(limit) {
        mutableStateOf(if ((limit.dataLimit >= 1024L * 1024L * 1024L) && (limit.dataLimit % (1024L * 1024L * 1024L) == 0L)) "GB" else "MB")
    }
    val (limitType, setLimitType) = remember(limit) { mutableStateOf(limit.limitType) }
    val (networkType, setNetworkType) = remember(limit) { mutableStateOf(limit.networkType) }

    val (wifiLimitInput, setWifiLimitInput) = remember(limit) {
        val mb = limit.wifiDataLimit / (1024 * 1024)
        mutableStateOf(if (((limit.wifiDataLimit % (1024L * 1024L * 1024L)) == 0L) && (limit.wifiDataLimit > 0)) (limit.wifiDataLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (wifiLimitUnit, setWifiLimitUnit) = remember(limit) {
        mutableStateOf(if ((limit.wifiDataLimit >= 1024L * 1024L * 1024L) && (limit.wifiDataLimit % (1024L * 1024L * 1024L) == 0L)) "GB" else "MB")
    }

    val (mobileLimitInput, setMobileLimitInput) = remember(limit) {
        val mb = limit.mobileDataLimit / (1024 * 1024)
        mutableStateOf(if (((limit.mobileDataLimit % (1024L * 1024L * 1024L)) == 0L) && (limit.mobileDataLimit > 0)) (limit.mobileDataLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (mobileLimitUnit, setMobileLimitUnit) = remember(limit) {
        mutableStateOf(if ((limit.mobileDataLimit >= 1024L * 1024L * 1024L) && (limit.mobileDataLimit % (1024L * 1024L * 1024L) == 0L)) "GB" else "MB")
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
                title = { Text(stringResource(R.string.title_edit_app_limit), fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
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
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(Modifier.padding(12.dp)) {
                                appIcon?.let {
                                    Image(
                                        bitmap = it.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
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
            ) {
                val value = limitInput.toLongOrNull() ?: 0L
                val multiplier = if (limitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                val wifiValue = wifiLimitInput.toLongOrNull() ?: 0L
                val wifiMultiplier = if (wifiLimitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                val mobileValue = mobileLimitInput.toLongOrNull() ?: 0L
                val mobileMultiplier = if (mobileLimitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L

                onConfirm(
                    limit.copy(
                        dataLimit = value * multiplier,
                        limitType = limitType,
                        networkType = networkType,
                        wifiDataLimit = wifiValue * wifiMultiplier,
                        mobileDataLimit = mobileValue * mobileMultiplier,
                        isBlocked = false,
                        isWifiBlocked = false,
                        isMobileBlocked = false,
                    )
                )
            }
        }
    }
}

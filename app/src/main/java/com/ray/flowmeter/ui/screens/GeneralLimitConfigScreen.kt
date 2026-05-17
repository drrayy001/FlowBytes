package com.ray.flowmeter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ray.flowmeter.R
import androidx.compose.foundation.shape.RoundedCornerShape

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

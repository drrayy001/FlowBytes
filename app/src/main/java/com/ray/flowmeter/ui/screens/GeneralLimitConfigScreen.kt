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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ray.flowmeter.R
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Select Date"
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialSelectedDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis ?: System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeneralLimitConfigScreen(
    initialDataDailyLimit: Long,
    initialWifiDailyLimit: Long,
    initialDataMonthlyLimit: Long,
    initialWifiMonthlyLimit: Long,
    initialDataCustomLimit: Long,
    initialWifiCustomLimit: Long,
    initialDataCustomStart: Long,
    initialDataCustomEnd: Long,
    initialWifiCustomStart: Long,
    initialWifiCustomEnd: Long,
    onBack: () -> Unit,
    onConfirm: (
        networkType: String,
        limitPeriod: String,
        dataDailyLimit: Long,
        wifiDailyLimit: Long,
        dataMonthlyLimit: Long,
        wifiMonthlyLimit: Long,
        dataCustomLimit: Long,
        wifiCustomLimit: Long,
        dataCustomStart: Long,
        dataCustomEnd: Long,
        wifiCustomStart: Long,
        wifiCustomEnd: Long
    ) -> Unit
) {
    val (networkType, setNetworkType) = remember { mutableStateOf("both") }
    val (limitPeriod, setLimitPeriod) = remember {
        val initialPeriod = when {
            initialDataCustomLimit > 0 || initialWifiCustomLimit > 0 -> "custom"
            initialDataMonthlyLimit > 0 || initialWifiMonthlyLimit > 0 -> "monthly"
            else -> "daily"
        }
        mutableStateOf(initialPeriod)
    }

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

    val (dataCustomInput, setDataCustomInput) = remember {
        val mb = initialDataCustomLimit / (1024 * 1024)
        mutableStateOf(if (initialDataCustomLimit % (1024 * 1024 * 1024) == 0L && initialDataCustomLimit > 0) (initialDataCustomLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (dataCustomUnit, setDataCustomUnit) = remember {
        mutableStateOf(if (initialDataCustomLimit >= 1024 * 1024 * 1024 && initialDataCustomLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val (wifiCustomInput, setWifiCustomInput) = remember {
        val mb = initialWifiCustomLimit / (1024 * 1024)
        mutableStateOf(if (initialWifiCustomLimit % (1024 * 1024 * 1024) == 0L && initialWifiCustomLimit > 0) (initialWifiCustomLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
    }
    val (wifiCustomUnit, setWifiCustomUnit) = remember {
        mutableStateOf(if (initialWifiCustomLimit >= 1024 * 1024 * 1024 && initialWifiCustomLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    var dataCustomStart by remember { mutableStateOf(if (initialDataCustomStart > 0) initialDataCustomStart else System.currentTimeMillis()) }
    var dataCustomEnd by remember { mutableStateOf(if (initialDataCustomEnd > 0) initialDataCustomEnd else System.currentTimeMillis()) }

    var wifiCustomStart by remember { mutableStateOf(if (initialWifiCustomStart > 0) initialWifiCustomStart else System.currentTimeMillis()) }
    var wifiCustomEnd by remember { mutableStateOf(if (initialWifiCustomEnd > 0) initialWifiCustomEnd else System.currentTimeMillis()) }

    var activeDatePicker by remember { mutableStateOf<String?>(null) }

    val isWifiDateRangeInvalid = (limitPeriod == "custom") && (networkType == "wifi" || networkType == "both") && (wifiCustomEnd < wifiCustomStart)
    val isMobileDateRangeInvalid = (limitPeriod == "custom") && (networkType == "mobile" || networkType == "both") && (dataCustomEnd < dataCustomStart)
    val isFormInvalid = isWifiDateRangeInvalid || isMobileDateRangeInvalid

    if (activeDatePicker != null) {
        val initialDate = when (activeDatePicker) {
            "data_start" -> dataCustomStart
            "data_end" -> dataCustomEnd
            "wifi_start" -> wifiCustomStart
            "wifi_end" -> wifiCustomEnd
            else -> System.currentTimeMillis()
        }
        SimpleDatePickerDialog(
            initialSelectedDateMillis = initialDate,
            onDismiss = { activeDatePicker = null },
            onDateSelected = { selectedDate ->
                when (activeDatePicker) {
                    "data_start" -> dataCustomStart = selectedDate
                    "data_end" -> dataCustomEnd = selectedDate
                    "wifi_start" -> wifiCustomStart = selectedDate
                    "wifi_end" -> wifiCustomEnd = selectedDate
                }
            }
        )
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
                PeriodChip(selected = limitPeriod == "daily", onClick = { setLimitPeriod("daily") }, label = stringResource(R.string.filter_daily))
                PeriodChip(selected = limitPeriod == "monthly", onClick = { setLimitPeriod("monthly") }, label = stringResource(R.string.filter_monthly))
                PeriodChip(selected = limitPeriod == "custom", onClick = { setLimitPeriod("custom") }, label = stringResource(R.string.filter_custom))
            }

            Spacer(Modifier.height(32.dp))

            if (isFormInvalid) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.msg_invalid_date_range),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

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

            if (limitPeriod == "custom") {
                if (networkType == "wifi" || networkType == "both") {
                    Text("Custom Wi-Fi Limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LimitInputRow(value = wifiCustomInput, onValueChange = setWifiCustomInput, unit = wifiCustomUnit, onUnitChange = setWifiCustomUnit)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { activeDatePicker = "wifi_start" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_start_date_select, formatDate(wifiCustomStart)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { activeDatePicker = "wifi_end" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_end_date_select, formatDate(wifiCustomEnd)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (networkType == "mobile" || networkType == "both") {
                    Text("Custom Mobile Limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LimitInputRow(value = dataCustomInput, onValueChange = setDataCustomInput, unit = dataCustomUnit, onUnitChange = setDataCustomUnit)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { activeDatePicker = "data_start" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_start_date_select, formatDate(dataCustomStart)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { activeDatePicker = "data_end" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_end_date_select, formatDate(dataCustomEnd)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
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
                        parse(wifiMonthlyInput, wifiMonthlyUnit),
                        parse(dataCustomInput, dataCustomUnit),
                        parse(wifiCustomInput, wifiCustomUnit),
                        dataCustomStart,
                        dataCustomEnd,
                        wifiCustomStart,
                        wifiCustomEnd
                    )
                },
                enabled = !isFormInvalid,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(stringResource(R.string.btn_save_config), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

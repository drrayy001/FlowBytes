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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralLimitConfigScreen(
    planType: String,
    initialLimit: Long,
    initialStart: Long,
    initialEnd: Long,
    onBack: () -> Unit,
    onConfirm: (limitBytes: Long, start: Long, end: Long) -> Unit
) {
    val (limitInput, setLimitInput) = remember {
        if (initialLimit <= 0L) {
            mutableStateOf("")
        } else {
            val mb = initialLimit / (1024 * 1024)
            mutableStateOf(if (initialLimit % (1024 * 1024 * 1024) == 0L) (initialLimit / (1024 * 1024 * 1024)).toString() else mb.toString())
        }
    }
    val (limitUnit, setLimitUnit) = remember {
        mutableStateOf(if (initialLimit >= 1024 * 1024 * 1024 && initialLimit % (1024 * 1024 * 1024) == 0L) "GB" else "MB")
    }

    val isCustom = planType.startsWith("custom")
    var customStart by remember { mutableStateOf(if (initialStart > 0) initialStart else System.currentTimeMillis()) }
    var customEnd by remember { mutableStateOf(if (initialEnd > 0) initialEnd else System.currentTimeMillis()) }

    var activeDatePicker by remember { mutableStateOf<String?>(null) }

    val isDateRangeInvalid = isCustom && (customEnd < customStart)
    val isFormInvalid = isDateRangeInvalid || limitInput.isBlank() || (limitInput.toLongOrNull() ?: 0L) <= 0L

    if (activeDatePicker != null) {
        val initialDate = when (activeDatePicker) {
            "start" -> customStart
            "end" -> customEnd
            else -> System.currentTimeMillis()
        }
        SimpleDatePickerDialog(
            initialSelectedDateMillis = initialDate,
            onDismiss = { activeDatePicker = null },
            onDateSelected = { selectedDate ->
                if (activeDatePicker == "start") {
                    customStart = selectedDate
                } else if (activeDatePicker == "end") {
                    customEnd = selectedDate
                }
            }
        )
    }

    val periodText = when {
        planType.startsWith("daily") -> stringResource(R.string.filter_daily)
        planType.startsWith("monthly") -> stringResource(R.string.filter_monthly)
        else -> stringResource(R.string.filter_custom)
    }
    val networkText = when {
        planType.endsWith("wifi") -> stringResource(R.string.label_wifi)
        else -> stringResource(R.string.label_mobile)
    }
    val titleText = "$periodText $networkText"

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, fontWeight = FontWeight.Black) },
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
            Text(
                text = "${stringResource(R.string.title_configure_limit)} for your $titleText plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            if (isDateRangeInvalid) {
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

            Text(
                text = stringResource(R.string.title_configure_limit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            LimitInputRow(
                value = limitInput,
                onValueChange = setLimitInput,
                unit = limitUnit,
                onUnitChange = setLimitUnit
            )
            Spacer(Modifier.height(16.dp))

            if (isCustom) {
                Text(
                    text = stringResource(R.string.label_plan_duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { activeDatePicker = "start" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.label_start_date_select, formatDate(customStart)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = { activeDatePicker = "end" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.label_end_date_select, formatDate(customEnd)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val v = limitInput.toLongOrNull() ?: 0L
                    val multiplier = if (limitUnit == "GB") 1024L * 1024L * 1024L else 1024L * 1024L
                    onConfirm(
                        v * multiplier,
                        if (isCustom) customStart else 0L,
                        if (isCustom) customEnd else 0L
                    )
                },
                enabled = !isFormInvalid,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_save_config),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

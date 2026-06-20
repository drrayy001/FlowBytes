// Settings dialog to customize traffic spike alert thresholds, check intervals, and cool-down limits.
package com.ray.flowmeter.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficSettingsDialog(
    currentSpeed: Long,
    currentTime: Long,
    currentCooldown: Long,
    currentResetTime: Long,
    currentResetSpeed: Long,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, Long, Long) -> Unit
) {
    var speedInput by remember { 
        mutableStateOf(if (currentSpeed >= 1_000_000) (currentSpeed / 1_000_000).toString() else (currentSpeed / 1000).toString()) 
    }
    var speedUnit by remember { 
        mutableStateOf(if (currentSpeed >= 1_000_000) "MB/s" else "KB/s") 
    }
    
    var timeInput by remember { 
        mutableStateOf(if (currentTime >= 60_000) (currentTime / 60_000).toString() else (currentTime / 1000).toString()) 
    }
    var timeUnit by remember { 
        mutableStateOf(if (currentTime >= 60_000) "Min" else "Sec") 
    }

    var cooldownInput by remember { 
        mutableStateOf(if (currentCooldown >= 60_000) (currentCooldown / 60_000).toString() else (currentCooldown / 1000).toString()) 
    }
    var cooldownUnit by remember { 
        mutableStateOf(if (currentCooldown >= 60_000) "Min" else "Sec") 
    }

    var resetTimeInput by remember { 
        mutableStateOf(if (currentResetTime >= 60_000) (currentResetTime / 60_000).toString() else (currentResetTime / 1000).toString()) 
    }
    var resetTimeUnit by remember { 
        mutableStateOf(if (currentResetTime >= 60_000) "Min" else "Sec") 
    }
    
    var resetSpeedInput by remember { 
        mutableStateOf(if (currentResetSpeed >= 1_000_000) (currentResetSpeed / 1_000_000).toString() else (currentResetSpeed / 1000).toString()) 
    }
    var resetSpeedUnit by remember { 
        mutableStateOf(if (currentResetSpeed >= 1_000_000) "MB/s" else "KB/s") 
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        AnimatedDialogContent(onBack = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.title_traffic_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { speedInput = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_speed_threshold)) },
                    trailingIcon = {
                        UnitSelector(
                            selectedUnit = speedUnit,
                            units = listOf("MB/s", "KB/s"),
                            onUnitSelected = { speedUnit = it }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_time_above_threshold)) },
                    trailingIcon = {
                        UnitSelector(
                            selectedUnit = timeUnit,
                            units = listOf("Min", "Sec"),
                            onUnitSelected = { timeUnit = it }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = cooldownInput,
                    onValueChange = { cooldownInput = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_alert_cooldown)) },
                    trailingIcon = {
                        UnitSelector(
                            selectedUnit = cooldownUnit,
                            units = listOf("Min", "Sec"),
                            onUnitSelected = { cooldownUnit = it }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = resetSpeedInput,
                    onValueChange = { resetSpeedInput = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_reset_speed_threshold)) },
                    trailingIcon = {
                        UnitSelector(
                            selectedUnit = resetSpeedUnit,
                            units = listOf("MB/s", "KB/s"),
                            onUnitSelected = { resetSpeedUnit = it }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = resetTimeInput,
                    onValueChange = { resetTimeInput = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_time_below_reset)) },
                    trailingIcon = {
                        UnitSelector(
                            selectedUnit = resetTimeUnit,
                            units = listOf("Min", "Sec"),
                            onUnitSelected = { resetTimeUnit = it }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick {
                            val sVal = speedInput.toLongOrNull() ?: 1L
                            val sMultiplier = if (speedUnit == "MB/s") 1_000_000L else 1_000L
                            
                            val tVal = timeInput.toLongOrNull() ?: 60L
                            val tMultiplier = if (timeUnit == "Min") 60000L else 1000L

                            val cVal = cooldownInput.toLongOrNull() ?: 10L
                            val cMultiplier = if (cooldownUnit == "Min") 60000L else 1000L

                            val rtVal = resetTimeInput.toLongOrNull() ?: 5L
                            val rtMultiplier = if (resetTimeUnit == "Min") 60000L else 1000L
                            
                            val rsVal = resetSpeedInput.toLongOrNull() ?: 200L
                            val rsMultiplier = if (resetSpeedUnit == "MB/s") 1_000_000L else 1_000L

                            onSave(
                                sVal * sMultiplier,
                                tVal * tMultiplier,
                                cVal * cMultiplier,
                                rtVal * rtMultiplier,
                                rsVal * rsMultiplier
                            )
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.btn_save_config),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnitSelector(
    selectedUnit: String,
    units: List<String>,
    onUnitSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(selectedUnit, fontWeight = FontWeight.ExtraBold)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit, fontWeight = FontWeight.Bold) },
                    onClick = { onUnitSelected(unit); expanded = false }
                )
            }
        }
    }
}

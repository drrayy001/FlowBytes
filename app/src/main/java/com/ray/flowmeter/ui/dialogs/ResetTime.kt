// Time picker dialog defining daily usage counter rollover times (defaults to midnight).
package com.ray.flowmeter.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ray.flowmeter.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetTimeDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = currentHour,
        initialMinute = currentMinute,
        is24Hour = true
    )
    var showingPicker by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dialog_reset_time_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showingPicker) {
                    TimePicker(state = state)
                } else {
                    TimeInput(state = state)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showingPicker = !showingPicker }) {
                        Icon(
                            imageVector = if (showingPicker) Icons.Rounded.Keyboard else Icons.Rounded.Schedule,
                            contentDescription = if (showingPicker) "Switch to input mode" else "Switch to picker mode"
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    TextButton(onClick = {
                        val time = LocalTime.of(state.hour, state.minute)
                        val formattedTime = time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
                            .replace("a.m.", "AM")
                            .replace("p.m.", "PM")
                            .replace("am", "AM")
                            .replace("pm", "PM")
                            .uppercase(Locale.ENGLISH)

                        onConfirm(state.hour, state.minute, formattedTime)
                    }) {
                        Text(stringResource(R.string.btn_ok))
                    }
                }
            }
        }
    }
}

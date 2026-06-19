package com.ray.flowmeter.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.components.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFeedbackDialog(
    onDismiss: () -> Unit,
    onTelegramClick: () -> Unit,
    onEmailClick: () -> Unit,
    onReportBugClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AnimatedDialogContent(onBack = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_help_feedback),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_telegram),
                    title = stringResource(R.string.option_telegram),
                    subtitle = stringResource(R.string.desc_telegram),
                    onClick = {
                        onTelegramClick()
                        onDismiss()
                    }
                )

                SettingsItem(
                    icon = Icons.Rounded.Email,
                    title = stringResource(R.string.option_email),
                    subtitle = stringResource(R.string.desc_email),
                    onClick = {
                        onEmailClick()
                        onDismiss()
                    }
                )

                SettingsItem(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.option_report_bug),
                    subtitle = stringResource(R.string.desc_report_bug),
                    onClick = {
                        onReportBugClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}

package com.ray.flowmeter.ui.dialogs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
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
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick { onDismiss() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.btn_done),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

object LegalContent {
    val PRIVACY_POLICY: String
        get() = """
            Privacy Policy for FlowBytes
            Last Updated: ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())}

            1. Overview
            FlowMeter ("we", "us", or "our") respects your privacy. This app is designed to help you monitor your network speed and data usage.

            2. Data Collection
            We DO NOT collect, store, or transmit any personal information, browsing history, or network traffic content. All data processing (speed calculation and usage tracking) happens locally on your device.

            3. Permissions Used
            - Usage Access: Required to calculate data usage per application.
            - Notifications: Used to display the real-time speed meter and usage alerts.
            - Battery Optimization: Required for the background service to provide accurate monitoring.

            4. Data Security
            Since no data is collected or transmitted to our servers, your information remains entirely on your device.

            5. Changes to This Policy
            We may update our Privacy Policy from time to time. You are advised to review this page periodically for any changes.
        """.trimIndent()

    val TERMS_AND_CONDITIONS: String
        get() = """
            Terms and Conditions
            
            1. Acceptance of Terms
            By using FlowMeter, you agree to these terms. If you do not agree, please do not use the app.

            2. Service Disclaimer
            FlowMeter provides network statistics for informational purposes. While we strive for accuracy, speed measurements and data usage calculations are estimates and may vary from your ISP's official billing records.

            3. Battery and Performance
            This app runs a background service to monitor network traffic. While optimized for efficiency, it may have a minor impact on battery life.

            4. Limitation of Liability
            We are not responsible for any data overages, loss of data, or device issues resulting from the use of this application.
        """.trimIndent()

    val LICENSES: String
        get() = """
            Open Source Licenses
            
            - Jetpack Compose: Apache License 2.0
            - Kotlin Coroutines: Apache License 2.0
            - Android Jetpack Libraries (Room, DataStore, Lifecycle): Apache License 2.0
            - Material Components for Android: Apache License 2.0
            - Kotlin Standard Library: Apache License 2.0
            
            This software is provided "as is" without warranty of any kind.
        """.trimIndent()
}

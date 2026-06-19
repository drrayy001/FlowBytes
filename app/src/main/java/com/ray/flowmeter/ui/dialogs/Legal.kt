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
            Privacy Policy
            Effective Date: April 29, 2026

I, the developer of FlowBytes (the "App"), provide this application free of charge. The App is offered as-is and is intended for use without any warranties or guarantees.
            1. Information Collection and Use
            FlowBytes monitors your device's network speeds and data usage (Wi-Fi and cellular) to help you stay in control of your internet. 
            All monitoring, data usage calculations, and blocking actions are performed locally on your device. We do not collect, store, or transmit any personal data, including:
            - Browsing history
            - Network traffic content
            - IP address
            - Any personally identifiable information

            2. Required Permissions
            To function correctly, the App requires the following permissions:
            - Usage Access (Network Stats): Used to access network usage statistics (via NetworkStatsManager and TrafficStats) in order to calculate download/upload speeds and track data usage per app.
            - Foreground Service: Allows the App to run a background service to display real-time network speed in the notification bar and ensure consistent monitoring.
            - VPN Service (Local VPN): FlowBytes uses a local VPN to manage internet access for apps that exceed their limits. This VPN is 100% local; your traffic never leaves your device and is never intercepted by us.

            All data accessed through these permissions remains on your device and is used solely to provide the monitoring and control features within the App.

            3. Third-Party Services
            The App does not use any third-party services, analytics tools, or advertising networks that collect information used to identify you.

            4. Security
            Since all data processing occurs locally on your device, the risk of data breaches from our side is minimal. However, no method of electronic storage or transmission over the internet is completely secure, and we cannot guarantee absolute security.

            5. Children’s Privacy
            This Service is not intended for individuals under the age of 13. We do not knowingly collect personally identifiable information from children under 13.

            6. Changes to This Privacy Policy
            We may update this Privacy Policy from time to time. Any changes will be posted on this page. You are advised to review this page periodically for updates.

            7. Contact Me
            If you have any questions or suggestions about this Privacy Policy, you can contact me at:
            Email: drrayy001@gmail.com
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

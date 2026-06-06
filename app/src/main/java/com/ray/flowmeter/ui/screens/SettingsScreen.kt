package com.ray.flowmeter.ui.screens

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.components.SettingsGroup
import com.ray.flowmeter.ui.components.SettingsItem
import com.ray.flowmeter.ui.dialogs.*
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.ThemeMode
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import com.ray.flowmeter.utils.BillingEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val monitoringEnabled by viewModel.monitoringEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val useMaterialYou by viewModel.useMaterialYou.collectAsState()
    val useAmoled by viewModel.useAmoled.collectAsState()
    val showNotification by viewModel.showNotification.collectAsState()
    val notificationContentType by viewModel.notificationContentType.collectAsState()
    val notificationIconScale by viewModel.notificationIconScale.collectAsState()

    val highPriorityNotification by viewModel.highPriorityNotification.collectAsState()
    val showOnlyWhenConnected by viewModel.showOnlyWhenConnected.collectAsState()
    val appBlockingMasterEnabled by viewModel.appBlockingMasterEnabled.collectAsState()
    val vpnDisclosureAccepted by viewModel.vpnDisclosureAccepted.collectAsState()

    val highTrafficDetectionEnabled by viewModel.highTrafficDetectionEnabled.collectAsState()
    val trafficThresholdSpeed by viewModel.trafficThresholdSpeed.collectAsState()
    val trafficThresholdTime by viewModel.trafficThresholdTime.collectAsState()
    val trafficAlertCooldown by viewModel.trafficAlertCooldown.collectAsState()
    val trafficResetBelowThresholdTime by viewModel.trafficResetBelowThresholdTime.collectAsState()
    val trafficResetSpeed by viewModel.trafficResetSpeed.collectAsState()

    val resetTimeHour by viewModel.resetTimeHour.collectAsState()
    val resetTimeMinute by viewModel.resetTimeMinute.collectAsState()
    val monthlyResetDay by viewModel.monthlyResetDay.collectAsState()

    val accentColor by viewModel.accentColor.collectAsState()

    val isSystemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemInDark
    }

    val switchColors = if (useMaterialYou) {
        SwitchDefaults.colors()
    } else {
        SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = Color.Transparent,
        )
    }

    val thumbContent: @Composable (Boolean) -> Unit = { checked ->
        Icon(
            imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }

    var showAccentColorDialog by remember { mutableStateOf(value = false) }

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    var showThemeDialog by remember { mutableStateOf(value = false) }
    var showNotificationContentDialog by remember { mutableStateOf(value = false) }
    var showIconScaleDialog by remember { mutableStateOf(value = false) }
    var showLicensesDialog by remember { mutableStateOf(value = false) }
    var showPrivacyDialog by remember { mutableStateOf(value = false) }
    var showTermsDialog by remember { mutableStateOf(value = false) }
    var showResetTimeDialog by remember { mutableStateOf(false) }
    var showResetDayDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showHelpFeedbackDialog by remember { mutableStateOf(false) }
    var isDonationSuccess by remember { mutableStateOf(false) }

    var showTrafficSettingsDialog by remember { mutableStateOf(false) }
    var showVpnDisclosure by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.initBilling(context)
    }

    val donationCancelledMessage = stringResource(R.string.msg_donation_cancelled)
    val donationFailedMessage = stringResource(R.string.msg_donation_failed)
    val shareTextTemplate = stringResource(R.string.share_text_body, context.packageName)

    LaunchedEffect(Unit) {
        viewModel.billingEvents.collect { event ->
            when (event) {
                is BillingEvent.Success -> {
                    isDonationSuccess = true
                }
                is BillingEvent.Cancelled -> {
                    snackbarHostState.showSnackbar(donationCancelledMessage)
                }
                is BillingEvent.Error -> {
                    snackbarHostState.showSnackbar(String.format(locale, donationFailedMessage, event.message))
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ================== 1. GENERAL ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_general)) {
                SettingsItem(
                    icon = Icons.Rounded.DataUsage,
                    title = stringResource(R.string.settings_monitoring_toggle),
                    subtitle = stringResource(R.string.settings_monitoring_desc),
                    trailingContent = {
                        Switch(
                            checked = monitoringEnabled == true,
                            onCheckedChange = { viewModel.toggleMonitoring(it) },
                            enabled = monitoringEnabled != null,
                            colors = switchColors,
                            thumbContent = { thumbContent(monitoringEnabled == true) }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.settings_reset_time),
                    subtitle = "${stringResource(R.string.settings_reset_time_desc)} (${
                        LocalTime.of(resetTimeHour, resetTimeMinute)
                            .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
                            .replace("a.m.", "AM")
                            .replace("p.m.", "PM")
                            .replace("am", "AM")
                            .replace("pm", "PM")
                            .uppercase(Locale.ENGLISH)
                    })",
                    onClick = { showResetTimeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Rounded.CalendarMonth,
                    title = stringResource(R.string.settings_monthly_reset_day),
                    subtitle = stringResource(R.string.settings_monthly_reset_day_desc, monthlyResetDay),
                    onClick = { showResetDayDialog = true }
                )
            }
        }

        // ================== 2. APPEARANCE ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_appearance)) {
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_app_theme),
                    subtitle = themeMode,
                    onClick = { showThemeDialog = true }
                )

                SettingsItem(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Material You",
                    subtitle = "Use system accent colors",
                    trailingContent = {
                        Switch(
                            checked = useMaterialYou,
                            onCheckedChange = { viewModel.setUseMaterialYou(it) },
                            colors = switchColors,
                            thumbContent = { thumbContent(useMaterialYou) }
                        )
                    }
                )

                if (!useMaterialYou) {
                    SettingsItem(
                        icon = Icons.Rounded.ColorLens,
                        title = "Accent Color",
                        subtitle = "Choose a custom primary color",
                        onClick = { showAccentColorDialog = true },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    )
                }

                // Only show Amoled if in Dark mode
                if (isDark) {
                    SettingsItem(
                        icon = Icons.Rounded.DarkMode,
                        title = "Amoled Mode",
                        subtitle = "Pitch black background",
                        trailingContent = {
                            Switch(
                                checked = useAmoled,
                                onCheckedChange = { viewModel.setUseAmoled(it) },
                                colors = switchColors,
                                thumbContent = { thumbContent(useAmoled) }
                            )
                        }
                    )
                }

                SettingsItem(
                    icon = Icons.Rounded.FormatSize,
                    title = stringResource(R.string.settings_indicator_size),
                    subtitle = when {
                        notificationIconScale < 1.25f -> "Small"
                        notificationIconScale > 1.32f -> "Large"
                        else -> "Medium"
                    },
                    onClick = { showIconScaleDialog = true }
                )
            }
        }

        // ================== 3. NOTIFICATIONS ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_notifications)) {
                SettingsItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_show_daily_usage),
                    subtitle = stringResource(R.string.settings_daily_usage_desc),
                    trailingContent = {
                        Switch(
                            checked = showNotification,
                            onCheckedChange = { viewModel.toggleNotification(it) },
                            enabled = monitoringEnabled == true,
                            colors = switchColors,
                            thumbContent = { thumbContent(showNotification) }
                        )
                    }
                )
                if (showNotification) {
                    SettingsItem(
                        icon = Icons.Rounded.Dashboard,
                        title = stringResource(R.string.settings_notification_content),
                        subtitle = when (notificationContentType) {
                            "SPEED" -> stringResource(R.string.option_speed_only)
                            "DAILY" -> stringResource(R.string.option_daily_only)
                            else -> stringResource(R.string.option_both)
                        },
                        onClick = if (monitoringEnabled == true) { { showNotificationContentDialog = true } } else null
                    )
                }
                SettingsItem(
                    icon = Icons.Rounded.NotificationImportant,
                    title = stringResource(R.string.settings_high_priority),
                    subtitle = stringResource(R.string.settings_high_priority_desc),
                    trailingContent = {
                        Switch(
                            checked = highPriorityNotification,
                            onCheckedChange = { viewModel.setHighPriorityNotification(it) },
                            enabled = monitoringEnabled == true,
                            colors = switchColors,
                            thumbContent = { thumbContent(highPriorityNotification) }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.WifiTethering,
                    title = stringResource(R.string.settings_hide_offline),
                    subtitle = stringResource(R.string.settings_hide_offline_desc),
                    trailingContent = {
                        Switch(
                            checked = showOnlyWhenConnected,
                            onCheckedChange = { viewModel.setShowOnlyWhenConnected(it) },
                            enabled = monitoringEnabled == true,
                            colors = switchColors,
                            thumbContent = { thumbContent(showOnlyWhenConnected) }
                        )
                    }
                )
            }
        }

        // ================== 4. MONITORING & SECURITY ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_alerts)) {
                SettingsItem(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(R.string.settings_data_alerts),
                    subtitle = stringResource(R.string.settings_data_alerts_desc),
                    trailingContent = {
                        Switch(
                            checked = highTrafficDetectionEnabled,
                            onCheckedChange = { viewModel.setHighTrafficDetectionEnabled(it) },
                            enabled = monitoringEnabled == true,
                            colors = switchColors,
                            thumbContent = { thumbContent(highTrafficDetectionEnabled) }
                        )
                    }
                )

                if (highTrafficDetectionEnabled) {
                    SettingsItem(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.label_advanced_settings),
                        subtitle = "Configure traffic detection thresholds",
                        onClick = { showTrafficSettingsDialog = true }
                    )
                }

                SettingsItem(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.label_block_apps),
                    subtitle = stringResource(R.string.desc_block_apps),
                    trailingContent = {
                        Switch(
                            checked = appBlockingMasterEnabled == true,
                            onCheckedChange = {
                                if (it && !vpnDisclosureAccepted) {
                                    showVpnDisclosure = true
                                } else {
                                    viewModel.toggleAppBlockingMaster(it)
                                }
                            },
                            colors = switchColors,
                            thumbContent = { thumbContent(appBlockingMasterEnabled == true) }
                        )
                    }
                )
            }
        }

        // ================== 6. SUPPORT ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_support)) {
                SettingsItem(
                    icon = Icons.Rounded.Star,
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = stringResource(R.string.settings_rate_app_desc),
                    onClick = {
                        viewModel.markAsReviewed()
                        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()))
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.Share,
                    title = stringResource(R.string.settings_share_app),
                    subtitle = stringResource(R.string.settings_share_app_desc),
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareTextTemplate)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.SupportAgent,
                    title = stringResource(R.string.settings_help_feedback),
                    subtitle = stringResource(R.string.settings_help_feedback_desc),
                    onClick = { showHelpFeedbackDialog = true }
                )
                SettingsItem(
                    icon = Icons.Rounded.Favorite,
                    title = stringResource(R.string.settings_donate),
                    subtitle = stringResource(R.string.settings_donate_desc),
                    onClick = { showDonateDialog = true }
                )
            }
        }

        // ================== 7. ABOUT ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_about)) {
                SettingsItem(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.settings_privacy_policy),
                    onClick = { showPrivacyDialog = true }
                )
                SettingsItem(
                    icon = Icons.Rounded.Gavel,
                    title = stringResource(R.string.settings_terms_conditions),
                    onClick = { showTermsDialog = true }
                )
                SettingsItem(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.settings_licenses),
                    onClick = { showLicensesDialog = true }
                )
                SettingsItem(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_view_source),
                    subtitle = stringResource(R.string.settings_view_source_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/drrayy001/FlowBytes".toUri())
                        context.startActivity(intent)
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version_label),
                    subtitle = versionName,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ================== DIALOGS ==================
        if (showThemeDialog) {
            ThemeDialog(
                currentTheme = themeMode,
                onDismiss = { showThemeDialog = false },
                onSelect = viewModel::setThemeMode
            )
        }

        if (showNotificationContentDialog) {
            NotificationContentDialog(
                currentType = notificationContentType,
                onDismiss = { showNotificationContentDialog = false },
                onSelect = viewModel::setNotificationContentType
            )
        }

        if (showIconScaleDialog) {
            IconScaleDialog(
                currentScale = notificationIconScale,
                onDismiss = { showIconScaleDialog = false },
            ) { viewModel.setNotificationIconScale(it) }
        }

        if (showLicensesDialog) {
            LegalDialog(
                title = stringResource(R.string.settings_licenses),
                content = LegalContent.LICENSES,
            ) { showLicensesDialog = false }
        }

        if (showPrivacyDialog) {
            LegalDialog(
                title = stringResource(R.string.settings_privacy_policy),
                content = LegalContent.PRIVACY_POLICY,
            ) { showPrivacyDialog = false }
        }

        if (showTermsDialog) {
            LegalDialog(
                title = stringResource(R.string.settings_terms_conditions),
                content = LegalContent.TERMS_AND_CONDITIONS,
            ) { showTermsDialog = false }
        }

        if (showResetTimeDialog) {
            ResetTimeDialog(
                currentHour = resetTimeHour,
                currentMinute = resetTimeMinute,
                onDismiss = { showResetTimeDialog = false },
            ) { hour, minute, _ ->
                viewModel.setResetTime(hour, minute)
                showResetTimeDialog = false
            }
        }

        if (showResetDayDialog) {
            ResetDayDialog(
                currentDay = monthlyResetDay,
                onDismiss = { showResetDayDialog = false },
            ) { day ->
                viewModel.setMonthlyResetDay(day)
                showResetDayDialog = false
            }
        }

        if (showDonateDialog) {
            DonateDialog(
                isSuccess = isDonationSuccess,
                onDismiss = {
                    showDonateDialog = false
                    isDonationSuccess = false
                },
            ) { amount ->
                val activity = context as? android.app.Activity
                activity?.let {
                    viewModel.makeDonation(it, amount)
                }
            }
        }

        if (showTrafficSettingsDialog) {
            TrafficSettingsDialog(
                currentSpeed = trafficThresholdSpeed,
                currentTime = trafficThresholdTime,
                currentCooldown = trafficAlertCooldown,
                currentResetTime = trafficResetBelowThresholdTime,
                currentResetSpeed = trafficResetSpeed,
                onDismiss = { showTrafficSettingsDialog = false },
            ) { speed, time, cooldown, resetTime, rSpeed ->
                viewModel.saveTrafficDetectionSettings(speed, time, cooldown, resetTime, rSpeed)
                showTrafficSettingsDialog = false
            }
        }

        if (showVpnDisclosure) {
            VpnDisclosureDialog(
                onDismiss = { showVpnDisclosure = false },
            ) {
                viewModel.setVpnDisclosureAccepted(accepted = true)
                viewModel.toggleAppBlockingMaster(enabled = true)
                showVpnDisclosure = false
            }
        }

        if (showHelpFeedbackDialog) {
            HelpFeedbackDialog(
                onDismiss = { showHelpFeedbackDialog = false },
                onTelegramClick = {
                    val username = "Rayy_TG"
                    val telegramAppIntent = Intent(Intent.ACTION_VIEW, "tg://resolve?domain=$username".toUri()).apply {
                        setPackage("org.telegram.messenger")
                    }
                    try {
                        context.startActivity(telegramAppIntent)
                    } catch (_: Exception) {
                        // Fallback to browser if Telegram app is not installed
                        val browserIntent = Intent(Intent.ACTION_VIEW, "https://t.me/$username".toUri())
                        try {
                            context.startActivity(browserIntent)
                        } catch (__: Exception) {}
                    }
                },
                onEmailClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:drrayy001@gmail.com".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback: FlowBytes (v$versionName)")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )
        }

        if (showAccentColorDialog) {
            AccentColorDialog(
                currentColor = accentColor,
                onDismiss = { showAccentColorDialog = false },
            ) { viewModel.setAccentColor(it) }
        }

    }
}

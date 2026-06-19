package com.ray.flowmeter.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
    val languageCode by viewModel.language.collectAsState()

    val highPriorityNotification by viewModel.highPriorityNotification.collectAsState()
    val showOnlyWhenConnected by viewModel.showOnlyWhenConnected.collectAsState()
    val widgetShowSpeed by viewModel.widgetShowSpeed.collectAsState()
    val widgetUsageType by viewModel.widgetUsageType.collectAsState()
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
    var showWidgetUsageDialog by remember { mutableStateOf(value = false) }
    var showIconScaleDialog by remember { mutableStateOf(value = false) }
    var showLanguageDialog by remember { mutableStateOf(value = false) }
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
    val shareChooserTitle = stringResource(R.string.label_share_via)

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
                    subtitle = when (themeMode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )

                SettingsItem(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.settings_material_you),
                    subtitle = stringResource(R.string.settings_material_you_desc),
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
                        title = stringResource(R.string.settings_accent_color),
                        subtitle = stringResource(R.string.settings_accent_color_desc),
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
                        title = stringResource(R.string.settings_amoled_mode),
                        subtitle = stringResource(R.string.settings_amoled_mode_desc),
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
                        notificationIconScale < 1.25f -> stringResource(R.string.size_small)
                        notificationIconScale > 1.32f -> stringResource(R.string.size_large)
                        else -> stringResource(R.string.size_medium)
                    },
                    onClick = { showIconScaleDialog = true }
                )

                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = when (languageCode) {
                        "ar" -> stringResource(R.string.language_arabic)
                        "fr" -> stringResource(R.string.language_french)
                        "es" -> stringResource(R.string.language_spanish)
                        "de" -> stringResource(R.string.language_german)
                        "pt" -> stringResource(R.string.language_portuguese)
                        "it" -> stringResource(R.string.language_italian)
                        "zh" -> stringResource(R.string.language_chinese)
                        "hi" -> stringResource(R.string.language_hindi)
                        "ja" -> stringResource(R.string.language_japanese)
                        "ko" -> stringResource(R.string.language_korean)
                        "ru" -> stringResource(R.string.language_russian)
                        else -> stringResource(R.string.language_default)
                    },
                    onClick = { showLanguageDialog = true }
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

        // ================== 4. WIDGET ==================
        StaggeredEntrance {
            SettingsGroup(title = stringResource(R.string.settings_section_widget)) {
                SettingsItem(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.settings_widget_show_speed),
                    subtitle = stringResource(R.string.settings_widget_show_speed_desc),
                    trailingContent = {
                        Switch(
                            checked = widgetShowSpeed,
                            onCheckedChange = { viewModel.setWidgetShowSpeed(it) },
                            colors = switchColors,
                            thumbContent = { thumbContent(widgetShowSpeed) }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.DataUsage,
                    title = stringResource(R.string.settings_widget_usage_type),
                    subtitle = when (widgetUsageType) {
                        "MONTHLY" -> stringResource(R.string.filter_monthly)
                        else -> stringResource(R.string.filter_daily)
                    },
                    onClick = { showWidgetUsageDialog = true }
                )
            }
        }

        // ================== 5. MONITORING & SECURITY ==================
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
                        subtitle = stringResource(R.string.settings_traffic_threshold_subtitle),
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
                        val activity = context.findActivity()
                        val targetContext = activity ?: context
                        if (targetContext !is Activity) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            targetContext.startActivity(intent)
                        } catch (_: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
                            if (targetContext !is Activity) {
                                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                targetContext.startActivity(webIntent)
                            } catch (_: Exception) {}
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
                        val chooserIntent = Intent.createChooser(shareIntent, shareChooserTitle)
                        val activity = context.findActivity()
                        val targetContext = activity ?: context
                        if (targetContext !is Activity) {
                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            targetContext.startActivity(chooserIntent)
                        } catch (_: Exception) {}
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
                        val activity = context.findActivity()
                        val targetContext = activity ?: context
                        if (targetContext !is Activity) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            targetContext.startActivity(intent)
                        } catch (_: Exception) {}
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

        if (showWidgetUsageDialog) {
            WidgetUsageDialog(
                currentType = widgetUsageType,
                onDismiss = { showWidgetUsageDialog = false },
                onSelect = {
                    viewModel.setWidgetUsageType(it)
                    showWidgetUsageDialog = false
                }
            )
        }

        if (showLanguageDialog) {
            LanguageDialog(
                currentLanguageCode = languageCode,
                onDismiss = { showLanguageDialog = false },
                onSelect = viewModel::setLanguage
            )
        }

        if (showLicensesDialog) {
            LegalDialog(
                title = stringResource(R.string.settings_licenses),
                content = stringResource(R.string.legal_licenses),
            ) { showLicensesDialog = false }
        }

        if (showPrivacyDialog) {
            LegalDialog(
                title = stringResource(R.string.settings_privacy_policy),
                content = stringResource(R.string.legal_privacy_policy),
            ) { showPrivacyDialog = false }
        }

        if (showTermsDialog) {
            LegalDialog(
                title = stringResource(R.string.settings_terms_conditions),
                content = stringResource(R.string.legal_terms_conditions),
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
                val activity = context.findActivity()
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
                    val activity = context.findActivity()
                    val targetContext = activity ?: context
                    if (targetContext !is Activity) {
                        telegramAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        targetContext.startActivity(telegramAppIntent)
                    } catch (_: Exception) {
                        // Fallback to browser if Telegram app is not installed
                        val browserIntent = Intent(Intent.ACTION_VIEW, "https://t.me/$username".toUri())
                        if (targetContext !is Activity) {
                            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            targetContext.startActivity(browserIntent)
                        } catch (_: Exception) {}
                    }
                },
                onEmailClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:drrayy001@gmail.com".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback: FlowBytes (v$versionName)")
                    }
                    val activity = context.findActivity()
                    val targetContext = activity ?: context
                    if (targetContext !is Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        targetContext.startActivity(intent)
                    } catch (_: Exception) {}
                },
                onReportBugClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/drrayy001/FlowBytes/issues".toUri())
                    val activity = context.findActivity()
                    val targetContext = activity ?: context
                    if (targetContext !is Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        targetContext.startActivity(intent)
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

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

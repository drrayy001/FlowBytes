// Historical network activity alerts list. Provides filters for different alert categories
// and actions to clear logs or temporarily mute active alerts.
package com.ray.flowmeter.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AppAlert
import com.ray.flowmeter.ui.theme.*
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.utils.SpeedFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    showFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    val alerts by viewModel.alerts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    
    val categories = listOf(
        "ALL" to stringResource(R.string.filter_all),
        "APP_LIMIT" to stringResource(R.string.label_app_limits),
        "DAILY_LIMIT" to stringResource(R.string.label_daily_limits),
        "MONTHLY_LIMIT" to stringResource(R.string.label_monthly_limits),
        "HIGH_TRAFFIC" to stringResource(R.string.label_high_traffic)
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
            indicator = {
                val indicatorSize = 40.dp
                val restingDistance = 1.dp
                val totalTravel = restingDistance + indicatorSize
                val pullFraction = pullRefreshState.distanceFraction
                val isActivelyPulling = pullFraction > 0f
                val rawOffset = -indicatorSize + (totalTravel * pullFraction)
                val cappedOffset = rawOffset.coerceAtMost(restingDistance)

                val animatedOffset by animateDpAsState(
                    targetValue = when {
                        viewModel.isRefreshing -> restingDistance
                        isActivelyPulling -> cappedOffset
                        else -> -indicatorSize - 20.dp
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "indicator_offset"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(indicatorSize)
                        .graphicsLayer {
                            translationY = animatedOffset.toPx()
                            alpha = if (viewModel.isRefreshing) 1f else (pullFraction * 2f).coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = viewModel.isRefreshing,
                        animationSpec = tween(durationMillis = 200),
                        label = "indicator_content"
                    ) { isRefreshing ->
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            val rotation = (pullFraction * 180f).coerceIn(0f, 180f)
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_pull_to_refresh),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Centered Pill-based Navigation Row
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically(animationSpec = premiumSpring()) + fadeIn(),
                    exit = shrinkVertically(animationSpec = premiumSpring()) + fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(categories, key = { it.first }) { (id, label) ->
                            val selected = selectedCategory == id
                            val contentColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.onSecondaryContainer 
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "TabContentColor"
                            )
                            val containerColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "TabContainerColor"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .clickable { viewModel.setSelectedCategory(id) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (alerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(120.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = when(selectedCategory) {
                                        "APP_LIMIT" -> Icons.Rounded.Block
                                        "DAILY_LIMIT" -> Icons.Rounded.History
                                        "MONTHLY_LIMIT" -> Icons.Rounded.CalendarMonth
                                        else -> Icons.Rounded.Notifications
                                    }
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                when(selectedCategory) {
                                    "ALL" -> stringResource(R.string.msg_no_alerts_all)
                                    "APP_LIMIT" -> stringResource(R.string.msg_no_alerts_app)
                                    "DAILY_LIMIT" -> stringResource(R.string.msg_no_alerts_daily)
                                    "MONTHLY_LIMIT" -> stringResource(R.string.msg_no_alerts_monthly)
                                    else -> stringResource(R.string.msg_no_alerts_traffic)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.msg_no_alerts_history),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(alerts, key = { it.id }) { alert ->
                            AlertItem(alert)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertItem(alert: AppAlert) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
    val dateTimeString = dateFormat.format(Date(alert.timestamp))

    val dailyWifiLimit = stringResource(R.string.label_daily_wifi_limit)
    val monthlyWifiLimit = stringResource(R.string.label_monthly_wifi_limit)
    val dailyMobileLimit = stringResource(R.string.label_daily_mobile_limit)
    val monthlyMobileLimit = stringResource(R.string.label_monthly_mobile_limit)
    val customWifiLimit = stringResource(R.string.label_custom_wifi_limit)
    val customMobileLimit = stringResource(R.string.label_custom_mobile_limit)
    val unknownLabel = stringResource(R.string.label_unknown)

    val displayAppName = remember(alert.packageName, alert.appName, dailyWifiLimit, monthlyWifiLimit, dailyMobileLimit, monthlyMobileLimit, customWifiLimit, customMobileLimit, unknownLabel) {
        val packageName = alert.packageName
        val appName = alert.appName ?: ""
        
        when {
            packageName == "system.wifi.daily" -> dailyWifiLimit
            packageName == "system.mobile.daily" -> dailyMobileLimit
            packageName == "system.wifi.monthly" -> monthlyWifiLimit
            packageName == "system.mobile.monthly" -> monthlyMobileLimit
            packageName == "system.wifi.custom" -> customWifiLimit
            packageName == "system.mobile.custom" -> customMobileLimit
            packageName != null && packageName.startsWith("system.") -> {
                val isWifi = packageName.contains("wifi")
                val isDaily = packageName.contains("daily")
                val isMonthly = packageName.contains("monthly")
                if (isWifi) {
                    when {
                        isDaily -> dailyWifiLimit
                        isMonthly -> monthlyWifiLimit
                        else -> customWifiLimit
                    }
                } else {
                    when {
                        isDaily -> dailyMobileLimit
                        isMonthly -> monthlyMobileLimit
                        else -> customMobileLimit
                    }
                }
            }
            else -> {
                val isWifi = appName.contains("Wi-Fi", ignoreCase = true) || 
                             appName.contains("WLAN", ignoreCase = true) || 
                             appName.contains("واي فاي") || 
                             appName.contains("वाई-फाई")
                
                val isMobile = appName.contains("Mobile", ignoreCase = true) || 
                               appName.contains("Mobil", ignoreCase = true) || 
                               appName.contains("جوال") || 
                               appName.contains("मोबाइल") || 
                               appName.contains("dati", ignoreCase = true) || 
                               appName.contains("móveis", ignoreCase = true) || 
                               appName.contains("Мобильные", ignoreCase = true) || 
                               appName.contains("移动") || 
                               appName.contains("모바일") || 
                               appName.contains("datos", ignoreCase = true) || 
                               appName.contains("données", ignoreCase = true)

                if (isWifi || isMobile) {
                    val isDaily = appName.contains("Daily", ignoreCase = true) || 
                                  appName.contains("Tägliches", ignoreCase = true) || 
                                  appName.contains("اليومي") || 
                                  appName.contains("Diario", ignoreCase = true) || 
                                  appName.contains("Quotidienne", ignoreCase = true) || 
                                  appName.contains("दैनिक") || 
                                  appName.contains("Giornaliero", ignoreCase = true) || 
                                  appName.contains("1日の") || 
                                  appName.contains("일일") || 
                                  appName.contains("Дневной", ignoreCase = true) || 
                                  appName.contains("每日")

                    val isMonthly = appName.contains("Monthly", ignoreCase = true) || 
                                    appName.contains("Monatliches", ignoreCase = true) || 
                                    appName.contains("الشهري") || 
                                    appName.contains("Mensual", ignoreCase = true) || 
                                    appName.contains("Mensuelle", ignoreCase = true) || 
                                    appName.contains("масик") || 
                                    appName.contains("Mensile", ignoreCase = true) || 
                                    appName.contains("月間の") || 
                                    appName.contains("월간") || 
                                    appName.contains("Месячный", ignoreCase = true) || 
                                    appName.contains("每月")

                    if (isWifi) {
                        when {
                            isDaily -> dailyWifiLimit
                            isMonthly -> monthlyWifiLimit
                            else -> customWifiLimit
                        }
                    } else {
                        when {
                            isDaily -> dailyMobileLimit
                            isMonthly -> monthlyMobileLimit
                            else -> customMobileLimit
                        }
                    }
                } else {
                    alert.appName ?: unknownLabel
                }
            }
        }
    }

    val appIcon = remember(alert.packageName) {
        if (alert.packageName != null) {
            try {
                context.packageManager.getApplicationIcon(alert.packageName)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val wifiLabel = stringResource(R.string.label_wifi)

    val subtitleText = when(alert.alertType) {
        "HIGH_TRAFFIC" -> {
            val speedStr = SpeedFormatter.formatBytes(alert.speed)
            "${stringResource(R.string.label_high_traffic)} ($speedStr)"
        }
        "APP_LIMIT" -> {
            val limitStr = formatUsage(alert.limitValue)
            "${stringResource(R.string.label_app_limit_reached)} (Limit: $limitStr)"
        }
        "DAILY_LIMIT" -> {
            val limitStr = formatUsage(alert.limitValue)
            "${stringResource(R.string.label_daily_limit_reached)} (Limit: $limitStr)"
        }
        "MONTHLY_LIMIT" -> {
            val limitStr = formatUsage(alert.limitValue)
            "${stringResource(R.string.label_monthly_limit_reached)} (Limit: $limitStr)"
        }
        else -> alert.alertType
    }

    val rightValue = formatUsage(alert.rxBytes + alert.txBytes)

    // Determine status color for Left Icon Background ONLY
    val statusColor = if (alert.isMuted) {
        MaterialTheme.colorScheme.outline
    } else {
        when (alert.alertType) {
            "HIGH_TRAFFIC" -> MaterialTheme.colorScheme.primary
            "APP_LIMIT", "DAILY_LIMIT", "MONTHLY_LIMIT" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.secondary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon / Indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = statusColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val icon = when(alert.alertType) {
                        "DAILY_LIMIT", "MONTHLY_LIMIT", "CUSTOM_LIMIT" -> {
                            val isWifi = displayAppName.contains("Wi-Fi", ignoreCase = true) || 
                                         displayAppName.contains("Wifi", ignoreCase = true) || 
                                         displayAppName.contains(wifiLabel, ignoreCase = true)
                            if (isWifi) Icons.Rounded.Wifi else Icons.Rounded.SignalCellularAlt
                        }
                        else -> Icons.Rounded.Category
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Subtitle (Left-aligned Column)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayAppName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Value & Timestamp (Right-aligned Column, NO status icon)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rightValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateTimeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

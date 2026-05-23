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
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { (id, label) ->
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
                        items(alerts) { alert ->
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
    var expanded by remember { mutableStateOf(value = false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { expanded = !expanded },
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                AlertItemFront(alert)

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(premiumSpring()) + expandVertically(premiumSpring()),
                    exit = fadeOut(premiumSpring()) + shrinkVertically(premiumSpring())
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        AlertItemBack(alert)
                    }
                }
            }
        }
    }
}


@Composable
fun AlertItemFront(alert: AppAlert) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = dateFormat.format(Date(alert.timestamp))

    // Mapping old names to new descriptive names for existing data
    val displayAppName = remember(alert.appName) {
        when (alert.appName) {
            "Wi-Fi (Daily)" -> "Daily Wi-Fi Limit"
            "Wi-Fi (Monthly)" -> "Monthly Wi-Fi Limit"
            "Mobile (Daily)" -> "Daily Mobile Limit"
            "Mobile (Monthly)" -> "Monthly Mobile Limit"
            else -> alert.appName ?: "Unknown"
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                val icon = when(alert.alertType) {
                    "DAILY_LIMIT", "MONTHLY_LIMIT" -> if (displayAppName.contains("Wi-Fi")) Icons.Rounded.Wifi else Icons.Rounded.SignalCellularAlt
                    else -> Icons.Rounded.Category
                }
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayAppName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
            
            val categoryText = when(alert.alertType) {
                "HIGH_TRAFFIC" -> stringResource(R.string.label_high_traffic)
                "APP_LIMIT" -> stringResource(R.string.label_app_limit_reached)
                "DAILY_LIMIT" -> stringResource(R.string.label_daily_limit_reached)
                "MONTHLY_LIMIT" -> stringResource(R.string.label_monthly_limit_reached)
                else -> alert.alertType
            }

            Text(
                text = categoryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AlertItemBack(alert: AppAlert) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(alert.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = when(alert.alertType) {
                        "HIGH_TRAFFIC" -> stringResource(R.string.label_trigger_speed_header).uppercase()
                        else -> stringResource(R.string.label_limit_set_header).uppercase()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (alert.alertType == "HIGH_TRAFFIC") SpeedFormatter.formatBytes(alert.speed) 
                           else formatUsage(alert.limitValue),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.label_usage_recorded_header).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = formatUsage(alert.rxBytes + alert.txBytes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (alert.isMuted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alert Muted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Placeholder to keep the date aligned to the right
                Spacer(modifier = Modifier.weight(1f))
            }

            Text(
                text = dateString,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.ray.flowmeter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.components.AppIcons
import com.ray.flowmeter.ui.components.ChartType
import com.ray.flowmeter.ui.components.WeeklyBarChart
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import java.util.Calendar
import java.util.Locale

// Main landing screen showing usage summary, weekly activity, and data insights
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToUsage: (Calendar) -> Unit = {},
    onNavigateToTodayUsage: () -> Unit = {},
    onNavigateToMonthUsage: () -> Unit = {},
) {
    val selectedChartType by viewModel.selectedChartType

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        item {
            StaggeredEntrance(index = 0) {
                UsageSummaryCard(
                    title = stringResource(R.string.label_todays_usage),
                    totalUsage = viewModel.dailyUsage,
                    subItems = listOf(
                        UsageItemData(viewModel.downloadReceived, stringResource(R.string.label_download), MaterialTheme.colorScheme.primary),
                        UsageItemData(viewModel.uploadSent, stringResource(R.string.label_upload), MaterialTheme.colorScheme.secondary),
                        UsageItemData(viewModel.dailyWifiUsage, stringResource(R.string.label_wifi), MaterialTheme.colorScheme.primary),
                        UsageItemData(viewModel.dailyMobileUsage, stringResource(R.string.label_mobile), MaterialTheme.colorScheme.secondary)
                    ),
                    icon = AppIcons.TodayUsage,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToTodayUsage
                )
            }
        }

        item {
            StaggeredEntrance(index = 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        AppIcons.ChartMain,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.label_usage_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        WeeklyBarChart(
                            days = viewModel.weekDays,
                            dates = viewModel.weeklyDates,
                            mobileData = viewModel.weeklyMobileData,
                            wifiData = viewModel.weeklyWifiData,
                            yLabels = viewModel.weeklyYAxisLabels,
                            selectedType = selectedChartType,
                            onDayClick = onNavigateToUsage
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChartType.entries.forEachIndexed { index, type ->
                                LegendItem(
                                    label = type.getLabel(),
                                    color = type.getColor(),
                                    isSelected = selectedChartType == type,
                                ) { viewModel.updateChartType(type) }
                                if (index < (ChartType.entries.size - 1)) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            StaggeredEntrance(index = 2) {
                UsageSummaryCard(
                    title = stringResource(R.string.label_this_month),
                    totalUsage = viewModel.monthlyUsage,
                    subItems = listOf(
                        UsageItemData(viewModel.monthlyWifiUsage, stringResource(R.string.label_wifi), MaterialTheme.colorScheme.primary),
                        UsageItemData(viewModel.monthlyMobileUsage, stringResource(R.string.label_mobile), MaterialTheme.colorScheme.secondary)
                    ),
                    icon = AppIcons.ThisMonthUsage,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToMonthUsage
                )
            }
        }

        if (viewModel.isDataLimitEnabled) {
            item {
                StaggeredEntrance(index = 3) {
                    ForecastCard(
                        currentUsage = viewModel.dailyMobileUsageBytes,
                        projectedUsage = viewModel.projectedDailyMobileBytes,
                        limit = viewModel.dailyMobileLimitBytes
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Legend item for the usage breakdown chart
@Composable
fun LegendItem(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.bounceClick(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) color else color.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Card showing data usage forecast and progress towards daily limit
@Composable
fun ForecastCard(
    currentUsage: Long,
    projectedUsage: Long,
    limit: Long
) {
    val progress = (currentUsage.toFloat() / limit).coerceIn(0f, 1f)
    val projectedProgress = (projectedUsage.toFloat() / limit).coerceIn(0f, 1.2f)
    val isOverLimit = projectedUsage > limit

    val accentColor = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.6f),
                    accentColor.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isOverLimit) AppIcons.ForecastWarning else AppIcons.ForecastSafe,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.label_usage_insight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isOverLimit)
                    stringResource(R.string.msg_limit_forecast_warning)
                else stringResource(R.string.msg_limit_forecast_safe),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(projectedProgress.coerceAtMost(1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.7f),
                                    accentColor
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.label_projected_upper), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(formatDataSize(projectedUsage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.label_daily_limit_upper), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(formatDataSize(limit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// Utility for formatting bytes into human-readable data sizes
fun formatDataSize(bytes: Long): String {
    val gb = 1024L * 1024L * 1024L
    val mb = 1024L * 1024L
    val kb = 1024L
    return when {
        bytes >= gb -> String.format(Locale.getDefault(), "%.2f GB", bytes.toDouble() / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

data class UsageItemData(
    val value: String,
    val label: String,
    val color: Color
)

// Reusable card component for displaying usage summaries
@Composable
fun UsageSummaryCard(
    title: String,
    totalUsage: String,
    subItems: List<UsageItemData>,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        UsageSummaryContent(title, totalUsage, subItems, icon, accentColor)
    }
}

// Inner content for UsageSummaryCard
@Composable
fun UsageSummaryContent(
    title: String,
    totalUsage: String,
    subItems: List<UsageItemData>,
    icon: ImageVector,
    accentColor: Color
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(totalUsage, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        val getIconForLabel = { label: String ->
            when {
                label.contains("Download", ignoreCase = true) -> AppIcons.Download
                label.contains("Upload", ignoreCase = true) -> AppIcons.Upload
                label.contains("Wi-Fi", ignoreCase = true) || label.contains("Wifi", ignoreCase = true) -> AppIcons.Wifi
                label.contains("Mobile", ignoreCase = true) -> AppIcons.Mobile
                else -> AppIcons.GenericData
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Column (Items 1 and 3 - e.g. Download and Mobile)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.Start) {
                    if (subItems.isNotEmpty()) {
                        UsageSubItem(
                            label = subItems[0].label,
                            value = subItems[0].value,
                            icon = getIconForLabel(subItems[0].label),
                            color = subItems[0].color
                        )
                    }
                    if (subItems.size >= 3) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageSubItem(
                            label = subItems[2].label,
                            value = subItems[2].value,
                            icon = getIconForLabel(subItems[2].label),
                            color = subItems[2].color
                        )
                    }
                }
            }

            // Right Column (Items 2 and 4 - e.g. Upload and Wi-Fi)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.Start) {
                    if (subItems.size >= 2) {
                        UsageSubItem(
                            label = subItems[1].label,
                            value = subItems[1].value,
                            icon = getIconForLabel(subItems[1].label),
                            color = subItems[1].color
                        )
                    }
                    if (subItems.size >= 4) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageSubItem(
                            label = subItems[3].label,
                            value = subItems[3].value,
                            icon = getIconForLabel(subItems[3].label),
                            color = subItems[3].color
                        )
                    }
                }
            }
        }
    }
}

// Smaller usage item detail (e.g., Download, Upload) with icon
@Composable
fun UsageSubItem(label: String, value: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

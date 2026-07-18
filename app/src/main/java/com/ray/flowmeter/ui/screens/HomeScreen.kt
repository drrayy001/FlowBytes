// Main dashboard screen displaying daily/monthly usage progress rings,
// data limit statistics, and a weekly usage chart.
package com.ray.flowmeter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ray.flowmeter.utils.PermissionHelper
import android.os.Build
import androidx.compose.material.icons.rounded.Notifications

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToUsage: (Calendar) -> Unit = {},
    onNavigateToTodayUsage: () -> Unit = {},
    onNavigateToMonthUsage: () -> Unit = {},
) {
    val selectedChartType by viewModel.selectedChartType

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasUsageAccess by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentHasAccess = PermissionHelper.hasUsageAccess(context)
                if (currentHasAccess != hasUsageAccess) {
                    hasUsageAccess = currentHasAccess
                    if (currentHasAccess) {
                        viewModel.updateTotalUsage()
                    }
                }

                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val usageAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val currentHasAccess = PermissionHelper.hasUsageAccess(context)
        hasUsageAccess = currentHasAccess
        if (currentHasAccess) {
            viewModel.updateTotalUsage()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = true
    ) {
        if (!hasUsageAccess || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
            item {
                val warnings = remember(hasUsageAccess, hasNotificationPermission) {
                    buildList {
                        if (!hasUsageAccess) add(0) // Usage stats warning
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) add(1) // Notification warning
                    }
                }

                val pagerState = rememberPagerState(pageCount = { warnings.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 12.dp
                ) { page ->
                    val warningType = warnings[page]
                    if (warningType == 0) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    usageAccessLauncher.launch(intent)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                                    .also {
                                        // Workaround layout hint to avoid compiler warning about unused import
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Usage stats stats permission disabled. Tap to grant.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Notification permission disabled. Tap to grant.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            StaggeredEntrance {
                UsageSummaryCard(
                    title = stringResource(R.string.label_todays_usage),
                    totalUsage = viewModel.dailyUsage,
                    subItems = listOf(
                        UsageItemData(viewModel.downloadReceived, stringResource(R.string.label_download), MaterialTheme.colorScheme.primary, AppIcons.Download),
                        UsageItemData(viewModel.uploadSent, stringResource(R.string.label_upload), MaterialTheme.colorScheme.secondary, AppIcons.Upload),
                        UsageItemData(viewModel.dailyWifiUsage, stringResource(R.string.label_wifi), MaterialTheme.colorScheme.primary, AppIcons.Wifi),
                        UsageItemData(viewModel.dailyMobileUsage, stringResource(R.string.label_mobile), MaterialTheme.colorScheme.secondary, AppIcons.Mobile)
                    ),
                    icon = AppIcons.TodayUsage,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToTodayUsage
                )
            }
        }



        item {
            StaggeredEntrance {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChartType.entries.forEach { type ->
                                LegendItem(
                                    label = type.getLabel(),
                                    color = type.getColor(),
                                    isSelected = selectedChartType == type,
                                ) { viewModel.updateChartType(type) }
                            }
                        }
                    }
                }
            }
        }

        item {
            StaggeredEntrance {
                UsageSummaryCard(
                    title = stringResource(R.string.label_this_month),
                    totalUsage = viewModel.monthlyUsage,
                    subItems = listOf(
                        UsageItemData(viewModel.monthlyWifiUsage, stringResource(R.string.label_wifi), MaterialTheme.colorScheme.primary, AppIcons.Wifi),
                        UsageItemData(viewModel.monthlyMobileUsage, stringResource(R.string.label_mobile), MaterialTheme.colorScheme.secondary, AppIcons.Mobile)
                    ),
                    icon = AppIcons.ThisMonthUsage,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToMonthUsage
                )
            }
        }
    }
}

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

data class UsageItemData(
    val value: String,
    val label: String,
    val color: Color,
    val icon: ImageVector
)

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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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
            Text(totalUsage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.Start) {
                    if (subItems.isNotEmpty()) {
                        UsageSubItem(
                            label = subItems[0].label,
                            value = subItems[0].value,
                            icon = subItems[0].icon,
                            color = subItems[0].color
                        )
                    }
                    if (subItems.size >= 3) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageSubItem(
                            label = subItems[2].label,
                            value = subItems[2].value,
                            icon = subItems[2].icon,
                            color = subItems[2].color
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.Start) {
                    if (subItems.size >= 2) {
                        UsageSubItem(
                            label = subItems[1].label,
                            value = subItems[1].value,
                            icon = subItems[1].icon,
                            color = subItems[1].color
                        )
                    }
                    if (subItems.size >= 4) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageSubItem(
                            label = subItems[3].label,
                            value = subItems[3].value,
                            icon = subItems[3].icon,
                            color = subItems[3].color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsageSubItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (horizontalAlignment == Alignment.End) Arrangement.End else Arrangement.Start
    ) {
        if (horizontalAlignment == Alignment.Start) {
            UsageSubItemIcon(icon, color)
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(horizontalAlignment = horizontalAlignment) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                textAlign = if (horizontalAlignment == Alignment.End) TextAlign.End else TextAlign.Start
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (horizontalAlignment == Alignment.End) TextAlign.End else TextAlign.Start
            )
        }

        if (horizontalAlignment == Alignment.End) {
            Spacer(modifier = Modifier.width(10.dp))
            UsageSubItemIcon(icon, color)
        }
    }
}

@Composable
private fun UsageSubItemIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
    }
}

package com.ray.flowmeter.ui.screens

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.receiver.DailyNetworkLimitWidget
import com.ray.flowmeter.receiver.MonthlyNetworkLimitWidget
import com.ray.flowmeter.receiver.CustomNetworkLimitWidget
import com.ray.flowmeter.receiver.DailyUsageWidget
import com.ray.flowmeter.receiver.MonthlyUsageWidget
import com.ray.flowmeter.receiver.SpeedMonitorWidget
import com.ray.flowmeter.receiver.TodayDataWidget
import com.ray.flowmeter.utils.SpeedFormatter
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.DataUsage
import com.ray.flowmeter.ui.components.SettingsItem
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.theme.StaggeredEntrance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    context: Context,
    repository: UserPreferencesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val wifiDailyLimit by repository.wifiDailyLimit.collectAsState(initial = 2147483648L)
    val mobileDailyLimit by repository.dataDailyLimit.collectAsState(initial = 2147483648L)
    val wifiMonthlyLimit by repository.wifiMonthlyLimit.collectAsState(initial = 53687091200L)
    val mobileMonthlyLimit by repository.dataMonthlyLimit.collectAsState(initial = 10737418240L)
    val wifiCustomLimit by repository.wifiCustomLimit.collectAsState(initial = 0L)
    val mobileCustomLimit by repository.dataCustomLimit.collectAsState(initial = 0L)

    val widgetUpdateInterval by repository.widgetUpdateInterval.collectAsState(initial = 30)

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_homescreen_widgets),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Description card
            StaggeredEntrance(index = 0) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.title_customize_add_widgets),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.desc_widgets_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Widget Settings Section
            StaggeredEntrance(index = 1) {
                Text(
                    text = stringResource(R.string.label_widget_settings_caps),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            StaggeredEntrance(index = 2) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        var showUpdateIntervalDialog by remember { mutableStateOf(false) }
                        val intervalText = getIntervalText(widgetUpdateInterval)
                        SettingsItem(
                            icon = Icons.Rounded.Update,
                            title = stringResource(R.string.label_update_interval),
                            subtitle = stringResource(R.string.desc_update_interval_format, intervalText),
                            onClick = { showUpdateIntervalDialog = true }
                        )


                        if (showUpdateIntervalDialog) {
                            WidgetUpdateIntervalDialog(
                                currentInterval = widgetUpdateInterval,
                                onDismiss = { showUpdateIntervalDialog = false },
                                onSelect = { selectedInterval ->
                                    scope.launch {
                                        repository.setWidgetUpdateInterval(selectedInterval)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Widget Previews Section
            StaggeredEntrance(index = 3) {
                Text(
                    text = stringResource(R.string.label_available_widgets),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 1. Daily Usage Widget Preview
            StaggeredEntrance(index = 4) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_daily_usage_widget),
                    sizeInfo = stringResource(R.string.desc_daily_usage_widget_size),
                    onAddClick = { pinWidget(context, DailyUsageWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = stringResource(R.string.label_todays_usage_caps),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "2.4 GB",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Monthly Usage Widget Preview
            StaggeredEntrance(index = 5) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_monthly_usage_widget),
                    sizeInfo = stringResource(R.string.desc_monthly_usage_widget_size),
                    onAddClick = { pinWidget(context, MonthlyUsageWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = stringResource(R.string.label_this_month).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "48.7 GB",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Today's Data Widget Preview
            StaggeredEntrance(index = 6) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_todays_data_widget),
                    sizeInfo = stringResource(R.string.desc_todays_data_size),
                    onAddClick = { pinWidget(context, TodayDataWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.label_todays_data_caps),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .border(
                                            width = 6.dp,
                                            color = Color(0xFF1C2330),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 6.dp,
                                            brush = Brush.sweepGradient(
                                                0f to Color(0xFF00daf3),
                                                0.65f to Color(0xFF00daf3),
                                                0.65f to Color.Transparent,
                                                1f to Color.Transparent
                                            ),
                                            shape = CircleShape
                                        )
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "2.4",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00daf3)
                                    )
                                    Text(
                                        text = "GB",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Daily Network Limit Widget Preview
            StaggeredEntrance(index = 7) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_daily_limit_widget),
                    sizeInfo = stringResource(R.string.desc_detailed_status_size),
                    onAddClick = { pinWidget(context, DailyNetworkLimitWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        NetworkLimitPreviewItem(
                            title = stringResource(R.string.label_daily_limit_caps),
                            wifiLimit = wifiDailyLimit,
                            mobileLimit = mobileDailyLimit,
                            wifiUsed = "0.4",
                            mobileUsed = "0.1",
                            wifiProgress = 0.20f,
                            mobileProgress = 0.05f
                        )
                    }
                }
            }

            // 4. Monthly Network Limit Widget Preview
            StaggeredEntrance(index = 8) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_monthly_limit_widget),
                    sizeInfo = stringResource(R.string.desc_detailed_status_size),
                    onAddClick = { pinWidget(context, MonthlyNetworkLimitWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        NetworkLimitPreviewItem(
                            title = stringResource(R.string.label_monthly_limit_caps),
                            wifiLimit = wifiMonthlyLimit,
                            mobileLimit = mobileMonthlyLimit,
                            wifiUsed = "12.5",
                            mobileUsed = "4.2",
                            wifiProgress = 0.25f,
                            mobileProgress = 0.42f
                        )
                    }
                }
            }

            // 5. Custom Network Limit Widget Preview
            StaggeredEntrance(index = 9) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_custom_limit_widget),
                    sizeInfo = stringResource(R.string.desc_detailed_status_size),
                    onAddClick = { pinWidget(context, CustomNetworkLimitWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        NetworkLimitPreviewItem(
                            title = stringResource(R.string.label_custom_limit_caps),
                            wifiLimit = wifiCustomLimit,
                            mobileLimit = mobileCustomLimit,
                            wifiUsed = "1.8",
                            mobileUsed = "0.9",
                            wifiProgress = 0.18f,
                            mobileProgress = 0.09f
                        )
                    }
                }
            }

            // 5. Speed Monitor Widget Preview
            StaggeredEntrance(index = 10) {
                WidgetPreviewCard(
                    title = stringResource(R.string.title_speed_monitor_widget),
                    sizeInfo = stringResource(R.string.desc_live_graphs_size),
                    onAddClick = { pinWidget(context, SpeedMonitorWidget::class.java) }
                ) {
                    WidgetBackgroundPreview {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header
                            Text(
                                text = stringResource(R.string.label_speed_monitor_caps),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 0.08.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Speeds and curves columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Download Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color(0xFF4DE8F4),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.label_download_caps),
                                            color = Color(0xFF4DE8F4),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.05.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "45.2",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Mbps",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    // Download Graph
                                    val downColor = Color(0xFF4DE8F4)
                                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(0f, h * 0.7f)
                                            cubicTo(w * 0.25f, h * 0.4f, w * 0.4f, h * 0.8f, w * 0.6f, h * 0.2f)
                                            cubicTo(w * 0.75f, h * 0.1f, w * 0.9f, h * 0.9f, w, h * 0.3f)
                                        }
                                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                                            addPath(path)
                                            lineTo(w, h)
                                            lineTo(0f, h)
                                            close()
                                        }
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(downColor.copy(alpha = 0.15f), Color.Transparent),
                                                startY = 0f,
                                                endY = h
                                            )
                                        )
                                        drawPath(
                                            path = path,
                                            color = downColor,
                                            style = Stroke(
                                                width = 2.5.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }

                                // Upload Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color(0xFFDDA7FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.label_upload_caps),
                                            color = Color(0xFFDDA7FF),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.05.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "12.8",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Mbps",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    // Upload Graph
                                    val upColor = Color(0xFFDDA7FF)
                                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(0f, h * 0.8f)
                                            cubicTo(w * 0.2f, h * 0.7f, w * 0.4f, h * 0.3f, w * 0.6f, h * 0.6f)
                                            cubicTo(w * 0.75f, h * 0.8f, w * 0.85f, h * 0.2f, w, h * 0.7f)
                                        }
                                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                                            addPath(path)
                                            lineTo(w, h)
                                            lineTo(0f, h)
                                            close()
                                        }
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(upColor.copy(alpha = 0.15f), Color.Transparent),
                                                startY = 0f,
                                                endY = h
                                            )
                                        )
                                        drawPath(
                                            path = path,
                                            color = upColor,
                                            style = Stroke(
                                                width = 2.5.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewCard(
    title: String,
    sizeInfo: String,
    onAddClick: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sizeInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_add),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                previewContent()
            }
        }
    }
}

@Composable
fun WidgetBackgroundPreview(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E232B), Color(0xFF101317))
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.13f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BadgePreview(
    text: String,
    icon: ImageVector,
    tintColor: Color
) {
    Row(
        modifier = Modifier
            .background(
                color = tintColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = tintColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tintColor
        )
    }
}

private fun triggerWidgetUpdate(context: Context) {
    val intent = Intent(DailyUsageWidget.ACTION_UPDATE_WIDGET).apply {
        setPackage(context.packageName)
    }
    context.sendBroadcast(intent)
}

private fun pinWidget(context: Context, providerClass: Class<out AppWidgetProvider>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
        val myProvider = ComponentName(context, providerClass)
        if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(myProvider, null, null)
        } else {
            Toast.makeText(context, context.getString(R.string.msg_widget_pin_failed), Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, context.getString(R.string.msg_widget_pin_api_error), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NetworkLimitPreviewItem(
    title: String,
    wifiLimit: Long,
    mobileLimit: Long,
    wifiUsed: String,
    mobileUsed: String,
    wifiProgress: Float,
    mobileProgress: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Wi-Fi Segment
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.label_wifi),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                val limitStr = SpeedFormatter.formatUsage(wifiLimit)
                Text(
                    text = "$wifiUsed / $limitStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(wifiProgress)
                        .fillMaxHeight()
                        .background(Color(0xFF00daf3), RoundedCornerShape(3.dp))
                )
            }
        }

        // Mobile Segment
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.label_mobile),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                val limitStr = SpeedFormatter.formatUsage(mobileLimit)
                Text(
                    text = "$mobileUsed / $limitStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(mobileProgress)
                        .fillMaxHeight()
                        .background(Color(0xFF2ae500), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetUpdateIntervalDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
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
        com.ray.flowmeter.ui.dialogs.AnimatedDialogContent(onBack = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_select_update_interval),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val intervalOptions = listOf(0, 15, 30, 60, 120, 360, 720, 1440)

                intervalOptions.forEach { minutes ->
                    val label = getIntervalText(minutes)
                    val isSelected = currentInterval == minutes
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .bounceClick {
                                onSelect(minutes)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getIntervalText(minutes: Int): String {
    return when (minutes) {
        0 -> stringResource(R.string.option_update_interval_manual)
        60 -> stringResource(R.string.option_update_interval_hour)
        else -> {
            if (minutes % 60 == 0) {
                stringResource(R.string.option_update_interval_hours, minutes / 60)
            } else {
                stringResource(R.string.option_update_interval_minutes, minutes)
            }
        }
    }
}

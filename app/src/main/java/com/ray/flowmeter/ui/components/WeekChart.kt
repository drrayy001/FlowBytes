package com.ray.flowmeter.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.theme.premiumSpring
import java.util.Calendar
import kotlin.math.pow

object AppIcons {
    // Top-level icons
    val TodayUsage = Icons.Rounded.Today
    val ThisMonthUsage = Icons.Rounded.CalendarMonth

    // Usage data types
    val Download = Icons.Rounded.ArrowDownward
    val Upload = Icons.Rounded.ArrowUpward
    val Wifi = Icons.Rounded.Wifi
    val Mobile = Icons.Rounded.SignalCellularAlt
    val GenericData = Icons.Rounded.DataUsage

    // Charts and forecast
    val ChartMain = Icons.Rounded.BarChart
    val ForecastSafe = Icons.Rounded.AutoGraph
    val ForecastWarning = Icons.Rounded.WarningAmber

    // UI Controls
    val Filter = Icons.Rounded.FilterList
}

enum class ChartType {
    COMBINED,
    WIFI,
    MOBILE;

    @Composable
    fun getLabel(): String = when (this) {
        COMBINED -> stringResource(R.string.label_combined)
        WIFI -> stringResource(R.string.label_wifi)
        MOBILE -> stringResource(R.string.label_mobile)
    }

    // Chart colors
    @Composable
    fun getColor(): Color = when (this) {
        COMBINED -> MaterialTheme.colorScheme.primary
        WIFI -> MaterialTheme.colorScheme.secondary
        MOBILE -> MaterialTheme.colorScheme.tertiary
    }
}

@Composable
fun WeeklyBarChart(
    days: List<String>,
    dates: List<Calendar>,
    mobileData: List<Float>,
    wifiData: List<Float>,
    yLabels: List<String>,
    selectedType: ChartType,
    onDayClick: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    val isDataEmpty = mobileData.all { it < 0.001f } && wifiData.all { it < 0.001f }

    val selectedColor = selectedType.getColor()

    LaunchedEffect(mobileData, wifiData, selectedType) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = premiumSpring()
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 54.dp, end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 54.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(yLabels.size) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (isDataEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            AppIcons.ChartMain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.msg_no_usage_recorded),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val todayStr = stringResource(R.string.label_today)

                days.forEachIndexed { index, day ->
                    val isToday = day == todayStr

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .bounceClick {
                                dates.getOrNull(index)?.let { onDayClick(it) }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 14.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barWidth = 32.dp
                            val barShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight()
                                    .clip(barShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                        shape = barShape
                                    )
                            )

                            val mRaw = mobileData.getOrNull(index) ?: 0f
                            val wRaw = wifiData.getOrNull(index) ?: 0f

                            val rawVal = when (selectedType) {
                                ChartType.MOBILE -> mRaw
                                ChartType.WIFI -> wRaw
                                ChartType.COMBINED -> mRaw + wRaw
                            }

                            val heightProgress = rawVal * animationProgress.value

                            if (heightProgress > 0f || (rawVal > 0f && animationProgress.value > 0.01f)) {
                                // Apply a "Soft Power" scale (0.75) for better visual detail of low values
                                // while maintaining general correlation with the linear axis.
                                val visualProgress = if (rawVal > 0f) {
                                    (rawVal.toDouble().pow(0.75)).toFloat().coerceIn(0.08f, 1f)
                                } else 0f
                                
                                val finalHeight = (visualProgress * animationProgress.value).coerceIn(0f, 1f)

                                if (finalHeight > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(finalHeight)
                                            .clip(barShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        selectedColor.copy(alpha = 0.7f),
                                                        selectedColor
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }

                        if (isToday) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

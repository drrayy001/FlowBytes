// Detailed breakdown listing network consumption per-app. Supports sorting/filtering
// by Wi-Fi/mobile usage and splitting applications into user and system categories.
package com.ray.flowmeter.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.components.AppIcons
import com.ray.flowmeter.ui.viewmodels.AppUsageInfo
import com.ray.flowmeter.ui.dialogs.UnifiedPickerContainer
import com.ray.flowmeter.ui.dialogs.UnifiedPickerHeader
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.theme.premiumSpring
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(
    viewModel: AppUsageViewModel,
    showFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    val filteredAppList by viewModel.filteredAppUsageList.collectAsState()
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullToRefreshState()

    val dayFilterLabel = stringResource(R.string.filter_daily)
    val monthFilterLabel = stringResource(R.string.filter_monthly)
    val customFilterLabel = stringResource(R.string.filter_custom)

    val savedTimeFilter by viewModel.timeFilter.collectAsState()
    val timeFilter = when (savedTimeFilter) {
        "month" -> monthFilterLabel
        "custom" -> customFilterLabel
        else -> dayFilterLabel
    }
    val (showTimeDropdown, setShowTimeDropdown) = remember { mutableStateOf(value = false) }
    val timeOptions = listOf(dayFilterLabel, monthFilterLabel, customFilterLabel)

    val mobileWifiFilterLabel = stringResource(R.string.filter_mobile_wifi)
    val mobileOnlyFilterLabel = stringResource(R.string.filter_mobile_only)
    val wifiOnlyFilterLabel = stringResource(R.string.filter_wifi_only)

    val savedNetworkFilter by viewModel.networkFilter.collectAsState()
    val networkFilter = when (savedNetworkFilter) {
        "mobile" -> mobileOnlyFilterLabel
        "wifi" -> wifiOnlyFilterLabel
        else -> mobileWifiFilterLabel
    }
    val (showNetworkDropdown, setShowNetworkDropdown) = remember { mutableStateOf(false) }
    val networkOptions = listOf(
        mobileWifiFilterLabel,
        mobileOnlyFilterLabel,
        wifiOnlyFilterLabel
    )

    val currentViewDate = viewModel.currentViewDate

    val (showDatePicker, setShowDatePicker) = remember { mutableStateOf(false) }
    val (showMonthPicker, setShowMonthPicker) = remember { mutableStateOf(false) }
    val (showDateRangePicker, setShowDateRangePicker) = remember { mutableStateOf(false) }

    val contentKey = remember(currentViewDate.timeInMillis, savedTimeFilter, savedNetworkFilter) {
        "${currentViewDate.timeInMillis}_${savedTimeFilter}_${savedNetworkFilter}"
    }

    LaunchedEffect(filteredAppList) {
        listState.scrollToItem(0)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentViewDate.timeInMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    return utcTimeMillis <= calendar.timeInMillis
                }
            }
        )
        UnifiedPickerContainer(
            onDismissRequest = { setShowDatePicker(false) },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    viewModel.loadAppUsageForDate(millis)
                }
                setShowDatePicker(false)
            }
        ) {
            val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
            val year = SimpleDateFormat("yyyy", locale).format(selectedDate)
            val date = SimpleDateFormat("MMM d", locale).format(selectedDate)

            UnifiedPickerHeader(
                title = date,
                subtitle = year,
                onPrevious = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    datePickerState.selectedDateMillis = cal.timeInMillis
                },
                onNext = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val today = Calendar.getInstance()
                    if (cal.before(today)) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        datePickerState.selectedDateMillis = cal.timeInMillis
                    }
                },
                nextEnabled = (datePickerState.selectedDateMillis ?: 0L) <
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
            )

            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showDateRangePicker) {
        val todayCal = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        val yesterdayCal = remember {
            (todayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        var startMillis by remember { mutableStateOf<Long?>(yesterdayCal.timeInMillis) }
        var endMillis by remember { mutableStateOf<Long?>(todayCal.timeInMillis) }

        val calendar = remember { Calendar.getInstance() }
        var viewMonth by remember { mutableIntStateOf(calendar[Calendar.MONTH]) }
        var viewYear by remember { mutableIntStateOf(calendar[Calendar.YEAR]) }

        UnifiedPickerContainer(
            onDismissRequest = { setShowDateRangePicker(false) },
            onConfirm = {
                if (startMillis != null && endMillis != null) {
                    viewModel.updateCustomRangeFilter(startMillis!!, endMillis!!)
                    setShowDateRangePicker(false)
                }
            },
            confirmEnabled = startMillis != null && endMillis != null
        ) {
            val startYear = startMillis?.let {
                SimpleDateFormat("yyyy", locale).format(it)
            } ?: "----"
            val startDate = startMillis?.let {
                SimpleDateFormat("MMM d", locale).format(it)
            } ?: stringResource(R.string.label_start_date)
            val endYear = endMillis?.let {
                SimpleDateFormat("yyyy", locale).format(it)
            } ?: "----"
            val endDate = endMillis?.let {
                SimpleDateFormat("MMM d", locale).format(it)
            } ?: stringResource(R.string.label_end_date)

            UnifiedPickerHeader(
                title = startDate,
                subtitle = startYear,
                title2 = endDate,
                subtitle2 = endYear,
                onPrevious = {
                    if (viewMonth == 0) {
                        viewMonth = 11
                        viewYear--
                    } else {
                        viewMonth--
                    }
                },
                onNext = {
                    if (viewMonth == 11) {
                        viewMonth = 0
                        viewYear++
                    } else {
                        viewMonth++
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            calendar[Calendar.YEAR] = viewYear
            calendar[Calendar.MONTH] = viewMonth
            Text(
                text = SimpleDateFormat("MMMM yyyy", locale).format(calendar.time),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            calendar[Calendar.YEAR] = viewYear
            calendar[Calendar.MONTH] = viewMonth
            calendar[Calendar.DAY_OF_MONTH] = 1
            val firstDay = calendar[Calendar.DAY_OF_WEEK]
            val prefixDays = if (firstDay == Calendar.SUNDAY) 6 else firstDay - 2
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            calendar.add(Calendar.MONTH, -1)
            val daysInPrevMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.add(Calendar.MONTH, 1)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                for (row in 0..5) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val index = row * 7 + col
                            val dayNum: Int
                            val isCurrentMonth: Boolean

                            val cellCal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            if (index < prefixDays) {
                                dayNum = daysInPrevMonth - prefixDays + index + 1
                                isCurrentMonth = false
                                cellCal.set(
                                    if (viewMonth == 0) viewYear - 1 else viewYear,
                                    if (viewMonth == 0) 11 else viewMonth - 1,
                                    dayNum
                                )
                            } else if (index < prefixDays + daysInMonth) {
                                dayNum = index - prefixDays + 1
                                isCurrentMonth = true
                                cellCal.set(viewYear, viewMonth, dayNum)
                            } else {
                                dayNum = index - (prefixDays + daysInMonth) + 1
                                isCurrentMonth = false
                                cellCal.set(
                                    if (viewMonth == 11) viewYear + 1 else viewYear,
                                    if (viewMonth == 11) 0 else viewMonth + 1,
                                    dayNum
                                )
                            }

                            val cellMillis = cellCal.timeInMillis
                            val isFuture = cellMillis > todayCal.timeInMillis

                            fun isSameDay(m1: Long, m2: Long): Boolean {
                                val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
                                val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
                                return (c1[Calendar.YEAR] == c2[Calendar.YEAR]) &&
                                        (c1[Calendar.DAY_OF_YEAR] == c2[Calendar.DAY_OF_YEAR])
                            }

                            val isStart = startMillis != null && isSameDay(cellMillis, startMillis!!)
                            val isEnd = endMillis != null && isSameDay(cellMillis, endMillis!!)
                            val isBetween = startMillis != null &&
                                    endMillis != null &&
                                    cellMillis > startMillis!! &&
                                    cellMillis < endMillis!!

                            key(index) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(
                                            color = when {
                                                isStart || isEnd -> MaterialTheme.colorScheme.primary
                                                isBetween -> MaterialTheme.colorScheme.primaryContainer
                                                else -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .bounceClick(enabled = !isFuture) {
                                            if (startMillis == null || (startMillis != null && endMillis != null)) {
                                                startMillis = cellMillis
                                                endMillis = null
                                            } else if (cellMillis >= startMillis!!) {
                                                endMillis = cellMillis
                                            } else {
                                                startMillis = cellMillis
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = when {
                                            isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
                                            isBetween -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                            isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showMonthPicker) {
        var pickerYear by remember {
            mutableIntStateOf(currentViewDate[Calendar.YEAR])
        }
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
        val currentMonth = Calendar.getInstance()[Calendar.MONTH]

        UnifiedPickerContainer(
            onDismissRequest = { setShowMonthPicker(false) },
            onConfirm = { setShowMonthPicker(false) }
        ) {
            UnifiedPickerHeader(
                title = stringResource(R.string.title_select_month),
                subtitle = pickerYear.toString(),
                onPrevious = { pickerYear -= 1 },
                onNext = { if (pickerYear < currentYear) pickerYear += 1 },
                nextEnabled = pickerYear < currentYear
            )

            Spacer(modifier = Modifier.height(16.dp))

            val months = listOf(
                "Jan", "Feb", "Mar", "Apr",
                "May", "Jun", "Jul", "Aug",
                "Sep", "Oct", "Nov", "Dec"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..2) {
                            val monthIndex = row * 3 + col
                            val isFuture = pickerYear == currentYear && monthIndex > currentMonth
                            val isSelected =
                                (pickerYear == currentViewDate[Calendar.YEAR]) &&
                                        (monthIndex == currentViewDate[Calendar.MONTH])

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .bounceClick(enabled = !isFuture) {
                                        val newCal = Calendar.getInstance()
                                        newCal[Calendar.YEAR] = pickerYear
                                        newCal[Calendar.MONTH] = monthIndex
                                        newCal[Calendar.DAY_OF_MONTH] = 1
                                        viewModel.updateMonthFilter(pickerYear, monthIndex)
                                        setShowMonthPicker(false)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = months[monthIndex],
                                        color = if (isFuture) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            AnimatedContent(
                targetState = contentKey,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                        .using(SizeTransform(clip = true))
                },
                label = "AppUsageContentTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) { targetContentKey ->
                key(targetContentKey) {
                    LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnimatedVisibility(
                                visible = showFilters,
                                enter = expandVertically(animationSpec = premiumSpring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = premiumSpring()) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppUsageFilterChip(
                                            selectedText = timeFilter,
                                            expanded = showTimeDropdown,
                                            onExpandedChange = setShowTimeDropdown,
                                            options = timeOptions,
                                            modifier = Modifier.weight(1f)
                                        ) { filter ->
                                            val storeValue = when (filter) {
                                                monthFilterLabel -> "month"
                                                customFilterLabel -> "custom"
                                                else -> "day"
                                            }
                                            viewModel.setTimeFilter(storeValue)
                                            if (filter != customFilterLabel) {
                                                if (filter == monthFilterLabel) {
                                                    viewModel.updateToThisMonth()
                                                } else {
                                                    viewModel.loadAppUsageForDate(System.currentTimeMillis())
                                                }
                                            } else {
                                                setShowDateRangePicker(true)
                                            }
                                        }

                                        AppUsageFilterChip(
                                            selectedText = networkFilter,
                                            expanded = showNetworkDropdown,
                                            onExpandedChange = setShowNetworkDropdown,
                                            options = networkOptions,
                                            modifier = Modifier.weight(1f)
                                        ) { filter ->
                                            val storeValue = when (filter) {
                                                mobileOnlyFilterLabel -> "mobile"
                                                wifiOnlyFilterLabel -> "wifi"
                                                else -> "all"
                                            }
                                            viewModel.setNetworkFilter(storeValue)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (timeFilter != customFilterLabel) {
                                            IconButton(onClick = { viewModel.moveDate(backwards = true) }) {
                                                Icon(
                                                    Icons.Default.ChevronLeft,
                                                    contentDescription = stringResource(R.string.cd_prev_date),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(48.dp))
                                        }

                                        val displayedDateText = viewModel.selectedDateString.ifBlank {
                                            if (timeFilter == monthFilterLabel) {
                                                val now = Calendar.getInstance()
                                                if (
                                                    (currentViewDate[Calendar.YEAR] == now[Calendar.YEAR]) &&
                                                    (currentViewDate[Calendar.MONTH] == now[Calendar.MONTH])
                                                ) {
                                                    stringResource(R.string.label_this_month)
                                                } else {
                                                    SimpleDateFormat("MMMM yyyy", locale)
                                                        .format(currentViewDate.time)
                                                }
                                            } else {
                                                stringResource(R.string.label_today)
                                            }
                                        }

                                        Text(
                                            text = displayedDateText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.bounceClick {
                                                when (timeFilter) {
                                                    monthFilterLabel -> setShowMonthPicker(true)
                                                    customFilterLabel -> setShowDateRangePicker(true)
                                                    else -> setShowDatePicker(true)
                                                }
                                            }
                                        )

                                        if (timeFilter != customFilterLabel) {
                                            val now = Calendar.getInstance()
                                            val isForwardDisabled = if (timeFilter == monthFilterLabel) {
                                                (currentViewDate[Calendar.YEAR] >= now[Calendar.YEAR]) &&
                                                        (currentViewDate[Calendar.MONTH] >= now[Calendar.MONTH])
                                            } else {
                                                (currentViewDate[Calendar.YEAR] >= now[Calendar.YEAR]) &&
                                                        (currentViewDate[Calendar.DAY_OF_YEAR] >= now[Calendar.DAY_OF_YEAR])
                                            }

                                            IconButton(
                                                onClick = { viewModel.moveDate(backwards = false) },
                                                enabled = !isForwardDisabled
                                            ) {
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = stringResource(R.string.cd_next_date),
                                                    tint = if (isForwardDisabled) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(48.dp))
                                        }
                                    }
                                }
                            }

                            val displayGlobalDown = when (networkFilter) {
                                mobileOnlyFilterLabel -> viewModel.globalCellDown
                                wifiOnlyFilterLabel -> viewModel.globalWifiDown
                                else -> viewModel.globalCellDown + viewModel.globalWifiDown
                            }
                            val displayGlobalUp = when (networkFilter) {
                                mobileOnlyFilterLabel -> viewModel.globalCellUp
                                wifiOnlyFilterLabel -> viewModel.globalWifiUp
                                else -> viewModel.globalCellUp + viewModel.globalWifiUp
                            }
                            val displayGlobalTotal = displayGlobalDown + displayGlobalUp

                            ModernUsageSummary(
                                totalUsage = displayGlobalTotal,
                                downUsage = displayGlobalDown,
                                upUsage = displayGlobalUp,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                    }

                    val maxUsageBytes = if (filteredAppList.isNotEmpty()) {
                        filteredAppList.maxOf {
                            when (networkFilter) {
                                mobileOnlyFilterLabel -> it.cellUsage
                                wifiOnlyFilterLabel -> it.wifiUsage
                                else -> it.totalUsage
                            }
                        }.coerceAtLeast(1L)
                    } else {
                        1L
                    }

                    if (filteredAppList.isEmpty() && !viewModel.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.msg_no_usage_data),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredAppList,
                            key = { _, it -> it.packageName }
                        ) { _, appUsage ->
                            val displayUsage = when (networkFilter) {
                                mobileOnlyFilterLabel -> appUsage.cellUsage
                                wifiOnlyFilterLabel -> appUsage.wifiUsage
                                else -> appUsage.totalUsage
                            }

                            StaggeredEntrance {
                                AppUsageItem(
                                    appUsage = appUsage,
                                    displayUsage = displayUsage,
                                    maxUsageBytes = maxUsageBytes,
                                    onClick = {
                                        if (appUsage.isSystemGroup) {
                                            viewModel.isViewingSystemApps = true
                                        }
                                    }
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

@Composable
fun ModernUsageSummary(
    totalUsage: Long,
    downUsage: Long,
    upUsage: Long,
    modifier: Modifier = Modifier
) {
    val downloadColor = MaterialTheme.colorScheme.primary
    val uploadColor = MaterialTheme.colorScheme.tertiary
    
    val totalParts = formatUsage(totalUsage).split(" ")
    val downRatio = if (totalUsage > 0) downUsage.toFloat() / totalUsage.toFloat() else 0.5f
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.label_usage_summary).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalParts[0],
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = totalParts.getOrNull(1) ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(downRatio.coerceAtLeast(0.01f))
                            .background(downloadColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - downRatio).coerceAtLeast(0.01f))
                            .background(uploadColor)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernUsageSubItem(
                    label = stringResource(R.string.label_download),
                    value = formatUsage(downUsage),
                    icon = AppIcons.Download,
                    color = downloadColor
                )
                
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                
                ModernUsageSubItem(
                    label = stringResource(R.string.label_upload),
                    value = formatUsage(upUsage),
                    icon = AppIcons.Upload,
                    color = uploadColor
                )
            }
        }
    }
}

@Composable
fun ModernUsageSubItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = color, 
                modifier = Modifier.size(14.dp)
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageFilterChip(
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier,
    onOptionSelected: (String) -> Unit,
) {
    var itemWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        val containerColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.secondaryContainer,
            label = "ChipContainerColor"
        )
        val contentColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.onSecondaryContainer,
            label = "ChipContentColor"
        )

        Surface(
            onClick = { onExpandedChange(true) },
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    itemWidth = with(density) { coordinates.size.width.toDp() }
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .width(itemWidth)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedText
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = { onOptionSelected(option); onExpandedChange(false) }
                )
            }
        }
    }
}

@Composable
fun AppUsageItem(
    appUsage: AppUsageInfo,
    displayUsage: Long,
    maxUsageBytes: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = if (maxUsageBytes > 0) (displayUsage.toFloat() / maxUsageBytes.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .bounceClick {
                if (appUsage.isSystemGroup) onClick()
                else expanded = !expanded
            },
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
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (appUsage.iconBitmap != null) {
                    Image(bitmap = appUsage.iconBitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                } else {
                    val fallbackIcon = when {
                        appUsage.packageName.startsWith("removed") -> Icons.Default.Delete
                        appUsage.packageName.startsWith("tethering") -> Icons.Default.Router
                        else -> Icons.Default.Android
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appUsage.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatUsage(displayUsage),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded && !appUsage.isSystemGroup,
                enter = fadeIn(premiumSpring()) + expandVertically(premiumSpring()),
                exit = fadeOut(premiumSpring()) + shrinkVertically(premiumSpring())
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    val downloadColor = MaterialTheme.colorScheme.primary
                    val uploadColor = MaterialTheme.colorScheme.tertiary
                    val wifiColor = MaterialTheme.colorScheme.secondary

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                ModernUsageSubItem(
                                    label = stringResource(R.string.label_download),
                                    value = formatUsage(appUsage.downUsage),
                                    icon = AppIcons.Download,
                                    color = downloadColor
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                ModernUsageSubItem(
                                    label = stringResource(R.string.label_wifi_usage),
                                    value = formatUsage(appUsage.wifiUsage),
                                    icon = AppIcons.Wifi,
                                    color = wifiColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(64.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                ModernUsageSubItem(
                                    label = stringResource(R.string.label_upload),
                                    value = formatUsage(appUsage.upUsage),
                                    icon = AppIcons.Upload,
                                    color = uploadColor
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                ModernUsageSubItem(
                                    label = stringResource(R.string.label_mobile_usage),
                                    value = formatUsage(appUsage.cellUsage),
                                    icon = AppIcons.Mobile,
                                    color = uploadColor
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}






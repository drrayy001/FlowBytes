package com.ray.flowmeter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.viewmodels.AppUsageInfo
import com.ray.flowmeter.ui.dialogs.UnifiedPickerContainer
import com.ray.flowmeter.ui.dialogs.UnifiedPickerHeader
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.theme.premiumSpring
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(
    viewModel: AppUsageViewModel,
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
                        else -> -indicatorSize - 20.dp // Hidden completely off-screen
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
                        // Uses graphicsLayer to directly manipulate GPU, preventing flashing
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppUsageFilterChip(
                                    selectedText = timeFilter,
                                    expanded = showTimeDropdown,
                                    onExpandedChange = setShowTimeDropdown,
                                    options = timeOptions,
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
                                ) { filter ->
                                    val storeValue = when (filter) {
                                        mobileOnlyFilterLabel -> "mobile"
                                        wifiOnlyFilterLabel -> "wifi"
                                        else -> "all"
                                    }
                                    viewModel.setNetworkFilter(storeValue)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val displayGlobalDown = when (networkFilter) {
                                mobileOnlyFilterLabel -> viewModel.globalCellDown
                                wifiOnlyFilterLabel -> viewModel.globalWifiDown
                                else -> viewModel.globalCellDown + viewModel.globalWifiDown
                            }
                            val displayGlobalUp = when (networkFilter) {
                                mobileOnlyFilterLabel -> viewModel.globalCellUp
                                wifiOnlyFilterLabel -> viewModel.globalWifiDown
                                else -> viewModel.globalCellUp + viewModel.globalWifiUp
                            }
                            val displayGlobalTotal = displayGlobalDown + displayGlobalUp

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            UsageSummaryCircle(
                                    usage = displayGlobalDown,
                                    icon = R.drawable.ic_arrow_down,
                                    color = MaterialTheme.colorScheme.primary,
                                    isSmall = true
                                )

                                UsageTotalCircle(
                                    totalUsage = displayGlobalTotal,
                                    primaryColor = MaterialTheme.colorScheme.primary,
                                    secondaryColor = MaterialTheme.colorScheme.secondary
                                )

                                UsageSummaryCircle(
                                    usage = displayGlobalUp,
                                    icon = R.drawable.ic_arrow_up,
                                    color = MaterialTheme.colorScheme.secondary,
                                    isSmall = true
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (timeFilter != customFilterLabel) {
                                    Box(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(48.dp)
                                            .bounceClick {
                                                viewModel.moveDate(backwards = true)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
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

                                Surface(
                                    modifier = Modifier.bounceClick {
                                        when (timeFilter) {
                                            monthFilterLabel -> setShowMonthPicker(true)
                                            customFilterLabel -> setShowDateRangePicker(true)
                                            else -> setShowDatePicker(true)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = displayedDateText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (timeFilter != customFilterLabel) {
                                    val now = Calendar.getInstance()
                                    val isForwardDisabled = if (timeFilter == monthFilterLabel) {
                                        (currentViewDate[Calendar.YEAR] >= now[Calendar.YEAR]) &&
                                                (currentViewDate[Calendar.MONTH] >= now[Calendar.MONTH])
                                    } else {
                                        (currentViewDate[Calendar.YEAR] >= now[Calendar.YEAR]) &&
                                                (currentViewDate[Calendar.DAY_OF_YEAR] >= now[Calendar.DAY_OF_YEAR])
                                    }

                                    Box(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(48.dp)
                                            .bounceClick(enabled = !isForwardDisabled) {
                                                viewModel.moveDate(backwards = false)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = if (isForwardDisabled) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
            itemsIndexed(
                        items = filteredAppList,
                        key = { _, it -> it.packageName }
                    ) { index, appUsage ->
                        val displayUsage = when (networkFilter) {
                            mobileOnlyFilterLabel -> appUsage.cellUsage
                            wifiOnlyFilterLabel -> appUsage.wifiUsage
                            else -> appUsage.totalUsage
                        }

                        StaggeredEntrance(index = index) {
                            AppUsageItem(
                                appUsage = appUsage,
                                displayUsage = displayUsage,
                                maxUsageBytes = maxUsageBytes,
                                locale = locale,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageFilterChip(
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
) {
    var itemWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box {
        FilterChip(
            selected = true,
            onClick = { onExpandedChange(true) },
            label = { Text(text = selectedText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = null,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                itemWidth = with(density) { coordinates.size.width.toDp() }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .width(itemWidth)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = option, 
                            color = if (option == selectedText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
    locale: Locale,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = if (maxUsageBytes > 0) (displayUsage.toFloat() / maxUsageBytes.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .bounceClick { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (appUsage.iconBitmap != null) {
                    Image(bitmap = appUsage.iconBitmap, contentDescription = null, modifier = Modifier.size(44.dp))
                } else {
                    val fallbackIcon = when {
                        appUsage.packageName.startsWith("removed") -> Icons.Default.Delete
                        appUsage.packageName.startsWith("tethering") -> Icons.Default.Router
                        else -> Icons.Default.Android
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = appUsage.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(text = formatUsage(displayUsage, locale), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val barPrimaryColor = MaterialTheme.colorScheme.primary
                    val barSecondaryColor = MaterialTheme.colorScheme.secondary
                    Canvas(modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)) {
                        drawLine(color = barPrimaryColor.copy(alpha = 0.15f), start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = size.height, cap = StrokeCap.Round)
                        if (progress > 0f) {
                            val progressWidth = size.width * progress
                            drawLine(color = barPrimaryColor, start = Offset(0f, size.height / 2), end = Offset(progressWidth, size.height / 2), strokeWidth = size.height, cap = StrokeCap.Round)
                            if (progress > 0.05f) {
                                drawLine(color = barSecondaryColor, start = Offset(maxOf(0f, progressWidth - 20f), size.height / 2), end = Offset(progressWidth, size.height / 2), strokeWidth = size.height, cap = StrokeCap.Round)
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn(premiumSpring()) + expandVertically(premiumSpring()), exit = fadeOut(premiumSpring()) + shrinkVertically(premiumSpring())) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                    .padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        UsageInfoItem(label = stringResource(R.string.label_download), value = formatUsage(appUsage.downUsage, locale))
                        UsageInfoItem(label = stringResource(R.string.label_upload), value = formatUsage(appUsage.upUsage, locale), alignment = Alignment.End)
                    }
                    Spacer(modifier = Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)); Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        UsageInfoItem(label = stringResource(R.string.label_wifi_usage), value = formatUsage(appUsage.wifiUsage, locale))
                        UsageInfoItem(label = stringResource(R.string.label_mobile_usage), value = formatUsage(appUsage.cellUsage, locale), alignment = Alignment.End)
                    }
                }
            }
        }
    }
}

@Composable
fun UsageInfoItem(
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start
        )
    }
}

@Composable
fun UsageSummaryCircle(usage: Long, icon: Int, color: Color, modifier: Modifier = Modifier, isSmall: Boolean = false) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        modifier = modifier.size(if (isSmall) 76.dp else 86.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painter = painterResource(id = icon), contentDescription = null, tint = color, modifier = Modifier.size(if (isSmall) 12.dp else 16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            val parts = formatUsage(usage, locale).split(" ")
            Text(text = parts[0], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, fontSize = if (isSmall) 14.sp else 16.sp, maxLines = 1)
            Text(text = parts.getOrNull(1) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = if (isSmall) 8.sp else 10.sp)
        }
    }
}

@Composable
fun UsageTotalCircle(totalUsage: Long, primaryColor: Color, secondaryColor: Color, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Box(modifier = modifier.size(115.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) { }
        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)) {
            drawArc(color = primaryColor.copy(alpha = 0.1f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = primaryColor, startAngle = -90f, sweepAngle = 260f, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = secondaryColor, startAngle = 170f, sweepAngle = 70f, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val parts = formatUsage(totalUsage, locale).split(" ")
            Text(text = parts[0], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, maxLines = 1)
            Text(text = parts.getOrNull(1) ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = "TOTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = primaryColor, fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

private fun formatUsage(bytes: Long, locale: Locale): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format(locale, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(locale, "%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(locale, "%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
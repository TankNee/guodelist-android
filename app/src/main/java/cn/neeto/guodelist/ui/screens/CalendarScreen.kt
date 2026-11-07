package cn.neeto.guodelist.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.ui.components.EmptyState
import cn.neeto.guodelist.ui.components.SwipeTaskRow
import cn.neeto.guodelist.ui.motion.rememberClickFeedback
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    tasks: List<TaskItem>,
    lists: Map<String, TaskList>,
    today: LocalDate,
    locale: Locale,
    onOpen: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onCreateAt: (LocalDate) -> Unit,
) {
    val colors = LocalGuoColors.current
    var mode by rememberSaveable { mutableStateOf("month") }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var monthAnchor by rememberSaveable {
        mutableLongStateOf(today.withDayOfMonth(1).toEpochDay())
    }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val visibleMonth = YearMonth.from(LocalDate.ofEpochDay(monthAnchor))
    val weekStart = Dates.startOfWeek(selected)
    val tasksByDate = remember(tasks) { tasks.groupBy { it.dueDate ?: "" } }
    val selectedTasks = remember(tasks, selected) {
        tasks.filter { it.dueDate == Dates.key(selected) }
            .sortedWith(compareBy({ it.dueTime ?: "99:99" }, { it.order }))
    }
    val title = if (mode == "week") {
        Dates.monthDay(weekStart, locale) + " – " + Dates.monthDay(weekStart.plusDays(6), locale)
    } else {
        Dates.monthTitle(visibleMonth.atDay(1), locale)
    }

    fun step(delta: Int) {
        if (mode == "week") {
            selectedEpoch = selected.plusDays(7L * delta).toEpochDay()
        } else {
            monthAnchor = visibleMonth.plusMonths(delta.toLong()).atDay(1).toEpochDay()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = title,
                    label = "calendarTitle",
                    transitionSpec = {
                        (fadeIn(tween(160)) + slideInHorizontally(tween(200)) { it / 10 }) togetherWith fadeOut(tween(100))
                    },
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            IconButton(onClick = { step(-1) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.cd_previous_month), tint = colors.ink2)
            }
            TextButton(
                onClick = {
                    selectedEpoch = today.toEpochDay()
                    if (mode == "month") {
                        monthAnchor = today.withDayOfMonth(1).toEpochDay()
                    }
                },
            ) {
                Icon(Icons.Outlined.Today, null, tint = colors.brandInk, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.calendar_today), color = colors.brandInk, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = { step(1) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.cd_next_month), tint = colors.ink2)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarModeSwitch(
                selected = mode,
                onSelect = { next ->
                    mode = next
                    if (next == "month") {
                        monthAnchor = selected.withDayOfMonth(1).toEpochDay()
                    }
                },
            )
        }

        if (mode == "month") {
            WeekHeader(locale)

            MonthGrid(
                month = visibleMonth,
                today = today,
                selected = selected,
                locale = locale,
                tasksByDate = tasksByDate,
                onSelect = { selectedEpoch = it.toEpochDay() },
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = colors.ink3,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = Dates.longDate(selected, locale),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                    Text(
                        text = stringResource(R.string.calendar_events_count, selectedTasks.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.ink3,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onCreateAt(selected) }) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.cd_add_task), tint = colors.brandInk)
                }
            }

            if (selectedTasks.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.calendar_selected_day_empty),
                    body = stringResource(R.string.empty_today_body),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(selectedTasks, key = { it.id }) { task ->
                        SwipeTaskRow(
                            task = task,
                            list = lists[task.listId],
                            today = today,
                            locale = locale,
                            showList = true,
                            onOpen = { onOpen(task) },
                            onToggle = { onToggle(task) },
                            onDelete = { onDelete(task) },
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                WeekView(
                    weekStart = weekStart,
                    today = today,
                    selected = selected,
                    tasks = tasks,
                    lists = lists,
                    locale = locale,
                    onOpen = onOpen,
                    onSelectDay = { selectedEpoch = it.toEpochDay() },
                )
            }
        }
    }
}

@Composable
private fun CalendarModeSwitch(selected: String, onSelect: (String) -> Unit) {
    val colors = LocalGuoColors.current
    val feedback = rememberClickFeedback()
    val buttonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = colors.brandTint,
        activeContentColor = colors.brandInk,
        activeBorderColor = colors.brand.copy(alpha = 0.3f),
        inactiveContainerColor = colors.panel,
        inactiveContentColor = colors.ink3,
        inactiveBorderColor = colors.lineStrong,
    )
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = selected == "month",
            onClick = { feedback(); onSelect("month") },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            colors = buttonColors,
            icon = { Icon(Icons.Outlined.CalendarViewMonth, null, modifier = Modifier.size(15.dp)) },
            label = { Text(stringResource(R.string.calendar_month_view), fontSize = 11.sp) },
        )
        SegmentedButton(
            selected = selected == "week",
            onClick = { feedback(); onSelect("week") },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            colors = buttonColors,
            icon = { Icon(Icons.Outlined.ViewWeek, null, modifier = Modifier.size(15.dp)) },
            label = { Text(stringResource(R.string.calendar_week_view), fontSize = 11.sp) },
        )
    }
}

@Composable
private fun WeekHeader(locale: Locale) {
    val colors = LocalGuoColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 6.dp)) {
        for (day in 1..7) {
            val date = LocalDate.of(2024, 1, day) // 2024-01-01 is a Monday
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    locale: Locale,
    tasksByDate: Map<String, List<TaskItem>>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGuoColors.current
    val first = month.atDay(1)
    val offset = Dates.mondayOffset(first)
    val total = month.lengthOfMonth()

    Column(modifier = modifier.padding(horizontal = 2.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayNumber = index - offset + 1
                    val date = if (dayNumber in 1..total) month.atDay(dayNumber) else null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(2.dp),
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                inMonth = true,
                                isToday = date == today,
                                isSelected = date == selected,
                                locale = locale,
                                tasks = tasksByDate[Dates.key(date)].orEmpty(),
                                onSelect = onSelect,
                            )
                        } else {
                            val faded = first.minusDays((offset - index).toLong())
                            if (faded.month == month.minusMonths(1).month) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = faded.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.ink3.copy(alpha = 0.25f),
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
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    locale: Locale,
    tasks: List<TaskItem>,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = LocalGuoColors.current
    val bg = when {
        isSelected && !isToday -> colors.brandTint
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onSelect(date) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isToday) colors.brand else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isToday -> colors.onBrand
                    isSelected -> colors.brandInk
                    inMonth -> colors.ink
                    else -> colors.ink3
                },
                fontSize = 13.sp,
            )
        }
        if (tasks.isNotEmpty() && !isToday && !isSelected) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 19.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                tasks.take(3).forEach { task ->
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (task.done) colors.ink3.copy(alpha = 0.45f) else colors.brand),
                    )
                }
            }
        }
    }
}

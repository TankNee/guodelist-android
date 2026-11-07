package cn.neeto.guodelist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private val HourHeight = 44.dp
private val GutterWidth = 42.dp

private data class EventTone(val bg: Color, val text: Color)

private val LightTones = listOf(
    EventTone(Color(0xFFFBE3E3), Color(0xFF8B2F2F)),
    EventTone(Color(0xFFFCE7D3), Color(0xFF8A4A1C)),
    EventTone(Color(0xFFFAF0C8), Color(0xFF74560F)),
    EventTone(Color(0xFFDBF0E2), Color(0xFF215B3B)),
    EventTone(Color(0xFFDAEAF7), Color(0xFF1D4C76)),
    EventTone(Color(0xFFE7E0F7), Color(0xFF483776)),
    EventTone(Color(0xFFD6EFEC), Color(0xFF1D5B57)),
    EventTone(Color(0xFFEEE6D8), Color(0xFF5B472A)),
)

private val DarkTones = listOf(
    EventTone(Color(0xFF4A2A2A), Color(0xFFF2B8B8)),
    EventTone(Color(0xFF4A3524), Color(0xFFEFC29A)),
    EventTone(Color(0xFF453C22), Color(0xFFE7D28A)),
    EventTone(Color(0xFF244534), Color(0xFFA9DCBC)),
    EventTone(Color(0xFF22384C), Color(0xFFA9CBE9)),
    EventTone(Color(0xFF342B4F), Color(0xFFC5B5E9)),
    EventTone(Color(0xFF1D3C38), Color(0xFF9FD8D2)),
    EventTone(Color(0xFF3A342A), Color(0xFFD1C0A5)),
)

private fun toneFor(listId: String, dark: Boolean): EventTone {
    val palette = if (dark) DarkTones else LightTones
    val hash = (listId.hashCode() and 0x7fffffff) % palette.size
    return palette[hash]
}

private data class TimedEvent(
    val task: TaskItem,
    val startMinutes: Int,
    val endMinutes: Int,
    val lane: Int,
    val lanes: Int,
)

private fun layoutDay(tasks: List<TaskItem>): List<TimedEvent> {
    val timed = tasks.mapNotNull { task ->
        val start = Dates.parseTime(task.dueTime) ?: return@mapNotNull null
        val end = Dates.parseTime(task.endTime) ?: start.plusHours(1)
        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute
        if (endMinutes <= startMinutes) endMinutes = startMinutes + 60
        Triple(task, startMinutes, endMinutes.coerceAtMost(24 * 60))
    }.sortedBy { it.second }

    val laneEnds = mutableListOf<Int>()
    val assigned = timed.map { (task, start, end) ->
        var lane = laneEnds.indexOfFirst { it <= start }
        if (lane < 0) {
            lane = laneEnds.size
            laneEnds.add(end)
        } else {
            laneEnds[lane] = end
        }
        Triple(task, start, end) to lane
    }
    val lanes = laneEnds.size.coerceAtLeast(1)
    return assigned.map { (triple, lane) ->
        TimedEvent(triple.first, triple.second, triple.third, lane, lanes)
    }
}

@Composable
fun WeekView(
    weekStart: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    tasks: List<TaskItem>,
    lists: Map<String, TaskList>,
    locale: Locale,
    onOpen: (TaskItem) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    val colors = LocalGuoColors.current
    val dark = isSystemInDarkTheme()
    val days = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val vScroll = rememberScrollState()
    val density = LocalDensity.current
    val tasksByDate = remember(tasks) { tasks.groupBy { it.dueDate ?: "" } }

    LaunchedEffect(Unit) {
        val startHour = (LocalTime.now().hour - 2).coerceAtLeast(0)
        vScroll.scrollTo((startHour * HourHeight.value * density.density).toInt())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(GutterWidth))
            days.forEach { date ->
                Box(modifier = Modifier.weight(1f)) {
                    DayHeader(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selected,
                        locale = locale,
                        onSelect = { onSelectDay(date) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 30.dp)
                .padding(vertical = 2.dp),
        ) {
            Box(
                modifier = Modifier.width(GutterWidth).padding(end = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.calendar_all_day),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                    fontSize = 9.sp,
                )
            }
            days.forEach { date ->
                val allDay = tasksByDate[Dates.key(date)].orEmpty()
                    .filter { it.dueTime == null }
                    .sortedBy { it.order }
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        allDay.take(2).forEach { task ->
                            val tone = toneFor(task.listId, dark)
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = tone.text,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tone.bg)
                                    .clickable { onOpen(task) }
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                            )
                        }
                        if (allDay.size > 2) {
                            Text(
                                text = "+${allDay.size - 2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.ink3,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(vScroll)) {
            Column(modifier = Modifier.width(GutterWidth)) {
                repeat(24) { hour ->
                    Box(modifier = Modifier.fillMaxWidth().height(HourHeight)) {
                        Text(
                            text = "%02d".format(hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.ink3,
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 1.dp),
                        )
                    }
                }
            }
            days.forEach { date ->
                Box(modifier = Modifier.weight(1f)) {
                    DayColumn(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selected,
                        tasks = tasksByDate[Dates.key(date)].orEmpty(),
                        dark = dark,
                        locale = locale,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    locale: Locale,
    onSelect: () -> Unit,
) {
    val colors = LocalGuoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = Dates.weekdayNarrow(date, locale),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) colors.brandInk else colors.ink3,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isToday -> colors.brand
                        isSelected -> colors.brandTint
                        else -> Color.Transparent
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isToday -> colors.onBrand
                    isSelected -> colors.brandInk
                    else -> colors.ink
                },
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    tasks: List<TaskItem>,
    dark: Boolean,
    locale: Locale,
    onOpen: (TaskItem) -> Unit,
) {
    val colors = LocalGuoColors.current
    val timed = remember(tasks) { layoutDay(tasks) }
    val now = LocalTime.now()
    val nowOffset = (now.hour * 60 + now.minute) / 60f * HourHeight.value

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(HourHeight * 24f)
            .background(if (isSelected) colors.brandTint.copy(alpha = 0.28f) else Color.Transparent)
            .drawBehind {
                drawLine(colors.line, Offset(0f, 0f), Offset(0f, size.height), 1f)
                for (hour in 0..24) {
                    val y = hour * HourHeight.toPx()
                    drawLine(colors.line.copy(alpha = 0.65f), Offset(0f, y), Offset(size.width, y), 1f)
                }
            },
    ) {
        val columnWidth = maxWidth
        timed.forEach { event ->
            val tone = toneFor(event.task.listId, dark)
            val laneWidth = columnWidth / event.lanes.toFloat()
            val blockHeight = ((event.endMinutes - event.startMinutes) / 60f * HourHeight.value)
                .coerceAtLeast(22f)
                .dp
            Box(
                modifier = Modifier
                    .offset(
                        x = laneWidth * event.lane.toFloat(),
                        y = (event.startMinutes / 60f * HourHeight.value).dp,
                    )
                    .width((laneWidth - 2.dp).coerceAtLeast(16.dp))
                    .height(blockHeight)
                    .clip(RoundedCornerShape(5.dp))
                    .background(tone.bg)
                    .clickable { onOpen(event.task) }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                Text(
                    text = event.task.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = tone.text,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isToday) {
            Box(
                modifier = Modifier
                    .offset(y = nowOffset.dp)
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(colors.brand),
            )
            Box(
                modifier = Modifier
                    .offset(x = (-3).dp, y = (nowOffset - 3).dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.brand),
            )
        }
    }
}

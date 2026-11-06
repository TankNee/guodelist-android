package cn.neeto.guodelist.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.ui.motion.rememberClickFeedback
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import java.time.LocalDate
import java.util.Locale

@Composable
fun TodayFeed(
    tasks: List<TaskItem>,
    lists: Map<String, TaskList>,
    today: LocalDate,
    locale: Locale,
    onOpen: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
) {
    val colors = LocalGuoColors.current
    val open = tasks.filterNot { it.done }
    val todayKey = Dates.key(today)
    val doneToday = tasks
        .filter { it.done && (it.dueDate == todayKey || it.completedAt?.take(10) == todayKey) }
        .sortedByDescending { it.completedAt ?: it.updatedAt }
    val overdue = open.filter { task -> Dates.parse(task.dueDate)?.isBefore(today) == true }
        .sortedBy { it.order }
    val todays = open.filter { Dates.parse(it.dueDate) == today }.sortedBy { it.order }
    val undated = open.filter { it.dueDate == null }.sortedBy { it.order }
    val totalToday = todays.size + doneToday.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") {
            TodayProgress(done = doneToday.size, total = totalToday)
        }

        if (overdue.isNotEmpty()) {
            item(key = "overdue-label") {
                GroupLabel(
                    text = stringResource(R.string.due_overdue),
                    count = overdue.size,
                    color = colors.danger,
                    icon = Icons.Outlined.Warning,
                )
            }
            items(overdue, key = { it.id }) { task ->
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

        item(key = "today-label") {
            GroupLabel(
                text = stringResource(R.string.common_today),
                count = todays.size,
                color = colors.brandInk,
                icon = Icons.Outlined.Today,
            )
        }
        if (todays.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.empty_today_title),
                    body = stringResource(R.string.empty_today_body),
                )
            }
        } else {
            items(todays, key = { it.id }) { task ->
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

        if (undated.isNotEmpty()) {
            item(key = "undated-label") {
                GroupLabel(
                    text = stringResource(R.string.section_nodate),
                    count = undated.size,
                    color = colors.ink3,
                    icon = Icons.Outlined.Schedule,
                )
            }
            items(undated, key = { it.id }) { task ->
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

        if (doneToday.isNotEmpty()) {
            item(key = "done-label") {
                GroupLabel(
                    text = stringResource(R.string.header_completed),
                    count = doneToday.size,
                    color = colors.ink3,
                    icon = Icons.Outlined.CheckCircle,
                )
            }
            items(doneToday, key = { "done-${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    list = lists[task.listId],
                    today = today,
                    locale = locale,
                    showList = true,
                    onClick = { onOpen(task) },
                    onToggle = { onToggle(task) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.panel),
                )
            }
        }
    }
}

@Composable
private fun TodayProgress(done: Int, total: Int) {
    val colors = LocalGuoColors.current
    if (total == 0) return
    val progress by animateFloatAsState(
        targetValue = done.toFloat() / total,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 220f),
        label = "todayProgress",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.line),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.brand),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.today_progress, done, total),
            style = MaterialTheme.typography.labelSmall,
            color = colors.ink3,
        )
    }
}

@Composable
fun TaskFeed(
    tasks: List<TaskItem>,
    lists: Map<String, TaskList>,
    today: LocalDate,
    locale: Locale,
    emptyTitle: String,
    emptyBody: String,
    showList: Boolean,
    onOpen: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    contentPadding: PaddingValues = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 104.dp),
    startOnDone: Boolean = false,
) {
    val colors = LocalGuoColors.current
    val open = tasks.filterNot { it.done }
    val done = tasks.filter { it.done }.sortedByDescending { it.completedAt ?: it.updatedAt }
    val overdue = open.filter { Dates.parse(it.dueDate)?.isBefore(today) == true }.sortedBy { it.order }
    val todays = open.filter { Dates.parse(it.dueDate) == today }.sortedBy { it.order }
    val upcoming = open.filter { (Dates.parse(it.dueDate)?.isAfter(today) == true) }
        .sortedWith(compareBy({ it.dueDate ?: "" }, { it.order }))
    val undated = open.filter { it.dueDate == null }.sortedBy { it.order }
    val overdueLabel = stringResource(R.string.due_overdue)
    val todayLabel = stringResource(R.string.common_today)
    val upcomingLabel = stringResource(R.string.section_upcoming)
    val noDateLabel = stringResource(R.string.section_nodate)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (startOnDone) {
            if (done.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.empty_done_title),
                        body = stringResource(R.string.empty_done_body),
                    )
                }
            } else {
                item(key = "done-label") {
                    GroupLabel(
                        text = stringResource(R.string.header_completed),
                        count = done.size,
                        color = colors.ink3,
                        icon = Icons.Outlined.CheckCircle,
                    )
                }
                items(done, key = { it.id }) { task ->
                    DoneRow(task, lists[task.listId], today, locale, showList, onOpen, onToggle)
                }
            }
        } else {
            if (open.isEmpty() && done.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = emptyTitle, body = emptyBody)
                }
            }
            val groups = listOf(
                GroupSpec(overdueLabel, overdue, colors.danger, Icons.Outlined.Warning),
                GroupSpec(todayLabel, todays, colors.brandInk, Icons.Outlined.Today),
                GroupSpec(upcomingLabel, upcoming, colors.ink3, Icons.Outlined.CalendarMonth),
                GroupSpec(noDateLabel, undated, colors.ink3, Icons.Outlined.Schedule),
            )
            groups.forEach { group ->
                if (group.tasks.isEmpty()) return@forEach
                item(key = "label-${group.label}") {
                    GroupLabel(
                        text = group.label,
                        count = group.tasks.size,
                        color = group.color,
                        icon = group.icon,
                    )
                }
                items(group.tasks, key = { it.id }) { task ->
                    SwipeTaskRow(
                        task = task,
                        list = lists[task.listId],
                        today = today,
                        locale = locale,
                        showList = showList,
                        onOpen = { onOpen(task) },
                        onToggle = { onToggle(task) },
                        onDelete = { onDelete(task) },
                    )
                }
            }
            if (done.isNotEmpty()) {
                item(key = "done-label") {
                    GroupLabel(
                        text = stringResource(R.string.header_completed),
                        count = done.size,
                        color = colors.ink3,
                        icon = Icons.Outlined.CheckCircle,
                    )
                }
                items(done, key = { "done-${it.id}" }) { task ->
                    DoneRow(task, lists[task.listId], today, locale, showList, onOpen, onToggle)
                }
            }
        }
    }
}

@Composable
private fun DoneRow(
    task: TaskItem,
    list: TaskList?,
    today: LocalDate,
    locale: Locale,
    showList: Boolean,
    onOpen: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
) {
    TaskRow(
        task = task,
        list = list,
        today = today,
        locale = locale,
        showList = showList,
        onClick = { onOpen(task) },
        onToggle = { onToggle(task) },
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LocalGuoColors.current.panel),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeTaskRow(
    task: TaskItem,
    list: TaskList?,
    today: LocalDate,
    locale: Locale,
    showList: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalGuoColors.current
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
                else -> Unit
            }
            false
        },
        positionalThreshold = { distance -> distance * 0.60f },
    )

    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp))) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.fillMaxWidth(),
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                val (bg, tint, icon, label) = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> SwipeStyle(
                        colors.success.copy(alpha = 0.16f),
                        colors.success,
                        Icons.Filled.Check,
                        stringResource(R.string.filter_done),
                    )
                    else -> SwipeStyle(
                        colors.danger.copy(alpha = 0.14f),
                        colors.danger,
                        Icons.Outlined.Delete,
                        stringResource(R.string.common_delete),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                        Arrangement.Start
                    } else {
                        Arrangement.End
                    },
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = tint, style = MaterialTheme.typography.labelMedium)
                }
            },
        ) {
            TaskRow(
                task = task,
                list = list,
                today = today,
                locale = locale,
                showList = showList,
                onClick = onOpen,
                onToggle = onToggle,
                modifier = Modifier.background(colors.panel),
            )
        }
    }
}

private data class SwipeStyle(
    val background: Color,
    val tint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
)

private data class GroupSpec(
    val label: String,
    val tasks: List<TaskItem>,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun GroupLabel(
    text: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}

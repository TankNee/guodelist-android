package cn.neeto.guodelist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.model.Priority
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.ui.motion.guoClickable
import cn.neeto.guodelist.ui.motion.pressScale
import cn.neeto.guodelist.ui.motion.rememberClickFeedback
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun TaskCheck(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    checkSize: Dp = 24.dp,
) {
    val colors = LocalGuoColors.current
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }

    val color by animateColorAsState(
        targetValue = if (checked) colors.brand else colors.lineStrong,
        animationSpec = tween(160),
        label = "checkColor",
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Checkbox,
                onClickLabel = "toggle",
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!checked) {
                    scope.launch {
                        progress.animateTo(1f, tween(150))
                        onToggle()
                    }
                } else {
                    onToggle()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(checkSize)) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(color = color.copy(alpha = 0.16f), radius = radius)
            drawCircle(
                color = color,
                radius = radius - 1.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            if (progress.value > 0.01f) {
                drawCircle(color = color, radius = radius - 1.dp.toPx(), alpha = progress.value)
                val path = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.68f)
                    lineTo(size.width * 0.73f, size.height * 0.35f)
                }
                scale(scale = progress.value, pivot = center) {
                    drawPath(
                        path = path,
                        color = colors.onBrand,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRow(
    task: TaskItem,
    list: TaskList?,
    today: LocalDate,
    locale: Locale,
    showList: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGuoColors.current
    val interaction = remember { MutableInteractionSource() }
    val dueDate = Dates.parse(task.dueDate)
    val done = task.done
    val overdue = !done && dueDate != null && dueDate.isBefore(today)
    val dueText = dueDate?.let { date ->
        val time = Dates.parseTime(task.dueTime)
        val label = dueLabel(date, today, locale)
        if (time != null) "$label ${Dates.formatTime(time, locale)}" else label
    }
    val priorityText = if (task.priority != Priority.NONE && !done) priorityLabel(task.priority) else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .guoClickable(interactionSource = interaction, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheck(checked = done, onToggle = onToggle)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) colors.ink3 else colors.ink,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            val meta = buildList {
                if (showList && list != null) add("${list.emoji} ${list.name}")
                if (dueText != null) add(dueText)
                val (doneCount, total) = task.subtasks.count { it.done } to task.subtasks.size
                if (total > 0) add("$doneCount/$total")
                if (task.recurrence != null) add("↻")
                if (priorityText != null) add(priorityText)
            }
            if (meta.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Text(
                        text = meta.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            done -> colors.ink3
                            overdue -> colors.danger
                            dueDate == today -> colors.brandInk
                            else -> colors.ink3
                        },
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (task.priority != Priority.NONE && !done) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(priorityColor(task.priority)),
            )
        }
    }
}

@Composable
fun dueLabel(date: LocalDate, today: LocalDate, locale: Locale): String {
    val diff = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    val override = when (diff) {
        0L -> stringResource(R.string.common_today)
        1L -> stringResource(R.string.common_tomorrow)
        -1L -> stringResource(R.string.common_yesterday)
        else -> null
    }
    return override ?: Dates.shortDate(date, locale)
}

@Composable
fun priorityLabel(priority: Priority): String = stringResource(
    when (priority) {
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
        Priority.URGENT -> R.string.priority_urgent
        Priority.NONE -> R.string.priority_none
    },
)

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.LOW -> Color(0xFF4F7259)
    Priority.MEDIUM -> Color(0xFF8A5F14)
    Priority.HIGH -> Color(0xFFA8411F)
    Priority.URGENT -> Color(0xFFA82A1A)
    Priority.NONE -> Color(0xFF6D655B)
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGuoColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = colors.brand.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink3,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun GuoTopBar(
    title: String,
    subtitle: String? = null,
    onMenu: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalGuoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
    ) {
        val feedback = rememberClickFeedback()
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onMenu != null) {
                IconButton(onClick = { feedback(); onMenu() }) {
                    Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.cd_menu), tint = colors.ink2)
                }
            } else if (leadingIcon != null) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(leadingIcon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(22.dp))
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium, color = colors.ink, maxLines = 1)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.ink3,
                        maxLines = 1,
                    )
                }
            }
            if (onSearch != null) {
                IconButton(onClick = { feedback(); onSearch() }) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.nav_search), tint = colors.ink2)
                }
            }
            actions()
        }
    }
}

enum class Tab { TASKS, CALENDAR, SETTINGS }

@Composable
fun GuoBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = LocalGuoColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.brandInk,
        selectedTextColor = colors.brandInk,
        indicatorColor = colors.brandTint,
        unselectedIconColor = colors.ink3,
        unselectedTextColor = colors.ink3,
    )
    NavigationBar(
        containerColor = colors.panel,
        tonalElevation = 0.dp,
    ) {
        val feedback = rememberClickFeedback()
        NavigationBarItem(
            selected = selected == Tab.TASKS,
            onClick = { feedback(); onSelect(Tab.TASKS) },
            icon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_tasks)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == Tab.CALENDAR,
            onClick = { feedback(); onSelect(Tab.CALENDAR) },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_calendar)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == Tab.SETTINGS,
            onClick = { feedback(); onSelect(Tab.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            colors = itemColors,
        )
    }
}

@Composable
private fun BottomItem(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val colors = LocalGuoColors.current
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(if (selected) colors.brandInk else colors.ink3, tween(60), label = "tabTint")
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = 0.55f, stiffness = 1900f), label = "tabScale")
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .pressScale(interaction, pressedScale = 0.92f, stiffness = 2200)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription, tint = LocalContentColor.current)
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    val colors = LocalGuoColors.current
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.cd_add_task), tint = colors.brandInk)
    }
}

@Composable
fun CloseButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.nav_close), tint = LocalGuoColors.current.ink3)
    }
}

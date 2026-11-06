package cn.neeto.guodelist.ui.screens

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.neeto.guodelist.BuildConfig
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.i18n.LocaleController
import cn.neeto.guodelist.core.model.Priority
import cn.neeto.guodelist.core.model.RepeatFreq
import cn.neeto.guodelist.core.model.SubtaskItem
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.ui.components.TaskCheck
import cn.neeto.guodelist.ui.components.dueLabel
import cn.neeto.guodelist.ui.motion.guoClickable
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: TaskItem,
    lists: List<TaskList>,
    today: LocalDate,
    locale: Locale,
    language: String,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onTitle: (String) -> Unit,
    onNotes: (String) -> Unit,
    onPriority: (String) -> Unit,
    onList: (String) -> Unit,
    onDate: (LocalDate?) -> Unit,
    onTime: (LocalTime?) -> Unit,
    onRepeat: (RepeatFreq?) -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onRemoveSubtask: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalGuoColors.current
    val baseContext = LocalContext.current
    val currentLocale = LocalConfiguration.current.locales[0]
    val resources = remember(currentLocale, baseContext) {
        LocaleController.languageContext(baseContext, currentLocale.language).resources
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val dueDate = Dates.parse(task.dueDate)
    val dueTime = Dates.parseTime(task.dueTime)
    val list = lists.firstOrNull { it.id == task.listId }
    val focus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val expanded by remember { derivedStateOf { sheetState.targetValue == SheetValue.Expanded } }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topInset = if (expanded) statusBarTop else 0.dp

    val dragToExpand = Modifier.pointerInput(sheetState) {
        var requested = false
        detectVerticalDragGestures(
            onDragStart = { requested = false },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                if (!requested && dragAmount < -4f) {
                    requested = true
                    scope.launch { sheetState.expand() }
                }
            },
        )
    }

    var title by remember(task.id) { mutableStateOf(task.title) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPriority by remember { mutableStateOf(false) }
    var showLists by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var subtaskText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (task.title.isBlank()) {
            kotlinx.coroutines.delay(260)
            focus.requestFocus()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.panel,
        contentColor = colors.ink,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp + topInset, bottom = 2.dp)
                    .then(dragToExpand)
                    .clickable {
                        scope.launch {
                            if (sheetState.currentValue != SheetValue.Expanded) sheetState.expand()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.lineStrong),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topInset, bottom = 26.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().then(dragToExpand),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TaskCheck(checked = task.done, onToggle = onToggle)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = resources.getString(R.string.task_detail_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink3,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Outlined.Delete, resources.getString(R.string.common_delete), tint = colors.ink3)
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = resources.getString(R.string.common_save),
                        color = colors.brandInk,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    onTitle(it)
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).padding(vertical = 6.dp),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = if (task.done) colors.ink3 else colors.ink,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box {
                    MetaChip(
                        icon = Icons.Outlined.CalendarMonth,
                        text = dueDate?.let { dueLabel(it, today, locale, resources) } ?: resources.getString(R.string.task_due_label),
                        active = dueDate != null,
                        onClick = { showDatePicker = true },
                    )
                }
                Box {
                    MetaChip(
                        icon = Icons.Outlined.Schedule,
                        text = dueTime?.let { Dates.formatTime(it, locale) } ?: resources.getString(R.string.task_time_label),
                        active = dueTime != null,
                        onClick = { showTimePicker = true },
                    )
                }
                Box {
                    MetaChip(
                        icon = Icons.Outlined.Flag,
                        text = cn.neeto.guodelist.ui.components.priorityLabel(task.priority, resources),
                        active = task.priority != Priority.NONE,
                        onClick = { showPriority = true },
                    )
                    DropdownMenu(expanded = showPriority, onDismissRequest = { showPriority = false }) {
                        Priority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(cn.neeto.guodelist.ui.components.priorityLabel(priority, resources)) },
                                onClick = {
                                    onPriority(priority.raw)
                                    showPriority = false
                                },
                            )
                        }
                    }
                }
                Box {
                    MetaChip(
                        icon = Icons.Outlined.Sell,
                        text = list?.let { "${it.emoji} ${it.name}" } ?: resources.getString(R.string.task_list_label),
                        active = list != null,
                        onClick = { showLists = true },
                    )
                    DropdownMenu(expanded = showLists, onDismissRequest = { showLists = false }) {
                        DropdownMenuItem(
                            text = { Text("📥 " + resources.getString(R.string.header_inbox)) },
                            onClick = {
                                onList("inbox")
                                showLists = false
                            },
                        )
                        lists.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.emoji}  ${item.name}") },
                                onClick = {
                                    onList(item.id)
                                    showLists = false
                                },
                            )
                        }
                    }
                }
                Box {
                    MetaChip(
                        icon = Icons.Outlined.Repeat,
                        text = repeatLabel(task.repeatFreq, resources),
                        active = task.repeatFreq != null,
                        onClick = { showRepeat = true },
                    )
                    DropdownMenu(expanded = showRepeat, onDismissRequest = { showRepeat = false }) {
                        DropdownMenuItem(
                            text = { Text(resources.getString(R.string.repeat_none)) },
                            onClick = {
                                onRepeat(null)
                                showRepeat = false
                            },
                        )
                        RepeatFreq.entries.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(repeatLabel(freq, resources)) },
                                onClick = {
                                    onRepeat(freq)
                                    showRepeat = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = resources.getString(R.string.task_notes_label),
                style = MaterialTheme.typography.labelSmall,
                color = colors.ink3,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.panel2)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        onNotes(it)
                    },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    textStyle = TextStyle(
                        color = colors.ink2,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brand),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) {
                            Text(
                                text = resources.getString(R.string.task_notes_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink3.copy(alpha = 0.7f),
                            )
                        }
                        inner()
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = resources.getString(R.string.task_subtasks_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink3,
                )
                Spacer(Modifier.width(8.dp))
                if (task.subtasks.isNotEmpty()) {
                    Text(
                        text = resources.getString(
                            R.string.task_subtask_progress,
                            task.subtasks.count { it.done },
                            task.subtasks.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.ink3,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            task.subtasks.forEach { subtask ->
                SubtaskRow(
                    subtask = subtask,
                    onToggle = { onToggleSubtask(subtask.id) },
                    onRemove = { onRemoveSubtask(subtask.id) },
                )
            }
            BasicTextField(
                value = subtaskText,
                onValueChange = { subtaskText = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (subtaskText.isNotBlank()) {
                        onAddSubtask(subtaskText)
                        subtaskText = ""
                    }
                }),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add, null, tint = colors.ink3, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (subtaskText.isEmpty()) {
                                Text(
                                    text = resources.getString(R.string.task_add_subtask_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.ink3.copy(alpha = 0.7f),
                                )
                            }
                            inner()
                        }
                    }
                },
            )

            Spacer(Modifier.height(22.dp))
            Text(
                text = resources.getString(R.string.version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = colors.ink3.copy(alpha = 0.7f),
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(resources.getString(R.string.common_save), color = colors.brandInk) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onDate(null)
                        onTime(null)
                        showDatePicker = false
                    }) { Text(resources.getString(R.string.task_no_due), color = colors.ink3) }
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(resources.getString(R.string.common_cancel))
                    }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initial = dueTime,
            resources = resources,
            onDismiss = { showTimePicker = false },
            onClear = {
                onTime(null)
                showTimePicker = false
            },
            onConfirm = { time ->
                if (dueDate == null) onDate(today)
                onTime(time)
                showTimePicker = false
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(resources.getString(R.string.task_delete)) },
            text = { Text(resources.getString(R.string.task_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text(resources.getString(R.string.common_delete), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(resources.getString(R.string.common_cancel)) }
            },
            containerColor = colors.panel,
        )
    }
}

@Composable
private fun MetaChip(
    icon: ImageVector,
    text: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalGuoColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) colors.brandTint else colors.panel)
            .border(
                width = 1.dp,
                color = if (active) colors.brand.copy(alpha = 0.25f) else colors.lineStrong,
                shape = CircleShape,
            )
            .guoClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) colors.brandInk else colors.ink3,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) colors.brandInk else colors.ink3,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SubtaskRow(subtask: SubtaskItem, onToggle: () -> Unit, onRemove: () -> Unit) {
    val colors = LocalGuoColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TaskCheck(checked = subtask.done, onToggle = onToggle, checkSize = 19.dp)
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.done) colors.ink3 else colors.ink,
            textDecoration = if (subtask.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Outlined.Close, null, tint = colors.ink3.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime?,
    resources: Resources,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val colors = LocalGuoColors.current
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 9,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = colors.panel,
            contentColor = colors.ink,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = resources.getString(R.string.task_time_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                TimePicker(state = state, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClear) {
                        Text(resources.getString(R.string.task_clear_time), color = colors.ink3)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(resources.getString(R.string.common_cancel), color = colors.ink3)
                    }
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                        Text(resources.getString(R.string.common_save), color = colors.brandInk)
                    }
                }
            }
        }
    }
}

@Composable
fun repeatLabel(freq: RepeatFreq?, resources: Resources): String = when (freq) {
    RepeatFreq.DAILY -> resources.getString(R.string.repeat_daily)
    RepeatFreq.WEEKDAYS -> resources.getString(R.string.repeat_weekdays)
    RepeatFreq.WEEKLY -> resources.getString(R.string.repeat_weekly)
    RepeatFreq.MONTHLY -> resources.getString(R.string.repeat_monthly)
    null -> resources.getString(R.string.task_repeat_label)
}

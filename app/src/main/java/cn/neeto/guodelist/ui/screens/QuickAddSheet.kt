package cn.neeto.guodelist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.neeto.guodelist.R
import cn.neeto.guodelist.core.data.TaskOps
import cn.neeto.guodelist.core.model.TaskList
import cn.neeto.guodelist.core.util.Dates
import cn.neeto.guodelist.core.util.NaturalDateParser
import cn.neeto.guodelist.ui.components.dueLabel
import cn.neeto.guodelist.ui.motion.guoClickable
import cn.neeto.guodelist.ui.motion.rememberClickFeedback
import cn.neeto.guodelist.ui.theme.LocalGuoColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    lists: List<TaskList>,
    defaultListId: String,
    onDismiss: () -> Unit,
    onAdd: (TaskOps.NewTask) -> Unit,
) {
    val colors = LocalGuoColors.current
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val feedback = rememberClickFeedback()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    var text by rememberSaveable { mutableStateOf("") }
    var listId by rememberSaveable { mutableStateOf(defaultListId) }
    var showLists by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dateOverride by rememberSaveable { mutableStateOf<String?>(null) }
    val parsed = remember(text) { NaturalDateParser.parse(text) }
    val selectedList = lists.firstOrNull { it.id == listId }
    val parsedDate = parsed.dueDate
    val dueDate = when (dateOverride) {
        "none" -> null
        null -> parsedDate
        else -> dateOverride?.toLongOrNull()?.let(LocalDate::ofEpochDay) ?: parsedDate
    }
    val parsedDateLabel = dueDate?.let { dueLabel(it, LocalDate.now(), locale) }
    val parsedTimeLabel = parsed.dueTime?.let { Dates.formatTime(it, locale) }

    fun submit() {
        if (parsed.title.isBlank()) return
        onAdd(
            TaskOps.NewTask(
                title = parsed.title,
                listId = listId,
                dueDate = dueDate,
                dueTime = parsed.dueTime,
            ),
        )
    }

    LaunchedEffect(Unit) {
        sheetState.expand()
        delay(180)
        focus.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.panel,
        contentColor = colors.ink,
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
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
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_add_title),
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink3,
            )
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = colors.ink),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.quick_add_hint),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink3.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InlineChip(
                    icon = if (dueDate != null) Icons.Outlined.CalendarMonth else Icons.Outlined.Schedule,
                    label = buildString {
                        append(parsedDateLabel ?: stringResource(R.string.quick_add_no_date))
                        if (parsedTimeLabel != null) {
                            append("  ")
                            append(parsedTimeLabel)
                        }
                    },
                    onClick = { showDatePicker = true },
                    active = dueDate != null,
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    InlineChip(
                        icon = null,
                        label = selectedList?.let { "${it.emoji} ${it.name}" }
                            ?: (if (listId == "inbox") "📥 " + stringResource(R.string.header_inbox)
                            else stringResource(R.string.quick_add_list)),
                        onClick = { showLists = true },
                        trailingArrow = true,
                    )
                    DropdownMenu(expanded = showLists, onDismissRequest = { showLists = false }) {
                        DropdownMenuItem(
                            text = { Text("📥 " + stringResource(R.string.header_inbox)) },
                            onClick = {
                                listId = "inbox"
                                showLists = false
                            },
                        )
                        lists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text("${list.emoji}  ${list.name}") },
                                onClick = {
                                    listId = list.id
                                    showLists = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (parsedDateLabel != null) {
                        stringResource(R.string.quick_add_parsed) + " " + buildString {
                            append(parsedDateLabel)
                            if (parsedTimeLabel != null) {
                                append(" · ")
                                append(parsedTimeLabel)
                            }
                        }
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink3,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { feedback(); submit() },
                    enabled = parsed.title.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brand,
                        contentColor = colors.onBrand,
                        disabledContainerColor = colors.line,
                        disabledContentColor = colors.ink3,
                    ),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.quick_add_submit))
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = dueDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            dateOverride = date.toEpochDay().toString()
                        } ?: run { dateOverride = "none" }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_save), color = colors.brandInk)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            dateOverride = "none"
                            showDatePicker = false
                        },
                    ) {
                        Text(stringResource(R.string.task_no_due), color = colors.ink3)
                    }
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun InlineChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    onClick: () -> Unit,
    active: Boolean = true,
    trailingArrow: Boolean = false,
) {
    val colors = LocalGuoColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) colors.brandTint else colors.panel2)
            .guoClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (active) colors.brandInk else colors.ink3, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) colors.brandInk else colors.ink3,
            fontSize = 12.sp,
        )
        if (trailingArrow) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = colors.ink3, modifier = Modifier.size(14.dp))
        }
    }
}

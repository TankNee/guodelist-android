package cn.neeto.guodelist.widget

import android.content.Context
import cn.neeto.guodelist.core.i18n.LocaleController
import cn.neeto.guodelist.core.model.StoredData
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.util.Dates
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

data class WidgetRow(
    val id: String,
    val title: String,
    val time: String?,
)

data class WidgetSnapshot(
    val dateLabel: String,
    val weekdayLabel: String,
    val openCount: Int,
    val doneCount: Int,
    val rows: List<WidgetRow>,
    val more: Int = 0,
)

/** Reads the same private JSON document the app writes; no sync or DB access. */
object WidgetData {

    private val json = Json { ignoreUnknownKeys = true }

    fun today(context: Context, maxRows: Int = 5): WidgetSnapshot {
        val localized = LocaleController.backgroundContext(context)
        val locale = localized.resources.configuration.locales[0]
        val today = LocalDate.now()
        val dateLabel = Dates.monthDay(today, locale)
        val weekdayLabel = Dates.weekdayLong(today, locale)
        val tasks = tasks(context)
        val selectedLists = selectedLists(context)

        val todays = tasks
            .filter { it.dueDate == Dates.key(today) }
            .filter { selectedLists.isEmpty() || it.listId in selectedLists }
            .sortedWith(compareBy({ it.dueTime ?: "99:99" }, { it.order }))

        val open = todays.filterNot { it.done }
        val done = todays.count { it.done }
        val limit = maxRows.coerceIn(1, 12)
        val shown = if (open.size > limit) open.take((limit - 1).coerceAtLeast(0)) else open.take(limit)
        val rows = shown.map { task ->
            WidgetRow(
                id = task.id,
                title = task.title,
                time = Dates.parseTime(task.dueTime)?.let { Dates.formatTime(it, locale) },
            )
        }
        return WidgetSnapshot(
            dateLabel = dateLabel,
            weekdayLabel = weekdayLabel,
            openCount = open.size,
            doneCount = done,
            rows = rows,
            more = (open.size - rows.size).coerceAtLeast(0),
        )
    }

    fun upcoming(context: Context, maxRows: Int = 3): WidgetSnapshot {
        val localized = LocaleController.backgroundContext(context)
        val locale = localized.resources.configuration.locales[0]
        val today = LocalDate.now()
        val tasks = tasks(context)
        val selectedLists = selectedLists(context)

        val next = tasks
            .filterNot { it.done }
            .filter { selectedLists.isEmpty() || it.listId in selectedLists }
            .filter { task -> Dates.parse(task.dueDate)?.isBefore(today) == false }
            .sortedWith(compareBy({ it.dueDate ?: "9999-99-99" }, { it.dueTime ?: "99:99" }, { it.order }))

        val limit = maxRows.coerceIn(1, 8)
        val shown = next.take(limit)
        val rows = shown.map { task ->
            val date = Dates.parse(task.dueDate)
            val time = Dates.parseTime(task.dueTime)
            val label = when (date) {
                today -> time?.let { Dates.formatTime(it, locale) }
                null -> null
                else -> Dates.monthDay(date, locale)
            }
            WidgetRow(
                id = task.id,
                title = task.title,
                time = label,
            )
        }
        return WidgetSnapshot(
            dateLabel = Dates.monthDay(today, locale),
            weekdayLabel = Dates.weekdayLong(today, locale),
            openCount = next.size,
            doneCount = 0,
            rows = rows,
            more = (next.size - rows.size).coerceAtLeast(0),
        )
    }

    private fun tasks(context: Context): List<TaskItem> {
        val file = File(context.filesDir, "guodelist.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<StoredData>(file.readText()).tasks.map(::TaskItem)
        }.getOrDefault(emptyList())
    }

    private fun selectedLists(context: Context): Set<String> = context
        .getSharedPreferences("guodelist_settings", Context.MODE_PRIVATE)
        .getStringSet("widget_list_ids", emptySet())
        .orEmpty()
}

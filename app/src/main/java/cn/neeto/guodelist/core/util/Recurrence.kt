package cn.neeto.guodelist.core.util

import cn.neeto.guodelist.core.model.RepeatFreq
import cn.neeto.guodelist.core.model.TaskItem
import cn.neeto.guodelist.core.model.double
import cn.neeto.guodelist.core.model.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate

/** Mirrors `src/lib/recurrence.ts`: the next occurrence is always in the future. */
object Recurrence {

    fun nextDate(task: TaskItem, today: LocalDate = LocalDate.now()): LocalDate? {
        val recurrence = task.recurrence ?: return null
        val freq = RepeatFreq.of(recurrence.string("freq")) ?: return null
        val base = Dates.parse(task.dueDate) ?: today
        val interval = recurrence.double("interval").toInt().coerceAtLeast(1)
        val weekdays = (recurrence["weekdays"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() }
            .orEmpty()

        return when (freq) {
            RepeatFreq.DAILY -> base.plusDays(interval.toLong())
            RepeatFreq.WEEKDAYS -> {
                var date = base.plusDays(1)
                while (Dates.isWeekend(date)) date = date.plusDays(1)
                date
            }
            RepeatFreq.WEEKLY -> {
                val days = weekdays.ifEmpty { listOf(base.dayOfWeek.value % 7) }
                val horizon = 7 * interval + 1
                (1..horizon).asSequence()
                    .map { base.plusDays(it.toLong()) }
                    .firstOrNull { (it.dayOfWeek.value % 7) in days }
                    ?: base.plusDays(7)
            }
            RepeatFreq.MONTHLY -> base.plusMonths(interval.toLong())
        }
    }

    fun json(freq: RepeatFreq, interval: Int = 1, weekdays: List<Int>? = null): JsonObject =
        JsonObject(
            linkedMapOf(
                "freq" to JsonPrimitive(freq.raw),
                "interval" to JsonPrimitive(interval),
                "weekdays" to (weekdays?.let { days -> JsonArray(days.map { JsonPrimitive(it) }) } ?: JsonNull),
            ),
        )
}

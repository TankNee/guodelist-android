package cn.neeto.guodelist.core.util

import java.time.LocalDate
import java.time.LocalTime

/**
 * Port of the desktop quick-capture parser (`src/lib/date.ts`).
 *
 * Understands the forms people actually type — 今天 / 明天 / 周五 / 5月20日 /
 * today / next friday / 3pm — and never guesses: when nothing matches the task
 * simply has no date.
 */
object NaturalDateParser {

    data class Parsed(
        val title: String,
        val dueDate: LocalDate? = null,
        val dueTime: LocalTime? = null,
    )

    private val zhWeekdays = mapOf(
        '一' to 1, '二' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '日' to 0, '天' to 0,
    )

    private val enWeekdays = mapOf(
        "monday" to 1, "mon" to 1,
        "tuesday" to 2, "tue" to 2, "tues" to 2,
        "wednesday" to 3, "wed" to 3,
        "thursday" to 4, "thu" to 4, "thurs" to 4,
        "friday" to 5, "fri" to 5,
        "saturday" to 6, "sat" to 6,
        "sunday" to 0, "sun" to 0,
    )

    private val relativeZh = listOf(
        Regex("(今天|今日)") to 0L,
        Regex("(明天|明日)") to 1L,
        Regex("(后天)") to 2L,
    )

    fun parse(raw: String, today: LocalDate = LocalDate.now()): Parsed {
        var text = " $raw "
        var dueDate: LocalDate? = null
        var dueTime: LocalTime? = null

        fun take(regex: Regex, handler: (MatchResult) -> Unit) {
            val match = regex.find(text) ?: return
            handler(match)
            text = text.replace(match.value, " ")
        }

        for ((regex, offset) in relativeZh) {
            take(regex) { dueDate = today.plusDays(offset) }
        }

        take(Regex("(下|这|本)?(?:周|星期|礼拜)([一二三四五六日天])")) { match ->
            val target = zhWeekdays[match.groupValues[2][0]] ?: return@take
            dueDate = if (match.groupValues[1] == "下") {
                val nextMonday = today.plusDays((7 - (today.dayOfWeek.value - 1)).toLong())
                nextMonday.plusDays(if (target == 0) 6 else (target - 1).toLong())
            } else {
                nextWeekday(target, today)
            }
        }

        take(Regex("(\\d{1,2})[月/](\\d{1,2})[日号]?")) { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return@take
            val day = match.groupValues[2].toIntOrNull() ?: return@take
            if (month !in 1..12 || day !in 1..31) return@take
            var date = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return@take
            if (date.isBefore(today)) date = runCatching { date.plusYears(1) }.getOrDefault(date)
            dueDate = date
        }

        take(Regex("\\b(today)\\b", RegexOption.IGNORE_CASE)) { dueDate = today }
        take(Regex("\\b(tomorrow)\\b", RegexOption.IGNORE_CASE)) { dueDate = today.plusDays(1) }

        take(
            Regex(
                "\\b(next\\s+)?(monday|mon|tuesday|tue|tues|wednesday|wed|thursday|thu|thurs|friday|fri|saturday|sat|sunday|sun)\\b",
                RegexOption.IGNORE_CASE,
            ),
        ) { match ->
            val target = enWeekdays[match.groupValues[2].lowercase()] ?: return@take
            var date = nextWeekday(target, today)
            if (match.groupValues[1].isNotBlank()) date = date.plusDays(7)
            dueDate = date
        }

        take(Regex("(上午|下午|晚上|中午|早上)?\\s*(\\d{1,2})\\s*[:：点]\\s*(\\d{1,2})?\\s*(?:分)?")) { match ->
            var hour = match.groupValues[2].toIntOrNull() ?: return@take
            val minute = match.groupValues[3].toIntOrNull() ?: 0
            val period = match.groupValues[1]
            if ((period == "下午" || period == "晚上") && hour < 12) hour += 12
            if (hour in 0..23 && minute in 0..59) dueTime = LocalTime.of(hour, minute)
        }

        take(Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)) { match ->
            var hour = match.groupValues[1].toIntOrNull() ?: return@take
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val suffix = match.groupValues[3].lowercase()
            if (suffix == "pm" && hour < 12) hour += 12
            if (suffix == "am" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) dueTime = LocalTime.of(hour, minute)
        }

        val title = text.replace(Regex("\\s+"), " ").trim().ifEmpty { raw.trim() }
        return Parsed(title = title, dueDate = dueDate, dueTime = dueTime)
    }

    private fun nextWeekday(target: Int, today: LocalDate): LocalDate {
        val current = today.dayOfWeek.value % 7 // Sunday = 0, matching the desktop parser
        val delta = ((target - current) % 7 + 7) % 7
        return today.plusDays((if (delta == 0) 7 else delta).toLong())
    }
}

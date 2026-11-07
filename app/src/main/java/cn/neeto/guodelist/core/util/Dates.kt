package cn.neeto.guodelist.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

object Dates {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun key(date: LocalDate): String = date.format(iso)

    fun parse(key: String?): LocalDate? {
        if (key.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(key, iso) }.getOrNull()
    }

    fun today(): LocalDate = LocalDate.now()

    fun daysBetween(from: LocalDate, to: LocalDate): Long =
        to.toEpochDay() - from.toEpochDay()

    fun parseTime(time: String?): LocalTime? {
        if (time.isNullOrBlank()) return null
        return runCatching { LocalTime.parse(time) }.getOrNull()
    }

    fun formatTime(time: LocalTime, locale: Locale): String =
        time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

    fun weekdayShort(date: LocalDate, locale: Locale): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

    fun weekdayNarrow(date: LocalDate, locale: Locale): String =
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)

    fun weekdayLong(date: LocalDate, locale: Locale): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    fun monthTitle(date: LocalDate, locale: Locale): String =
        if (locale.language == "zh") "${date.year}年${date.monthValue}月"
        else date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

    fun monthDay(date: LocalDate, locale: Locale): String =
        if (locale.language == "zh") "${date.monthValue}月${date.dayOfMonth}日"
        else date.format(DateTimeFormatter.ofPattern("MMMM d", locale))

    fun monthShort(date: LocalDate, locale: Locale): String =
        if (locale.language == "zh") "${date.monthValue}月"
        else date.format(DateTimeFormatter.ofPattern("MMM", locale))

    fun dayOfMonth(date: LocalDate): Int = date.dayOfMonth

    fun shortDate(date: LocalDate, locale: Locale): String =
        if (locale.language == "zh") "${date.monthValue}月${date.dayOfMonth}日"
        else date.format(DateTimeFormatter.ofPattern("MMM d", locale))

    fun longDate(date: LocalDate, locale: Locale): String =
        if (locale.language == "zh") "${date.monthValue}月${date.dayOfMonth}日 · ${weekdayLong(date, locale)}"
        else date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", locale))

    fun isoDateTime(instant: java.time.Instant): String = instant.toString()

    /** Monday-first grid offset used by the month view. */
    fun mondayOffset(date: LocalDate): Int = (date.dayOfWeek.value + 6) % 7

    fun startOfWeek(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value + 7L) % 7)

    fun isWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
}

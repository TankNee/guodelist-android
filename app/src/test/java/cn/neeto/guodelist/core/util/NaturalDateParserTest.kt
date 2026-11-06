package cn.neeto.guodelist.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NaturalDateParserTest {

    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun chineseRelativeDayAndTime() {
        val parsed = NaturalDateParser.parse("明天 10 点 给房东打电话", monday)
        assertEquals("给房东打电话", parsed.title)
        assertEquals(LocalDate.of(2026, 11, 3), parsed.dueDate)
        assertEquals(LocalTime.of(10, 0), parsed.dueTime)
    }

    @Test
    fun englishRelativeDayAndTime() {
        val parsed = NaturalDateParser.parse("tomorrow 3pm call the landlord", monday)
        assertEquals("call the landlord", parsed.title)
        assertEquals(LocalDate.of(2026, 11, 3), parsed.dueDate)
        assertEquals(LocalTime.of(15, 0), parsed.dueTime)
    }

    @Test
    fun weekdayIsStrictlyInTheFuture() {
        val parsed = NaturalDateParser.parse("周五 开会", monday)
        assertEquals(LocalDate.of(2026, 11, 6), parsed.dueDate)
        assertEquals("开会", parsed.title)

        val onFriday = LocalDate.of(2026, 11, 6)
        assertEquals(LocalDate.of(2026, 11, 13), NaturalDateParser.parse("friday standup", onFriday).dueDate)
    }

    @Test
    fun explicitDateRollsForwardWhenPast() {
        val parsed = NaturalDateParser.parse("5月20日 交房", monday)
        assertEquals(LocalDate.of(2027, 5, 20), parsed.dueDate)
        assertEquals("交房", parsed.title)
    }

    @Test
    fun noDateMeansNoGuess() {
        val parsed = NaturalDateParser.parse("买牛奶", monday)
        assertEquals("买牛奶", parsed.title)
        assertNull(parsed.dueDate)
        assertNull(parsed.dueTime)
    }

    @Test
    fun afternoonMarkerAddsTwelveHours() {
        val parsed = NaturalDateParser.parse("下午3点 复盘", monday)
        assertEquals(LocalTime.of(15, 0), parsed.dueTime)
        assertEquals("复盘", parsed.title)
    }

    @Test
    fun nextWeekdayAddsAWeek() {
        val parsed = NaturalDateParser.parse("下周一 提交报销", monday)
        assertEquals(LocalDate.of(2026, 11, 9), parsed.dueDate)
    }
}

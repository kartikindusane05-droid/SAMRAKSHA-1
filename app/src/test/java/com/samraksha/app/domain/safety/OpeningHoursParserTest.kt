package com.samraksha.app.domain.safety

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class OpeningHoursParserTest {

    private fun getNextDateTime(dayOfWeek: DayOfWeek, hour: Int, minute: Int): LocalDateTime {
        val today = LocalDate.now()
        val nextDay = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        return LocalDateTime.of(nextDay, LocalTime.of(hour, minute))
    }

    @Test
    fun isOpen_handles24Seven() {
        val dateTime = getNextDateTime(DayOfWeek.WEDNESDAY, 3, 30)
        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen("24/7", dateTime))
        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(" 24/7 ", dateTime))
    }

    @Test
    fun isOpen_standardDayAndTimeRange() {
        val wednesdayAfternoon = getNextDateTime(DayOfWeek.WEDNESDAY, 14, 0)
        val wednesdayNight = getNextDateTime(DayOfWeek.WEDNESDAY, 21, 0)
        val sundayAfternoon = getNextDateTime(DayOfWeek.SUNDAY, 14, 0)

        // Mo-Fr 08:00-20:00
        val rule = "Mo-Fr 08:00-20:00"
        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(rule, wednesdayAfternoon))
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen(rule, wednesdayNight))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen(rule, sundayAfternoon))
    }

    @Test
    fun isOpen_handlesMultiRuleSemicolon() {
        val rule = "Mo-Fr 08:00-20:00; Sa-Su 10:00-18:00"

        val fridayMorning = getNextDateTime(DayOfWeek.FRIDAY, 9, 30)
        val saturdayNoon = getNextDateTime(DayOfWeek.SATURDAY, 12, 0)
        val saturdayEvening = getNextDateTime(DayOfWeek.SATURDAY, 19, 30)

        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(rule, fridayMorning))
        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(rule, saturdayNoon))
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen(rule, saturdayEvening))
    }

    @Test
    fun isOpen_handlesCrossMidnightInterval() {
        // Bar or late-night pharmacy open from 22:00 to 04:00
        val rule = "Mo-Su 22:00-04:00"

        val monday11PM = getNextDateTime(DayOfWeek.MONDAY, 23, 0)
        val mondayNoon = getNextDateTime(DayOfWeek.MONDAY, 12, 0)

        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(rule, monday11PM))
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen(rule, mondayNoon))
    }

    @Test
    fun isOpen_handlesMultipleTimeIntervalsPerDay() {
        val rule = "Mo-Fr 09:00-13:00, 15:00-20:00"

        val morning = getNextDateTime(DayOfWeek.TUESDAY, 10, 0)
        val lunchBreak = getNextDateTime(DayOfWeek.TUESDAY, 13, 30)
        val evening = getNextDateTime(DayOfWeek.TUESDAY, 17, 0)
        val night = getNextDateTime(DayOfWeek.TUESDAY, 22, 0)

        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(rule, morning))
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen(rule, lunchBreak))
        assertEquals(VenueOpenStatus.OPEN, OpeningHoursParser.isOpen(rule, evening))
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen(rule, night))
    }

    @Test
    fun isOpen_handlesOffAndClosedKeywords() {
        val rule = "Mo-Sa 09:00-18:00; Su off"
        val sunday = getNextDateTime(DayOfWeek.SUNDAY, 12, 0)
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen(rule, sunday))

        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen("closed", sunday))
        assertEquals(VenueOpenStatus.CLOSED, OpeningHoursParser.isOpen("off", sunday))
    }

    @Test
    fun isOpen_gracefullyReturnsUnknownForMissingOrUnparseable() {
        val dateTime = LocalDateTime.now()

        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen(null, dateTime))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen("", dateTime))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen("   ", dateTime))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen("unknown", dateTime))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen("variable", dateTime))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen("invalid syntax random text", dateTime))
        assertEquals(VenueOpenStatus.UNKNOWN, OpeningHoursParser.isOpen("sunrise-sunset", dateTime))
    }
}

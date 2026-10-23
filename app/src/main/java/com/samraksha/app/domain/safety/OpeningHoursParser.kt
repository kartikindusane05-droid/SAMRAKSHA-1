package com.samraksha.app.domain.safety

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Operating status for commercial or public venues.
 * Any missing or unparseable hours strictly resolve to UNKNOWN.
 * UNKNOWN is never assumed to be OPEN or CLOSED, and never treated as danger.
 */
enum class VenueOpenStatus {
    OPEN,
    CLOSED,
    UNKNOWN
}

/**
 * Robust, deterministic parser for OpenStreetMap (OSM) `opening_hours` syntax.
 * Handles:
 * - "24/7" -> OPEN
 * - Day ranges: "Mo-Su", "Mo-Fr", "Sa-Su", "Tu-Sa", "We-Su", etc.
 * - Comma separated days: "Mo,We,Fr", "Sa,Su"
 * - Standard time ranges: "10:00-18:00", "08:00-22:00"
 * - Cross-midnight time ranges: "22:00-04:00", "20:00-02:00"
 * - Multiple semicolon-separated day rules: "Mo-Fr 08:00-20:00; Sa-Su 10:00-18:00"
 * - Multiple comma-separated time intervals: "09:00-13:00,14:00-20:00"
 * - "off" or "closed" keywords: "Su off", "closed"
 * - Graceful fallback: unparseable, null, empty, or "unknown" strings return [VenueOpenStatus.UNKNOWN].
 */
object OpeningHoursParser {

    private val TIME_FORMATTER_COLON = DateTimeFormatter.ofPattern("H:m", Locale.US)

    private val DAY_OF_WEEK_MAP = mapOf(
        "mo" to DayOfWeek.MONDAY,
        "mon" to DayOfWeek.MONDAY,
        "tu" to DayOfWeek.TUESDAY,
        "tue" to DayOfWeek.TUESDAY,
        "we" to DayOfWeek.WEDNESDAY,
        "wed" to DayOfWeek.WEDNESDAY,
        "th" to DayOfWeek.THURSDAY,
        "thu" to DayOfWeek.THURSDAY,
        "fr" to DayOfWeek.FRIDAY,
        "fri" to DayOfWeek.FRIDAY,
        "sa" to DayOfWeek.SATURDAY,
        "sat" to DayOfWeek.SATURDAY,
        "su" to DayOfWeek.SUNDAY,
        "sun" to DayOfWeek.SUNDAY
    )

    private val DAY_ORDER = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    /**
     * Determines if a venue is open at the specified [dateTime].
     *
     * @param openingHours Raw OSM opening_hours string (e.g. "Mo-Su 10:00-18:00", "24/7").
     * @param dateTime Local date-time to evaluate against (defaults to now).
     * @return [VenueOpenStatus.OPEN], [VenueOpenStatus.CLOSED], or [VenueOpenStatus.UNKNOWN].
     */
    fun isOpen(
        openingHours: String?,
        dateTime: LocalDateTime = LocalDateTime.now()
    ): VenueOpenStatus {
        if (openingHours.isNullOrBlank()) return VenueOpenStatus.UNKNOWN

        val normalized = openingHours.trim().lowercase(Locale.US)
        if (normalized == "24/7") return VenueOpenStatus.OPEN
        if (normalized == "unknown" || normalized == "variable") return VenueOpenStatus.UNKNOWN
        if (normalized == "closed" || normalized == "off") return VenueOpenStatus.CLOSED

        return try {
            parseRuleSet(normalized, dateTime)
        } catch (_: Exception) {
            // Defensive parsing fallback: never crash or guess
            VenueOpenStatus.UNKNOWN
        }
    }

    private fun parseRuleSet(ruleString: String, targetDateTime: LocalDateTime): VenueOpenStatus {
        val rules = ruleString.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (rules.isEmpty()) return VenueOpenStatus.UNKNOWN

        var anyRuleMatchedDay = false

        for (rule in rules) {
            val status = evaluateSingleRule(rule, targetDateTime)
            if (status == VenueOpenStatus.OPEN) {
                return VenueOpenStatus.OPEN
            }
            if (status == VenueOpenStatus.CLOSED) {
                anyRuleMatchedDay = true
            }
        }

        // Also check if previous day had a cross-midnight interval that covers current time
        val yesterday = targetDateTime.minusDays(1)
        for (rule in rules) {
            val statusYesterday = evaluateCrossMidnightRule(rule, yesterday, targetDateTime.toLocalTime())
            if (statusYesterday == VenueOpenStatus.OPEN) {
                return VenueOpenStatus.OPEN
            }
        }

        return if (anyRuleMatchedDay) VenueOpenStatus.CLOSED else VenueOpenStatus.UNKNOWN
    }

    private fun evaluateSingleRule(rule: String, targetDateTime: LocalDateTime): VenueOpenStatus {
        val targetDay = targetDateTime.dayOfWeek
        val targetTime = targetDateTime.toLocalTime()

        // Handle rules like "Su off" or "Su closed"
        if (rule.endsWith("off") || rule.endsWith("closed")) {
            val dayPart = rule.substringBefore("off").substringBefore("closed").trim()
            val days = parseDays(dayPart)
            return if (days.contains(targetDay)) VenueOpenStatus.CLOSED else VenueOpenStatus.UNKNOWN
        }

        // Split into day part and time part.
        // Example: "Mo-Fr 08:00-20:00" -> days: "Mo-Fr", times: "08:00-20:00"
        // Example: "10:00-18:00" (no day part specified -> all days apply)
        val firstDigitIndex = rule.indexOfFirst { it.isDigit() }
        val (daysPart, timesPart) = if (firstDigitIndex > 0) {
            Pair(rule.substring(0, firstDigitIndex).trim(), rule.substring(firstDigitIndex).trim())
        } else if (firstDigitIndex == 0) {
            Pair("", rule.trim())
        } else {
            return VenueOpenStatus.UNKNOWN
        }

        val days = if (daysPart.isNotEmpty()) parseDays(daysPart) else DAY_ORDER.toSet()
        if (days.isEmpty()) return VenueOpenStatus.UNKNOWN

        if (!days.contains(targetDay)) {
            return VenueOpenStatus.UNKNOWN // Rule doesn't apply to this day
        }

        // Parse time intervals separated by commas (e.g., "09:00-13:00, 14:00-20:00")
        val intervals = timesPart.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (intervals.isEmpty()) return VenueOpenStatus.UNKNOWN

        for (interval in intervals) {
            val status = isTimeInInterval(interval, targetTime)
            if (status == VenueOpenStatus.OPEN) {
                return VenueOpenStatus.OPEN
            }
        }

        // The day matched, but the time did not match any open interval
        return VenueOpenStatus.CLOSED
    }

    /**
     * Checks if yesterday had a cross-midnight interval (e.g. "22:00-04:00")
     * and the current time is before the end time (e.g. 02:30 is before 04:00).
     */
    private fun evaluateCrossMidnightRule(
        rule: String,
        yesterdayDateTime: LocalDateTime,
        currentLocalTime: LocalTime
    ): VenueOpenStatus {
        val firstDigitIndex = rule.indexOfFirst { it.isDigit() }
        val (daysPart, timesPart) = if (firstDigitIndex > 0) {
            Pair(rule.substring(0, firstDigitIndex).trim(), rule.substring(firstDigitIndex).trim())
        } else if (firstDigitIndex == 0) {
            Pair("", rule.trim())
        } else {
            return VenueOpenStatus.UNKNOWN
        }

        val days = if (daysPart.isNotEmpty()) parseDays(daysPart) else DAY_ORDER.toSet()
        if (!days.contains(yesterdayDateTime.dayOfWeek)) {
            return VenueOpenStatus.UNKNOWN
        }

        val intervals = timesPart.split(",").map { it.trim() }
        for (interval in intervals) {
            val parts = interval.split("-").map { it.trim() }
            if (parts.size == 2) {
                val start = parseTime(parts[0])
                val end = parseTime(parts[1])
                if (start != null && end != null && end < start) {
                    // Cross-midnight interval: open from yesterday's start until today's end
                    if (currentLocalTime < end) {
                        return VenueOpenStatus.OPEN
                    }
                }
            }
        }

        return VenueOpenStatus.UNKNOWN
    }

    private fun isTimeInInterval(interval: String, targetTime: LocalTime): VenueOpenStatus {
        val parts = interval.split("-").map { it.trim() }
        if (parts.size != 2) return VenueOpenStatus.UNKNOWN

        val start = parseTime(parts[0]) ?: return VenueOpenStatus.UNKNOWN
        val end = parseTime(parts[1]) ?: return VenueOpenStatus.UNKNOWN

        return if (end >= start) {
            // Same-day interval: e.g. 09:00 - 18:00
            if (!targetTime.isBefore(start) && targetTime.isBefore(end)) {
                VenueOpenStatus.OPEN
            } else {
                VenueOpenStatus.CLOSED
            }
        } else {
            // Cross-midnight interval: e.g. 22:00 - 04:00
            if (!targetTime.isBefore(start) || targetTime.isBefore(end)) {
                VenueOpenStatus.OPEN
            } else {
                VenueOpenStatus.CLOSED
            }
        }
    }

    private fun parseDays(dayPart: String): Set<DayOfWeek> {
        val result = mutableSetOf<DayOfWeek>()
        val segments = dayPart.split(",").map { it.trim() }

        for (segment in segments) {
            if (segment.contains("-")) {
                val rangeParts = segment.split("-").map { it.trim() }
                if (rangeParts.size == 2) {
                    val startDay = DAY_OF_WEEK_MAP[rangeParts[0]]
                    val endDay = DAY_OF_WEEK_MAP[rangeParts[1]]
                    if (startDay != null && endDay != null) {
                        result.addAll(getDaysInRange(startDay, endDay))
                    }
                }
            } else {
                DAY_OF_WEEK_MAP[segment]?.let { result.add(it) }
            }
        }
        return result
    }

    private fun getDaysInRange(start: DayOfWeek, end: DayOfWeek): List<DayOfWeek> {
        val startIndex = DAY_ORDER.indexOf(start)
        val endIndex = DAY_ORDER.indexOf(end)
        if (startIndex == -1 || endIndex == -1) return emptyList()

        return if (startIndex <= endIndex) {
            DAY_ORDER.subList(startIndex, endIndex + 1)
        } else {
            DAY_ORDER.subList(startIndex, DAY_ORDER.size) + DAY_ORDER.subList(0, endIndex + 1)
        }
    }

    private fun parseTime(timeStr: String): LocalTime? {
        val clean = timeStr.trim()
        if (clean == "24:00" || clean == "24:00:00") {
            return LocalTime.MAX
        }
        return try {
            val parts = clean.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: return null
                val minute = parts[1].toIntOrNull() ?: return null
                if (hour in 0..23 && minute in 0..59) {
                    LocalTime.of(hour, minute)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

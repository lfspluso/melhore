package com.melhoreapp.core.scheduling

import com.melhoreapp.core.common.RecurrenceDaysConverter
import com.melhoreapp.core.database.entity.RecurrenceType
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Computes the next occurrence time for a recurring reminder.
 * Uses system default zone for DST-safe day/week advancement.
 *
 * @param dueAtMillis current due time (epoch millis)
 * @param type recurrence type
 * @param customRecurrenceDaysString optional comma-separated string of days of week for CUSTOM type (e.g., "MONDAY,WEDNESDAY,FRIDAY")
 * @return next trigger time in the future, or null for NONE or if no next occurrence
 */
fun nextOccurrenceMillis(
    dueAtMillis: Long,
    type: RecurrenceType,
    customRecurrenceDaysString: String? = null
): Long? {
    if (type == RecurrenceType.NONE) return null
    val zone = ZoneId.systemDefault()
    var instant = Instant.ofEpochMilli(dueAtMillis).atZone(zone)
    val now = Instant.now().atZone(zone)
    when (type) {
        RecurrenceType.NONE -> return null
        RecurrenceType.DAILY -> {
            // Next occurrence is the first occurrence at or after now (may be today if time not yet passed)
            while (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusDays(1)
            }
        }
        RecurrenceType.WEEKDAYS -> {
            // Advance day-by-day, skipping weekends (Saturday and Sunday)
            while (instant.isBefore(now) || instant.isEqual(now)) {
                val dayOfWeek = instant.dayOfWeek
                when {
                    dayOfWeek == DayOfWeek.FRIDAY -> instant = instant.plusDays(3) // Skip to Monday
                    dayOfWeek == DayOfWeek.SATURDAY -> instant = instant.plusDays(2) // Skip to Monday
                    else -> instant = instant.plusDays(1) // Next day
                }
            }
        }
        RecurrenceType.WEEKENDS -> {
            // Advance to next Saturday or Sunday only (weekend days)
            while (instant.isBefore(now) || instant.isEqual(now)) {
                val dayOfWeek = instant.dayOfWeek
                when {
                    dayOfWeek == DayOfWeek.SATURDAY -> instant = instant.plusDays(1) // Sunday
                    dayOfWeek == DayOfWeek.SUNDAY -> instant = instant.plusDays(6) // Next Saturday
                    else -> instant = instant.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)) // Mon–Fri -> next Saturday
                }
            }
        }
        RecurrenceType.WEEKLY -> {
            while (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusWeeks(1)
            }
        }
        RecurrenceType.BIWEEKLY -> {
            while (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusWeeks(2)
            }
        }
        RecurrenceType.MONTHLY -> {
            while (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusMonths(1)
            }
        }
        RecurrenceType.CUSTOM -> {
            val daysOfWeek = RecurrenceDaysConverter.deserializeDays(customRecurrenceDaysString)
            if (daysOfWeek.isEmpty()) return null
            return nextCustomOccurrenceMillis(dueAtMillis, daysOfWeek)
        }
    }
    return instant.toInstant().toEpochMilli()
}

/**
 * Computes the next occurrence time for a custom recurrence pattern (specific days of week).
 * Finds the next date that falls on one of the specified days.
 *
 * @param dueAtMillis current due time (epoch millis)
 * @param daysOfWeek set of days of week when the reminder should occur
 * @return next trigger time in the future, or null if no valid days specified
 */
fun nextCustomOccurrenceMillis(
    dueAtMillis: Long,
    daysOfWeek: Set<DayOfWeek>
): Long? {
    if (daysOfWeek.isEmpty()) return null
    
    val zone = ZoneId.systemDefault()
    var instant = Instant.ofEpochMilli(dueAtMillis).atZone(zone)
    val now = Instant.now().atZone(zone)
    val currentDayOfWeek = instant.dayOfWeek
    
    // If current day matches and time hasn't passed, use current day
    if (daysOfWeek.contains(currentDayOfWeek) && (instant.isAfter(now) || instant.isEqual(now))) {
        return instant.toInstant().toEpochMilli()
    }
    
    // Find next matching day in current week
    var daysToAdd = 0L
    var checkDay = currentDayOfWeek
    for (i in 1..7) {
        checkDay = checkDay.plus(1)
        daysToAdd++
        if (daysOfWeek.contains(checkDay)) {
            instant = instant.plusDays(daysToAdd)
            // If this time is still in the past, advance to next week
            if (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusWeeks(1)
            }
            return instant.toInstant().toEpochMilli()
        }
    }
    
    // If no match found in current week, advance to next week and find first matching day
    instant = instant.plusWeeks(1)
    val nextWeekStart = instant.with(DayOfWeek.MONDAY)
    val sortedDays = daysOfWeek.sortedBy { it.value }
    
    for (day in sortedDays) {
        val candidate = nextWeekStart.with(day)
        if (candidate.isAfter(now) || candidate.isEqual(now)) {
            return candidate.toInstant().toEpochMilli()
        }
    }
    
    // Fallback: use first day of next week
    return nextWeekStart.with(sortedDays.first()).toInstant().toEpochMilli()
}

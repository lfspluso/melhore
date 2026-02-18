package com.melhoreapp.core.scheduling

import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Utility class for calculating Rotina period boundaries based on recurrence type.
 * Used by RotinaTaskSetupViewModel and ReminderAlarmReceiver to determine current period.
 */
object RotinaPeriodHelper {
    /**
     * Start of the current period (inclusive), in epoch millis.
     * Based on reminder's recurrence type.
     */
    fun getCurrentPeriodStart(reminder: ReminderEntity): Long {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.now(zone).toLocalDate()
        return when (reminder.type) {
            RecurrenceType.NONE -> today.atStartOfDay(zone).toInstant().toEpochMilli()
            RecurrenceType.DAILY, RecurrenceType.WEEKDAYS -> today.atStartOfDay(zone).toInstant().toEpochMilli()
            RecurrenceType.WEEKLY, RecurrenceType.CUSTOM -> {
                val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
                val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            }
            RecurrenceType.BIWEEKLY -> {
                val wf = WeekFields.of(Locale.getDefault())
                val weekStart = today.with(TemporalAdjusters.previousOrSame(wf.firstDayOfWeek))
                val weekNumber = weekStart.get(wf.weekOfWeekBasedYear())
                val weeksIntoBiweek = (weekNumber - 1) % 2
                val periodStartDate = weekStart.minusWeeks(weeksIntoBiweek.toLong())
                periodStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
            }
            RecurrenceType.MONTHLY -> today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }

    /**
     * End of the current period (end of day), in epoch millis.
     */
    fun getCurrentPeriodEnd(reminder: ReminderEntity): Long {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.now(zone).toLocalDate()
        return when (reminder.type) {
            RecurrenceType.NONE, RecurrenceType.DAILY, RecurrenceType.WEEKDAYS ->
                today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
            RecurrenceType.WEEKLY, RecurrenceType.CUSTOM -> {
                val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
                val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                val weekEnd = weekStart.plusDays(6)
                weekEnd.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
            }
            RecurrenceType.BIWEEKLY -> {
                val wf = WeekFields.of(Locale.getDefault())
                val weekStart = today.with(TemporalAdjusters.previousOrSame(wf.firstDayOfWeek))
                val weekNumber = weekStart.get(wf.weekOfWeekBasedYear())
                val weeksIntoBiweek = (weekNumber - 1) % 2
                val periodStartDate = weekStart.minusWeeks(weeksIntoBiweek.toLong())
                val periodEndDate = periodStartDate.plusWeeks(2).minusDays(1)
                periodEndDate.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
            }
            RecurrenceType.MONTHLY ->
                today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        }
    }

    /**
     * Checks if a timestamp is within the current period for a reminder.
     */
    fun isWithinPeriod(timestamp: Long, reminder: ReminderEntity): Boolean {
        val periodStart = getCurrentPeriodStart(reminder)
        val periodEnd = getCurrentPeriodEnd(reminder)
        return timestamp in periodStart..periodEnd
    }
}

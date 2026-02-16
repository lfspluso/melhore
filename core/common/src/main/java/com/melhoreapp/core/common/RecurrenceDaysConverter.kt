package com.melhoreapp.core.common

import java.time.DayOfWeek

/**
 * Utility functions for converting between Set<DayOfWeek> and comma-separated string format
 * for storing custom recurrence days in the database.
 */
object RecurrenceDaysConverter {
    
    /**
     * Serializes a set of days of week to a comma-separated string.
     * Example: {MONDAY, WEDNESDAY, FRIDAY} -> "MONDAY,WEDNESDAY,FRIDAY"
     */
    fun serializeDays(days: Set<DayOfWeek>): String {
        return days.joinToString(separator = ",") { it.name }
    }
    
    /**
     * Deserializes a comma-separated string to a set of days of week.
     * Returns empty set if the string is null or empty.
     * Example: "MONDAY,WEDNESDAY,FRIDAY" -> {MONDAY, WEDNESDAY, FRIDAY}
     */
    fun deserializeDays(daysString: String?): Set<DayOfWeek> {
        if (daysString.isNullOrBlank()) return emptySet()
        return daysString.split(",")
            .mapNotNull { dayName ->
                try {
                    DayOfWeek.valueOf(dayName.trim())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .toSet()
    }
}

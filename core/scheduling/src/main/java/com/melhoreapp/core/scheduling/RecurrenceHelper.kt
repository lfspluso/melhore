package com.melhoreapp.core.scheduling

import com.melhoreapp.core.database.entity.RecurrenceType
import java.time.Instant
import java.time.ZoneId

/**
 * Computes the next occurrence time for a recurring reminder.
 * Uses system default zone for DST-safe day/week advancement.
 *
 * @param dueAtMillis current due time (epoch millis)
 * @param type recurrence type
 * @return next trigger time in the future, or null for NONE or if no next occurrence
 */
fun nextOccurrenceMillis(dueAtMillis: Long, type: RecurrenceType): Long? {
    if (type == RecurrenceType.NONE) return null
    val zone = ZoneId.systemDefault()
    var instant = Instant.ofEpochMilli(dueAtMillis).atZone(zone)
    val now = Instant.now().atZone(zone)
    when (type) {
        RecurrenceType.NONE -> return null
        RecurrenceType.DAILY -> {
            instant = instant.plusDays(1)
            while (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusDays(1)
            }
        }
        RecurrenceType.WEEKLY -> {
            instant = instant.plusWeeks(1)
            while (instant.isBefore(now) || instant.isEqual(now)) {
                instant = instant.plusWeeks(1)
            }
        }
    }
    return instant.toInstant().toEpochMilli()
}

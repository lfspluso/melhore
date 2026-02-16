package com.melhoreapp.core.database.entity

/**
 * Status of a reminder.
 * - ACTIVE: Reminder is active and will notify the user
 * - COMPLETED: User has manually marked the reminder as complete
 * - CANCELLED: User has cancelled the reminder
 */
enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

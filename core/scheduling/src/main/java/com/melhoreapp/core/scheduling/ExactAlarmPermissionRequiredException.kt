package com.melhoreapp.core.scheduling

/**
 * Thrown when the user must grant "Alarms & reminders" (exact alarm permission) before
 * a reminder can be scheduled. The UI should open the exact-alarm settings and prompt the user.
 */
class ExactAlarmPermissionRequiredException : Exception(
    "Exact alarm permission (SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM) is required to schedule reminders"
)

package com.melhoreapp.feature.reminders.ui.list

/**
 * Filter state for the reminder list.
 * When categoryIds is empty, priorityOrdinals is empty, and no date range is set, treat as "All".
 */
data class ReminderListFilter(
    val categoryIds: Set<Long> = emptySet(),
    val priorityOrdinals: Set<Int> = emptySet(),
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val showCompleted: Boolean = true,
    val showCancelled: Boolean = true
) {
    /** True when no filter is applied (show all reminders). */
    fun isAll(): Boolean =
        categoryIds.isEmpty() &&
            priorityOrdinals.isEmpty() &&
            dateFromMillis == null &&
            dateToMillis == null &&
            showCompleted &&
            showCancelled
}

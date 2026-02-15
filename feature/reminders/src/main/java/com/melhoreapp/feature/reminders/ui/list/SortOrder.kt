package com.melhoreapp.feature.reminders.ui.list

enum class SortOrder {
    /** Sort by due date ascending (soonest first). */
    DUE_DATE_ASC,
    /** Sort by priority descending (urgent first), then by due date ascending. */
    PRIORITY_DESC_THEN_DUE_ASC
}

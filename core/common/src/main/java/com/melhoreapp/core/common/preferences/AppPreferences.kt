package com.melhoreapp.core.common.preferences

import android.content.Context

private const val PREFS_NAME = "melhore_app_prefs"
private const val KEY_DEFAULT_SNOOZE_DURATION_MS = "default_snooze_duration_ms"
private const val KEY_LAST_FILTER_CATEGORY_IDS = "last_filter_category_ids"
private const val KEY_LAST_FILTER_PRIORITIES = "last_filter_priorities"
private const val KEY_LAST_FILTER_DATE_FROM = "last_filter_date_from"
private const val KEY_LAST_FILTER_DATE_TO = "last_filter_date_to"
private const val KEY_LAST_SORT_ORDER = "last_sort_order"
private const val KEY_GROUP_BY_TAG = "group_by_tag"
private const val KEY_SHOW_ADVANCED_FILTERS = "show_advanced_filters"
private const val KEY_AUTO_DELETE_COMPLETED_REMINDERS = "auto_delete_completed_reminders"
private const val KEY_SHOW_COMPLETED_REMINDERS = "show_completed_reminders"
private const val KEY_SHOW_CANCELLED_REMINDERS = "show_cancelled_reminders"
private const val KEY_ENABLED_SNOOZE_OPTIONS = "enabled_snooze_options"

/** Sentinel for "no date filter" (SharedPreferences cannot store null). */
private const val DATE_FILTER_NOT_SET = 0L

/** Fallback default snooze when no value is stored (10 minutes). */
const val DEFAULT_SNOOZE_DURATION_MS = 10 * 60 * 1000L

/**
 * App-level preferences stored in SharedPreferences.
 * Used by feature:settings (write) and core:scheduling (read in SnoozeReceiver).
 * Also stores last-used reminder list filter and sort (feature:reminders).
 */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultSnoozeDurationMs(): Long =
        prefs.getLong(KEY_DEFAULT_SNOOZE_DURATION_MS, DEFAULT_SNOOZE_DURATION_MS)

    fun setDefaultSnoozeDurationMs(ms: Long) {
        prefs.edit().putLong(KEY_DEFAULT_SNOOZE_DURATION_MS, ms).apply()
    }

    // Reminder list filter/sort persistence (Sprint 6)

    fun getLastFilterCategoryIds(): Set<Long> {
        val raw = prefs.getString(KEY_LAST_FILTER_CATEGORY_IDS, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    fun setLastFilterCategoryIds(ids: Set<Long>) {
        prefs.edit().putString(KEY_LAST_FILTER_CATEGORY_IDS, ids.joinToString(",")).apply()
    }

    fun getLastFilterPriorities(): Set<Int> {
        val raw = prefs.getString(KEY_LAST_FILTER_PRIORITIES, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun setLastFilterPriorities(ordinals: Set<Int>) {
        prefs.edit().putString(KEY_LAST_FILTER_PRIORITIES, ordinals.joinToString(",")).apply()
    }

    fun getLastFilterDateFrom(): Long? {
        val v = prefs.getLong(KEY_LAST_FILTER_DATE_FROM, DATE_FILTER_NOT_SET)
        return if (v == DATE_FILTER_NOT_SET) null else v
    }

    fun setLastFilterDateFrom(millis: Long?) {
        prefs.edit().putLong(KEY_LAST_FILTER_DATE_FROM, millis ?: DATE_FILTER_NOT_SET).apply()
    }

    fun getLastFilterDateTo(): Long? {
        val v = prefs.getLong(KEY_LAST_FILTER_DATE_TO, DATE_FILTER_NOT_SET)
        return if (v == DATE_FILTER_NOT_SET) null else v
    }

    fun setLastFilterDateTo(millis: Long?) {
        prefs.edit().putLong(KEY_LAST_FILTER_DATE_TO, millis ?: DATE_FILTER_NOT_SET).apply()
    }

    fun getLastSortOrder(): String? =
        prefs.getString(KEY_LAST_SORT_ORDER, null)?.takeIf { it.isNotBlank() }

    fun setLastSortOrder(enumName: String) {
        prefs.edit().putString(KEY_LAST_SORT_ORDER, enumName).apply()
    }

    // Reminder list group-by-tag (Sprint 7)

    fun getGroupByTag(): Boolean = prefs.getBoolean(KEY_GROUP_BY_TAG, false)

    fun setGroupByTag(groupByTag: Boolean) {
        prefs.edit().putBoolean(KEY_GROUP_BY_TAG, groupByTag).apply()
    }

    // Advanced filters toggle (Sprint 10)

    fun getShowAdvancedFilters(): Boolean = prefs.getBoolean(KEY_SHOW_ADVANCED_FILTERS, false)

    fun setShowAdvancedFilters(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ADVANCED_FILTERS, show).apply()
    }

    // Delete after completion (Sprint 13 - reworked from Sprint 11.5)
    // Note: KEY name kept for backward compatibility, but behavior changed to only delete COMPLETED reminders

    fun getDeleteAfterCompletion(): Boolean =
        prefs.getBoolean(KEY_AUTO_DELETE_COMPLETED_REMINDERS, false)

    fun setDeleteAfterCompletion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DELETE_COMPLETED_REMINDERS, enabled).apply()
    }

    // Backward compatibility methods (deprecated)
    @Deprecated("Use getDeleteAfterCompletion() instead", ReplaceWith("getDeleteAfterCompletion()"))
    fun getAutoDeleteCompletedReminders(): Boolean = getDeleteAfterCompletion()

    @Deprecated("Use setDeleteAfterCompletion() instead", ReplaceWith("setDeleteAfterCompletion(enabled)"))
    fun setAutoDeleteCompletedReminders(enabled: Boolean) = setDeleteAfterCompletion(enabled)

    // Show completed reminders filter (Sprint 13)

    fun getShowCompletedReminders(): Boolean = prefs.getBoolean(KEY_SHOW_COMPLETED_REMINDERS, true)

    fun setShowCompletedReminders(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_COMPLETED_REMINDERS, show).apply()
    }

    // Show cancelled reminders filter

    fun getShowCancelledReminders(): Boolean = prefs.getBoolean(KEY_SHOW_CANCELLED_REMINDERS, false)

    fun setShowCancelledReminders(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CANCELLED_REMINDERS, show).apply()
    }

    // Snooze options settings (Sprint 15)

    fun getEnabledSnoozeOptions(): Set<String> {
        val raw = prefs.getString(KEY_ENABLED_SNOOZE_OPTIONS, null) ?: return getDefaultEnabledSnoozeOptions()
        if (raw.isBlank()) return getDefaultEnabledSnoozeOptions()
        return raw.split(",").mapNotNull { it.trim().takeIf { it.isNotEmpty() } }.toSet()
    }

    fun setEnabledSnoozeOptions(options: Set<String>) {
        prefs.edit().putString(KEY_ENABLED_SNOOZE_OPTIONS, options.joinToString(",")).apply()
    }

    private fun getDefaultEnabledSnoozeOptions(): Set<String> {
        return setOf("5_min", "15_min", "1_hour")
    }
}

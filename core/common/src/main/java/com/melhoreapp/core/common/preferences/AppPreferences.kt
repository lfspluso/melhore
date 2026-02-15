package com.melhoreapp.core.common.preferences

import android.content.Context

private const val PREFS_NAME = "melhore_app_prefs"
private const val KEY_DEFAULT_SNOOZE_DURATION_MS = "default_snooze_duration_ms"
private const val KEY_LAST_FILTER_CATEGORY_IDS = "last_filter_category_ids"
private const val KEY_LAST_FILTER_PRIORITIES = "last_filter_priorities"
private const val KEY_LAST_FILTER_DATE_FROM = "last_filter_date_from"
private const val KEY_LAST_FILTER_DATE_TO = "last_filter_date_to"
private const val KEY_LAST_SORT_ORDER = "last_sort_order"

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
}

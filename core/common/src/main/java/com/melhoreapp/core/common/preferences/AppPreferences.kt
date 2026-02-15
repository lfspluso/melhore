package com.melhoreapp.core.common.preferences

import android.content.Context

private const val PREFS_NAME = "melhore_app_prefs"
private const val KEY_DEFAULT_SNOOZE_DURATION_MS = "default_snooze_duration_ms"

/** Fallback default snooze when no value is stored (10 minutes). */
const val DEFAULT_SNOOZE_DURATION_MS = 10 * 60 * 1000L

/**
 * App-level preferences stored in SharedPreferences.
 * Used by feature:settings (write) and core:scheduling (read in SnoozeReceiver).
 */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultSnoozeDurationMs(): Long =
        prefs.getLong(KEY_DEFAULT_SNOOZE_DURATION_MS, DEFAULT_SNOOZE_DURATION_MS)

    fun setDefaultSnoozeDurationMs(ms: Long) {
        prefs.edit().putLong(KEY_DEFAULT_SNOOZE_DURATION_MS, ms).apply()
    }
}

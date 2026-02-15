package com.melhoreapp.feature.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.melhoreapp.core.common.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Snooze duration options for the settings UI (label, duration in ms). */
data class SnoozeOption(val label: String, val durationMs: Long)

val SNOOZE_OPTIONS: List<SnoozeOption> = listOf(
    SnoozeOption("5 minutos", 5 * 60 * 1000L),
    SnoozeOption("10 minutos", 10 * 60 * 1000L),
    SnoozeOption("15 minutos", 15 * 60 * 1000L),
    SnoozeOption("1 hora", 60 * 60 * 1000L),
    SnoozeOption("1 dia", 24 * 60 * 60 * 1000L)
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val appPreferences = AppPreferences(context)

    private val _defaultSnoozeDurationMs = MutableStateFlow(appPreferences.getDefaultSnoozeDurationMs())
    val defaultSnoozeDurationMs: StateFlow<Long> = _defaultSnoozeDurationMs.asStateFlow()

    fun setDefaultSnoozeDuration(ms: Long) {
        appPreferences.setDefaultSnoozeDurationMs(ms)
        _defaultSnoozeDurationMs.value = ms
    }
}

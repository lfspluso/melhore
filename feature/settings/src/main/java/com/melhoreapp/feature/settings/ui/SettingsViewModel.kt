package com.melhoreapp.feature.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderStatus
import com.melhoreapp.core.scheduling.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val appPreferences = AppPreferences(context)

    private val _defaultSnoozeDurationMs = MutableStateFlow(appPreferences.getDefaultSnoozeDurationMs())
    val defaultSnoozeDurationMs: StateFlow<Long> = _defaultSnoozeDurationMs.asStateFlow()

    private val _autoDeleteCompletedReminders = MutableStateFlow(appPreferences.getDeleteAfterCompletion())
    val autoDeleteCompletedReminders: StateFlow<Boolean> = _autoDeleteCompletedReminders.asStateFlow()

    private val _enabledSnoozeOptions = MutableStateFlow(appPreferences.getEnabledSnoozeOptions())
    val enabledSnoozeOptions: StateFlow<Set<String>> = _enabledSnoozeOptions.asStateFlow()

    fun setDefaultSnoozeDuration(ms: Long) {
        appPreferences.setDefaultSnoozeDurationMs(ms)
        _defaultSnoozeDurationMs.value = ms
    }

    fun setSnoozeOptionEnabled(option: String, enabled: Boolean) {
        val current = _enabledSnoozeOptions.value.toMutableSet()
        if (enabled) {
            current.add(option)
        } else {
            // Ensure at least one option remains enabled
            if (current.size > 1) {
                current.remove(option)
            } else {
                return // Don't allow disabling the last option
            }
        }
        appPreferences.setEnabledSnoozeOptions(current)
        _enabledSnoozeOptions.value = current
    }

    fun setAutoDeleteCompletedReminders(enabled: Boolean) {
        appPreferences.setDeleteAfterCompletion(enabled)
        _autoDeleteCompletedReminders.value = enabled

        // Note: Delete logic for COMPLETED reminders is handled when marking as complete
        // When enabled, all existing COMPLETED reminders should be deleted
        if (enabled) {
            viewModelScope.launch {
                val allReminders = reminderDao.getAllReminders().first()
                val toDelete = allReminders.filter {
                    it.status == ReminderStatus.COMPLETED
                }
                toDelete.forEach { reminder ->
                    reminderScheduler.cancelReminder(reminder.id)
                    reminderDao.deleteById(reminder.id)
                }
            }
        }
    }
}

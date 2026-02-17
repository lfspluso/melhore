package com.melhoreapp.feature.reminders.ui.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.database.entity.ReminderStatus
import com.melhoreapp.core.scheduling.ReminderScheduler
import com.melhoreapp.core.scheduling.RotinaPeriodHelper
import com.melhoreapp.core.scheduling.nextOccurrenceMillis
import com.melhoreapp.core.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskInput(
    val title: String,
    val startTime: Long,
    val checkupFrequencyHours: Int
)

@HiltViewModel
class RotinaTaskSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
    private val syncRepository: SyncRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>("reminderId") ?: -1L

    private val _parentReminder = MutableStateFlow<ReminderEntity?>(null)
    val parentReminder: StateFlow<ReminderEntity?> = _parentReminder.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskInput>>(emptyList())
    val tasks: StateFlow<List<TaskInput>> = _tasks.asStateFlow()

    private val _showSkipDayConfirmation = MutableStateFlow(false)
    val showSkipDayConfirmation: StateFlow<Boolean> = _showSkipDayConfirmation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _periodValidationError = MutableStateFlow<String?>(null)
    val periodValidationError: StateFlow<String?> = _periodValidationError.asStateFlow()

    val currentPeriodStart: StateFlow<Long?> = parentReminder.map { parent ->
        parent?.let { RotinaPeriodHelper.getCurrentPeriodStart(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPeriodEnd: StateFlow<Long?> = parentReminder.map { parent ->
        parent?.let { RotinaPeriodHelper.getCurrentPeriodEnd(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        if (reminderId > 0) {
            loadParentReminder()
        }
    }


    private fun loadParentReminder() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reminder = reminderDao.getReminderById(reminderId)
                _parentReminder.value = reminder
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTask() {
        _periodValidationError.value = null
        val parent = _parentReminder.value ?: return
        val periodStart = RotinaPeriodHelper.getCurrentPeriodStart(parent)
        val periodEnd = RotinaPeriodHelper.getCurrentPeriodEnd(parent)
        val now = System.currentTimeMillis()
        val oneHourFromNow = now + (60 * 60 * 1000L)
        val defaultStartTime = oneHourFromNow.coerceIn(periodStart, periodEnd)
        _tasks.update { it + TaskInput(
            title = "",
            startTime = defaultStartTime,
            checkupFrequencyHours = 1
        ) }
    }

    fun removeTask(index: Int) {
        _tasks.update { it.filterIndexed { i, _ -> i != index } }
    }

    /**
     * @return true if the task was updated, false if validation failed (task outside current period).
     */
    fun updateTask(index: Int, task: TaskInput): Boolean {
        _periodValidationError.value = null
        val parent = _parentReminder.value ?: return false
        if (!RotinaPeriodHelper.isWithinPeriod(task.startTime, parent)) {
            _periodValidationError.value = "O horário da tarefa deve estar dentro do período atual."
            return false
        }
        _tasks.update { list ->
            list.mapIndexed { i, t -> if (i == index) task else t }
        }
        return true
    }

    fun clearPeriodValidationError() {
        _periodValidationError.value = null
    }

    fun showSkipDayConfirmation() {
        _showSkipDayConfirmation.value = true
    }

    fun dismissSkipDayConfirmation() {
        _showSkipDayConfirmation.value = false
    }

    fun skipDay() {
        viewModelScope.launch {
            val reminder = _parentReminder.value ?: return@launch
            if (reminder.status != ReminderStatus.ACTIVE || !reminder.isRoutine) return@launch

            val now = System.currentTimeMillis()
            val nextDue = nextOccurrenceMillis(
                reminder.dueAt,
                reminder.type,
                reminder.customRecurrenceDays
            ) ?: return@launch

            val updated = reminder.copy(dueAt = nextDue, updatedAt = now)
            reminderDao.update(updated)

            val uid = reminder.userId ?: "local"
            if (uid != "local") syncRepository.uploadAllInBackground(uid)
            reminderScheduler.scheduleReminder(
                reminderId = reminderId,
                triggerAtMillis = nextDue,
                title = updated.title,
                notes = updated.notes.ifEmpty() { "Reminder" },
                isSnoozeFire = false
            )

            _showSkipDayConfirmation.value = false
        }
    }

    suspend fun saveTasks(): Boolean {
        _periodValidationError.value = null
        val parent = _parentReminder.value ?: return false
        val tasksToSave = _tasks.value.filter { it.title.isNotBlank() }
        if (tasksToSave.isEmpty()) return false

        val outOfPeriod = tasksToSave.any { !RotinaPeriodHelper.isWithinPeriod(it.startTime, parent) }
        if (outOfPeriod) {
            _periodValidationError.value = "Todas as tarefas devem estar dentro do período atual."
            return false
        }

        val now = System.currentTimeMillis()

        val uid = parent.userId ?: "local"
        tasksToSave.forEachIndexed { _, task ->
            val taskReminder = ReminderEntity(
                userId = uid,
                title = task.title,
                notes = "",
                type = com.melhoreapp.core.database.entity.RecurrenceType.NONE,
                dueAt = task.startTime,
                categoryId = null,
                listId = null,
                priority = com.melhoreapp.core.database.entity.Priority.MEDIUM,
                snoozedUntil = null,
                status = ReminderStatus.ACTIVE,
                isActive = true,
                isRoutine = false,
                customRecurrenceDays = null,
                parentReminderId = reminderId,
                startTime = task.startTime,
                checkupFrequencyHours = task.checkupFrequencyHours,
                isTask = true,
                createdAt = now,
                updatedAt = now
            )

            val taskId = reminderDao.insert(taskReminder)

            // Schedule initial notification at start time
            reminderScheduler.scheduleReminder(
                reminderId = taskId,
                triggerAtMillis = task.startTime,
                title = task.title,
                notes = "",
                isSnoozeFire = false
            )
        }

        if (uid != "local") syncRepository.uploadAllInBackground(uid)
        // Clear tasks list after saving
        _tasks.value = emptyList()
        return true
    }
}

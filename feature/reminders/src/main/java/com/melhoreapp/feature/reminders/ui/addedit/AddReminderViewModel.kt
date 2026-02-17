package com.melhoreapp.feature.reminders.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.common.RecurrenceDaysConverter
import com.melhoreapp.core.database.entity.ChecklistItemEntity
import com.melhoreapp.core.database.entity.Priority
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.database.entity.ReminderStatus
import com.melhoreapp.core.scheduling.ExactAlarmPermissionRequiredException
import com.melhoreapp.core.scheduling.ReminderScheduler
import com.melhoreapp.core.sync.SyncRepository
import com.melhoreapp.core.database.entity.CategoryEntity
import java.time.DayOfWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChecklistItemUi(
    val id: Long,
    val label: String,
    val sortOrder: Int,
    val checked: Boolean
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao,
    private val reminderScheduler: ReminderScheduler,
    private val syncRepository: SyncRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: Long? = savedStateHandle.get<Long>("reminderId")

    val isEditMode: Boolean get() = reminderId != null

    private var nextTempId = -1L
    private fun nextChecklistTempId(): Long = nextTempId--

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _dueAt = MutableStateFlow(System.currentTimeMillis() + 3600_000)
    val dueAt: StateFlow<Long> = _dueAt.asStateFlow()

    private val _categoryId = MutableStateFlow<Long?>(null)
    val categoryId: StateFlow<Long?> = _categoryId.asStateFlow()

    private val _priority = MutableStateFlow(Priority.MEDIUM)
    val priority: StateFlow<Priority> = _priority.asStateFlow()

    private val _recurrenceType = MutableStateFlow(RecurrenceType.NONE)
    val recurrenceType: StateFlow<RecurrenceType> = _recurrenceType.asStateFlow()

    private val _isRoutine = MutableStateFlow(false)
    val isRoutine: StateFlow<Boolean> = _isRoutine.asStateFlow()

    private val _customRecurrenceDays = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    val customRecurrenceDays: StateFlow<Set<DayOfWeek>> = _customRecurrenceDays.asStateFlow()

    private val _checklistItems = MutableStateFlow<List<ChecklistItemUi>>(emptyList())
    val checklistItems: StateFlow<List<ChecklistItemUi>> = _checklistItems.asStateFlow()

    private val _showCancellationConfirmation = MutableStateFlow(false)
    val showCancellationConfirmation: StateFlow<Boolean> = _showCancellationConfirmation.asStateFlow()

    private suspend fun currentUserId(): String =
        authRepository.currentUser.value?.userId ?: "local"

    val categories: StateFlow<List<CategoryEntity>> = authRepository.currentUser
        .map { it?.userId ?: "local" }
        .flatMapLatest { uid -> categoryDao.getAllCategories(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        reminderId?.let { id ->
            viewModelScope.launch {
                reminderDao.getReminderById(id)?.let { reminder ->
                    _title.value = reminder.title
                    _dueAt.value = reminder.dueAt
                    _categoryId.value = reminder.categoryId
                    _priority.value = reminder.priority
                    _recurrenceType.value = reminder.type
                    _isRoutine.value = reminder.isRoutine
                    _customRecurrenceDays.value = RecurrenceDaysConverter.deserializeDays(reminder.customRecurrenceDays)
                }
                val uid = currentUserId()
                checklistItemDao.getItemsByReminderIdOnce(uid, id).let { items ->
                    _checklistItems.value = items.map { e ->
                        ChecklistItemUi(
                            id = e.id,
                            label = e.label,
                            sortOrder = e.sortOrder,
                            checked = e.checked
                        )
                    }
                }
            }
        }
    }

    fun setTitle(value: String) { _title.value = value }
    fun setDueAt(epochMillis: Long) { _dueAt.value = epochMillis }
    fun setCategoryId(id: Long?) { _categoryId.value = id }
    fun setPriority(p: Priority) { _priority.value = p }
    fun setRecurrenceType(type: RecurrenceType) { 
        _recurrenceType.value = type
        // Clear custom days if not CUSTOM type
        if (type != RecurrenceType.CUSTOM) {
            _customRecurrenceDays.value = emptySet()
        }
    }
    fun setIsRoutine(isRoutine: Boolean) { _isRoutine.value = isRoutine }
    fun setCustomRecurrenceDays(days: Set<DayOfWeek>) { _customRecurrenceDays.value = days }

    fun addChecklistItem(label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        val items = _checklistItems.value
        _checklistItems.value = items + ChecklistItemUi(
            id = nextChecklistTempId(),
            label = trimmed,
            sortOrder = items.size,
            checked = false
        )
    }

    fun removeChecklistItem(id: Long) {
        _checklistItems.update { list ->
            list.filter { it.id != id }.mapIndexed { index, item ->
                item.copy(sortOrder = index)
            }
        }
    }

    fun toggleChecklistItem(id: Long) {
        _checklistItems.update { list ->
            list.map { if (it.id == id) it.copy(checked = !it.checked) else it }
        }
    }

    fun updateChecklistItemLabel(id: Long, label: String) {
        _checklistItems.update { list ->
            list.map { if (it.id == id) it.copy(label = label) else it }
        }
    }

    suspend fun save(): Result<Unit> {
        val t = _title.value.trim()
        if (t.isBlank()) return Result.failure(IllegalArgumentException("Title is required"))
        if (_recurrenceType.value == RecurrenceType.CUSTOM && _customRecurrenceDays.value.isEmpty()) {
            return Result.failure(IllegalArgumentException("Selecione pelo menos um dia para recorrência personalizada"))
        }
        if (!reminderScheduler.canScheduleExactAlarms()) {
            return Result.failure(ExactAlarmPermissionRequiredException())
        }
        val now = System.currentTimeMillis()
        val items = _checklistItems.value
        val uid = currentUserId()

        return try {
            if (reminderId != null) {
                val existing = reminderDao.getReminderById(reminderId) ?: return Result.failure(NoSuchElementException("Reminder not found"))
                val customDaysString = if (_recurrenceType.value == RecurrenceType.CUSTOM) {
                    RecurrenceDaysConverter.serializeDays(_customRecurrenceDays.value)
                } else {
                    null
                }
                val updated = existing.copy(
                    title = t,
                    type = _recurrenceType.value,
                    dueAt = _dueAt.value,
                    categoryId = _categoryId.value,
                    listId = null,
                    priority = _priority.value,
                    isRoutine = _isRoutine.value,
                    customRecurrenceDays = customDaysString,
                    updatedAt = now
                )
                reminderDao.update(updated)
                checklistItemDao.deleteByReminderId(uid, reminderId)
                if (items.isNotEmpty()) {
                    checklistItemDao.insertAll(
                        items.mapIndexed { index, ui ->
                            ChecklistItemEntity(
                                userId = uid,
                                reminderId = reminderId,
                                label = ui.label,
                                sortOrder = index,
                                checked = ui.checked
                            )
                        }
                    )
                }
                reminderScheduler.scheduleReminder(reminderId, _dueAt.value, t, existing.notes, isSnoozeFire = false)
                reminderScheduler.cancelPendingConfirmationCheck(reminderId)
                if (_recurrenceType.value == RecurrenceType.NONE) {
                    reminderScheduler.schedulePendingConfirmationCheck(reminderId, _dueAt.value)
                }
                if (uid != "local") syncRepository.uploadAllInBackground(uid)
            } else {
                val customDaysString = if (_recurrenceType.value == RecurrenceType.CUSTOM) {
                    RecurrenceDaysConverter.serializeDays(_customRecurrenceDays.value)
                } else {
                    null
                }
                val entity = ReminderEntity(
                    userId = uid,
                    title = t,
                    notes = "",
                    type = _recurrenceType.value,
                    dueAt = _dueAt.value,
                    categoryId = _categoryId.value,
                    listId = null,
                    priority = _priority.value,
                    snoozedUntil = null,
                    isActive = true,
                    isRoutine = _isRoutine.value,
                    customRecurrenceDays = customDaysString,
                    createdAt = now,
                    updatedAt = now
                )
                val id = reminderDao.insert(entity)
                if (items.isNotEmpty()) {
                    checklistItemDao.insertAll(
                        items.mapIndexed { index, ui ->
                            ChecklistItemEntity(
                                userId = uid,
                                reminderId = id,
                                label = ui.label,
                                sortOrder = index,
                                checked = ui.checked
                            )
                        }
                    )
                }
                reminderScheduler.scheduleReminder(id, _dueAt.value, t, entity.notes, isSnoozeFire = false)
                if (_recurrenceType.value == RecurrenceType.NONE) {
                    reminderScheduler.schedulePendingConfirmationCheck(id, _dueAt.value)
                }
                if (uid != "local") syncRepository.uploadAllInBackground(uid)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun showCancellationConfirmation() {
        _showCancellationConfirmation.value = true
    }

    fun dismissCancellationConfirmation() {
        _showCancellationConfirmation.value = false
    }

    suspend fun cancelReminder(): Result<Unit> {
        val id = reminderId ?: return Result.failure(IllegalStateException("Not in edit mode"))
        return try {
            val reminder = reminderDao.getReminderById(id) ?: return Result.failure(NoSuchElementException("Reminder not found"))
            val now = System.currentTimeMillis()
            
            // Update status to CANCELLED
            val updated = reminder.copy(
                status = ReminderStatus.CANCELLED,
                updatedAt = now
            )
            reminderDao.update(updated)
            
            // Cancel any scheduled alarms
            reminderScheduler.cancelReminder(id)
            
            val uid = currentUserId()
            if (uid != "local") syncRepository.uploadAllInBackground(uid)
            _showCancellationConfirmation.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

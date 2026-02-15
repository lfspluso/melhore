package com.melhoreapp.feature.reminders.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.ChecklistItemEntity
import com.melhoreapp.core.database.entity.Priority
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.scheduling.ExactAlarmPermissionRequiredException
import com.melhoreapp.core.scheduling.ReminderScheduler
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val listDao: ListDao,
    private val checklistItemDao: ChecklistItemDao,
    private val reminderScheduler: ReminderScheduler,
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

    private val _listId = MutableStateFlow<Long?>(null)
    val listId: StateFlow<Long?> = _listId.asStateFlow()

    private val _priority = MutableStateFlow(Priority.MEDIUM)
    val priority: StateFlow<Priority> = _priority.asStateFlow()

    private val _recurrenceType = MutableStateFlow(RecurrenceType.NONE)
    val recurrenceType: StateFlow<RecurrenceType> = _recurrenceType.asStateFlow()

    private val _checklistItems = MutableStateFlow<List<ChecklistItemUi>>(emptyList())
    val checklistItems: StateFlow<List<ChecklistItemUi>> = _checklistItems.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val lists: StateFlow<List<ListEntity>> = listDao.getAllLists()
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
                    _listId.value = reminder.listId
                    _priority.value = reminder.priority
                    _recurrenceType.value = reminder.type
                }
                checklistItemDao.getItemsByReminderIdOnce(id).let { items ->
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
    fun setListId(id: Long?) { _listId.value = id }
    fun setPriority(p: Priority) { _priority.value = p }
    fun setRecurrenceType(type: RecurrenceType) { _recurrenceType.value = type }

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
        if (!reminderScheduler.canScheduleExactAlarms()) {
            return Result.failure(ExactAlarmPermissionRequiredException())
        }
        val now = System.currentTimeMillis()
        val items = _checklistItems.value

        return try {
            if (reminderId != null) {
                val existing = reminderDao.getReminderById(reminderId) ?: return Result.failure(NoSuchElementException("Reminder not found"))
                val updated = existing.copy(
                    title = t,
                    type = _recurrenceType.value,
                    dueAt = _dueAt.value,
                    categoryId = _categoryId.value,
                    listId = _listId.value,
                    priority = _priority.value,
                    updatedAt = now
                )
                reminderDao.update(updated)
                checklistItemDao.deleteByReminderId(reminderId)
                if (items.isNotEmpty()) {
                    checklistItemDao.insertAll(
                        items.mapIndexed { index, ui ->
                            ChecklistItemEntity(
                                reminderId = reminderId,
                                label = ui.label,
                                sortOrder = index,
                                checked = ui.checked
                            )
                        }
                    )
                }
                reminderScheduler.scheduleReminder(reminderId, _dueAt.value, t, existing.notes, isSnoozeFire = false)
            } else {
                val entity = ReminderEntity(
                    title = t,
                    notes = "",
                    type = _recurrenceType.value,
                    dueAt = _dueAt.value,
                    categoryId = _categoryId.value,
                    listId = _listId.value,
                    priority = _priority.value,
                    snoozedUntil = null,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
                val id = reminderDao.insert(entity)
                if (items.isNotEmpty()) {
                    checklistItemDao.insertAll(
                        items.mapIndexed { index, ui ->
                            ChecklistItemEntity(
                                reminderId = id,
                                label = ui.label,
                                sortOrder = index,
                                checked = ui.checked
                            )
                        }
                    )
                }
                reminderScheduler.scheduleReminder(id, _dueAt.value, t, entity.notes, isSnoozeFire = false)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.melhoreapp.feature.reminders.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ListEntity
import com.melhoreapp.core.database.entity.Priority
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val listDao: ListDao
) : ViewModel() {

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

    fun setTitle(value: String) { _title.value = value }
    fun setDueAt(epochMillis: Long) { _dueAt.value = epochMillis }
    fun setCategoryId(id: Long?) { _categoryId.value = id }
    fun setListId(id: Long?) { _listId.value = id }
    fun setPriority(p: Priority) { _priority.value = p }

    suspend fun save(): Result<Unit> {
        val t = _title.value.trim()
        if (t.isBlank()) return Result.failure(IllegalArgumentException("Title is required"))
        val now = System.currentTimeMillis()
        val entity = ReminderEntity(
            title = t,
            notes = "",
            type = RecurrenceType.NONE,
            dueAt = _dueAt.value,
            categoryId = _categoryId.value,
            listId = _listId.value,
            priority = _priority.value,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        return try {
            reminderDao.insert(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

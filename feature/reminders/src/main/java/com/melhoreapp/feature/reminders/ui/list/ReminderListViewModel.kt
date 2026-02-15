package com.melhoreapp.feature.reminders.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.scheduling.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderWithChecklist(
    val reminder: ReminderEntity,
    val checkedCount: Int,
    val totalCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _filterByCategoryId = MutableStateFlow<Long?>(null)
    val filterByCategoryId: StateFlow<Long?> = _filterByCategoryId.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val remindersFlow = _filterByCategoryId.flatMapLatest { categoryId ->
        if (categoryId != null) reminderDao.getRemindersByCategoryId(categoryId)
        else reminderDao.getAllReminders()
    }.combine(_sortOrder) { list, order ->
        when (order) {
            SortOrder.DUE_DATE_ASC -> list.sortedBy { it.dueAt }
            SortOrder.PRIORITY_DESC_THEN_DUE_ASC -> list.sortedWith(
                compareByDescending<ReminderEntity> { it.priority.ordinal }.thenBy { it.dueAt }
            )
        }
    }

    val remindersWithChecklist: StateFlow<List<ReminderWithChecklist>> = combine(
        remindersFlow,
        checklistItemDao.getAllItems()
    ) { reminders, allItems ->
        reminders.map { reminder ->
            val items = allItems.filter { it.reminderId == reminder.id }
            ReminderWithChecklist(
                reminder = reminder,
                checkedCount = items.count { it.checked },
                totalCount = items.size
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setFilterByCategory(id: Long?) {
        _filterByCategoryId.value = id
    }

    fun clearFilter() {
        _filterByCategoryId.value = null
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(id)
            reminderDao.deleteById(id)
        }
    }
}

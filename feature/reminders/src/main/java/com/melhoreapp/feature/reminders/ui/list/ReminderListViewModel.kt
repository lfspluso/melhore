package com.melhoreapp.feature.reminders.ui.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.scheduling.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val appPreferences = AppPreferences(context)

    private val _filter = MutableStateFlow(loadInitialFilter())
    val filter: StateFlow<ReminderListFilter> = _filter.asStateFlow()

    private val _sortOrder = MutableStateFlow(loadInitialSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val remindersFlow = _filter.flatMapLatest { f ->
        if (f.categoryIds.isEmpty()) {
            reminderDao.getAllReminders()
        } else {
            reminderDao.getRemindersByCategoryIds(f.categoryIds.toList())
        }
    }.combine(_filter) { list, f ->
        var result = list
        if (f.priorityOrdinals.isNotEmpty()) {
            result = result.filter { it.priority.ordinal in f.priorityOrdinals }
        }
        f.dateFromMillis?.let { from ->
            result = result.filter { it.dueAt >= from }
        }
        f.dateToMillis?.let { to ->
            result = result.filter { it.dueAt <= to }
        }
        result
    }.combine(_sortOrder) { list, order ->
        when (order) {
            SortOrder.DUE_DATE_ASC -> list.sortedBy { it.dueAt }
            SortOrder.PRIORITY_DESC_THEN_DUE_ASC -> list.sortedWith(
                compareByDescending<ReminderEntity> { it.priority.ordinal }.thenBy { it.dueAt }
            )
            SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOrder.CREATED_AT_ASC -> list.sortedBy { it.createdAt }
            SortOrder.CREATED_AT_DESC -> list.sortedByDescending { it.createdAt }
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

    private fun loadInitialFilter(): ReminderListFilter {
        val categoryIds = appPreferences.getLastFilterCategoryIds()
        val priorityOrdinals = appPreferences.getLastFilterPriorities()
        val dateFrom = appPreferences.getLastFilterDateFrom()
        val dateTo = appPreferences.getLastFilterDateTo()
        return ReminderListFilter(
            categoryIds = categoryIds,
            priorityOrdinals = priorityOrdinals,
            dateFromMillis = dateFrom,
            dateToMillis = dateTo
        )
    }

    private fun loadInitialSortOrder(): SortOrder {
        val name = appPreferences.getLastSortOrder() ?: return SortOrder.DUE_DATE_ASC
        return SortOrder.entries.find { it.name == name } ?: SortOrder.DUE_DATE_ASC
    }

    private fun persistFilter() {
        val f = _filter.value
        appPreferences.setLastFilterCategoryIds(f.categoryIds)
        appPreferences.setLastFilterPriorities(f.priorityOrdinals)
        appPreferences.setLastFilterDateFrom(f.dateFromMillis)
        appPreferences.setLastFilterDateTo(f.dateToMillis)
    }

    private fun persistSortOrder() {
        appPreferences.setLastSortOrder(_sortOrder.value.name)
    }

    fun setFilter(newFilter: ReminderListFilter) {
        _filter.value = newFilter
        persistFilter()
    }

    fun setFilterByCategoryIds(ids: Set<Long>) {
        _filter.value = _filter.value.copy(categoryIds = ids)
        persistFilter()
    }

    fun setFilterPriorities(ordinals: Set<Int>) {
        _filter.value = _filter.value.copy(priorityOrdinals = ordinals)
        persistFilter()
    }

    fun setFilterDateRange(from: Long?, to: Long?) {
        _filter.value = _filter.value.copy(dateFromMillis = from, dateToMillis = to)
        persistFilter()
    }

    fun clearFilter() {
        _filter.value = ReminderListFilter()
        persistFilter()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        persistSortOrder()
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(id)
            reminderDao.deleteById(id)
        }
    }
}

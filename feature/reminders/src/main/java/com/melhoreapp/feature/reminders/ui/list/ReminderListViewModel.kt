package com.melhoreapp.feature.reminders.ui.list

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.database.entity.ReminderStatus
import com.melhoreapp.core.scheduling.ReminderScheduler
import com.melhoreapp.core.sync.SyncRepository
import com.melhoreapp.core.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderWithChecklist(
    val reminder: ReminderEntity,
    val checkedCount: Int,
    val totalCount: Int
)

/** Section for grouped-by-tag list: tag label (e.g. "Sem tag" or category name) and its reminders. */
data class ReminderSection(
    val tagLabel: String,
    val items: List<ReminderWithChecklist>
)

private const val TAG = "PendingConfirmation"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReminderListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao,
    private val reminderScheduler: ReminderScheduler,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val appPreferences = AppPreferences(context)

    private val userIdFlow: StateFlow<String> = authRepository.currentUser
        .map { it?.userId ?: "local" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "local"
        )

    private val _filter = MutableStateFlow(loadInitialFilter())
    val filter: StateFlow<ReminderListFilter> = _filter.asStateFlow()

    private val _sortOrder = MutableStateFlow(loadInitialSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _groupByTag = MutableStateFlow(appPreferences.getGroupByTag())
    val groupByTag: StateFlow<Boolean> = _groupByTag.asStateFlow()

    private val _showAdvancedFilters = MutableStateFlow(appPreferences.getShowAdvancedFilters())
    val showAdvancedFilters: StateFlow<Boolean> = _showAdvancedFilters.asStateFlow()

    private val _showFilterBottomSheet = MutableStateFlow(false)
    val showFilterBottomSheet: StateFlow<Boolean> = _showFilterBottomSheet.asStateFlow()

    private val _showSortBottomSheet = MutableStateFlow(false)
    val showSortBottomSheet: StateFlow<Boolean> = _showSortBottomSheet.asStateFlow()

    private val _completionConfirmationReminderId = MutableStateFlow<Long?>(null)
    val completionConfirmationReminderId: StateFlow<Long?> = _completionConfirmationReminderId.asStateFlow()

    /** Sync status for UI (Sprint 19). */
    val syncStatus: StateFlow<SyncStatus> = syncRepository.syncStatus

    /** True when user chose local-only (no Google sign-in); show "Apenas neste aparelho" instead of sync status (Sprint 19.5). */
    val isLocalOnly: StateFlow<Boolean> = userIdFlow
        .map { it == "local" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    private val _selectedTab = MutableStateFlow(loadInitialTab())
    val selectedTab: StateFlow<ReminderTab> = _selectedTab.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = userIdFlow
        .flatMapLatest { uid -> categoryDao.getAllCategories(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val remindersFlow = userIdFlow.flatMapLatest { uid ->
        _filter.flatMapLatest { f ->
            if (f.categoryIds.isEmpty()) {
                reminderDao.getAllReminders(uid)
            } else {
                reminderDao.getRemindersByCategoryIds(uid, f.categoryIds.toList())
            }
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
        // Filter by status
        if (!f.showCompleted) {
            result = result.filter { it.status != ReminderStatus.COMPLETED }
        }
        if (!f.showCancelled) {
            result = result.filter { it.status != ReminderStatus.CANCELLED }
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
        userIdFlow.flatMapLatest { uid -> checklistItemDao.getAllItems(uid) }
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

    // Unfiltered reminders + checklist (no date/category filter) so past-due items are always included
    private val allRemindersWithChecklist: StateFlow<List<ReminderWithChecklist>> = combine(
        userIdFlow.flatMapLatest { uid -> reminderDao.getAllReminders(uid) },
        userIdFlow.flatMapLatest { uid -> checklistItemDao.getAllItems(uid) }
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

    // Periodic flow that emits every minute to trigger re-evaluation of pending confirmation reminders
    // Emits immediately on start, then every minute thereafter
    private val timeTickFlow: StateFlow<Unit> = flow {
        emit(Unit) // Emit immediately on first subscription
        while (true) {
            delay(60_000) // Wait 1 minute
            emit(Unit) // Emit every minute
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Unit
    )

    // Pending confirmation: from UNFILTERED list so past-due reminders are never hidden by date/category filter.
    // Only Tarefas (!isRoutine) so warning section matches Tarefas tab.
    val pendingConfirmationReminders: StateFlow<List<ReminderWithChecklist>> = allRemindersWithChecklist
        .combine(timeTickFlow) { reminders, _ ->
            val now = System.currentTimeMillis()
            reminders.filter { item ->
                val reminder = item.reminder
                if (reminder.isRoutine) return@filter false // Only show on Tarefas tab
                val snoozedUntil = reminder.snoozedUntil
                reminder.status == ReminderStatus.ACTIVE &&
                    reminder.dueAt <= now &&
                    (snoozedUntil == null || snoozedUntil <= now) &&
                    reminder.type == RecurrenceType.NONE
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            pendingConfirmationReminders.collect { list ->
                Log.d(TAG, "pendingConfirmationReminders size=${list.size}, ids=${list.map { it.reminder.id }}, dueAts=${list.map { it.reminder.dueAt }}, titles=${list.map { it.reminder.title }}")
            }
        }
        viewModelScope.launch {
            allRemindersWithChecklist.collect { list ->
                val now = System.currentTimeMillis()
                val pastDue = list.count { it.reminder.dueAt <= now }
                val activeNone = list.count { it.reminder.status == ReminderStatus.ACTIVE && it.reminder.type == RecurrenceType.NONE && !it.reminder.isRoutine }
                Log.d(TAG, "allRemindersWithChecklist size=${list.size} pastDueCount=$pastDue ACTIVE+NONE+!isRoutine=$activeNone now=$now")
            }
        }
    }

    // Tab-filtered list: TAREFAS = includes task reminders (excludes Rotinas), ROTINAS = routine (non-task) reminders (Sprint 20)
    private val remindersWithChecklistForTab: StateFlow<List<ReminderWithChecklist>> = combine(
        remindersWithChecklist,
        _selectedTab
    ) { list, tab ->
        when (tab) {
            ReminderTab.TAREFAS -> list.filter { !it.reminder.isRoutine }
            ReminderTab.ROTINAS -> list.filter { it.reminder.isRoutine && !it.reminder.isTask }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Filtered list excluding pending confirmation reminders on Tarefas (they appear only in warning section)
    val filteredRemindersWithChecklist: StateFlow<List<ReminderWithChecklist>> = combine(
        remindersWithChecklistForTab,
        pendingConfirmationReminders,
        _selectedTab
    ) { tabList, pendingReminders, tab ->
        val pendingIds = pendingReminders.map { it.reminder.id }.toSet()
        if (tab == ReminderTab.ROTINAS) tabList else tabList.filter { it.reminder.id !in pendingIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val groupedSections: StateFlow<List<ReminderSection>> = combine(
        filteredRemindersWithChecklist,
        categories,
        _groupByTag
    ) { list, cats, group ->
        if (!group) return@combine emptyList()
        val categoryByName = cats.associateBy { it.id }
        val grouped = list.groupBy { item ->
            item.reminder.categoryId?.let { id ->
                categoryByName[id]?.name ?: "Sem tag"
            } ?: "Sem tag"
        }
        val sortedLabels = grouped.keys.sortedWith(
            compareBy { label: String -> if (label == "Sem tag") "" else label }
        )
        sortedLabels.map { label ->
            ReminderSection(tagLabel = label, items = grouped.getValue(label))
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
        val showCompleted = appPreferences.getShowCompletedReminders()
        val showCancelled = appPreferences.getShowCancelledReminders()
        return ReminderListFilter(
            categoryIds = categoryIds,
            priorityOrdinals = priorityOrdinals,
            dateFromMillis = dateFrom,
            dateToMillis = dateTo,
            showCompleted = showCompleted,
            showCancelled = showCancelled
        )
    }

    private fun loadInitialSortOrder(): SortOrder {
        val name = appPreferences.getLastSortOrder()
        return if (name != null) {
            SortOrder.entries.find { it.name == name } ?: SortOrder.DUE_DATE_ASC
        } else {
            SortOrder.DUE_DATE_ASC
        }
    }

    private fun loadInitialTab(): ReminderTab {
        val name = appPreferences.getSelectedReminderTab()
        return if (name != null) {
            ReminderTab.entries.find { it.name == name } ?: ReminderTab.TAREFAS
        } else {
            ReminderTab.TAREFAS
        }
    }

    private fun persistFilter() {
        val f = _filter.value
        appPreferences.setLastFilterCategoryIds(f.categoryIds)
        appPreferences.setLastFilterPriorities(f.priorityOrdinals)
        appPreferences.setLastFilterDateFrom(f.dateFromMillis)
        appPreferences.setLastFilterDateTo(f.dateToMillis)
        appPreferences.setShowCompletedReminders(f.showCompleted)
        appPreferences.setShowCancelledReminders(f.showCancelled)
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

    fun setShowCompleted(show: Boolean) {
        _filter.value = _filter.value.copy(showCompleted = show)
        persistFilter()
    }

    fun setShowCancelled(show: Boolean) {
        _filter.value = _filter.value.copy(showCancelled = show)
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

    fun setGroupByTag(groupByTag: Boolean) {
        _groupByTag.value = groupByTag
        appPreferences.setGroupByTag(groupByTag)
    }

    fun setShowAdvancedFilters(show: Boolean) {
        _showAdvancedFilters.value = show
        appPreferences.setShowAdvancedFilters(show)
    }

    fun setShowFilterBottomSheet(show: Boolean) {
        _showFilterBottomSheet.value = show
    }

    fun setShowSortBottomSheet(show: Boolean) {
        _showSortBottomSheet.value = show
    }

    fun setSelectedTab(tab: ReminderTab) {
        _selectedTab.value = tab
        appPreferences.setSelectedReminderTab(tab.name)
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            val uid = userIdFlow.value
            reminderScheduler.cancelReminder(id)
            reminderDao.deleteById(id)
            if (uid != "local") syncRepository.deleteReminderFromCloud(uid, id)
        }
    }

    fun showCompletionConfirmation(reminderId: Long) {
        _completionConfirmationReminderId.value = reminderId
    }

    fun dismissCompletionConfirmation() {
        _completionConfirmationReminderId.value = null
    }

    fun markAsCompleted(reminderId: Long) {
        viewModelScope.launch {
            val uid = userIdFlow.value
            val reminder = reminderDao.getReminderById(reminderId) ?: return@launch
            val now = System.currentTimeMillis()
            
            // Update status to COMPLETED
            val updated = reminder.copy(
                status = ReminderStatus.COMPLETED,
                updatedAt = now
            )
            reminderDao.update(updated)
            
            // Cancel any scheduled alarms
            reminderScheduler.cancelReminder(reminderId)
            
            // Check if delete after completion is enabled and delete if so
            if (appPreferences.getDeleteAfterCompletion()) {
                reminderScheduler.cancelReminder(reminderId)
                reminderDao.deleteById(reminderId)
                if (uid != "local") syncRepository.deleteReminderFromCloud(uid, reminderId)
            } else {
                if (uid != "local") syncRepository.uploadAllInBackground(uid)
            }
            
            // Dismiss confirmation dialog
            _completionConfirmationReminderId.value = null
        }
    }

    /** Retry sync after error (Sprint 19). */
    fun retrySync() {
        viewModelScope.launch {
            val uid = userIdFlow.value
            if (uid != "local") syncRepository.retrySync(uid).collect { }
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)
package com.melhoreapp.feature.reminders.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.Priority
import com.melhoreapp.core.common.RecurrenceDaysConverter
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.database.entity.ReminderStatus
import com.melhoreapp.core.sync.SyncStatus
import com.melhoreapp.core.scheduling.nextOccurrenceMillis
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderListViewModel,
    onAddClick: () -> Unit,
    onReminderClick: (Long) -> Unit = {},
    onTemplatesClick: () -> Unit = {}
) {
    val filteredRemindersWithChecklist by viewModel.filteredRemindersWithChecklist.collectAsState()
    val pendingConfirmationReminders by viewModel.pendingConfirmationReminders.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val groupByTag by viewModel.groupByTag.collectAsState()
    val groupedSections by viewModel.groupedSections.collectAsState()
    val showAdvancedFilters by viewModel.showAdvancedFilters.collectAsState()
    val completionConfirmationReminderId by viewModel.completionConfirmationReminderId.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val hasActiveFilter = !filter.isAll()
    val syncStatus by viewModel.syncStatus.collectAsState(initial = SyncStatus.Idle)
    val isLocalOnly by viewModel.isLocalOnly.collectAsState(initial = true)
    val showFilterBottomSheet by viewModel.showFilterBottomSheet.collectAsState()
    val showSortBottomSheet by viewModel.showSortBottomSheet.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Melhore, Maria Luiza") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        // Filter icon with badge indicator when filters are active
                        BadgedBox(
                            badge = {
                                if (hasActiveFilter) {
                                    Badge()
                                }
                            }
                        ) {
                            IconButton(onClick = { viewModel.setShowFilterBottomSheet(true) }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filtrar",
                                    tint = if (hasActiveFilter) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                        // Sort icon
                        IconButton(onClick = { viewModel.setShowSortBottomSheet(true) }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Ordenar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onTemplatesClick) {
                            Icon(
                                Icons.Default.DashboardCustomize,
                                contentDescription = "Modelos de lembretes"
                            )
                        }
                    }
                )
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Melhore")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(
                selectedTabIndex = if (selectedTab == ReminderTab.TAREFAS) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = when (selectedTab) {
                            ReminderTab.TAREFAS -> "Aba Tarefas selecionada"
                            ReminderTab.ROTINAS -> "Aba Rotinas selecionada"
                        }
                    }
            ) {
                Tab(
                    selected = selectedTab == ReminderTab.TAREFAS,
                    onClick = { viewModel.setSelectedTab(ReminderTab.TAREFAS) },
                    text = { Text("Tarefas") },
                    modifier = Modifier.semantics { contentDescription = "Aba Tarefas" }
                )
                Tab(
                    selected = selectedTab == ReminderTab.ROTINAS,
                    onClick = { viewModel.setSelectedTab(ReminderTab.ROTINAS) },
                    text = { Text("Rotinas") },
                    modifier = Modifier.semantics { contentDescription = "Aba Rotinas" }
                )
            }
            val showPendingSection = selectedTab == ReminderTab.TAREFAS && pendingConfirmationReminders.isNotEmpty()
            val isEmptyList = filteredRemindersWithChecklist.isEmpty() && !showPendingSection
            if (isEmptyList) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasActiveFilter) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Nenhum Melhore com esses filtros",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = viewModel::clearFilter,
                                modifier = Modifier.semantics {
                                    contentDescription = "Limpar filtros"
                                }
                            ) {
                                Text("Limpar filtros")
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    ReminderTab.TAREFAS -> "Nenhum Melhore ainda"
                                    ReminderTab.ROTINAS -> "Nenhuma rotina ainda"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (selectedTab) {
                                    ReminderTab.TAREFAS -> "Toque em + para adicionar um Melhore"
                                    ReminderTab.ROTINAS -> "Crie um Melhore e marque como Rotina para ver aqui"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Warning section for pending confirmation (Tarefas tab only)
                    if (showPendingSection) {
                        item(key = "warning_section") {
                            PendingConfirmationWarningSection(
                                pendingReminders = pendingConfirmationReminders,
                                onReminderClick = onReminderClick,
                                onDelete = { viewModel.deleteReminder(it) },
                                onComplete = { viewModel.showCompletionConfirmation(it) }
                            )
                        }
                    }
                    if (groupByTag && groupedSections.isNotEmpty()) {
                        groupedSections.forEachIndexed { sectionIndex, section ->
                            item(key = "header_$sectionIndex") {
                                SectionHeader(tagLabel = section.tagLabel)
                            }
                            items(
                                items = section.items,
                                key = { it.reminder.id }
                            ) { item ->
                                ReminderItem(
                                    reminderWithChecklist = item,
                                    onClick = { onReminderClick(item.reminder.id) },
                                    onDelete = { viewModel.deleteReminder(item.reminder.id) },
                                    onComplete = { viewModel.showCompletionConfirmation(item.reminder.id) }
                                )
                            }
                        }
                    } else {
                        items(
                            items = filteredRemindersWithChecklist,
                            key = { it.reminder.id }
                        ) { item ->
                            ReminderItem(
                                reminderWithChecklist = item,
                                onClick = { onReminderClick(item.reminder.id) },
                                onDelete = { viewModel.deleteReminder(item.reminder.id) },
                                onComplete = { viewModel.showCompletionConfirmation(item.reminder.id) }
                            )
                        }
                    }
                }
            }
        }
        }
        
        // Sync status overlay banner at top
        var showSyncBanner by remember { mutableStateOf(false) }
        
        // Show banner when sync status changes from Idle
        LaunchedEffect(syncStatus) {
            if (!isLocalOnly && syncStatus != SyncStatus.Idle) {
                showSyncBanner = true
                // Auto-hide after delay based on status
                when (syncStatus) {
                    is SyncStatus.Synced -> {
                        delay(3000) // 3 seconds for "Sincronizado"
                        showSyncBanner = false
                    }
                    is SyncStatus.Error -> {
                        delay(5000) // 5 seconds for error
                        showSyncBanner = false
                    }
                    else -> { } // Syncing stays visible
                }
            } else {
                showSyncBanner = false
            }
        }
        
        if (!isLocalOnly && showSyncBanner && syncStatus != SyncStatus.Idle) {
            SyncStatusBanner(
                syncStatus = syncStatus,
                onRetry = viewModel::retrySync,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            )
        }
    }

    // Completion confirmation dialog
    completionConfirmationReminderId?.let { reminderId ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissCompletionConfirmation() },
            title = { Text("Você tem certeza?") },
            text = { Text("Deseja marcar este Melhore como concluído?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.markAsCompleted(reminderId) }
                ) {
                    Text("Sim")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.dismissCompletionConfirmation() }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Filter bottom sheet
    if (showFilterBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowFilterBottomSheet(false) },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            FilterBottomSheetContent(
                filter = filter,
                categories = categories,
                groupByTag = groupByTag,
                onFilterByCategoryIds = viewModel::setFilterByCategoryIds,
                onFilterPriorities = viewModel::setFilterPriorities,
                onFilterDateRange = viewModel::setFilterDateRange,
                onShowCompletedChange = viewModel::setShowCompleted,
                onShowCancelledChange = viewModel::setShowCancelled,
                onGroupByTagChange = viewModel::setGroupByTag,
                onClearFilter = viewModel::clearFilter,
                onDismiss = { viewModel.setShowFilterBottomSheet(false) }
            )
        }
    }

    // Sort bottom sheet
    if (showSortBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowSortBottomSheet(false) },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SortBottomSheetContent(
                sortOrder = sortOrder,
                onSortOrderChange = { order ->
                    viewModel.setSortOrder(order)
                    viewModel.setShowSortBottomSheet(false)
                }
            )
        }
    }
}

@Composable
private fun LocalOnlyStatusRow() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Sync,
                contentDescription = "Apenas neste aparelho",
                modifier = Modifier.padding(2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                "Apenas neste aparelho",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncStatusBanner(
    syncStatus: SyncStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (syncStatus) {
        is SyncStatus.Idle -> { }
        is SyncStatus.Syncing -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                modifier = modifier,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp)
                    )
                    Text(
                        "Sincronizando…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is SyncStatus.Synced -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                modifier = modifier,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Sincronizado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is SyncStatus.Error -> {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                modifier = modifier,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Erro de sincronização",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = onRetry,
                        content = { Text("Tentar novamente") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderSortRow(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ordenar:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 4.dp)
                .semantics { heading() }
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = sortOrder == SortOrder.DUE_DATE_ASC,
                onClick = { onSortOrderChange(SortOrder.DUE_DATE_ASC) },
                label = { Text("Por data") },
                modifier = Modifier.semantics {
                    contentDescription = "Ordenar por data"
                }
            )
            FilterChip(
                selected = sortOrder == SortOrder.PRIORITY_DESC_THEN_DUE_ASC,
                onClick = { onSortOrderChange(SortOrder.PRIORITY_DESC_THEN_DUE_ASC) },
                label = { Text("Por prioridade") },
                modifier = Modifier.semantics {
                    contentDescription = "Ordenar por prioridade"
                }
            )
            FilterChip(
                selected = sortOrder == SortOrder.TITLE_ASC,
                onClick = { onSortOrderChange(SortOrder.TITLE_ASC) },
                label = { Text("Por título") },
                modifier = Modifier.semantics {
                    contentDescription = "Ordenar por título"
                }
            )
            FilterChip(
                selected = sortOrder == SortOrder.CREATED_AT_ASC,
                onClick = { onSortOrderChange(SortOrder.CREATED_AT_ASC) },
                label = { Text("Por criação") },
                modifier = Modifier.semantics {
                    contentDescription = "Ordenar por data de criação, mais antigos primeiro"
                }
            )
            FilterChip(
                selected = sortOrder == SortOrder.CREATED_AT_DESC,
                onClick = { onSortOrderChange(SortOrder.CREATED_AT_DESC) },
                label = { Text("Mais recentes") },
                modifier = Modifier.semantics {
                    contentDescription = "Ordenar por mais recentes primeiro"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderFilterRow(
    filter: ReminderListFilter,
    categories: List<CategoryEntity>,
    onFilterByCategoryIds: (Set<Long>) -> Unit,
    onFilterPriorities: (Set<Int>) -> Unit,
    onFilterDateRange: (Long?, Long?) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit,
    onShowCancelledChange: (Boolean) -> Unit,
    onClearFilter: () -> Unit
) {
    val scrollState = rememberScrollState()
    val hasAnyFilter = !filter.isAll()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filtrar:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .semantics { heading() }
            )
            FilterChip(
                selected = !hasAnyFilter,
                onClick = onClearFilter,
                label = { Text("Todos") },
                modifier = Modifier.semantics {
                    contentDescription = "Mostrar todos os Melhores"
                }
            )
            filter.categoryIds.forEach { categoryId ->
                val category = categories.find { it.id == categoryId } ?: return@forEach
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterByCategoryIds(filter.categoryIds - categoryId)
                    },
                    label = { Text(category.name) },
                    modifier = Modifier.semantics {
                        contentDescription = "Filtrar por tag ${category.name}. Toque para remover."
                    }
                )
            }
            if (categories.isNotEmpty()) {
                var categoryExpanded by remember { mutableStateOf(false) }
                val availableToAdd = categories.filter { it.id !in filter.categoryIds }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Adicionar tag",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(0.35f)
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableToAdd.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onFilterByCategoryIds(filter.categoryIds + category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                        if (availableToAdd.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhuma outra tag") },
                                onClick = { categoryExpanded = false }
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Priority.LOW to "Baixa",
                Priority.MEDIUM to "Média",
                Priority.HIGH to "Alta",
                Priority.URGENT to "Urgente"
            ).forEach { (priority, label) ->
                val selected = priority.ordinal in filter.priorityOrdinals
                FilterChip(
                    selected = selected,
                    onClick = {
                        val newSet = if (selected) {
                            filter.priorityOrdinals - priority.ordinal
                        } else {
                            filter.priorityOrdinals + priority.ordinal
                        }
                        onFilterPriorities(newSet)
                    },
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        contentDescription = "Filtrar por prioridade $label"
                    }
                )
            }
            val now = ZonedDateTime.now()
            FilterChip(
                selected = filter.dateFromMillis != null && filter.dateToMillis != null &&
                    isNext7Days(filter.dateFromMillis, filter.dateToMillis),
                onClick = {
                    val from = now.toInstant().toEpochMilli()
                    val to = now.plusDays(7).toInstant().toEpochMilli()
                    onFilterDateRange(from, to)
                },
                label = { Text("Próximos 7 dias") },
                modifier = Modifier.semantics {
                    contentDescription = "Filtrar por próximos 7 dias"
                }
            )
            FilterChip(
                selected = filter.dateFromMillis != null && filter.dateToMillis != null &&
                    isThisMonth(filter.dateFromMillis, filter.dateToMillis),
                onClick = {
                    val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                    val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_000_000)
                    onFilterDateRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
                },
                label = { Text("Este mês") },
                modifier = Modifier.semantics {
                    contentDescription = "Filtrar por este mês"
                }
            )
            FilterChip(
                selected = filter.dateFromMillis == null && filter.dateToMillis == null,
                onClick = { onFilterDateRange(null, null) },
                label = { Text("Sem filtro de data") },
                modifier = Modifier.semantics {
                    contentDescription = "Remover filtro de data"
                }
            )
        }
        // Show completed and cancelled filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filter.showCompleted,
                onClick = { onShowCompletedChange(!filter.showCompleted) },
                label = { Text("Mostrar concluídos") },
                modifier = Modifier.semantics {
                    contentDescription = if (filter.showCompleted) {
                        "Ocultar Melhores concluídos"
                    } else {
                        "Mostrar Melhores concluídos"
                    }
                }
            )
            FilterChip(
                selected = filter.showCancelled,
                onClick = { onShowCancelledChange(!filter.showCancelled) },
                label = { Text("Mostrar cancelados") },
                modifier = Modifier.semantics {
                    contentDescription = if (filter.showCancelled) {
                        "Ocultar Melhores cancelados"
                    } else {
                        "Mostrar Melhores cancelados"
                    }
                }
            )
        }
    }
}

@Composable
private fun ReminderGroupByRow(
    groupByTag: Boolean,
    onGroupByTagChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Exibir:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 4.dp)
                .semantics { heading() }
        )
        FilterChip(
            selected = groupByTag,
            onClick = { onGroupByTagChange(true) },
            label = { Text("Agrupar por tag") },
            modifier = Modifier.semantics {
                contentDescription = "Agrupar por tag"
            }
        )
        FilterChip(
            selected = !groupByTag,
            onClick = { onGroupByTagChange(false) },
            label = { Text("Lista plana") },
            modifier = Modifier.semantics {
                contentDescription = "Lista plana"
            }
        )
    }
}

@Composable
private fun PendingConfirmationWarningSection(
    pendingReminders: List<ReminderWithChecklist>,
    onReminderClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onComplete: (Long) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "Aviso: Melhores pendentes de confirmação"
            },
        color = Color(0xFFFFB74D).copy(alpha = 0.15f), // Orange/yellow background
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = "PENDENTE CONFIRMAÇÃO",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFB74D), // Orange/yellow text
                modifier = Modifier.semantics {
                    heading()
                }
            )
            // Subtitle
            Text(
                text = "É importante não deixar Melhores sem estarem completos, agendados ou cancelados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // List of pending reminders
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingReminders.forEach { item ->
                    ReminderItem(
                        reminderWithChecklist = item,
                        onClick = { onReminderClick(item.reminder.id) },
                        onDelete = { onDelete(item.reminder.id) },
                        onComplete = { onComplete(item.reminder.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(tagLabel: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "Seção: $tagLabel"
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Text(
            text = tagLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
    }
}

private fun isNext7Days(from: Long?, to: Long?): Boolean {
    if (from == null || to == null) return false
    val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
    val duration = to - from
    return duration in (sevenDaysMs - 60_000)..(sevenDaysMs + 60_000)
}

private fun isThisMonth(from: Long?, to: Long?): Boolean {
    if (from == null || to == null) return false
    val start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(from), ZoneId.systemDefault())
    val end = ZonedDateTime.ofInstant(Instant.ofEpochMilli(to), ZoneId.systemDefault())
    val now = ZonedDateTime.now()
    return start.year == now.year && start.month == now.month &&
        end.year == now.year && end.month == now.month
}

private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Seg"
    DayOfWeek.TUESDAY -> "Ter"
    DayOfWeek.WEDNESDAY -> "Qua"
    DayOfWeek.THURSDAY -> "Qui"
    DayOfWeek.FRIDAY -> "Sex"
    DayOfWeek.SATURDAY -> "Sáb"
    DayOfWeek.SUNDAY -> "Dom"
}

/**
 * Calculates the next notification date for a reminder.
 * Returns a Pair of (timestamp, displayText) where timestamp may be null for completed/cancelled reminders.
 */
@Composable
private fun getNextNotificationDate(reminder: ReminderEntity): Pair<Long?, String> {
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun formatInLocalZone(epochMillis: Long): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return "${dateFormatter.format(zdt.toLocalDate())} · ${timeFormatter.format(zdt.toLocalTime())}"
    }

    // If completed or cancelled, return empty string (status shown via tag, not text)
    if (reminder.status == ReminderStatus.COMPLETED) {
        return Pair(null, "")
    }
    if (reminder.status == ReminderStatus.CANCELLED) {
        return Pair(null, "")
    }

    val snoozedUntil = reminder.snoozedUntil

    // If snoozed, show snooze time
    if (snoozedUntil != null && snoozedUntil > now) {
        return Pair(snoozedUntil, formatInLocalZone(snoozedUntil))
    }

    // If recurring, calculate next occurrence
    if (reminder.type != RecurrenceType.NONE) {
        val nextOccurrence = nextOccurrenceMillis(reminder.dueAt, reminder.type, reminder.customRecurrenceDays)
        return if (nextOccurrence != null) {
            Pair(nextOccurrence, formatInLocalZone(nextOccurrence))
        } else {
            Pair(null, "")
        }
    }

    // Non-recurring: show empty string if past (status shown via tag), else show dueAt
    return if (reminder.dueAt <= now) {
        Pair(null, "")
    } else {
        Pair(reminder.dueAt, formatInLocalZone(reminder.dueAt))
    }
}

@Composable
private fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (priority) {
        Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        Priority.MEDIUM -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        Priority.HIGH -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Priority.URGENT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    val priorityLabel = when (priority) {
        Priority.LOW -> "Baixa"
        Priority.MEDIUM -> "Média"
        Priority.HIGH -> "Alta"
        Priority.URGENT -> "Urgente"
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = priorityLabel,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReminderItem(
    reminderWithChecklist: ReminderWithChecklist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    val reminder = reminderWithChecklist.reminder
    val isCompleted = reminder.status == ReminderStatus.COMPLETED
    val isCancelled = reminder.status == ReminderStatus.CANCELLED
    val isActive = reminder.status == ReminderStatus.ACTIVE
    val (_, dateText) = getNextNotificationDate(reminder)
    
    // Check if reminder is pending confirmation (ACTIVE but past due date and not snoozed)
    val now = System.currentTimeMillis()
    val snoozedUntil = reminder.snoozedUntil
    val isPendingConfirmation = isActive && 
        reminder.dueAt <= now && 
        (snoozedUntil == null || snoozedUntil <= now) &&
        reminder.type == RecurrenceType.NONE

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted || isCancelled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isCompleted || isCancelled) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompleted || isCancelled) 8.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dateText.isNotEmpty()) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (isPendingConfirmation) {
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFFFFB74D).copy(alpha = 0.25f) // Orange/yellow background (works with dark theme)
                    ) {
                        Text(
                            text = "PENDENTE CONFIRMAÇÃO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D), // Orange/yellow text
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (isCompleted) {
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "MELHORADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (isCancelled) {
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "CANCELADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                // Show Routine badge
                if (reminder.isRoutine && !isCompleted && !isCancelled) {
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Rotina",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (reminder.type != RecurrenceType.NONE && !isCompleted && !isCancelled) {
                    val recurrenceLabel = when (reminder.type) {
                        RecurrenceType.NONE -> ""
                        RecurrenceType.DAILY -> "Diário"
                        RecurrenceType.WEEKLY -> "Semanal"
                        RecurrenceType.BIWEEKLY -> "Quinzenal"
                        RecurrenceType.MONTHLY -> "Mensal"
                        RecurrenceType.CUSTOM -> {
                            val days = RecurrenceDaysConverter.deserializeDays(reminder.customRecurrenceDays)
                            if (days.isNotEmpty()) {
                                days.sortedBy { it.value }.joinToString(", ") { dayLabel(it) }
                            } else {
                                "Personalizado"
                            }
                        }
                    }
                    Text(
                        text = recurrenceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (reminderWithChecklist.totalCount > 0 && !isCompleted && !isCancelled) {
                    Text(
                        text = "${reminderWithChecklist.checkedCount}/${reminderWithChecklist.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (!isCompleted && !isCancelled) {
                    PriorityBadge(
                        priority = reminder.priority,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            // Show complete button only for ACTIVE reminders
            if (isActive) {
                IconButton(onClick = onComplete) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Marcar como concluído"
                    )
                }
            }
            // Show delete button only for COMPLETED reminders
            if (isCompleted) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Melhore"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheetContent(
    filter: ReminderListFilter,
    categories: List<CategoryEntity>,
    groupByTag: Boolean,
    onFilterByCategoryIds: (Set<Long>) -> Unit,
    onFilterPriorities: (Set<Int>) -> Unit,
    onFilterDateRange: (Long?, Long?) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit,
    onShowCancelledChange: (Boolean) -> Unit,
    onGroupByTagChange: (Boolean) -> Unit,
    onClearFilter: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filtros",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Categories filter
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleMedium
        )
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.categoryIds.isEmpty(),
                onClick = {
                    onFilterByCategoryIds(emptySet())
                },
                label = { Text("Todas") }
            )
            filter.categoryIds.forEach { categoryId ->
                val category = categories.find { it.id == categoryId } ?: return@forEach
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterByCategoryIds(filter.categoryIds - categoryId)
                    },
                    label = { Text(category.name) }
                )
            }
            if (categories.isNotEmpty()) {
                var categoryExpanded by remember { mutableStateOf(false) }
                val availableToAdd = categories.filter { it.id !in filter.categoryIds }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Adicionar tag",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(0.4f)
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableToAdd.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onFilterByCategoryIds(filter.categoryIds + category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                        if (availableToAdd.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhuma outra tag") },
                                onClick = { categoryExpanded = false }
                            )
                        }
                    }
                }
            }
        }
        
        // Priority filter
        Text(
            text = "Prioridade",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Priority.LOW to "Baixa",
                Priority.MEDIUM to "Média",
                Priority.HIGH to "Alta",
                Priority.URGENT to "Urgente"
            ).forEach { (priority, label) ->
                val selected = priority.ordinal in filter.priorityOrdinals
                FilterChip(
                    selected = selected,
                    onClick = {
                        val newSet = if (selected) {
                            filter.priorityOrdinals - priority.ordinal
                        } else {
                            filter.priorityOrdinals + priority.ordinal
                        }
                        onFilterPriorities(newSet)
                    },
                    label = { Text(label) }
                )
            }
        }
        
        // Date range filter
        Text(
            text = "Data",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val now = ZonedDateTime.now()
            FilterChip(
                selected = filter.dateFromMillis != null && filter.dateToMillis != null &&
                    isNext7Days(filter.dateFromMillis, filter.dateToMillis),
                onClick = {
                    val from = now.toInstant().toEpochMilli()
                    val to = now.plusDays(7).toInstant().toEpochMilli()
                    onFilterDateRange(from, to)
                },
                label = { Text("Próximos 7 dias") }
            )
            FilterChip(
                selected = filter.dateFromMillis != null && filter.dateToMillis != null &&
                    isThisMonth(filter.dateFromMillis, filter.dateToMillis),
                onClick = {
                    val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                    val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_000_000)
                    onFilterDateRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
                },
                label = { Text("Este mês") }
            )
            FilterChip(
                selected = filter.dateFromMillis == null && filter.dateToMillis == null,
                onClick = { onFilterDateRange(null, null) },
                label = { Text("Sem filtro de data") }
            )
        }
        
        // Show completed/cancelled
        Text(
            text = "Status",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.showCompleted,
                onClick = { onShowCompletedChange(!filter.showCompleted) },
                label = { Text("Mostrar concluídos") }
            )
            FilterChip(
                selected = filter.showCancelled,
                onClick = { onShowCancelledChange(!filter.showCancelled) },
                label = { Text("Mostrar cancelados") }
            )
        }
        
        // Group by tag
        Text(
            text = "Exibição",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = groupByTag,
                onClick = { onGroupByTagChange(true) },
                label = { Text("Agrupar por tag") }
            )
            FilterChip(
                selected = !groupByTag,
                onClick = { onGroupByTagChange(false) },
                label = { Text("Lista plana") }
            )
        }
        
        // Clear filter button
        if (!filter.isAll()) {
            Button(
                onClick = {
                    onClearFilter()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Limpar filtros")
            }
        }
    }
}

@Composable
private fun SortBottomSheetContent(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Ordenar",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        listOf(
            SortOrder.DUE_DATE_ASC to "Por data",
            SortOrder.PRIORITY_DESC_THEN_DUE_ASC to "Por prioridade",
            SortOrder.TITLE_ASC to "Por título",
            SortOrder.CREATED_AT_ASC to "Por criação",
            SortOrder.CREATED_AT_DESC to "Mais recentes"
        ).forEach { (order, label) ->
            FilterChip(
                selected = sortOrder == order,
                onClick = { onSortOrderChange(order) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderListScreenEmptyPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Melhores") })
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum Melhore ainda")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderListScreenWithItemsPreview() {
    val reminder = ReminderEntity(
        id = 1,
        userId = "preview",
        title = "Call mom",
        dueAt = System.currentTimeMillis() + 3600_000,
        priority = Priority.HIGH,
        type = RecurrenceType.NONE,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val item = ReminderWithChecklist(reminder = reminder, checkedCount = 2, totalCount = 5)
    MaterialTheme {
        ReminderItem(
            reminderWithChecklist = item,
            onClick = {},
            onDelete = {},
            onComplete = {}
        )
    }
}

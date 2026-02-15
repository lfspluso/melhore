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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.Priority
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
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
    onReminderClick: (Long) -> Unit = {}
) {
    val remindersWithChecklist by viewModel.remindersWithChecklist.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val hasActiveFilter = !filter.isAll()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Melhores") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar melhore")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    ReminderSortRow(
                        sortOrder = sortOrder,
                        onSortOrderChange = viewModel::setSortOrder
                    )
                    ReminderFilterRow(
                        filter = filter,
                        categories = categories,
                        onFilterByCategoryIds = viewModel::setFilterByCategoryIds,
                        onFilterPriorities = viewModel::setFilterPriorities,
                        onFilterDateRange = viewModel::setFilterDateRange,
                        onClearFilter = viewModel::clearFilter
                    )
                }
            }
            if (remindersWithChecklist.isEmpty()) {
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
                                text = "Nenhum melhore com esses filtros",
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
                                text = "Nenhum melhore ainda",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Toque em + para adicionar um melhore",
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
                    items(
                        items = remindersWithChecklist,
                        key = { it.reminder.id }
                    ) { item ->
                        ReminderItem(
                            reminderWithChecklist = item,
                            onClick = { onReminderClick(item.reminder.id) },
                            onDelete = { viewModel.deleteReminder(item.reminder.id) }
                        )
                    }
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
                    contentDescription = "Mostrar todos os melhores"
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
    onDelete: () -> Unit
) {
    val reminder = reminderWithChecklist.reminder
    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.getDefault())
    val dueText = dateTimeFormatter.format(
        Instant.ofEpochMilli(reminder.dueAt).atZone(ZoneId.systemDefault())
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                Text(
                    text = dueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (reminder.type != RecurrenceType.NONE) {
                    val recurrenceLabel = when (reminder.type) {
                        RecurrenceType.NONE -> ""
                        RecurrenceType.DAILY -> "Diário"
                        RecurrenceType.WEEKLY -> "Semanal"
                    }
                    Text(
                        text = recurrenceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (reminderWithChecklist.totalCount > 0) {
                    Text(
                        text = "${reminderWithChecklist.checkedCount}/${reminderWithChecklist.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                PriorityBadge(
                    priority = reminder.priority,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir melhore"
                )
            }
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
                Text("Nenhum melhore ainda")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderListScreenWithItemsPreview() {
    val reminder = ReminderEntity(
        id = 1,
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
            onDelete = {}
        )
    }
}

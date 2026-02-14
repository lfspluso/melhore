@file:OptIn(ExperimentalMaterial3Api::class)
package com.melhoreapp.feature.reminders.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ListEntity
import com.melhoreapp.core.database.entity.Priority
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    viewModel: AddReminderViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val title by viewModel.title.collectAsState()
    val dueAt by viewModel.dueAt.collectAsState()
    val categoryId by viewModel.categoryId.collectAsState()
    val listId by viewModel.listId.collectAsState()
    val priority by viewModel.priority.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val scope = rememberCoroutineScope()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AddReminderDatePickerDialog(
            initialMillis = dueAt,
            onConfirm = { millis ->
                viewModel.setDueAt(millis)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showTimePicker) {
        AddReminderTimePickerDialog(
            initialMillis = dueAt,
            onConfirm = { millis ->
                viewModel.setDueAt(millis)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New reminder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::setTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val zoned = Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault())
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dateFormatter.format(zoned))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(timeFormatter.format(zoned))
                }
            }

            CategoryDropdown(
                categories = categories,
                selectedId = categoryId,
                onSelect = viewModel::setCategoryId
            )
            ListDropdown(
                lists = lists,
                selectedId = listId,
                onSelect = viewModel::setListId
            )
            PriorityDropdown(
                selected = priority,
                onSelect = viewModel::setPriority
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        viewModel.save()
                            .onSuccess { onSaved() }
                            .onFailure { /* TODO: show snackbar */ }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save reminder")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedId }?.name ?: "None"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                        onSelect(cat.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListDropdown(
    lists: List<ListEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = lists.find { it.id == selectedId }?.name ?: "None"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("List") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            lists.forEach { list ->
                DropdownMenuItem(
                    text = { Text(list.name) },
                    onClick = {
                        onSelect(list.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityDropdown(
    selected: Priority,
    onSelect: (Priority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Priority") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Priority.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelect(p)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddReminderDatePickerDialog(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val zone = ZoneId.systemDefault()
                        val initial = Instant.ofEpochMilli(initialMillis).atZone(zone)
                        val picked = Instant.ofEpochMilli(dateMillis).atZone(zone)
                        val merged = picked.toLocalDate().atTime(initial.toLocalTime()).atZone(zone)
                        onConfirm(merged.toInstant().toEpochMilli())
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        androidx.compose.material3.DatePicker(state = datePickerState)
    }
}

@Composable
private fun AddReminderTimePickerDialog(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initial = Instant.ofEpochMilli(initialMillis).atZone(zone)
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val date = initial.toLocalDate()
                    val merged = date.atTime(timePickerState.hour, timePickerState.minute).atZone(zone)
                    onConfirm(merged.toInstant().toEpochMilli())
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            androidx.compose.material3.TimePicker(state = timePickerState)
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AddReminderScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add reminder form preview")
        }
    }
}

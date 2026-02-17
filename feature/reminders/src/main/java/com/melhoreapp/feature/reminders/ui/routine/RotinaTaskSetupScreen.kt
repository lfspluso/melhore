@file:OptIn(ExperimentalMaterial3Api::class)
package com.melhoreapp.feature.reminders.ui.routine

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotinaTaskSetupScreen(
    viewModel: RotinaTaskSetupViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val parentReminder by viewModel.parentReminder.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val showSkipDayConfirmation by viewModel.showSkipDayConfirmation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val periodValidationError by viewModel.periodValidationError.collectAsState()
    val periodStart by viewModel.currentPeriodStart.collectAsState()
    val periodEnd by viewModel.currentPeriodEnd.collectAsState()
    val scope = rememberCoroutineScope()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    
    var showDatePickerForIndex by remember { mutableStateOf<Int?>(null) }
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }
    var showFrequencyPickerForIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Tarefas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
            if (isLoading) {
                Text("Carregando...")
            } else {
                parentReminder?.let { reminder ->
                    // Display Rotina reminder info
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormatter.format(
                            Instant.ofEpochMilli(reminder.dueAt)
                                .atZone(ZoneId.systemDefault())
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    periodStart?.let { start ->
                        periodEnd?.let { end ->
                            val zone = ZoneId.systemDefault()
                            val startStr = dateFormatter.format(Instant.ofEpochMilli(start).atZone(zone))
                            val endStr = dateFormatter.format(Instant.ofEpochMilli(end).atZone(zone))
                            Text(
                                text = "Tarefas para: $startStr – $endStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    periodValidationError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Tasks list
                    Text(
                        text = "Tarefas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    tasks.forEachIndexed { index, task ->
                        TaskInputRow(
                            task = task,
                            onTaskChange = { updatedTask ->
                                viewModel.updateTask(index, updatedTask)
                            },
                            onRemove = { viewModel.removeTask(index) },
                            onDateClick = { showDatePickerForIndex = index },
                            onTimeClick = { showTimePickerForIndex = index },
                            onFrequencyClick = { showFrequencyPickerForIndex = index },
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Date picker dialogs (restricted to current period)
                    showDatePickerForIndex?.let { index ->
                        val task = tasks.getOrNull(index)
                        task?.let {
                            TaskDatePickerDialog(
                                initialMillis = it.startTime,
                                minDateMillis = periodStart,
                                maxDateMillis = periodEnd,
                                onConfirm = { millis ->
                                    if (viewModel.updateTask(index, it.copy(startTime = millis))) {
                                        showDatePickerForIndex = null
                                    }
                                },
                                onDismiss = {
                                    viewModel.clearPeriodValidationError()
                                    showDatePickerForIndex = null
                                }
                            )
                        }
                    }
                    
                    // Time picker dialogs (restricted to current period when on boundary dates)
                    showTimePickerForIndex?.let { index ->
                        val task = tasks.getOrNull(index)
                        task?.let {
                            TaskTimePickerDialog(
                                initialMillis = it.startTime,
                                minMillis = periodStart,
                                maxMillis = periodEnd,
                                onConfirm = { millis ->
                                    if (viewModel.updateTask(index, it.copy(startTime = millis))) {
                                        showTimePickerForIndex = null
                                    }
                                },
                                onDismiss = {
                                    viewModel.clearPeriodValidationError()
                                    showTimePickerForIndex = null
                                }
                            )
                        }
                    }
                    
                    // Frequency picker dialogs
                    showFrequencyPickerForIndex?.let { index ->
                        val task = tasks.getOrNull(index)
                        task?.let {
                            TaskFrequencyPickerDialog(
                                initialHours = it.checkupFrequencyHours,
                                onConfirm = { hours ->
                                    viewModel.updateTask(index, it.copy(checkupFrequencyHours = hours))
                                    showFrequencyPickerForIndex = null
                                },
                                onDismiss = { showFrequencyPickerForIndex = null }
                            )
                        }
                    }

                    // Add task button
                    Button(
                        onClick = { viewModel.addTask() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Adicionar tarefa")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showSkipDayConfirmation() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pular dia")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val success = viewModel.saveTasks()
                                        if (success) onSaved()
                                    } catch (e: Exception) {
                                        onSaved()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = tasks.any { it.title.isNotBlank() }
                        ) {
                            Text("Salvar tarefas")
                        }
                    }
                } ?: Text("Rotina não encontrada")
            }
        }
    }

    // Skip day confirmation dialog
    if (showSkipDayConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSkipDayConfirmation() },
            title = { Text("Pular dia?") },
            text = { Text("Esta ação avançará a Rotina para o próximo dia. Deseja continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.skipDay()
                            onSaved()
                        }
                    }
                ) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSkipDayConfirmation() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TaskInputRow(
    task: TaskInput,
    onTaskChange: (TaskInput) -> Unit,
    onRemove: () -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onFrequencyClick: () -> Unit,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = task.title,
                onValueChange = { onTaskChange(task.copy(title = it)) },
                label = { Text("Nome da tarefa") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remover")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onDateClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateFormatter.format(
                        Instant.ofEpochMilli(task.startTime)
                            .atZone(ZoneId.systemDefault())
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Button(
                onClick = { onTimeClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = timeFormatter.format(
                        Instant.ofEpochMilli(task.startTime)
                            .atZone(ZoneId.systemDefault())
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onFrequencyClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${task.checkupFrequencyHours}h",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun TaskDatePickerDialog(
    initialMillis: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initialLocalDate = Instant.ofEpochMilli(initialMillis).atZone(zone).toLocalDate()
    val initialDateMillisForPicker = initialLocalDate
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val selectableDates = remember(minDateMillis, maxDateMillis) {
        object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (minDateMillis == null && maxDateMillis == null) return true
                val localDate = java.time.Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(zone).toLocalDate()
                val minLocal = minDateMillis?.let {
                    java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                }
                val maxLocal = maxDateMillis?.let {
                    java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                }
                if (minLocal != null && localDate.isBefore(minLocal)) return false
                if (maxLocal != null && localDate.isAfter(maxLocal)) return false
                return true
            }
        }
    }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillisForPicker,
        selectableDates = selectableDates
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val initial = Instant.ofEpochMilli(initialMillis).atZone(zone)
                        val pickedLocalDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        val merged = pickedLocalDate.atTime(initial.toLocalTime()).atZone(zone)
                        onConfirm(merged.toInstant().toEpochMilli())
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        androidx.compose.material3.DatePicker(state = datePickerState)
    }
}

@Composable
private fun TaskTimePickerDialog(
    initialMillis: Long,
    minMillis: Long?,
    maxMillis: Long?,
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
                    var merged = date.atTime(timePickerState.hour, timePickerState.minute).atZone(zone).toInstant().toEpochMilli()
                    if (minMillis != null && maxMillis != null) {
                        merged = merged.coerceIn(minMillis, maxMillis)
                    }
                    onConfirm(merged)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            androidx.compose.material3.TimePicker(state = timePickerState)
        }
    )
}

@Composable
private fun TaskFrequencyPickerDialog(
    initialHours: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHours by remember { mutableStateOf(initialHours) }
    val frequencyOptions = listOf(1, 2, 3, 4, 6, 8, 12, 24)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frequência de verificação") },
        text = {
            Column {
                Text("Selecione a frequência (em horas):")
                Spacer(modifier = Modifier.height(8.dp))
                frequencyOptions.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { hours ->
                            Button(
                                onClick = { selectedHours = hours },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${hours}h")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedHours) }) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

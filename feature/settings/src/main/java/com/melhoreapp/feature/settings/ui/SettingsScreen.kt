package com.melhoreapp.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val defaultSnoozeMs by viewModel.defaultSnoozeDurationMs.collectAsState()
    val autoDeleteCompleted by viewModel.autoDeleteCompletedReminders.collectAsState()
    val enabledSnoozeOptions by viewModel.enabledSnoozeOptions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Notificações",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Duração padrão do adiamento",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SNOOZE_OPTIONS.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = defaultSnoozeMs == option.durationMs,
                        onClick = { viewModel.setDefaultSnoozeDuration(option.durationMs) }
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Text(
                text = "Opções de adiamento",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                text = "Escolha até 3 opções que aparecem nas notificações",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // "5 minutos" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("5_min"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("5_min", it) },
                    enabled = enabledSnoozeOptions.contains("5_min") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "5 minutos",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // "15 minutos" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("15_min"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("15_min", it) },
                    enabled = enabledSnoozeOptions.contains("15_min") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "15 minutos",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // "30 minutos" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("30_min"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("30_min", it) },
                    enabled = enabledSnoozeOptions.contains("30_min") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "30 minutos",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // "1 hora" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("1_hour"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("1_hour", it) },
                    enabled = enabledSnoozeOptions.contains("1_hour") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "1 hora",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // "2 horas" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("2_hours"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("2_hours", it) },
                    enabled = enabledSnoozeOptions.contains("2_hours") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "2 horas",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // "1 dia" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("1_day"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("1_day", it) },
                    enabled = enabledSnoozeOptions.contains("1_day") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "1 dia",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            // "Personalizar" option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabledSnoozeOptions.contains("personalizar"),
                    onCheckedChange = { viewModel.setSnoozeOptionEnabled("personalizar", it) },
                    enabled = enabledSnoozeOptions.contains("personalizar") || enabledSnoozeOptions.size < 3
                )
                Text(
                    text = "Personalizar",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Lembretes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Excluir automaticamente após conclusão",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Remove automaticamente melhores marcados como concluídos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = autoDeleteCompleted,
                    onCheckedChange = viewModel::setAutoDeleteCompletedReminders
                )
            }
            Text(
                text = "Conta",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Button(
                onClick = viewModel::signOut
            ) {
                Text("Sair")
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}

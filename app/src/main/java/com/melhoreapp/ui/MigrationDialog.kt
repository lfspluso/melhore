package com.melhoreapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melhoreapp.core.sync.MigrationStrategy

@Composable
fun MigrationDialog(
    authGateViewModel: AuthGateViewModel,
    snackbarHostState: SnackbarHostState
) {
    val showMigrationDialog by authGateViewModel.showMigrationDialog.collectAsStateWithLifecycle(initialValue = false)
    val migrationInProgress by authGateViewModel.migrationInProgress.collectAsStateWithLifecycle(initialValue = false)
    val migrationError by authGateViewModel.migrationError.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(migrationError) {
        val msg = migrationError ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = "Tentar novamente"
        )
        if (result == SnackbarResult.ActionPerformed) {
            authGateViewModel.retryMigration()
        }
        authGateViewModel.clearMigrationError()
    }

    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Dados neste aparelho") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (migrationInProgress) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Sincronizando…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Você tem dados só neste aparelho. O que deseja fazer?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { authGateViewModel.onMigrationStrategyChosen(MigrationStrategy.UploadLocal) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fazer upload para esta conta")
                        }
                        TextButton(
                            onClick = { authGateViewModel.onMigrationStrategyChosen(MigrationStrategy.MergeWithCloud) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mesclar com dados da nuvem")
                        }
                        OutlinedButton(
                            onClick = { authGateViewModel.onMigrationStrategyChosen(MigrationStrategy.StartFresh) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Começar do zero")
                        }
                    }
                }
            },
            confirmButton = { }
        )
    }
}

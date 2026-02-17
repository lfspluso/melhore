package com.melhoreapp.feature.auth.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.melhoreapp.core.common.Result

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val signInState by viewModel.signInState.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.resultCode, result.data)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Melhore",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Entre com sua conta Google para continuar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (signInState) {
            is Result.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { viewModel.useLocalOnly() }) {
                    Text("Continuar sem entrar")
                }
            }
            is Result.Error -> {
                Text(
                    text = (signInState as Result.Error).exception.message ?: "Erro ao entrar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        viewModel.clearSignInState()
                        if (activity != null) {
                            signInLauncher.launch(viewModel.getSignInIntent(activity))
                        }
                    }
                ) {
                    Text("Tentar novamente")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.useLocalOnly() }) {
                    Text("Continuar sem entrar")
                }
            }
            else -> {
                Button(
                    onClick = {
                        if (activity != null) {
                            signInLauncher.launch(viewModel.getSignInIntent(activity))
                        }
                    }
                ) {
                    Text("Entrar com Google")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.useLocalOnly() }) {
                    Text("Continuar sem entrar")
                }
            }
        }
    }
}

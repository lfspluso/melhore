package com.melhoreapp.feature.integrations.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val DEFAULT_SHARE_MESSAGE = "Lembrete do MelhoreApp"

private data class IntegrationItem(
    val name: String,
    val shareContentDescription: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun launchShare(text: String = DEFAULT_SHARE_MESSAGE) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar para…"))
    }

    val items = listOf(
        IntegrationItem("Telegram", "Compartilhar no Telegram"),
        IntegrationItem("Slack", "Compartilhar no Slack"),
        IntegrationItem("WhatsApp", "Compartilhar no WhatsApp")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integrações") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Envie lembretes ou mensagens para seus apps favoritos usando o compartilhamento do sistema.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            items.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { launchShare() },
                            modifier = Modifier.semantics {
                                contentDescription = item.shareContentDescription
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null
                                )
                                Text("Enviar para…")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun IntegrationsScreenPreview() {
    MaterialTheme {
        IntegrationsScreen()
    }
}

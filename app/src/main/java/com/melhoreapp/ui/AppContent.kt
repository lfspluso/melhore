package com.melhoreapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melhoreapp.feature.auth.ui.LoginScreen
import com.melhoreapp.feature.auth.ui.LoginViewModel
import com.melhoreapp.ui.navigation.MelhoreNavHost
import com.melhoreapp.ui.theme.MelhoreAppTheme

@Composable
fun AppContent(
    initialRoute: String?,
    authGateViewModel: AuthGateViewModel = hiltViewModel()
) {
    val currentUser by authGateViewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)
    val showMigrationDialog by authGateViewModel.showMigrationDialog.collectAsStateWithLifecycle(initialValue = false)

    if (currentUser == null) {
        val loginViewModel: LoginViewModel = hiltViewModel()
        LoginScreen(viewModel = loginViewModel)
    } else if (showMigrationDialog) {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MigrationDialog(
                    authGateViewModel = authGateViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MelhoreNavHost(initialRoute = initialRoute)
        }
    }
}

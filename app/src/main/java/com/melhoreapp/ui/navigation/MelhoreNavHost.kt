package com.melhoreapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.melhoreapp.feature.reminders.ui.addedit.AddReminderScreen
import com.melhoreapp.feature.reminders.ui.addedit.AddReminderViewModel
import com.melhoreapp.feature.reminders.ui.list.ReminderListScreen
import com.melhoreapp.feature.reminders.ui.list.ReminderListViewModel

@Composable
fun MelhoreNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "reminders"
    ) {
        composable("reminders") {
            val listViewModel: ReminderListViewModel = hiltViewModel()
            ReminderListScreen(
                viewModel = listViewModel,
                onAddClick = { navController.navigate("reminders/add") }
            )
        }
        composable("reminders/add") {
            val addViewModel: AddReminderViewModel = hiltViewModel()
            AddReminderScreen(
                viewModel = addViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}

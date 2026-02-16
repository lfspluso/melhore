package com.melhoreapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.melhoreapp.feature.categories.ui.addedit.AddEditCategoryScreen
import com.melhoreapp.feature.categories.ui.list.CategoryListScreen
import com.melhoreapp.feature.categories.ui.list.CategoryListViewModel
import com.melhoreapp.feature.reminders.ui.addedit.AddReminderScreen
import com.melhoreapp.feature.reminders.ui.addedit.AddReminderViewModel
import com.melhoreapp.feature.reminders.ui.list.ReminderListScreen
import com.melhoreapp.feature.reminders.ui.list.ReminderListViewModel
import com.melhoreapp.feature.reminders.ui.templates.TemplatesComingSoonScreen
import com.melhoreapp.feature.reminders.ui.routine.RotinaTaskSetupScreen
import com.melhoreapp.feature.reminders.ui.routine.RotinaTaskSetupViewModel
import com.melhoreapp.feature.integrations.ui.IntegrationsScreen
import com.melhoreapp.feature.settings.ui.SettingsScreen

private sealed class Tab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Reminders : Tab("reminders", "Melhores", Icons.Default.Notifications)
    data object Categories : Tab("categories", "Tags", Icons.Default.Star)
    data object Integrations : Tab("integrations", "Integrações", Icons.Default.Share)
    data object Settings : Tab("settings", "Configurações", Icons.Default.Settings)
}

@Composable
fun MelhoreNavHost(initialRoute: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle deep link navigation
    androidx.compose.runtime.LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            navController.navigate(initialRoute) {
                popUpTo(0) { inclusive = false }
            }
        }
    }

    val currentTab = when {
        currentRoute?.startsWith("reminders") == true -> Tab.Reminders
        currentRoute?.startsWith("categories") == true -> Tab.Categories
        currentRoute?.startsWith("integrations") == true -> Tab.Integrations
        currentRoute?.startsWith("settings") == true -> Tab.Settings
        else -> Tab.Reminders
    }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Tab.Reminders, Tab.Categories, Tab.Integrations, Tab.Settings).forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Tab.Reminders.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Tab.Reminders.route) {
                val listViewModel: ReminderListViewModel = hiltViewModel()
                ReminderListScreen(
                    viewModel = listViewModel,
                    onAddClick = { navController.navigate("reminders/add") },
                    onReminderClick = { id -> navController.navigate("reminders/edit/$id") },
                    onTemplatesClick = { navController.navigate("reminders/templates") }
                )
            }
            composable("reminders/templates") {
                TemplatesComingSoonScreen(onBack = { navController.popBackStack() })
            }
            composable("reminders/add") {
                val addViewModel: AddReminderViewModel = hiltViewModel()
                AddReminderScreen(
                    viewModel = addViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "reminders/edit/{reminderId}",
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) {
                val editViewModel: AddReminderViewModel = hiltViewModel()
                AddReminderScreen(
                    viewModel = editViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "reminders/routine/{reminderId}/setup",
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) {
                val setupViewModel: RotinaTaskSetupViewModel = hiltViewModel()
                val navigateToRemindersHome: () -> Unit = {
                    navController.navigate(Tab.Reminders.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
                RotinaTaskSetupScreen(
                    viewModel = setupViewModel,
                    onBack = navigateToRemindersHome,
                    onSaved = navigateToRemindersHome
                )
            }
            composable(Tab.Categories.route) {
                val listViewModel: CategoryListViewModel = hiltViewModel()
                CategoryListScreen(
                    viewModel = listViewModel,
                    onAddClick = { navController.navigate("categories/add") },
                    onCategoryClick = { id -> navController.navigate("categories/edit/$id") }
                )
            }
            composable("categories/add") {
                AddEditCategoryScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "categories/edit/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
            ) {
                AddEditCategoryScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Tab.Integrations.route) {
                IntegrationsScreen()
            }
            composable(Tab.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

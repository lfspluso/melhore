package com.melhoreapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
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
import com.melhoreapp.feature.lists.ui.addedit.AddEditListScreen
import com.melhoreapp.feature.lists.ui.list.ListListScreen
import com.melhoreapp.feature.lists.ui.list.ListListViewModel
import com.melhoreapp.feature.reminders.ui.addedit.AddReminderScreen
import com.melhoreapp.feature.reminders.ui.addedit.AddReminderViewModel
import com.melhoreapp.feature.reminders.ui.list.ReminderListScreen
import com.melhoreapp.feature.reminders.ui.list.ReminderListViewModel
import com.melhoreapp.feature.settings.ui.SettingsScreen

private sealed class Tab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Reminders : Tab("reminders", "Reminders", Icons.Default.Notifications)
    data object Categories : Tab("categories", "Categories", Icons.Default.Star)
    data object Lists : Tab("lists", "Lists", Icons.Default.List)
    data object Settings : Tab("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MelhoreNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTab = when {
        currentRoute?.startsWith("reminders") == true -> Tab.Reminders
        currentRoute?.startsWith("categories") == true -> Tab.Categories
        currentRoute?.startsWith("lists") == true -> Tab.Lists
        currentRoute?.startsWith("settings") == true -> Tab.Settings
        else -> Tab.Reminders
    }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Tab.Reminders, Tab.Categories, Tab.Lists, Tab.Settings).forEach { tab ->
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
                    onReminderClick = { id -> navController.navigate("reminders/edit/$id") }
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
            composable(Tab.Lists.route) {
                val listViewModel: ListListViewModel = hiltViewModel()
                ListListScreen(
                    viewModel = listViewModel,
                    onAddClick = { navController.navigate("lists/add") },
                    onListClick = { id -> navController.navigate("lists/edit/$id") }
                )
            }
            composable("lists/add") {
                AddEditListScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "lists/edit/{listId}",
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) {
                AddEditListScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Tab.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

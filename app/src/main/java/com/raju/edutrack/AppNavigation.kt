package com.raju.edutrack

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raju.edutrack.screens.BatchScreen
import com.raju.edutrack.screens.HomeScreen
import com.raju.edutrack.screens.OverviewScreen
import com.raju.edutrack.screens.SettingsScreen
import com.raju.edutrack.screens.StudentsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Students,
        BottomNavItem.Batches,
        BottomNavItem.Settings
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by
                navController.currentBackStackEntryAsState()
                val currentRoute =
                    navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected =
                            currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route)
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(item.title)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination =
                BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                BottomNavItem.Home.route
            ) {
                HomeScreen(navController)
            }
            composable(
                BottomNavItem.Students.route
            ) {
                StudentsScreen()
            }
            composable(
                BottomNavItem.Batches.route
            ) {
                BatchScreen()
            }
            composable(
                BottomNavItem.Settings.route
            ) {
                SettingsScreen()
            }
            composable(
                route = "overview/{title}"
            ) { backStackEntry ->
                val title =
                    backStackEntry.arguments
                        ?.getString("title") ?: ""
                OverviewScreen(title)
            }
        }
    }
}
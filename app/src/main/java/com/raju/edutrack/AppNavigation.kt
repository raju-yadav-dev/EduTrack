package com.raju.edutrack

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.raju.edutrack.screens.BatchScreen
import com.raju.edutrack.screens.HomeScreen
import com.raju.edutrack.screens.OverviewScreen
import com.raju.edutrack.screens.SettingsScreen
import com.raju.edutrack.screens.StudentsScreen
import kotlinx.coroutines.launch

private const val MAIN_ROUTE = "main"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Students,
        BottomNavItem.Batches,
        BottomNavItem.Settings
    )
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )
    fun openPage(index: Int) {
        scope.launch {
            if (navController.currentDestination?.route != MAIN_ROUTE) {
                navController.popBackStack(
                    route = MAIN_ROUTE,
                    inclusive = false
                )
            }
            pagerState.animateScrollToPage(index)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected =
                            pagerState.currentPage == index,
                        onClick = {
                            openPage(index)
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
            startDestination = MAIN_ROUTE,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(MAIN_ROUTE) {
                HorizontalPager(
                    state = pagerState
                ) { page ->
                    when (items[page]) {
                        BottomNavItem.Home ->
                            HomeScreen(
                                navController = navController,
                                onOpenStudents = {
                                    openPage(items.indexOf(BottomNavItem.Students))
                                },
                                onOpenBatches = {
                                    openPage(items.indexOf(BottomNavItem.Batches))
                                }
                            )
                        BottomNavItem.Students ->
                            StudentsScreen()
                        BottomNavItem.Batches ->
                            BatchScreen()
                        BottomNavItem.Settings ->
                            SettingsScreen()
                        BottomNavItem.Overview ->
                            HomeScreen(
                                navController = navController,
                                onOpenStudents = {
                                    openPage(items.indexOf(BottomNavItem.Students))
                                },
                                onOpenBatches = {
                                    openPage(items.indexOf(BottomNavItem.Batches))
                                }
                            )
                    }
                }
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

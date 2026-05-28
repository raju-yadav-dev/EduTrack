package com.raju.edutrack

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raju.edutrack.screens.BatchScreen
import com.raju.edutrack.screens.HomeScreen
import com.raju.edutrack.screens.OverviewScreen
import com.raju.edutrack.screens.SettingsScreen
import com.raju.edutrack.screens.StudentsScreen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Students,
        BottomNavItem.Batches,
        BottomNavItem.Settings
    )
    val navBackStackEntry by
    navController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.route
    val currentIndex =
        items.indexOfFirst { item -> item.route == currentRoute }
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val scope = rememberCoroutineScope()
    fun navigateToIndex(index: Int) {
        if (index in items.indices && index != currentIndex) {
            navController.navigate(items[index].route) {
                launchSingleTop = true
            }
        }
    }
    val dragOffset = remember { Animatable(0f) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected =
                            currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                            }
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
        BoxWithConstraints(
            modifier = Modifier.padding(paddingValues)
        ) {
            val contentWidth = with(LocalDensity.current) { maxWidth.toPx() }
            val maxDrag = contentWidth * 0.85f
            NavHost(
                navController = navController,
                startDestination =
                    BottomNavItem.Home.route,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dragOffset.value
                    }
                    .pointerInput(currentRoute, contentWidth) {
                        if (currentIndex < 0) {
                            return@pointerInput
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var totalDrag = 0f
                            val drag = awaitHorizontalTouchSlopOrCancellation(
                                pointerId = down.id
                            ) { change, over ->
                                totalDrag = (totalDrag + over)
                                    .coerceIn(-maxDrag, maxDrag)
                                scope.launch {
                                    dragOffset.snapTo(totalDrag)
                                }
                                change.consume()
                            }
                            if (drag == null) {
                                return@awaitEachGesture
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { pointer ->
                                    pointer.id == down.id
                                } ?: break
                                if (!change.pressed) {
                                    break
                                }
                                val deltaX =
                                    change.position.x -
                                        change.previousPosition.x
                                totalDrag = (totalDrag + deltaX)
                                    .coerceIn(-maxDrag, maxDrag)
                                scope.launch {
                                    dragOffset.snapTo(totalDrag)
                                }
                                change.consume()
                            }

                            val direction = when {
                                totalDrag > swipeThreshold -> 1
                                totalDrag < -swipeThreshold -> -1
                                else -> 0
                            }

                            if (direction == 0) {
                                scope.launch {
                                    dragOffset.stop()
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                                return@awaitEachGesture
                            }

                            scope.launch {
                                dragOffset.stop()
                                dragOffset.animateTo(
                                    targetValue = direction * maxDrag,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                navigateToIndex(currentIndex - direction)
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
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
}

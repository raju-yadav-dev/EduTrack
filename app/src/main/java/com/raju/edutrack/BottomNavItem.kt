package com.raju.edutrack

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector


sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    object Home : BottomNavItem(
        "home",
        "Home",
        Icons.Default.Home
    )

    object Students : BottomNavItem(
        "students",
        "Students",
        Icons.Default.School
    )

    object Batches : BottomNavItem(
        "batches",
        "Batches",
        Icons.Default.Groups
    )

    object Settings : BottomNavItem(
        "settings",
        "Settings",
        Icons.Default.Settings
    )

    object Overview : BottomNavItem(
        "overview",
        "Overview",
        Icons.Default.List
    )
}
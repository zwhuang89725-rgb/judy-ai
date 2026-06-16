package com.pixelbuddy.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 一级路由定义
 */
sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Chat : Screen(
        route = "chat",
        label = "CHAT",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    )

    data object Voice : Screen(
        route = "voice",
        label = "VOICE",
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.Mic
    )

    data object Story : Screen(
        route = "story",
        label = "STORY",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )

    data object Game : Screen(
        route = "game",
        label = "GAME",
        selectedIcon = Icons.Filled.VideogameAsset,
        unselectedIcon = Icons.Outlined.VideogameAsset
    )

    data object Settings : Screen(
        route = "settings",
        label = "CONFIG",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavScreens = listOf(
    Screen.Chat,
    Screen.Voice,
    Screen.Story,
    Screen.Game,
    Screen.Settings
)

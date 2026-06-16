package com.pixelbuddy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelbuddy.app.presentation.chat.ChatScreen
import com.pixelbuddy.app.presentation.game.GameScreen
import com.pixelbuddy.app.presentation.navigation.Screen
import com.pixelbuddy.app.presentation.navigation.bottomNavScreens
import com.pixelbuddy.app.presentation.onboarding.OnboardingScreen
import com.pixelbuddy.app.presentation.settings.SettingsScreen
import com.pixelbuddy.app.presentation.story.StoryScreen
import com.pixelbuddy.app.presentation.theme.*
import com.pixelbuddy.app.presentation.voice.VoiceScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferenceStore: ThemePreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by themePreferenceStore.themeFlow
                .collectAsState(initial = AppTheme.PIXEL_BUDDY)

            PixelBuddyTheme(theme = theme) {
                PixelBuddyMain()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelBuddyMain() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 始终先展示 onboarding，用户可以跳过或完成设置
    val startDest = "onboarding"

    // 判断是否在引导页（不显示 BottomBar + TopBar）
    val isOnboarding = currentRoute == "onboarding"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isOnboarding) {
                TopAppBar(
                    title = {
                        Text(
                            text = "PIXEL BUDDY",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = PixelNeonGreen
                    )
                )
            }
        },
        bottomBar = {
            if (!isOnboarding) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PixelNeonGreen
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PixelNeonGreen,
                                selectedTextColor = PixelNeonGreen,
                                unselectedIconColor = PixelTextDim,
                                unselectedTextColor = PixelTextDim,
                                indicatorColor = PixelNeonGreen.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Voice.route) { VoiceScreen() }
            composable(Screen.Story.route) { StoryScreen() }
            composable(Screen.Game.route) { GameScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

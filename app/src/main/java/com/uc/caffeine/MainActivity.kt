package com.uc.caffeine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
// Navigation 3 Imports
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
// Your screens
import com.uc.caffeine.ui.screens.AddScreen
import com.uc.caffeine.ui.screens.HomeScreen
import com.uc.caffeine.ui.screens.SettingsScreen
import com.uc.caffeine.ui.theme.CaffeineTheme
// Transitions
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaffeineTheme {
                CaffeineApp()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) : NavKey {
    HOME("Home", R.drawable.ic_home),
    ADD("Add", R.drawable.ic_add),
    SETTINGS("Settings", R.drawable.ic_account),
}

@PreviewScreenSizes
@Composable
fun CaffeineApp() {
    val backStack = rememberNavBackStack(AppDestinations.HOME)
    val currentDestination = backStack.lastOrNull() ?: AppDestinations.HOME

    Scaffold(
        bottomBar = {
            // Floating pill-shaped bottom bar, just like Tomato!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape) // Makes it pill-shaped
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppDestinations.entries.forEach { destination ->
                        val isSelected = currentDestination == destination

                        // Tab Item
                        IconButton(
                            onClick = {
                                if (currentDestination != destination) {
                                    // 1. If we aren't on HOME, pop the current tab
                                    if (currentDestination != AppDestinations.HOME) {
                                        backStack.removeLastOrNull()
                                    }
                                    // 2. If the new tab isn't HOME, add it.
                                    // This guarantees our backstack is always either [HOME] or [HOME, TAB]
                                    if (destination != AppDestinations.HOME) {
                                        backStack.add(destination)
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = destination.label
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding()
            ),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator()
            ),
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            popTransitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            // THIS IS THE SECRET SAUCE FROM TOMATO!
            predictivePopTransitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            entryProvider = entryProvider {
                entry                  { key ->
                    when (key) {
                        AppDestinations.HOME -> HomeScreen()
                        AppDestinations.ADD -> AddScreen()
                        AppDestinations.SETTINGS -> SettingsScreen()
                    }
                }
            }
        )
    }
}

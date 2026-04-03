package com.uc.caffeine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.ui.screens.AddScreen
import com.uc.caffeine.ui.screens.HomeScreen
import com.uc.caffeine.ui.screens.SettingsScreen
import com.uc.caffeine.ui.theme.CaffeineTheme

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
) {
    HOME("Home", R.drawable.ic_home),
    ADD("Add", R.drawable.ic_add),
    SETTINGS("Settings", R.drawable.ic_account),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@PreviewScreenSizes
@Composable
fun CaffeineApp() {
    val backStack = rememberNavBackStack(AppDestinations.HOME)
    val systemBarsInsets = WindowInsets.systemBars.asPaddingValues()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier.padding(
                        bottom = systemBarsInsets.calculateBottomPadding() + 16.dp
                    )
                ) {
                    AppDestinations.entries.forEach { destination ->
                        val selected by remember {
                            derivedStateOf { backStack.lastOrNull() == destination }
                        }
                        ToggleButton(
                            checked = selected,
                            onCheckedChange = {
                                if (!selected) {
                                    if (destination == AppDestinations.HOME) {
                                        if (backStack.size > 1) backStack.removeAt(1)
                                    } else {
                                        if (backStack.size < 2) backStack.add(destination)
                                        else backStack[1] = destination
                                    }
                                }
                            },
                            shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape)
                        ) {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = destination.label
                            )
                        }
                    }
                }
            }
        }
    ) { contentPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            popTransitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            predictivePopTransitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            entryProvider = entryProvider {
                entry<AppDestinations> {
                    when (key) {
                        AppDestinations.HOME -> HomeScreen()
                        AppDestinations.ADD -> AddScreen()
                        AppDestinations.SETTINGS -> SettingsScreen()
                    }
                }
            },
            modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}

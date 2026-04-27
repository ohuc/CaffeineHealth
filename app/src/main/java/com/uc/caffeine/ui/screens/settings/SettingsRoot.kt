package com.uc.caffeine.ui.screens.settings

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel

private enum class SettingsDestination : NavKey {
    Main,
    CaffeineProfile,
    Appearance,
    Language,
    DateTime,
    HealthConnect,
    MyData,
}

@Composable
fun SettingsScreen(
    onRedoOnboarding: () -> Unit,
    viewModel: CaffeineViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val nestedBackStack = rememberNavBackStack(SettingsDestination.Main)

    NavDisplay(
        backStack = nestedBackStack,
        onBack = {
            if (nestedBackStack.size > 1) {
                nestedBackStack.removeLastOrNull()
            }
        },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
        ),
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                (slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut())
        },
        popTransitionSpec = {
            (slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            (slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<SettingsDestination> { destination ->
                when (destination) {
                    SettingsDestination.Main -> SettingsListScreen(
                        onCaffeineProfileClick = {
                            nestedBackStack.add(SettingsDestination.CaffeineProfile)
                        },
                        onAppearanceClick = {
                            nestedBackStack.add(SettingsDestination.Appearance)
                        },
                        onLanguageClick = {
                            nestedBackStack.add(SettingsDestination.Language)
                        },
                        onDateTimeClick = {
                            nestedBackStack.add(SettingsDestination.DateTime)
                        },
                        onHealthConnectClick = {
                            nestedBackStack.add(SettingsDestination.HealthConnect)
                        },
                        onMyDataClick = {
                            nestedBackStack.add(SettingsDestination.MyData)
                        },
                    )

                    SettingsDestination.CaffeineProfile -> CaffeineProfileSettingsScreen(
                        userSettings = userSettings,
                        viewModel = viewModel,
                        onBack = { nestedBackStack.removeLastOrNull() },
                        onRedoOnboarding = onRedoOnboarding,
                    )

                    SettingsDestination.Appearance -> AppearanceSettingsScreen(
                        userSettings = userSettings,
                        onThemeModeChange = viewModel::updateThemeMode,
                        onDynamicColorChange = viewModel::updateDynamicColor,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Language -> LanguageSettingsScreen(
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.DateTime -> DateTimeSettingsScreen(
                        userSettings = userSettings,
                        onUse24HourClockChange = viewModel::updateUse24HourClock,
                        onDateFormatChange = viewModel::updateDateFormat,
                        onTimeZoneIdChange = viewModel::updateTimeZoneId,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.HealthConnect -> HealthConnectSettingsScreen(
                        userSettings = userSettings,
                        healthConnectManager = viewModel.healthConnectManager,
                        onHealthConnectToggle = viewModel::updateHealthConnectEnabled,
                        onHcSleepEnabledToggle = viewModel::updateHcSleepEnabled,
                        onHcSleepModeChange = viewModel::updateHcSleepMode,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.MyData -> MyDataSettingsScreen(
                        onBack = { nestedBackStack.removeLastOrNull() },
                        viewModel = viewModel,
                    )
                }
            }
        },
    )
}

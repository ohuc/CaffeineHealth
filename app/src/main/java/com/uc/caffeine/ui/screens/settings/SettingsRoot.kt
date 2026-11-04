package com.uc.caffeine.ui.screens.settings

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import com.uc.caffeine.ui.components.LocalNestedDetailBack
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.AdaptivePaneContent
import com.uc.caffeine.ui.components.AdaptivePanePlaceholder
import com.uc.caffeine.ui.components.caffeinePaneDirective
import com.uc.caffeine.util.openPane
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.ui.viewmodel.SettingsDeepLink
import androidx.compose.runtime.LaunchedEffect
import com.uc.caffeine.monetization.ProvidePlusSettings
import com.uc.caffeine.monetization.plusSettingState
import com.uc.caffeine.monetization.ProFeature

private enum class SettingsDestination : NavKey {
    Main,
    CaffeineProfile,
    WeeklySleepRota,
    Duration,
    Appearance,
    Language,
    DateTime,
    HealthConnect,
    MyData,
    Notifications,
    Automation,
    LookAndFeel,
    Connections,
    Developer,
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsScreen(
    onRedoOnboarding: () -> Unit,
    viewModel: CaffeineViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val userSettings by viewModel.savedUserSettings.collectAsStateWithLifecycle()
    val drinkPresets by viewModel.drinkPresets.collectAsStateWithLifecycle()
    val nestedBackStack = rememberNavBackStack(SettingsDestination.Main)
    val paneStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = caffeinePaneDirective(), backNavigationBehavior = BackNavigationBehavior.PopLatest,
    )
    val deepLink by viewModel.settingsDeepLink.collectAsStateWithLifecycle()
    LaunchedEffect(deepLink) {
        when (deepLink) {
            SettingsDeepLink.HealthConnect -> {
                // Replace the open category, like a tap on Connections, then open the page so
                // Back returns to Connections and then to the list.
                nestedBackStack.openPane(SettingsDestination.Connections)
                nestedBackStack.add(SettingsDestination.HealthConnect)
                viewModel.consumeSettingsDeepLink()
            }
            null -> Unit
        }
    }

    ProvidePlusSettings(viewModel.distributionServices.featureAccess, { viewModel.openPlus() }, { viewModel.requestFeature(it) }) {
    NavDisplay(
        backStack = nestedBackStack,
        onBack = {
            if (nestedBackStack.size > 1) {
                nestedBackStack.removeLastOrNull()
            }
        },
        modifier = modifier,
        sceneStrategies = listOf(paneStrategy),
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
            SettingsDestination.entries.forEach { destination ->
            entry(key = destination, metadata = if (destination == SettingsDestination.Main) {
                ListDetailSceneStrategy.listPane(detailPlaceholder = {
                    AdaptivePanePlaceholder(stringResource(R.string.settings_title),
                        stringResource(R.string.adaptive_settings_placeholder))
                }) + ListDetailSceneStrategy.preferredPaneSize(width = 360.dp)
            } else ListDetailSceneStrategy.detailPane()) {
                AdaptivePaneContent {
                CompositionLocalProvider(LocalNestedDetailBack provides (nestedBackStack.indexOf(destination) > 1)) {
                when (destination) {
                    SettingsDestination.Main -> SettingsListScreen(
                        onCaffeineProfileClick = { nestedBackStack.openPane(SettingsDestination.CaffeineProfile) },
                        onNotificationsClick = { nestedBackStack.openPane(SettingsDestination.Notifications) },
                        onLookAndFeelClick = { nestedBackStack.openPane(SettingsDestination.LookAndFeel) },
                        onConnectionsClick = { nestedBackStack.openPane(SettingsDestination.Connections) },
                        onLanguageClick = { nestedBackStack.openPane(SettingsDestination.Language) },
                        onWhatsNewSeen = viewModel::markWhatsNewSeen,
                        distributionUi = viewModel.distributionServices.distributionUi,
                        onDistributionClick = { viewModel.openPlus() },
                        onDeveloperClick = { nestedBackStack.openPane(SettingsDestination.Developer) },
                        selectedCategory = when (nestedBackStack.getOrNull(1)) {
                            SettingsDestination.CaffeineProfile -> "profile"
                            SettingsDestination.Notifications -> "notifications"
                            SettingsDestination.LookAndFeel -> "appearance"
                            SettingsDestination.Connections -> "connections"
                            SettingsDestination.Language -> "language"
                            else -> null
                        },
                    )

                    SettingsDestination.LookAndFeel -> LookAndFeelSettingsScreen(
                        onAppearanceClick = { nestedBackStack.add(SettingsDestination.Appearance) },
                        onDateTimeClick = { nestedBackStack.add(SettingsDestination.DateTime) },
                        distributionUi = viewModel.distributionServices.distributionUi,
                        onOpenPlus = { viewModel.openPlus() },
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Connections -> ConnectionsSettingsScreen(
                        userSettings = userSettings,
                        onHealthConnectClick = {
                            if (viewModel.distributionServices.featureAccess.plusSettingState(ProFeature.HealthConnect,
                                    userSettings.healthConnectEnabled || userSettings.hcSleepEnabled).canOpenSettings) {
                                nestedBackStack.add(SettingsDestination.HealthConnect)
                            } else viewModel.requestFeature(com.uc.caffeine.monetization.ProFeature.HealthConnect) {
                                nestedBackStack.add(SettingsDestination.HealthConnect)
                            }
                        },
                        onAutomationClick = {
                            if (viewModel.distributionServices.featureAccess.plusSettingState(ProFeature.Automation,
                                    userSettings.automationEnabled).canOpenSettings) {
                                nestedBackStack.add(SettingsDestination.Automation)
                            } else viewModel.requestFeature(com.uc.caffeine.monetization.ProFeature.Automation) {
                                nestedBackStack.add(SettingsDestination.Automation)
                            }
                        },
                        onMyDataClick = { nestedBackStack.add(SettingsDestination.MyData) },
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Developer -> DeveloperSettingsScreen(
                        distributionUi = viewModel.distributionServices.distributionUi,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.CaffeineProfile -> CaffeineProfileSettingsScreen(
                        userSettings = userSettings,
                        viewModel = viewModel,
                        onBack = { nestedBackStack.removeLastOrNull() },
                        onRedoOnboarding = onRedoOnboarding,
                        onWeeklySleepRotaClick = { nestedBackStack.add(SettingsDestination.WeeklySleepRota) },
                        onTimeToFinishClick = { nestedBackStack.add(SettingsDestination.Duration) },
                    )

                    SettingsDestination.WeeklySleepRota -> WeeklySleepRotaSettingsScreen(
                        userSettings = userSettings,
                        onRotaEnabledChange = viewModel::updateWeeklySleepRotaEnabled,
                        onDayTimeChange = viewModel::setWeeklySleepRotaEntry,
                        onDayTimeClear = viewModel::clearWeeklySleepRotaEntry,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Duration -> DurationSettingsScreen(
                        userSettings = userSettings,
                        drinkPresets = drinkPresets,
                        onDefaultDurationChange = viewModel::updateDefaultDurationMinutes,
                        onCategoryDurationChange = viewModel::setCategoryDurationMinutes,
                        onDrinkDurationChange = viewModel::setDrinkDurationMinutes,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Appearance -> AppearanceSettingsScreen(
                        userSettings = userSettings,
                        onThemeModeChange = viewModel::updateThemeMode,
                        onExpressiveBlurChange = viewModel::updateExpressiveBlur,
                        onHomeViewModeChange = viewModel::updateHomeViewMode,
                        onStickyHomeChartChange = viewModel::updateStickyHomeChart,
                        onColorPaletteChange = viewModel::updateColorPalette,
                        onRecentServingsCountChange = viewModel::updateRecentServingsCount,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Language -> LanguageSettingsScreen(
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.DateTime -> DateTimeSettingsScreen(
                        userSettings = userSettings,
                        onUse24HourClockChange = viewModel::updateUse24HourClock,
                        onDateFormatChange = viewModel::updateDateFormat,
                        onUseSystemTimeZoneChange = viewModel::updateUseSystemTimeZone,
                        onTimeZoneIdChange = viewModel::updateTimeZoneId,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.HealthConnect -> HealthConnectSettingsScreen(
                        userSettings = userSettings,
                        healthConnectManager = viewModel.healthConnectManager,
                        onHealthConnectToggle = viewModel::updateHealthConnectEnabled,
                        onHcSleepEnabledToggle = viewModel::updateHcSleepEnabled,
                        onHcSleepModeChange = viewModel::updateHcSleepMode,
                        canEnable = { viewModel.distributionServices.featureAccess.canUse(com.uc.caffeine.monetization.ProFeature.HealthConnect) },
                        onLocked = { viewModel.requestFeature(com.uc.caffeine.monetization.ProFeature.HealthConnect) },
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.MyData -> MyDataSettingsScreen(
                        onBack = { nestedBackStack.removeLastOrNull() },
                        viewModel = viewModel,
                    )

                    SettingsDestination.Notifications -> NotificationsSettingsScreen(
                        userSettings = userSettings,
                        onNotificationsAvailable = viewModel::rescheduleReminders,
                        onInactivityReminderToggle = viewModel::updateInactivityReminderEnabled,
                        onDailyReminderTimesChange = viewModel::updateDailyReminderTimes,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    SettingsDestination.Automation -> AutomationSettingsScreen(
                        userSettings = userSettings,
                        onAutomationEnabledChange = viewModel::updateAutomationEnabled,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )
                }
                }
                }
            }
            }
        },
    )
    }
}

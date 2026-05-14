package com.uc.caffeine.ui.screens.analytics

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import java.time.LocalDate

private enum class AnalyticsDestination : NavKey {
    Main,
    Sources,
    Bedtime,
    TimeOfDay,
}

@Composable
internal fun AnalyticsRoot(
    viewModel: CaffeineViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.analyticsUiState.collectAsStateWithLifecycle()
    val nestedBackStack = rememberNavBackStack(AnalyticsDestination.Main)

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
            entry<AnalyticsDestination> { destination ->
                when (destination) {
                    AnalyticsDestination.Main -> AnalyticsMainPage(
                        uiState = uiState,
                        onRangeSelected = viewModel::setAnalyticsRange,
                        onSourcesClick = { nestedBackStack.add(AnalyticsDestination.Sources) },
                        onBedtimeClick = { nestedBackStack.add(AnalyticsDestination.Bedtime) },
                        onTimeOfDayClick = { nestedBackStack.add(AnalyticsDestination.TimeOfDay) },
                    )

                    AnalyticsDestination.Sources -> AnalyticsBySourcePage(
                        uiState = uiState,
                        onRangeSelected = viewModel::setAnalyticsRange,
                        onCustomRange = viewModel::setCustomRange,
                        onBack = { nestedBackStack.removeLastOrNull() },
                        modelProducer = viewModel.analyticsSourcePieChartModelProducer,
                    )

                    AnalyticsDestination.Bedtime -> AnalyticsBedtimePage(
                        uiState = uiState,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )

                    AnalyticsDestination.TimeOfDay -> AnalyticsTimeOfDayPage(
                        uiState = uiState,
                        onRangeSelected = viewModel::setAnalyticsRange,
                        onCustomRange = viewModel::setCustomRange,
                        onBack = { nestedBackStack.removeLastOrNull() },
                    )
                }
            }
        },
    )
}

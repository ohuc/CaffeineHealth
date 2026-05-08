package com.uc.caffeine.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState

@Composable
internal fun AnalyticsBedtimePage(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit,
    modelProducer: CartesianChartModelProducer,
) {
    val haptics = rememberAppHaptics()
    var showRangeSheet by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(R.string.analytics_bedtime_impact), showBackButton = true, onBack = onBack) { bottomPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AnalyticsRangeButton(
                    selectedRange = uiState.selectedRange,
                    customStartDate = uiState.customStartDate,
                    customEndDate = uiState.customEndDate,
                    onClick = { showRangeSheet = true },
                )
            }

            if (uiState.hasData) {
                item {
                    AnalyticsChartCard(
                        title = stringResource(R.string.analytics_bedtime_impact),
                        supportingText = stringResource(R.string.analytics_bedtime_card_supporting),
                    ) {
                        SleepImpactChart(
                            axisLabels = uiState.bedtimeAxisLabels,
                            modelProducer = modelProducer,
                            sleepThresholdMg = uiState.sleepThresholdMg,
                        )
                    }
                }
            } else {
                item {
                    AnalyticsEmptyState(selectedRange = uiState.selectedRange)
                }
            }
        }
    }

    if (showRangeSheet) {
        AnalyticsRangeBottomSheet(
            selectedRange = uiState.selectedRange,
            onRangeSelected = { range ->
                haptics.toggle()
                onRangeSelected(range)
            },
            onCustomRange = { start, end ->
                haptics.toggle()
                onCustomRange(start, end)
            },
            onDismiss = { showRangeSheet = false },
        )
    }
}

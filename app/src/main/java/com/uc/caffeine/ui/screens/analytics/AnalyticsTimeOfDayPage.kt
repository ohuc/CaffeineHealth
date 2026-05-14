package com.uc.caffeine.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState
import java.time.LocalDate

private data class TimeOfDayBucketDetail(val nameRes: Int, val rangeRes: Int, val index: Int)

private val timeOfDayBucketDetails = listOf(
    TimeOfDayBucketDetail(R.string.analytics_bucket_night, R.string.analytics_bucket_night_range, 0),
    TimeOfDayBucketDetail(R.string.analytics_bucket_morning, R.string.analytics_bucket_morning_range, 1),
    TimeOfDayBucketDetail(R.string.analytics_bucket_afternoon, R.string.analytics_bucket_afternoon_range, 2),
    TimeOfDayBucketDetail(R.string.analytics_bucket_evening, R.string.analytics_bucket_evening_range, 3),
)

@Composable
internal fun AnalyticsTimeOfDayPage(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var showRangeSheet by remember { mutableStateOf(false) }

    SettingsPageScaffold(
        title = stringResource(R.string.analytics_when_you_drink),
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding + 16.dp),
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
                    TimeOfDayBarsCard(uiState = uiState)
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

@Composable
private fun TimeOfDayBarsCard(uiState: AnalyticsUiState) {
    val maxValue = uiState.timeOfDayValues.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.analytics_when_you_drink_summary),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                timeOfDayBucketDetails.forEach { (nameRes, rangeRes, index) ->
                    val value = uiState.timeOfDayValues.getOrElse(index) { 0.0 }
                    TimeOfDayBucketRow(
                        nameRes = nameRes,
                        rangeRes = rangeRes,
                        value = value,
                        maxValue = maxValue,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeOfDayBucketRow(
    nameRes: Int,
    rangeRes: Int,
    value: Double,
    maxValue: Double,
) {
    val fraction = (value / maxValue).toFloat().coerceIn(0.05f, 1f)
    val isEmpty = value == 0.0

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = stringResource(nameRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(rangeRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.analytics_value_mg, value.toInt()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isEmpty) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        if (isEmpty) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    ),
            )
        }
    }
}

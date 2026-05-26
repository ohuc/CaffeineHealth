package com.uc.caffeine.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.formatTimeOfDay
import kotlinx.coroutines.flow.drop
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val WeekOrder: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WeeklySleepRotaSettingsScreen(
    userSettings: UserSettings,
    onRotaEnabledChange: (Boolean) -> Unit,
    onDayTimeChange: (DayOfWeek, LocalTime) -> Unit,
    onDayTimeClear: (DayOfWeek) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var pickerDay by remember { mutableStateOf<DayOfWeek?>(null) }

    val rotaEnabled = userSettings.weeklySleepRotaEnabled
    val typicalBedtime = formatTimeOfDay(userSettings.sleepTimeHour, userSettings.sleepTimeMinute, userSettings)

    SettingsPageScaffold(
        title = stringResource(R.string.profile_weekly_sleep_rota_title),
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SegmentedListItem(
                onClick = {
                    haptics.toggle()
                    onRotaEnabledChange(!rotaEnabled)
                },
                leadingContent = {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                },
                content = {
                    Text(text = stringResource(R.string.weekly_sleep_rota_enable_title))
                },
                supportingContent = {
                    Text(text = stringResource(R.string.weekly_sleep_rota_enable_description))
                },
                trailingContent = {
                    Switch(
                        checked = rotaEnabled,
                        onCheckedChange = { enabled ->
                            haptics.toggle()
                            onRotaEnabledChange(enabled)
                        },
                    )
                },
                shapes = segmentedListItemShapes(0, 1),
                colors = ListItemDefaults.colors(
                    containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                ),
            )

            val sectionAlpha by animateFloatAsState(
                targetValue = if (rotaEnabled) 1f else 0.5f,
                label = "rotaSectionAlpha",
            )
            Column(
                modifier = Modifier.alpha(sectionAlpha),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.weekly_sleep_rota_days_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Text(
                    text = stringResource(R.string.weekly_sleep_rota_fallback, typicalBedtime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                WeekOrder.forEachIndexed { index, day ->
                    val customTime = userSettings.weeklySleepRota[day]
                    val dayLabel = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    SegmentedListItem(
                        onClick = {
                            if (rotaEnabled) {
                                haptics.toggle()
                                pickerDay = day
                            }
                        },
                        content = {
                            Text(text = dayLabel)
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (customTime != null) {
                                    IconButton(
                                        enabled = rotaEnabled,
                                        onClick = {
                                            haptics.toggle()
                                            onDayTimeClear(day)
                                        },
                                    ) {
                                        Icon(
                                            Icons.Filled.Replay,
                                            contentDescription = stringResource(
                                                R.string.weekly_sleep_rota_clear_cd,
                                                dayLabel,
                                            ),
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    enabled = rotaEnabled,
                                    onClick = {
                                        haptics.toggle()
                                        pickerDay = day
                                    },
                                ) {
                                    Text(
                                        text = if (customTime != null) {
                                            formatTimeOfDay(customTime.hour, customTime.minute, userSettings)
                                        } else {
                                            typicalBedtime
                                        },
                                    )
                                }
                            }
                        },
                        shapes = segmentedListItemShapes(index, WeekOrder.size),
                        colors = ListItemDefaults.colors(
                            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                        ),
                    )
                }

                if (!rotaEnabled) {
                    Text(
                        text = stringResource(R.string.weekly_sleep_rota_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    pickerDay?.let { day ->
        val initial = userSettings.weeklySleepRota[day]
            ?: LocalTime.of(userSettings.sleepTimeHour, userSettings.sleepTimeMinute)
        WeeklySleepRotaTimePickerDialog(
            day = day,
            initialTime = initial,
            is24HourClock = userSettings.use24HourClock,
            onConfirm = { time ->
                onDayTimeChange(day, time)
                pickerDay = null
            },
            onDismiss = { pickerDay = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklySleepRotaTimePickerDialog(
    day: DayOfWeek,
    initialTime: LocalTime,
    is24HourClock: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24HourClock,
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = AlertDialogDefaults.shape) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        R.string.weekly_sleep_rota_pick_title,
                        day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                LaunchedEffect(timePickerState) {
                    snapshotFlow { timePickerState.hour to timePickerState.minute }
                        .drop(1)
                        .collect { haptics.tick() }
                }
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        },
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

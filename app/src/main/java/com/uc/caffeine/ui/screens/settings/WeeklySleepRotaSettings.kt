package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.SettingsSwitchRow
import com.uc.caffeine.ui.components.SettingsValueRow
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.formatTimeOfDay
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle

private val WeekOrder: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)

@Composable
internal fun WeeklySleepRotaSettingsScreen(
    userSettings: UserSettings,
    onRotaEnabledChange: (Boolean) -> Unit,
    onDayTimeChange: (DayOfWeek, LocalTime) -> Unit,
    onDayTimeClear: (DayOfWeek) -> Unit,
    onBack: () -> Unit,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val haptics = rememberAppHaptics()
    var pickerDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.weekly_sleep_rota_enable_title),
                summary = stringResource(R.string.weekly_sleep_rota_enable_description),
                checked = rotaEnabled,
                onCheckedChange = onRotaEnabledChange,
                icon = Icons.Rounded.CalendarMonth,
            )

            val sectionAlpha by animateFloatAsState(
                targetValue = if (rotaEnabled) 1f else 0.5f,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "rotaSectionAlpha",
            )
            Column(modifier = Modifier.alpha(sectionAlpha)) {
                SettingsSectionLabel(stringResource(R.string.weekly_sleep_rota_days_section))
                Text(
                    text = stringResource(R.string.weekly_sleep_rota_fallback, typicalBedtime) +
                        " " + stringResource(R.string.weekly_sleep_rota_after_midnight_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    WeekOrder.forEachIndexed { index, day ->
                        val customTime = userSettings.weeklySleepRota[day]
                        val dayLabel = day.getDisplayName(TextStyle.FULL, locale)
                        SettingsValueRow(
                            title = dayLabel,
                            summary = if (customTime == null) stringResource(R.string.weekly_sleep_rota_uses_typical) else null,
                            value = customTime
                                ?.let { formatTimeOfDay(it.hour, it.minute, userSettings) }
                                ?: typicalBedtime,
                            enabled = rotaEnabled,
                            onClick = { pickerDay = day },
                            index = index,
                            count = WeekOrder.size,
                            trailingAction = if (customTime != null) {
                                {
                                    IconButton(
                                        enabled = rotaEnabled,
                                        shapes = IconButtonDefaults.shapes(),
                                        onClick = {
                                            haptics.toggle()
                                            onDayTimeClear(day)
                                        },
                                    ) {
                                        Icon(
                                            Icons.Rounded.Replay,
                                            contentDescription = stringResource(
                                                R.string.weekly_sleep_rota_clear_cd,
                                                dayLabel,
                                            ),
                                        )
                                    }
                                }
                            } else null,
                        )
                    }
                }

                if (!rotaEnabled) {
                    Text(
                        text = stringResource(R.string.weekly_sleep_rota_disabled_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
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
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24HourClock,
    )

    com.uc.caffeine.ui.components.AdaptiveTimePickerDialog(
        title = stringResource(R.string.weekly_sleep_rota_pick_title, day.getDisplayName(TextStyle.FULL, locale)),
        state = timePickerState,
        onConfirm = { onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute)) },
        onDismiss = onDismiss,
    )
}

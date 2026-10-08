package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.MAX_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.util.MIN_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.util.combineDatePickerSelectionWithTime
import com.uc.caffeine.util.dateFormatter
import com.uc.caffeine.util.formatDurationMinutes
import com.uc.caffeine.util.formatTimestampToTime
import com.uc.caffeine.util.resolvedZoneId
import com.uc.caffeine.util.toDatePickerMillis
import kotlinx.coroutines.flow.drop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

private const val MINUTE_MILLIS = 60_000L

/** One-tap "I started this a while ago" offsets, in minutes before now. */
private val QuickStartOffsetsMinutes = listOf(0, 10, 30, 60)

/**
 * Lets the duration picker save the chosen value as this drink's own default
 * ("Always use for Espresso"). [isSet] reflects whether the drink already has one;
 * [onChange] receives the minutes to save, or null to clear the drink's default.
 */
class DrinkDurationDefault(
    val drinkName: String,
    val isSet: Boolean,
    val onChange: (Int?) -> Unit,
)

@Composable
fun ConsumptionTimingSection(
    startedAtMillis: Long,
    durationMinutes: Int,
    settings: UserSettings,
    onStartedAtChange: (Long) -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    drinkDurationDefault: DrinkDurationDefault? = null,
) {
    val haptics = rememberAppHaptics()
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    // Which quick option is active. Starts on "Now" only if the entry really is from just now
    // (new entries), so editing an old entry doesn't light up a misleading option.
    var selectedQuickOffset by remember {
        mutableStateOf(
            QuickStartOffsetsMinutes.first().takeIf {
                abs(System.currentTimeMillis() - startedAtMillis) < MINUTE_MILLIS
            },
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.timing_started_drinking),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            QuickStartOffsetsMinutes.forEachIndexed { index, offset ->
                ToggleButton(
                    checked = selectedQuickOffset == offset,
                    onCheckedChange = {
                        haptics.toggle()
                        selectedQuickOffset = offset
                        onStartedAtChange(System.currentTimeMillis() - offset * MINUTE_MILLIS)
                    },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        QuickStartOffsetsMinutes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                ) {
                    Text(
                        text = quickStartLabel(offset),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            TimingRow(
                index = 0,
                icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                label = stringResource(R.string.timing_start_time),
                value = friendlyDateTime(startedAtMillis, settings),
                onClick = {
                    haptics.toggle()
                    showDateTimePicker = true
                },
            )
            TimingRow(
                index = 1,
                icon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                label = stringResource(R.string.timing_time_to_finish),
                value = formatDurationMinutes(durationMinutes),
                onClick = {
                    haptics.toggle()
                    showDurationPicker = true
                },
            )
        }
    }

    if (showDateTimePicker) {
        DateTimePickerDialog(
            currentTimestampMillis = startedAtMillis,
            settings = settings,
            onDateTimeSelected = { newMillis ->
                selectedQuickOffset = null
                onStartedAtChange(newMillis)
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false },
        )
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            currentDurationMinutes = durationMinutes,
            drinkDurationDefault = drinkDurationDefault,
            onDurationSelected = {
                onDurationChange(it)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false },
        )
    }
}

@Composable
private fun TimingRow(
    index: Int,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        leadingContent = icon,
        content = { Text(text = label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        },
        shapes = segmentedListItemShapes(index, 2),
        colors = ListItemDefaults.colors(
            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
        ),
    )
}

@Composable
private fun quickStartLabel(offsetMinutes: Int): String = when {
    offsetMinutes == 0 -> stringResource(R.string.timing_quick_now)
    offsetMinutes % 60 == 0 -> stringResource(R.string.timing_quick_hours_ago, offsetMinutes / 60)
    else -> stringResource(R.string.timing_quick_minutes_ago, offsetMinutes)
}

/** "Today, 1:24 PM" / "Yesterday, 9:05 AM" / "06/10/2026, 8:00 AM". */
@Composable
private fun friendlyDateTime(timestampMillis: Long, settings: UserSettings): String {
    val zone = settings.resolvedZoneId()
    val date = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
    return stringResource(
        R.string.timing_day_and_time,
        dayLabel(date, settings),
        formatTimestampToTime(timestampMillis, settings),
    )
}

@Composable
private fun dayLabel(date: LocalDate, settings: UserSettings): String {
    val today = LocalDate.now(settings.resolvedZoneId())
    return when (date) {
        today -> stringResource(R.string.analytics_range_today)
        today.minusDays(1) -> stringResource(R.string.analytics_range_yesterday)
        else -> date.format(settings.dateFormatter())
    }
}

private enum class DateTimePickerStep { Date, Time }

/**
 * Edits a start timestamp. Opens on the clock — changing the time is by far the common
 * case — with the date one tap away above it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    currentTimestampMillis: Long,
    settings: UserSettings,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(DateTimePickerStep.Time) }
    // Date picker selections are UTC-midnight millis; keep the picked date in that form.
    var pickedDateMillis by remember {
        mutableLongStateOf(toDatePickerMillis(currentTimestampMillis, settings))
    }
    val currentZonedTime = remember(currentTimestampMillis, settings.timeZoneId) {
        Instant.ofEpochMilli(currentTimestampMillis).atZone(settings.resolvedZoneId())
    }
    // Hoisted so the chosen time survives a detour to the date picker.
    val timePickerState = rememberTimePickerState(
        initialHour = currentZonedTime.hour,
        initialMinute = currentZonedTime.minute,
        is24Hour = settings.use24HourClock,
    )

    when (step) {
        DateTimePickerStep.Time -> TimeStepDialog(
            timePickerState = timePickerState,
            dateLabel = dayLabel(
                Instant.ofEpochMilli(pickedDateMillis).atZone(ZoneOffset.UTC).toLocalDate(),
                settings,
            ),
            onChangeDate = { step = DateTimePickerStep.Date },
            onConfirm = {
                onDateTimeSelected(
                    combineDatePickerSelectionWithTime(
                        pickedDateMillis,
                        timePickerState.hour,
                        timePickerState.minute,
                        settings,
                    ),
                )
            },
            onDismiss = onDismiss,
        )

        DateTimePickerStep.Date -> {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = pickedDateMillis)
            DatePickerDialog(
                onDismissRequest = { step = DateTimePickerStep.Time },
                confirmButton = {
                    TextButton(
                        enabled = datePickerState.selectedDateMillis != null,
                        onClick = {
                            datePickerState.selectedDateMillis?.let { pickedDateMillis = it }
                            step = DateTimePickerStep.Time
                        },
                    ) { Text(stringResource(R.string.action_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { step = DateTimePickerStep.Time }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeStepDialog(
    timePickerState: TimePickerState,
    dateLabel: String,
    onChangeDate: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    LaunchedEffect(timePickerState) {
        snapshotFlow { timePickerState.hour to timePickerState.minute }
            .drop(1)
            .collect { haptics.tick() }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.timing_started_drinking),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                FilledTonalButton(
                    onClick = {
                        haptics.toggle()
                        onChangeDate()
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.timing_change_date, dateLabel))
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
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}

@Composable
fun DurationPickerDialog(
    currentDurationMinutes: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.timing_time_to_finish),
    drinkDurationDefault: DrinkDurationDefault? = null,
) {
    val range = MIN_CONSUMPTION_DURATION_MINUTES..MAX_CONSUMPTION_DURATION_MINUTES
    var selectedDuration by remember(currentDurationMinutes) {
        mutableIntStateOf(currentDurationMinutes.coerceIn(range))
    }
    var saveForDrink by remember(drinkDurationDefault) {
        mutableStateOf(drinkDurationDefault?.isSet == true)
    }
    val haptics = rememberAppHaptics()

    fun nudge(delta: Int) {
        val next = (selectedDuration + delta).coerceIn(range)
        if (next != selectedDuration) {
            if (next == range.first || next == range.last) haptics.sliderEdge() else haptics.tick()
            selectedDuration = next
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    drinkDurationDefault?.let { default ->
                        when {
                            saveForDrink -> default.onChange(selectedDuration)
                            default.isSet -> default.onChange(null)
                        }
                    }
                    onDurationSelected(selectedDuration)
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Fine adjust around the headline value — the slider covers 3 hours, so
                // landing on an exact minute by dragging alone is fiddly.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = { nudge(-1) },
                        enabled = selectedDuration > range.first,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(IconButtonDefaults.smallContainerSize()),
                    ) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = stringResource(R.string.timing_duration_decrease_cd),
                        )
                    }
                    RollingNumberText(
                        text = formatDurationMinutes(selectedDuration),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f),
                        labelPrefix = "duration_picker_value",
                    )
                    FilledTonalIconButton(
                        onClick = { nudge(1) },
                        enabled = selectedDuration < range.last,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(IconButtonDefaults.smallContainerSize()),
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.timing_duration_increase_cd),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExpressiveIntSlider(
                        value = selectedDuration,
                        onValueChange = { selectedDuration = it },
                        valueRange = range,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatDurationMinutes(range.first),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatDurationMinutes(range.last),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.timing_duration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (drinkDurationDefault != null) {
                    SegmentedListItem(
                        checked = saveForDrink,
                        onCheckedChange = {
                            haptics.toggle()
                            saveForDrink = it
                        },
                        shapes = segmentedListItemShapes(0, 1),
                        content = {
                            Text(
                                text = stringResource(
                                    R.string.timing_always_use_for_drink,
                                    drinkDurationDefault.drinkName,
                                ),
                            )
                        },
                        trailingContent = {
                            Switch(checked = saveForDrink, onCheckedChange = null)
                        },
                        colors = ListItemDefaults.colors(
                            // The dialog itself is surfaceContainerHigh; step up one tone.
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    )
                }
            }
        },
    )
}

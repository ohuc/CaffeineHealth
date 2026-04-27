package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.util.combineDateWithTime
import com.uc.caffeine.util.formatDurationMinutes
import com.uc.caffeine.util.formatTimestampToTime
import com.uc.caffeine.util.resolvedZoneId
import kotlin.math.roundToInt

private const val MIN_DURATION_MINUTES = 1
private const val MAX_DURATION_MINUTES = 180

@Composable
fun ConsumptionTimingSection(
    startedAtMillis: Long,
    durationMinutes: Int,
    settings: UserSettings,
    onStartedAtChange: (Long) -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LabeledPickerRow(label = "Started drinking", onClick = { showTimePicker = true }) {
            Text(formatTimestampToTime(startedAtMillis, settings))
        }
        LabeledPickerRow(label = "Time to finish", onClick = { showDurationPicker = true }) {
            Text(formatDurationMinutes(durationMinutes))
        }
    }

    if (showTimePicker) {
        ClockTimePickerDialog(
            is24HourClock = settings.use24HourClock,
            currentTimestampMillis = startedAtMillis,
            settings = settings,
            onTimeSelected = { hour, minute ->
                onStartedAtChange(combineDateWithTime(startedAtMillis, hour, minute, settings))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            currentDurationMinutes = durationMinutes,
            onDurationSelected = {
                onDurationChange(it)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false },
        )
    }
}

@Composable
private fun LabeledPickerRow(
    label: String,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            
        )
        OutlinedButton(onClick = onClick, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTimePickerDialog(
    is24HourClock: Boolean,
    currentTimestampMillis: Long,
    settings: UserSettings,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentZonedTime = remember(currentTimestampMillis, settings.timeZoneId) {
        java.time.Instant.ofEpochMilli(currentTimestampMillis).atZone(settings.resolvedZoneId())
    }
    val timePickerState = rememberTimePickerState(
        initialHour = currentZonedTime.hour,
        initialMinute = currentZonedTime.minute,
        is24Hour = is24HourClock,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        },
    )
}

@Composable
fun DurationPickerDialog(
    currentDurationMinutes: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDuration by remember(currentDurationMinutes) {
        mutableFloatStateOf(
            currentDurationMinutes
                .coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
                .toFloat()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onDurationSelected(selectedDuration.roundToInt()) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text("Time to finish")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RollingNumberText(
                    text = formatDurationMinutes(selectedDuration.roundToInt()),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    labelPrefix = "duration_picker_value",
                )
                Slider(
                    value = selectedDuration,
                    onValueChange = { selectedDuration = it },
                    valueRange = MIN_DURATION_MINUTES.toFloat()..MAX_DURATION_MINUTES.toFloat(),
                    steps = MAX_DURATION_MINUTES - MIN_DURATION_MINUTES - 1,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDurationMinutes(MIN_DURATION_MINUTES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDurationMinutes(MAX_DURATION_MINUTES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Use a longer finish time to spread the caffeine intake over the drink window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
    )
}

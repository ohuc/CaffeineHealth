package com.uc.caffeine.ui.screens.settings

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
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
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.notifications.NotificationScheduler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationsSettingsScreen(
    userSettings: UserSettings,
    onInactivityReminderToggle: (Boolean) -> Unit,
    onDailyReminderTimesChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = userSettings.use24HourClock)

    val sortedTimes = remember(userSettings.dailyReminderTimes) {
        userSettings.dailyReminderTimes.sortedWith(compareBy {
            val (h, m) = NotificationScheduler.parseTime(it) ?: (0 to 0)
            h * 60 + m
        })
    }
    val reminderCount = sortedTimes.size
    val totalItems = reminderCount + 1 // +1 for the Add button

    SettingsPageScaffold(
        title = stringResource(R.string.settings_notifications_title),
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.notifications_inactivity_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        onInactivityReminderToggle(!userSettings.inactivityReminderEnabled)
                    },
                    leadingContent = {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    },
                    content = {
                        Text(text = stringResource(R.string.notifications_no_log_title))
                    },
                    supportingContent = {
                        Text(text = stringResource(R.string.notifications_no_log_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = userSettings.inactivityReminderEnabled,
                            onCheckedChange = { enabled ->
                                haptics.toggle()
                                onInactivityReminderToggle(enabled)
                            },
                        )
                    },
                    shapes = segmentedListItemShapes(0, 1),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.notifications_daily_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                sortedTimes.forEachIndexed { index, timeStr ->
                    SegmentedListItem(
                        onClick = {},
                        leadingContent = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        content = {
                            Text(text = formatReminderTime(timeStr, userSettings.use24HourClock))
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    haptics.toggle()
                                    onDailyReminderTimesChange(userSettings.dailyReminderTimes - timeStr)
                                },
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.notifications_remove_reminder_cd))
                            }
                        },
                        shapes = segmentedListItemShapes(index, totalItems),
                        colors = ListItemDefaults.colors(
                            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                        ),
                    )
                }

                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        showTimePicker = true
                    },
                    leadingContent = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    },
                    content = {
                        Text(text = stringResource(R.string.notifications_add_reminder))
                    },
                    shapes = segmentedListItemShapes(reminderCount, totalItems),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }
        }
    }

    if (showTimePicker) {
        BasicAlertDialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = AlertDialogDefaults.shape) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.notifications_add_daily_dialog_title),
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
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                val key = NotificationScheduler.formatTimeKey(timePickerState.hour, timePickerState.minute)
                                onDailyReminderTimesChange(userSettings.dailyReminderTimes + key)
                                showTimePicker = false
                            },
                        ) {
                            Text(stringResource(R.string.action_add))
                        }
                    }
                }
            }
        }
    }
}

private fun formatReminderTime(timeStr: String, use24Hour: Boolean): String {
    val (hour, minute) = NotificationScheduler.parseTime(timeStr) ?: return timeStr
    return if (use24Hour) {
        "%02d:%02d".format(hour, minute)
    } else {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "%d:%02d %s".format(displayHour, minute, amPm)
    }
}

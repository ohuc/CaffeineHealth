package com.uc.caffeine.ui.screens.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsActionRow
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.SettingsSwitchRow
import com.uc.caffeine.ui.components.SettingsValueRow
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.notifications.NotificationScheduler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationsSettingsScreen(
    userSettings: UserSettings,
    onInactivityReminderToggle: (Boolean) -> Unit,
    onDailyReminderTimesChange: (Set<String>) -> Unit,
    onNotificationsAvailable: () -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = androidx.activity.compose.LocalActivity.current
    var showRationale by remember { mutableStateOf(activity?.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) == true) }
    var requested by remember { mutableStateOf(com.uc.caffeine.util.notifications.NotificationConsent.requested(context)) }
    var access by remember { mutableStateOf(com.uc.caffeine.util.notifications.NotificationAccess.read(context)) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) {
        requested = true
        com.uc.caffeine.util.notifications.NotificationConsent.markRequested(context)
        access = com.uc.caffeine.util.notifications.NotificationAccess.read(context)
        showRationale = activity?.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) == true
        if (access.appEnabled) onNotificationsAvailable()
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        access = com.uc.caffeine.util.notifications.NotificationAccess.read(context)
        showRationale = activity?.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) == true
        if (access.appEnabled) onNotificationsAvailable()
        onPauseOrDispose {}
    }
    val blocked = !access.appEnabled ||
        (userSettings.dailyReminderTimes.isNotEmpty() && !access.dailyEnabled) ||
        (userSettings.inactivityReminderEnabled && !access.inactivityEnabled)
    val accessAction = com.uc.caffeine.util.notifications.notificationAccessAction(
        android.os.Build.VERSION.SDK_INT, access.runtimeGranted, requested,
        showRationale,
    )

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.notifications_permission_intro), style = MaterialTheme.typography.bodyMedium)
                if (blocked) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.notifications_paused_title), style = MaterialTheme.typography.titleMediumEmphasized)
                            Text(stringResource(R.string.notifications_paused_body), style = MaterialTheme.typography.bodyMedium)
                            if (access.appEnabled && !access.dailyEnabled) Text(stringResource(R.string.notifications_daily_blocked))
                            if (access.appEnabled && !access.inactivityEnabled) Text(stringResource(R.string.notifications_inactivity_blocked))
                            androidx.compose.material3.Button(
                                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                                onClick = {
                                    if (accessAction == com.uc.caffeine.util.notifications.NotificationAccessAction.REQUEST) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
                                    }
                                },
                            ) { Text(stringResource(if (accessAction == com.uc.caffeine.util.notifications.NotificationAccessAction.REQUEST) R.string.notifications_enable_action else R.string.notifications_settings_action)) }
                        }
                    }
                }
            }
            ReminderSettingsSections(
                userSettings = userSettings,
                onInactivityReminderToggle = onInactivityReminderToggle,
                onDailyReminderTimesChange = onDailyReminderTimesChange,
            )
        }
    }
}

/**
 * The reminder controls (inactivity switch, daily times, add) shared by Settings → Notifications
 * and the onboarding Notifications step once access is on. Owns its own time-picker dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderSettingsSections(
    userSettings: UserSettings,
    onInactivityReminderToggle: (Boolean) -> Unit,
    onDailyReminderTimesChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var editingTime by rememberSaveable { mutableStateOf<String?>(null) }
    val sortedTimes = remember(userSettings.dailyReminderTimes) {
        userSettings.dailyReminderTimes.sortedWith(compareBy {
            val (h, m) = NotificationScheduler.parseTime(it) ?: (0 to 0)
            h * 60 + m
        })
    }
    val reminderCount = sortedTimes.size
    val totalItems = reminderCount + 1 // +1 for the Add button

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            SettingsSectionLabel(stringResource(R.string.notifications_inactivity_section))
            SettingsSwitchRow(
                title = stringResource(R.string.notifications_no_log_title),
                summary = stringResource(R.string.notifications_no_log_description),
                checked = userSettings.inactivityReminderEnabled,
                onCheckedChange = onInactivityReminderToggle,
                icon = Icons.Rounded.NotificationsActive,
            )
        }

        Column {
            SettingsSectionLabel(stringResource(R.string.notifications_daily_section))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                sortedTimes.forEachIndexed { index, timeStr ->
                    SettingsValueRow(
                        icon = Icons.Rounded.Schedule,
                        title = formatReminderTime(timeStr, userSettings),
                        value = null,
                        onClick = { editingTime = timeStr; showTimePicker = true },
                        index = index,
                        count = totalItems,
                        trailingAction = {
                            IconButton(
                                shapes = IconButtonDefaults.shapes(),
                                onClick = {
                                    haptics.toggle()
                                    onDailyReminderTimesChange(userSettings.dailyReminderTimes - timeStr)
                                },
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.notifications_remove_reminder_cd))
                            }
                        },
                    )
                }
                SettingsActionRow(
                    icon = Icons.Rounded.Add,
                    title = stringResource(R.string.notifications_add_reminder),
                    onClick = { editingTime = null; showTimePicker = true },
                    index = reminderCount,
                    count = totalItems,
                )
            }
        }
    }

    if (showTimePicker) {
        val (hour, minute) = editingTime?.let(NotificationScheduler::parseTime) ?: (8 to 0)
        androidx.compose.runtime.key(editingTime) {
            val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = userSettings.use24HourClock)
            com.uc.caffeine.ui.components.AdaptiveTimePickerDialog(
                title = stringResource(if (editingTime == null) R.string.notifications_add_daily_dialog_title else R.string.notifications_edit_daily_dialog_title),
                state = state,
                confirmLabel = stringResource(if (editingTime == null) R.string.action_add else R.string.action_save),
                onDismiss = { showTimePicker = false },
                onConfirm = {
                    val key = NotificationScheduler.formatTimeKey(state.hour, state.minute)
                    onDailyReminderTimesChange((userSettings.dailyReminderTimes - setOfNotNull(editingTime)) + key)
                    showTimePicker = false
                },
            )
        }
    }
}

private fun formatReminderTime(timeStr: String, settings: UserSettings): String {
    val (hour, minute) = NotificationScheduler.parseTime(timeStr) ?: return timeStr
    return com.uc.caffeine.util.formatTimeOfDay(hour, minute, settings)
}

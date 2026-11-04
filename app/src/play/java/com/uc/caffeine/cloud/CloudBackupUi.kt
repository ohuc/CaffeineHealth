package com.uc.caffeine.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uc.caffeine.R
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.ui.components.SettingsActionRow
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.SettingsSwitchRow
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.formatTimestampToDateTime
import kotlinx.coroutines.launch

/**
 * Settings → My data, Play edition: automatic Google Drive backup, free for everyone. Uses the
 * shared settings row kit (segmented Material 3 Expressive list items) so it reads like the rows
 * above it. Every Drive action is user-started; the only background work is the daily backup.
 */
@Composable
internal fun CloudBackupSettingsSection(onRestore: (String) -> Unit) {
    val context = LocalContext.current
    val state by remember(context) { CloudBackup.state(context) }.collectAsStateWithLifecycle()
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = settingsRepo.defaultSettings)
    val authorizer = rememberDriveAuthorizer()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // A backup already in Drive while this install has never backed up: never overwrite it silently.
    var existing by remember { mutableStateOf<Pair<String, DriveBackupFile>?>(null) }

    fun driveAction(block: suspend (token: String) -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                when (val auth = authorizer.authorize()) {
                    is DriveAuthOutcome.Granted -> block(auth.accessToken)
                    is DriveAuthOutcome.Failed -> if (auth.reason != DriveAuthFailure.CANCELLED) notice = R.string.cloud_backup_unavailable
                }
            } catch (_: Exception) {
                notice = R.string.cloud_backup_unavailable
            } finally {
                busy = false
            }
        }
    }

    fun restore(token: String, file: DriveBackupFile?) = scope.launch {
        busy = true
        try {
            val json = if (file != null) CloudBackup.download(token, file) else CloudBackup.downloadLatest(token)
            if (json == null) notice = R.string.cloud_backup_none_found else onRestore(json)
        } catch (_: Exception) {
            notice = R.string.cloud_backup_restore_failed
        } finally {
            busy = false
        }
    }

    val working = busy || state.running
    val summary = when {
        !state.enabled -> stringResource(R.string.cloud_backup_off_summary)
        state.running -> stringResource(R.string.cloud_backup_running)
        state.problem == CloudBackupProblem.SIGN_IN_NEEDED -> stringResource(R.string.cloud_backup_sign_in_needed)
        state.problem == CloudBackupProblem.FAILED -> stringResource(R.string.cloud_backup_failed)
        state.lastBackupMillis == null -> stringResource(R.string.cloud_backup_first_pending)
        else -> {
            val time = formatTimestampToDateTime(state.lastBackupMillis ?: 0L, settings)
            state.account?.let { stringResource(R.string.cloud_backup_last_account, it, time) }
                ?: stringResource(R.string.cloud_backup_last, time)
        }
    }
    val showDelete = state.enabled || state.lastBackupMillis != null
    val count = if (state.enabled) 3 + (if (showDelete) 1 else 0) else 2 + (if (showDelete) 1 else 0)
    var index = 0

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsSectionLabel(stringResource(R.string.cloud_backup_section))
        SettingsSwitchRow(
            title = stringResource(R.string.cloud_backup_auto_title),
            summary = summary,
            icon = when {
                !state.enabled -> Icons.Rounded.CloudOff
                state.running -> Icons.Rounded.CloudSync
                else -> Icons.Rounded.CloudDone
            },
            checked = state.enabled,
            enabled = !busy,
            onCheckedChange = { on ->
                if (!on) CloudBackup.disable(context)
                else driveAction { token ->
                    val inDrive = if (state.lastBackupMillis == null) CloudBackup.latestInDrive(token) else null
                    if (inDrive != null) existing = token to inDrive
                    else CloudBackup.enable(context, token, uploadNow = true).join()
                }
            },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            index = index++,
            count = count,
        )
        if (state.enabled) {
            SettingsActionRow(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(R.string.cloud_backup_now),
                summary = stringResource(R.string.cloud_backup_now_summary),
                onClick = { driveAction { token -> CloudBackup.backUpNow(context, token) } },
                enabled = !working,
                index = index++,
                count = count,
            )
        }
        SettingsActionRow(
            icon = Icons.Rounded.CloudDownload,
            title = stringResource(R.string.cloud_backup_restore),
            summary = stringResource(R.string.cloud_backup_restore_summary),
            onClick = { driveAction { token -> restore(token, null).join() } },
            enabled = !working,
            index = index++,
            count = count,
        )
        if (showDelete) {
            SettingsActionRow(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(R.string.cloud_backup_delete),
                summary = stringResource(R.string.cloud_backup_delete_summary),
                onClick = { confirmDelete = true },
                enabled = !working,
                index = index++,
                count = count,
            )
        }
        if (working) {
            LoadingIndicator(Modifier.align(Alignment.CenterHorizontally).size(36.dp))
        }
    }

    val haptics = rememberAppHaptics()
    existing?.let { (token, file) ->
        AlertDialog(
            onDismissRequest = { existing = null },
            icon = { Icon(Icons.Rounded.CloudSync, contentDescription = null) },
            title = { Text(stringResource(R.string.cloud_backup_existing_title)) },
            text = {
                Text(stringResource(R.string.cloud_backup_existing_body,
                    file.modifiedAtMillis?.let { formatTimestampToDateTime(it, settings) } ?: "—"))
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.toggle(); existing = null
                    // Restore only. The import may still be cancelled, so backups stay off until the user
                    // turns them on again, which then replaces the Drive copy with the merged history.
                    restore(token, file)
                }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cloud_backup_existing_restore)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.toggle(); existing = null
                    CloudBackup.enable(context, token, uploadNow = true)
                }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cloud_backup_existing_replace)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.CloudOff, contentDescription = null) },
            title = { Text(stringResource(R.string.cloud_backup_delete_title)) },
            text = { Text(stringResource(R.string.cloud_backup_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    haptics.toggle(); confirmDelete = false
                    driveAction { token -> CloudBackup.deleteFromDrive(context, token) }
                }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cloud_backup_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { haptics.toggle(); confirmDelete = false }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cloud_backup_cancel))
                }
            },
        )
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = { notice = null }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cloud_backup_ok)) }
            },
        )
    }
}

/**
 * "Restore from Google Drive" on the first setup screen. A returning user skips the questionnaire:
 * restoring imports their profile and history, which also finishes setup.
 */
@Composable
internal fun OnboardingDriveRestore(onRestore: suspend (String) -> Boolean) {
    val context = LocalContext.current
    val authorizer = rememberDriveAuthorizer()
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    var working by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (working) {
            Row(Modifier.heightIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically) {
                LoadingIndicator(Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.onboarding_restore_working), style = MaterialTheme.typography.labelLarge)
            }
        } else {
            TextButton(
                onClick = {
                    haptics.toggle()
                    message = null
                    scope.launch {
                        working = true
                        try {
                            when (val auth = authorizer.authorize()) {
                                is DriveAuthOutcome.Failed ->
                                    if (auth.reason != DriveAuthFailure.CANCELLED) message = R.string.cloud_backup_unavailable
                                is DriveAuthOutcome.Granted -> {
                                    val json = CloudBackup.downloadLatest(auth.accessToken)
                                    when {
                                        json == null -> message = R.string.cloud_backup_none_found
                                        onRestore(json) -> CloudBackup.enable(context, auth.accessToken, uploadNow = false)
                                        else -> message = R.string.cloud_backup_restore_failed
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            message = R.string.cloud_backup_unavailable
                        } finally {
                            working = false
                        }
                    }
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.onboarding_restore_drive))
            }
        }
        message?.let {
            Text(stringResource(it), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}

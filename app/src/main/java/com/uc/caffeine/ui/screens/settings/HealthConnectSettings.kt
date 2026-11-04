package com.uc.caffeine.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.uc.caffeine.LocalAppScaffoldPadding
import com.uc.caffeine.R
import com.uc.caffeine.data.HcCaffeineState
import com.uc.caffeine.data.HcSleepMode
import com.uc.caffeine.data.HcSleepState
import com.uc.caffeine.data.HealthConnectManager
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.hcCaffeineState
import com.uc.caffeine.data.hcSleepState
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.SettingsSwitchRow
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.onboarding.ConnectedChoiceButtonGroup
import com.uc.caffeine.util.formatTimeOfDay
import kotlinx.coroutines.launch

@Composable
internal fun HealthConnectSettingsScreen(
    userSettings: UserSettings,
    healthConnectManager: HealthConnectManager,
    onHealthConnectToggle: (Boolean) -> Unit,
    onHcSleepEnabledToggle: (Boolean) -> Unit,
    onHcSleepModeChange: (HcSleepMode) -> Unit,
    onBack: () -> Unit,
    canEnable: () -> Boolean = { true },
    onLocked: () -> Unit = {},
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val appPadding = LocalAppScaffoldPadding.current

    val installed = healthConnectManager.isAvailable()
    // Until the first check returns, trust the saved settings so the screen never flashes "not granted".
    var grantsLoaded by remember { mutableStateOf(false) }
    var caffeineGranted by remember { mutableStateOf(false) }
    var sleepGranted by remember { mutableStateOf(false) }
    // Health Connect stops showing its dialog after repeated denials; then only its own settings can help.
    var sleepRequestDenied by rememberSaveable { mutableStateOf(false) }

    val noPermissionsMessage = stringResource(R.string.health_connect_no_permissions_granted)
    val notInstalledMessage = stringResource(R.string.health_connect_not_installed)

    suspend fun refreshGrants() {
        caffeineGranted = healthConnectManager.hasPermission()
        sleepGranted = healthConnectManager.hasSleepPermission()
        grantsLoaded = true
    }
    LifecycleResumeEffect(Unit) {
        scope.launch { refreshGrants() }
        onPauseOrDispose {}
    }

    val caffeineLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) {
        scope.launch {
            refreshGrants()
            if (caffeineGranted) onHealthConnectToggle(true) else snackbarHostState.showSnackbar(noPermissionsMessage)
        }
    }
    val sleepLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) {
        scope.launch {
            refreshGrants()
            if (sleepGranted) {
                sleepRequestDenied = false
                onHcSleepEnabledToggle(true)
            } else {
                sleepRequestDenied = true
                snackbarHostState.showSnackbar(noPermissionsMessage)
            }
        }
    }

    fun setCaffeineSync(enable: Boolean) {
        if (!enable) { onHealthConnectToggle(false); return }
        // Locked but still saved on (access was revoked): the tap clears the setting, because turning off is always allowed.
        if (!canEnable()) { if (userSettings.healthConnectEnabled) onHealthConnectToggle(false) else onLocked(); return }
        if (!installed) { scope.launch { snackbarHostState.showSnackbar(notInstalledMessage) }; return }
        if (caffeineGranted) onHealthConnectToggle(true) else caffeineLauncher.launch(healthConnectManager.caffeinePermissions)
    }

    fun setSleep(enable: Boolean) {
        if (!enable) { onHcSleepEnabledToggle(false); return }
        if (!canEnable()) { if (userSettings.hcSleepEnabled) onHcSleepEnabledToggle(false) else onLocked(); return }
        if (!installed) { scope.launch { snackbarHostState.showSnackbar(notInstalledMessage) }; return }
        if (sleepGranted) onHcSleepEnabledToggle(true) else sleepLauncher.launch(healthConnectManager.sleepPermissions)
    }

    fun openHealthConnect() {
        runCatching { context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) }
            .onFailure { scope.launch { snackbarHostState.showSnackbar(notInstalledMessage) } }
    }

    val caffeineState = hcCaffeineState(
        installed = installed,
        granted = if (grantsLoaded) caffeineGranted else userSettings.healthConnectEnabled,
        enabled = userSettings.healthConnectEnabled,
    )
    val sleepState = hcSleepState(
        installed = installed,
        sleepGranted = if (grantsLoaded) sleepGranted else userSettings.hcSleepEnabled,
        sleepEnabled = userSettings.hcSleepEnabled,
        bedtimeKnown = userSettings.hcSleepTimeHour != null,
    )
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val sizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsPageScaffold(
            title = stringResource(R.string.settings_health_connect_title),
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
                Text(
                    text = AnnotatedString.fromHtml(stringResource(R.string.health_connect_description)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.health_connect_caffeine_title),
                        summary = stringResource(
                            when (caffeineState) {
                                HcCaffeineState.NOT_INSTALLED -> R.string.health_connect_not_installed
                                HcCaffeineState.NOT_GRANTED -> R.string.health_connect_caffeine_not_granted
                                HcCaffeineState.OFF -> R.string.health_connect_caffeine_off
                                HcCaffeineState.ACTIVE -> R.string.health_connect_caffeine_active
                            },
                        ),
                        checked = caffeineState == HcCaffeineState.ACTIVE,
                        onCheckedChange = ::setCaffeineSync,
                        icon = Icons.Rounded.Sync,
                        enabled = installed || userSettings.healthConnectEnabled,
                        index = 0,
                        count = 2,
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.health_connect_sleep_title),
                        summary = stringResource(
                            when (sleepState) {
                                HcSleepState.NOT_INSTALLED -> R.string.health_connect_not_installed
                                HcSleepState.NOT_GRANTED -> R.string.health_connect_sleep_summary_not_granted
                                HcSleepState.OFF -> R.string.health_connect_sleep_summary_off
                                HcSleepState.WAITING_FOR_DATA -> R.string.health_connect_sleep_summary_waiting
                                HcSleepState.ACTIVE -> R.string.health_connect_sleep_summary_active
                            },
                        ),
                        checked = sleepState == HcSleepState.ACTIVE || sleepState == HcSleepState.WAITING_FOR_DATA,
                        onCheckedChange = ::setSleep,
                        icon = Icons.Rounded.Bedtime,
                        enabled = installed || userSettings.hcSleepEnabled,
                        index = 1,
                        count = 2,
                    )
                }

                AnimatedContent(
                    targetState = sleepState,
                    transitionSpec = {
                        (fadeIn(effects) togetherWith fadeOut(fastEffects))
                            .using(SizeTransform(clip = false) { _, _ -> sizeSpec })
                    },
                    label = "healthConnectSleepState",
                ) { state ->
                    when (state) {
                        HcSleepState.NOT_INSTALLED -> Unit

                        HcSleepState.NOT_GRANTED -> HealthConnectStatusCard(
                            icon = Icons.Rounded.Bedtime,
                            title = stringResource(R.string.health_connect_sleep_missing_title),
                            body = stringResource(R.string.health_connect_sleep_missing_body),
                            actionLabel = stringResource(
                                if (sleepRequestDenied) R.string.health_connect_open_settings else R.string.health_connect_allow_sleep,
                            ),
                            onAction = { haptics.toggle(); if (sleepRequestDenied) openHealthConnect() else setSleep(true) },
                        )

                        HcSleepState.OFF -> HealthConnectStatusCard(
                            icon = Icons.Rounded.Bedtime,
                            title = stringResource(R.string.health_connect_sleep_off_title),
                            body = stringResource(R.string.health_connect_sleep_off_body),
                            actionLabel = stringResource(R.string.health_connect_turn_on),
                            onAction = { haptics.toggle(); setSleep(true) },
                        )

                        HcSleepState.WAITING_FOR_DATA -> HealthConnectStatusCard(
                            icon = Icons.Rounded.Bedtime,
                            title = stringResource(R.string.health_connect_sleep_waiting_title),
                            body = stringResource(R.string.health_connect_sleep_waiting_body),
                        )

                        HcSleepState.ACTIVE -> Column {
                            SettingsSectionLabel(stringResource(R.string.health_connect_bedtime_source))
                            val reading = formatTimeOfDay(
                                userSettings.hcSleepTimeHour ?: 0,
                                userSettings.hcSleepTimeMinute ?: 0,
                                userSettings,
                            )
                            Text(
                                text = stringResource(R.string.health_connect_bedtime_reading, reading),
                                style = MaterialTheme.typography.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                            val lastNight = stringResource(R.string.health_connect_last_night)
                            val sevenDay = stringResource(R.string.health_connect_seven_day_avg)
                            ConnectedChoiceButtonGroup(
                                options = HcSleepMode.entries,
                                selectedOption = userSettings.hcSleepMode,
                                labelFor = { if (it == HcSleepMode.PREVIOUS_DAY) lastNight else sevenDay },
                                onOptionSelected = onHcSleepModeChange,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                text = stringResource(
                                    if (userSettings.hcSleepMode == HcSleepMode.PREVIOUS_DAY) {
                                        R.string.health_connect_uses_recent_session
                                    } else {
                                        R.string.health_connect_seven_day_avg_description
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = appPadding.calculateBottomPadding()),
        )
    }
}

/** A calm, non-error explanation of a Health Connect state, with at most one action. */
@Composable
private fun HealthConnectStatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    shapes = ButtonDefaults.shapes(),
                ) { Text(actionLabel) }
            }
        }
    }
}

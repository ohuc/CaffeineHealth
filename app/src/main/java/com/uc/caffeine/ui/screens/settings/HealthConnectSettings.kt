package com.uc.caffeine.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.uc.caffeine.LocalAppScaffoldPadding
import com.uc.caffeine.R
import com.uc.caffeine.data.HcSleepMode
import com.uc.caffeine.data.HealthConnectManager
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.formatTimeOfDay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HealthConnectSettingsScreen(
    userSettings: UserSettings,
    healthConnectManager: HealthConnectManager,
    onHealthConnectToggle: (Boolean) -> Unit,
    onHcSleepEnabledToggle: (Boolean) -> Unit,
    onHcSleepModeChange: (HcSleepMode) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appPadding = LocalAppScaffoldPadding.current

    val isConnected = userSettings.healthConnectEnabled || userSettings.hcSleepEnabled

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch {
            val syncGranted = healthConnectManager.hasPermission()
            val sleepGranted = healthConnectManager.hasSleepPermission()
            if (!syncGranted && !sleepGranted) {
                snackbarHostState.showSnackbar("No Health Connect permissions were granted")
            } else {
                if (syncGranted) onHealthConnectToggle(true)
                if (sleepGranted) onHcSleepEnabledToggle(true)
            }
        }
    }

    fun onToggle(enable: Boolean) {
        haptics.toggle()
        if (!enable) {
            onHealthConnectToggle(false)
            onHcSleepEnabledToggle(false)
            return
        }
        if (!healthConnectManager.isAvailable()) {
            scope.launch {
                snackbarHostState.showSnackbar("Health Connect is not installed on this device")
            }
            return
        }
        permissionLauncher.launch(healthConnectManager.allPermissions)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsPageScaffold(
            title = "Health Connect",
            showBackButton = true,
            onBack = onBack,
        ) { bottomPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = bottomPadding + 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentedListItem(
                    onClick = { onToggle(!isConnected) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.health_connect_logo),
                            contentDescription = "Health Connect",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    content = { Text(text = "Sync with Health Connect") },
                    supportingContent = {
                        val desc = when {
                            !healthConnectManager.isAvailable() ->
                                "Health Connect is not installed on this device"
                            !isConnected ->
                                "Grant access to caffeine and sleep data"
                            userSettings.healthConnectEnabled && userSettings.hcSleepEnabled ->
                                "Caffeine sync and sleep data active"
                            userSettings.healthConnectEnabled ->
                                "Caffeine sync active"
                            else ->
                                "Sleep data active"
                        }
                        Text(text = desc)
                    },
                    trailingContent = {
                        Switch(
                            checked = isConnected,
                            onCheckedChange = { onToggle(it) },
                            enabled = healthConnectManager.isAvailable(),
                        )
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )

                Text(
                    text = buildAnnotatedString {
                        append("Syncs your ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("caffeine intake") }
                        append(" and if sleep access is granted ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("auto-sets your bedtime") }
                        append(" from Health Connect.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedVisibility(
                    visible = isConnected && userSettings.hcSleepEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (userSettings.hcSleepTimeHour == null) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "No sleep data found in Health Connect. Your manual bedtime is used until sleep sessions are recorded.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Bedtime source",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Column {
                            SegmentedListItem(
                                onClick = { onHcSleepModeChange(HcSleepMode.PREVIOUS_DAY) },
                                content = { Text(text = "Last night's sleep") },
                                supportingContent = {
                                    val fetchedTime = if (
                                        userSettings.hcSleepMode == HcSleepMode.PREVIOUS_DAY &&
                                        userSettings.hcSleepTimeHour != null &&
                                        userSettings.hcSleepTimeMinute != null
                                    ) {
                                        formatTimeOfDay(
                                            userSettings.hcSleepTimeHour,
                                            userSettings.hcSleepTimeMinute,
                                            userSettings,
                                        )
                                    } else null
                                    Text(
                                        text = fetchedTime
                                            ?.let { "Bedtime reading: $it" }
                                            ?: "Uses the most recent sleep session"
                                    )
                                },
                                trailingContent = {
                                    RadioButton(
                                        selected = userSettings.hcSleepMode == HcSleepMode.PREVIOUS_DAY,
                                        onClick = { onHcSleepModeChange(HcSleepMode.PREVIOUS_DAY) },
                                    )
                                },
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                                colors = ListItemDefaults.colors(
                                    containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                                ),
                            )
                            SegmentedListItem(
                                onClick = { onHcSleepModeChange(HcSleepMode.SEVEN_DAY_AVERAGE) },
                                content = { Text(text = "7-day average") },
                                supportingContent = {
                                    val fetchedTime = if (
                                        userSettings.hcSleepMode == HcSleepMode.SEVEN_DAY_AVERAGE &&
                                        userSettings.hcSleepTimeHour != null &&
                                        userSettings.hcSleepTimeMinute != null
                                    ) {
                                        formatTimeOfDay(
                                            userSettings.hcSleepTimeHour,
                                            userSettings.hcSleepTimeMinute,
                                            userSettings,
                                        )
                                    } else null
                                    Text(
                                        text = fetchedTime
                                            ?.let { "Bedtime reading: $it" }
                                            ?: "Averages bedtime across the past week"
                                    )
                                },
                                trailingContent = {
                                    RadioButton(
                                        selected = userSettings.hcSleepMode == HcSleepMode.SEVEN_DAY_AVERAGE,
                                        onClick = { onHcSleepModeChange(HcSleepMode.SEVEN_DAY_AVERAGE) },
                                    )
                                },
                                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                                colors = ListItemDefaults.colors(
                                    containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                                ),
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

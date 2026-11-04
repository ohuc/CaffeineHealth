package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.monetization.DistributionUi
import com.uc.caffeine.ui.components.SettingsNavGroup
import com.uc.caffeine.ui.components.SettingsNavItem
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.monetization.*

/** Appearance and Date & time, plus the Play app-icon choice. */
@Composable
internal fun LookAndFeelSettingsScreen(
    onAppearanceClick: () -> Unit,
    onDateTimeClick: () -> Unit,
    distributionUi: DistributionUi,
    onOpenPlus: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsHubPage(title = stringResource(R.string.settings_look_feel_title), onBack = onBack) {
        SettingsNavGroup(
            listOf(
                SettingsNavItem(
                    title = stringResource(R.string.settings_appearance_title),
                    summary = stringResource(R.string.settings_appearance_summary),
                    icon = Icons.Rounded.Palette,
                    onClick = onAppearanceClick,
                ),
                SettingsNavItem(
                    title = stringResource(R.string.settings_date_time_title),
                    summary = stringResource(R.string.settings_date_time_summary),
                    icon = Icons.Rounded.Schedule,
                    onClick = onDateTimeClick,
                ),
            ),
        )
        distributionUi.AppIconSettings(onOpenPlus)
    }
}

/** Health Connect, automation and the local data tools. */
@Composable
internal fun ConnectionsSettingsScreen(
    userSettings: UserSettings,
    onHealthConnectClick: () -> Unit,
    onAutomationClick: () -> Unit,
    onMyDataClick: () -> Unit,
    onBack: () -> Unit,
) {
    val healthLocked = isPlusFeatureLocked(ProFeature.HealthConnect)
    val automationLocked = isPlusFeatureLocked(ProFeature.Automation)
    val openPlus = LocalOpenPlus.current
    SettingsHubPage(title = stringResource(R.string.settings_connections_title), onBack = onBack) {
        SettingsNavGroup(listOf(SettingsNavItem(
            title = stringResource(R.string.settings_my_data_title), summary = stringResource(R.string.settings_my_data_summary),
            icon = Icons.Rounded.Storage, onClick = onMyDataClick)))
        if (healthLocked || automationLocked) PlusDivider(stringResource(R.string.plus_divider_label), openPlus)
        SettingsNavGroup(
            listOf(
                SettingsNavItem(
                    title = stringResource(R.string.settings_health_connect_title),
                    summary = stringResource(R.string.settings_health_connect_summary),
                    icon = Icons.Rounded.Favorite,
                    onClick = onHealthConnectClick,
                    locked = healthLocked,
                    savedEnabled = userSettings.healthConnectEnabled || userSettings.hcSleepEnabled,
                ),
                SettingsNavItem(
                    title = stringResource(R.string.settings_automation_title),
                    summary = stringResource(R.string.settings_automation_summary),
                    icon = Icons.Rounded.SettingsRemote,
                    onClick = onAutomationClick,
                    locked = automationLocked,
                    savedEnabled = userSettings.automationEnabled,
                ),
            ),
        )
    }
}

@Composable
internal fun DeveloperSettingsScreen(distributionUi: DistributionUi, onBack: () -> Unit) {
    SettingsHubPage(title = stringResource(R.string.settings_developer_title), onBack = onBack) {
        distributionUi.DeveloperSettings()
    }
}

@Composable
private fun SettingsHubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    SettingsPageScaffold(title = title, showBackButton = true, onBack = onBack) { bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

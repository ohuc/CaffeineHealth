package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uc.caffeine.ui.theme.CaffeineTheme

@Preview(name = "Phone", widthDp = 412, heightDp = 892)
@Preview(name = "Phone large text", widthDp = 412, heightDp = 892, fontScale = 2f)
@Preview(name = "Narrow", widthDp = 320, heightDp = 720)
@Preview(name = "Dark", widthDp = 412, heightDp = 892, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
annotation class SettingsRowPreviews

@SettingsRowPreviews
@Composable
private fun SettingsRowKitPreview() {
    CaffeineTheme {
        Surface {
            var sleepOn by remember { mutableStateOf(true) }
            var expanded by remember { mutableStateOf(true) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SettingsSwitchRow(
                        title = "Weekly sleep rota",
                        summary = "Use a different bedtime for each weekday",
                        checked = sleepOn,
                        onCheckedChange = { sleepOn = it },
                        icon = Icons.Rounded.CalendarMonth,
                        index = 0,
                        count = 2,
                    )
                    SettingsValueRow(
                        title = "Monday",
                        value = "11:00 PM",
                        onClick = {},
                        index = 1,
                        count = 2,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SettingsValueRow(
                        title = "Time to finish",
                        summary = "How long a drink takes to drink",
                        value = "20 min",
                        onClick = {},
                        icon = Icons.Rounded.Timer,
                        index = 0,
                        count = 2,
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Redo onboarding",
                        summary = "Answer the setup questions again",
                        onClick = {},
                        index = 1,
                        count = 2,
                    )
                }
                SettingsExpandableGroup(
                    icon = Icons.Filled.Tune,
                    title = "Re-adjust health profile",
                    summary = "Half-life, bedtime and health factors",
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    SettingsField("Sleep sensitivity", "Do you find it hard to fall asleep?") {
                        Text("Control goes here")
                    }
                }
            }
        }
    }
}

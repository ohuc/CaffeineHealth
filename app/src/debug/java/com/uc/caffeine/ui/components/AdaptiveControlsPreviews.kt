package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.theme.CaffeineTheme

@Preview(name = "Narrow cover", widthDp = 320, heightDp = 720)
@Preview(name = "Phone", widthDp = 412, heightDp = 892)
@Preview(name = "Phone large text", widthDp = 412, heightDp = 892, fontScale = 2f)
@Preview(name = "Foldable", widthDp = 673, heightDp = 841)
@Preview(name = "Tablet", widthDp = 840, heightDp = 1100)
@Preview(name = "Desktop window", widthDp = 1200, heightDp = 800)
@Preview(name = "Short landscape", widthDp = 720, heightDp = 320, fontScale = 1.3f)
@Preview(name = "German", widthDp = 360, heightDp = 800, locale = "de", fontScale = 1.5f)
@Preview(name = "RTL", widthDp = 360, heightDp = 800, locale = "ar", fontScale = 1.5f)
@Preview(name = "Dark large text", widthDp = 412, heightDp = 892, fontScale = 2f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
annotation class AdaptiveControlPreviews

@AdaptiveControlPreviews
@Composable
private fun AdaptiveControlsPreview() {
    CaffeineTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.appearance_theme_label), style = MaterialTheme.typography.titleLarge)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_theme_label)) },
                    supportingContent = {
                        val labels = listOf(
                            stringResource(R.string.appearance_theme_mode_system),
                            stringResource(R.string.appearance_theme_mode_light),
                            stringResource(R.string.appearance_theme_mode_dark),
                        )
                        AdaptiveConnectedButtonGroup(labels.size) { index, shapes ->
                            ToggleButton(checked = index == 0, onCheckedChange = {}, shapes = shapes) {
                                Text(labels[index])
                            }
                        }
                    },
                )
                val labels = listOf("No medication", "Medication that slows caffeine clearance", "Medication that speeds clearance")
                AdaptiveConnectedButtonGroup(labels.size) { index, shapes ->
                    ToggleButton(checked = index == 0, onCheckedChange = {}, shapes = shapes) { Text(labels[index]) }
                }
                RollingNumberText(text = "1,234.5 mg", style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

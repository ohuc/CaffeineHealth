package com.uc.caffeine.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.uc.caffeine.BuildConfig
import com.uc.caffeine.R
import com.uc.caffeine.automation.AutomationContract
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults

private data class AutomationExtraDoc(val key: String, val descriptionRes: Int)

private val automationExtras = listOf(
    AutomationExtraDoc(AutomationContract.EXTRA_DRINK_NAME, R.string.automation_extra_drink_name),
    AutomationExtraDoc(AutomationContract.EXTRA_UNIT, R.string.automation_extra_unit),
    AutomationExtraDoc(AutomationContract.EXTRA_QUANTITY, R.string.automation_extra_quantity),
    AutomationExtraDoc(AutomationContract.EXTRA_CAFFEINE_MG, R.string.automation_extra_caffeine_mg),
    AutomationExtraDoc(AutomationContract.EXTRA_TIMESTAMP, R.string.automation_extra_timestamp),
    AutomationExtraDoc(AutomationContract.EXTRA_DURATION_MINUTES, R.string.automation_extra_duration_minutes),
)

@Composable
internal fun AutomationSettingsScreen(
    userSettings: UserSettings,
    onAutomationEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.automation_copied)
    val listColors = ListItemDefaults.colors(
        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
    )

    SettingsPageScaffold(
        title = stringResource(R.string.settings_automation_title),
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
            SegmentedListItem(
                onClick = {
                    haptics.toggle()
                    onAutomationEnabledChange(!userSettings.automationEnabled)
                },
                leadingContent = {
                    Icon(Icons.Rounded.SettingsRemote, contentDescription = null)
                },
                content = {
                    Text(text = stringResource(R.string.automation_enable_title))
                },
                supportingContent = {
                    Text(text = stringResource(R.string.automation_enable_description))
                },
                trailingContent = {
                    Switch(
                        checked = userSettings.automationEnabled,
                        onCheckedChange = { enabled ->
                            haptics.toggle()
                            onAutomationEnabledChange(enabled)
                        },
                    )
                },
                shapes = segmentedListItemShapes(0, 1),
                colors = listColors,
            )

            AutomationSection(title = stringResource(R.string.automation_setup_section)) {
                val setupRows = listOf(
                    stringResource(R.string.automation_action_label) to AutomationContract.ACTION_LOG_DRINK,
                    stringResource(R.string.automation_package_label) to BuildConfig.APPLICATION_ID,
                    stringResource(R.string.automation_target_label) to stringResource(R.string.automation_target_value),
                )
                setupRows.forEachIndexed { index, (label, value) ->
                    // Only the machine-readable values are worth copying.
                    val copyable = index < 2
                    SegmentedListItem(
                        onClick = {
                            if (copyable) {
                                haptics.toggle()
                                copyToClipboard(context, value, copiedMessage)
                            }
                        },
                        content = { Text(text = label) },
                        supportingContent = {
                            Text(text = value, fontFamily = if (copyable) FontFamily.Monospace else null)
                        },
                        trailingContent = if (copyable) {
                            {
                                IconButton(
                                    onClick = {
                                        haptics.toggle()
                                        copyToClipboard(context, value, copiedMessage)
                                    },
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(R.string.automation_copy_cd),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        shapes = segmentedListItemShapes(index, setupRows.size),
                        colors = listColors,
                    )
                }
            }

            AutomationSection(title = stringResource(R.string.automation_extras_section)) {
                automationExtras.forEachIndexed { index, extra ->
                    SegmentedListItem(
                        onClick = {},
                        content = { Text(text = extra.key, fontFamily = FontFamily.Monospace) },
                        supportingContent = { Text(text = stringResource(extra.descriptionRes)) },
                        shapes = segmentedListItemShapes(index, automationExtras.size),
                        colors = listColors,
                    )
                }
            }

            AutomationSection(title = stringResource(R.string.automation_example_section)) {
                Text(
                    text = stringResource(R.string.automation_example_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AutomationSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
        content()
    }
}

private fun copyToClipboard(context: Context, text: String, confirmation: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(text, text))
    // Android 13+ shows its own clipboard confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
    }
}

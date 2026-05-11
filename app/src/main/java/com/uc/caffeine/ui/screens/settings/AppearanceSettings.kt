package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.HomeViewMode
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun AppearanceSettingsScreen(
    userSettings: UserSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val themeModes = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.appearance_theme_mode_system),
        ThemeMode.LIGHT to stringResource(R.string.appearance_theme_mode_light),
        ThemeMode.DARK to stringResource(R.string.appearance_theme_mode_dark),
    )
    val homeViewModes = listOf(
        HomeViewMode.GRAPH to stringResource(R.string.appearance_home_view_graph),
        HomeViewMode.CIRCULAR to stringResource(R.string.appearance_home_view_circular),
    )

    SettingsPageScaffold(
        title = stringResource(R.string.settings_appearance_title),
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
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SegmentedListItem(
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                        )
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.appearance_theme_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            themeModes.forEachIndexed { index, (themeMode, label) ->
                                ToggleButton(
                                    modifier = Modifier.weight(1f),
                                    checked = userSettings.themeMode == themeMode,
                                    onCheckedChange = { checked ->
                                        if (checked && userSettings.themeMode != themeMode) {
                                            haptics.toggle()
                                            onThemeModeChange(themeMode)
                                        }
                                    },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        themeModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )

                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        onDynamicColorChange(!userSettings.useDynamicColor)
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.InvertColors,
                            contentDescription = null,
                        )
                    },
                    content = {
                        Text(text = stringResource(R.string.appearance_dynamic_color_label))
                    },
                    supportingContent = {
                        Text(text = stringResource(R.string.appearance_dynamic_color_summary))
                    },
                    trailingContent = {
                        Switch(
                            checked = userSettings.useDynamicColor,
                            onCheckedChange = { enabled ->
                                haptics.toggle()
                                onDynamicColorChange(enabled)
                            },
                        )
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        index = 1,
                        count = 2,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SegmentedListItem(
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                        )
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.appearance_home_view_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            homeViewModes.forEachIndexed { index, (mode, label) ->
                                ToggleButton(
                                    modifier = Modifier.weight(1f),
                                    checked = userSettings.homeViewMode == mode,
                                    onCheckedChange = { checked ->
                                        if (checked && userSettings.homeViewMode != mode) {
                                            haptics.toggle()
                                            onHomeViewModeChange(mode)
                                        }
                                    },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    },
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }
        }
    }
}

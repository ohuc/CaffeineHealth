package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.DurationPickerDialog
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.CategoryIcons
import com.uc.caffeine.util.CategoryUtils
import com.uc.caffeine.util.defaultDurationForCategory
import com.uc.caffeine.util.formatDurationMinutes

/** What the duration picker is currently editing. */
private sealed interface DurationTarget {
    data object Default : DurationTarget
    data class Category(val key: String) : DurationTarget
    data class Drink(val itemId: String) : DurationTarget
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DurationSettingsScreen(
    userSettings: UserSettings,
    drinkNamesByItemId: Map<String, String>,
    onDefaultDurationChange: (Int) -> Unit,
    onCategoryDurationChange: (String, Int?) -> Unit,
    onDrinkDurationChange: (String, Int?) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var pickerTarget by remember { mutableStateOf<DurationTarget?>(null) }
    val categoryKeys = CategoryUtils.getCategoryOrder()
    val drinkOverrides = userSettings.drinkDurationMinutes.entries
        .map { (itemId, minutes) -> Triple(itemId, drinkNamesByItemId[itemId] ?: itemId, minutes) }
        .sortedBy { it.second.lowercase() }

    SettingsPageScaffold(
        title = stringResource(R.string.settings_duration_title),
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
                    pickerTarget = DurationTarget.Default
                },
                leadingContent = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                content = { Text(text = stringResource(R.string.duration_settings_default_title)) },
                supportingContent = {
                    Text(text = stringResource(R.string.duration_settings_default_description))
                },
                trailingContent = {
                    FilledTonalButton(
                        onClick = {
                            haptics.toggle()
                            pickerTarget = DurationTarget.Default
                        },
                    ) {
                        Text(formatDurationMinutes(userSettings.defaultDurationMinutes))
                    }
                },
                shapes = segmentedListItemShapes(0, 1),
                colors = ListItemDefaults.colors(
                    containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(
                    title = stringResource(R.string.duration_settings_categories_section),
                    hint = stringResource(R.string.duration_settings_categories_hint),
                )
                categoryKeys.forEachIndexed { index, key ->
                    val label = CategoryUtils.getCategoryDisplayName(key)
                    val hasOverride = key in userSettings.categoryDurationMinutes
                    SegmentedListItem(
                        onClick = {
                            haptics.toggle()
                            pickerTarget = DurationTarget.Category(key)
                        },
                        leadingContent = {
                            Icon(CategoryIcons.getIcon(label), contentDescription = null)
                        },
                        content = { Text(text = label) },
                        trailingContent = {
                            OverrideTrailing(
                                valueText = formatDurationMinutes(userSettings.defaultDurationForCategory(key)),
                                clearIcon = if (hasOverride) Icons.Rounded.Replay else null,
                                clearContentDescription = stringResource(R.string.duration_settings_reset_cd, label),
                                onClear = {
                                    haptics.toggle()
                                    onCategoryDurationChange(key, null)
                                },
                                onEdit = {
                                    haptics.toggle()
                                    pickerTarget = DurationTarget.Category(key)
                                },
                            )
                        },
                        shapes = segmentedListItemShapes(index, categoryKeys.size),
                        colors = ListItemDefaults.colors(
                            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                        ),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(
                    title = stringResource(R.string.duration_settings_drinks_section),
                    hint = if (drinkOverrides.isEmpty()) {
                        stringResource(R.string.duration_settings_drinks_empty)
                    } else {
                        null
                    },
                )
                drinkOverrides.forEachIndexed { index, (itemId, name, minutes) ->
                    SegmentedListItem(
                        onClick = {
                            haptics.toggle()
                            pickerTarget = DurationTarget.Drink(itemId)
                        },
                        content = { Text(text = name) },
                        trailingContent = {
                            OverrideTrailing(
                                valueText = formatDurationMinutes(minutes),
                                clearIcon = Icons.Rounded.Close,
                                clearContentDescription = stringResource(R.string.duration_settings_remove_cd, name),
                                onClear = {
                                    haptics.toggle()
                                    onDrinkDurationChange(itemId, null)
                                },
                                onEdit = {
                                    haptics.toggle()
                                    pickerTarget = DurationTarget.Drink(itemId)
                                },
                            )
                        },
                        shapes = segmentedListItemShapes(index, drinkOverrides.size),
                        colors = ListItemDefaults.colors(
                            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                        ),
                    )
                }
            }
        }
    }

    pickerTarget?.let { target ->
        val (title, current) = when (target) {
            DurationTarget.Default -> stringResource(R.string.duration_settings_default_title) to
                userSettings.defaultDurationMinutes
            is DurationTarget.Category -> CategoryUtils.getCategoryDisplayName(target.key) to
                userSettings.defaultDurationForCategory(target.key)
            is DurationTarget.Drink -> (drinkNamesByItemId[target.itemId] ?: target.itemId) to
                (userSettings.drinkDurationMinutes[target.itemId] ?: userSettings.defaultDurationMinutes)
        }
        DurationPickerDialog(
            currentDurationMinutes = current,
            title = title,
            onDurationSelected = { minutes ->
                when (target) {
                    DurationTarget.Default -> onDefaultDurationChange(minutes)
                    is DurationTarget.Category -> onCategoryDurationChange(target.key, minutes)
                    is DurationTarget.Drink -> onDrinkDurationChange(target.itemId, minutes)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }
}

@Composable
private fun SectionHeader(title: String, hint: String?) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    if (hint != null) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun OverrideTrailing(
    valueText: String,
    clearIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    clearContentDescription: String,
    onClear: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (clearIcon != null) {
            IconButton(onClick = onClear) {
                Icon(clearIcon, contentDescription = clearContentDescription)
            }
        }
        FilledTonalButton(onClick = onEdit) {
            Text(valueText)
        }
    }
}

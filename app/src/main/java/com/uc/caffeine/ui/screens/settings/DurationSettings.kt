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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.ui.components.DurationPickerDialog
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.SettingsValueRow
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.CategoryIcons
import com.uc.caffeine.util.CategoryUtils
import com.uc.caffeine.util.defaultDurationForCategory
import com.uc.caffeine.util.formatDurationMinutes
import com.uc.caffeine.util.resolveVisibleDrinkDurationOverrides

/** What the duration picker is currently editing. */
private sealed interface DurationTarget {
    data object Default : DurationTarget
    data class Category(val key: String) : DurationTarget
    data class Drink(val itemId: String) : DurationTarget
}

private val DurationTargetSaver = listSaver<DurationTarget?, String>(
    save = {
        when (it) {
            DurationTarget.Default -> listOf("default")
            is DurationTarget.Category -> listOf("category", it.key)
            is DurationTarget.Drink -> listOf("drink", it.itemId)
            null -> emptyList()
        }
    },
    restore = {
        when (it.firstOrNull()) {
            "default" -> DurationTarget.Default
            "category" -> DurationTarget.Category(it[1])
            "drink" -> DurationTarget.Drink(it[1])
            else -> null
        }
    },
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DurationSettingsScreen(
    userSettings: UserSettings,
    drinkPresets: List<DrinkPreset>,
    onDefaultDurationChange: (Int) -> Unit,
    onCategoryDurationChange: (String, Int?) -> Unit,
    onDrinkDurationChange: (String, Int?) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var pickerTarget by rememberSaveable(stateSaver = DurationTargetSaver) { mutableStateOf<DurationTarget?>(null) }
    val categoryKeys = CategoryUtils.getCategoryOrder()
    val drinkOverrides = remember(userSettings.drinkDurationMinutes, drinkPresets) {
        resolveVisibleDrinkDurationOverrides(
            drinkDurationMinutes = userSettings.drinkDurationMinutes,
            presets = drinkPresets,
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsValueRow(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.duration_settings_default_title),
                summary = stringResource(R.string.duration_settings_default_description),
                value = formatDurationMinutes(userSettings.defaultDurationMinutes),
                onClick = { pickerTarget = DurationTarget.Default },
            )

            Column {
                SectionHeader(
                    title = stringResource(R.string.duration_settings_categories_section),
                    hint = stringResource(R.string.duration_settings_categories_hint),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    categoryKeys.forEachIndexed { index, key ->
                        val label = CategoryUtils.getCategoryDisplayName(key)
                        val hasOverride = key in userSettings.categoryDurationMinutes
                        SettingsValueRow(
                            icon = CategoryIcons.getIcon(label),
                            title = label,
                            value = formatDurationMinutes(userSettings.defaultDurationForCategory(key)),
                            onClick = { pickerTarget = DurationTarget.Category(key) },
                            index = index,
                            count = categoryKeys.size,
                            trailingAction = if (hasOverride) {
                                {
                                    OverrideAction(
                                        icon = Icons.Rounded.Replay,
                                        contentDescription = stringResource(R.string.duration_settings_reset_cd, label),
                                        onClick = {
                                            haptics.toggle()
                                            onCategoryDurationChange(key, null)
                                        },
                                    )
                                }
                            } else null,
                        )
                    }
                }
            }

            Column {
                SectionHeader(
                    title = stringResource(R.string.duration_settings_drinks_section),
                    hint = if (drinkOverrides.isEmpty()) {
                        stringResource(R.string.duration_settings_drinks_empty)
                    } else {
                        null
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    drinkOverrides.forEachIndexed { index, (itemId, name, minutes) ->
                        SettingsValueRow(
                            title = name,
                            value = formatDurationMinutes(minutes),
                            onClick = { pickerTarget = DurationTarget.Drink(itemId) },
                            index = index,
                            count = drinkOverrides.size,
                            trailingAction = {
                                OverrideAction(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.duration_settings_remove_cd, name),
                                    onClick = {
                                        haptics.toggle()
                                        onDrinkDurationChange(itemId, null)
                                    },
                                )
                            },
                        )
                    }
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
            is DurationTarget.Drink -> (
                drinkOverrides.firstOrNull { it.first == target.itemId }?.second ?: target.itemId
                ) to (userSettings.drinkDurationMinutes[target.itemId] ?: userSettings.defaultDurationMinutes)
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
    SettingsSectionLabel(title)
    if (hint != null) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun OverrideAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
        Icon(icon, contentDescription = contentDescription)
    }
}

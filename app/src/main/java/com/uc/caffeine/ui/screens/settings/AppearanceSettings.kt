package com.uc.caffeine.ui.screens.settings

import com.uc.caffeine.monetization.*
import androidx.compose.ui.semantics.*
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import com.uc.caffeine.ui.components.SettingsSwitchRow
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.AppColorPalette
import com.uc.caffeine.data.HomeViewMode
import com.uc.caffeine.data.MAX_RECENT_SERVINGS
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.ExpressiveIntSlider
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.AdaptiveConnectedButtonGroup
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.ui.theme.colorSchemeForPalette

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun AppearanceSettingsScreen(
    userSettings: UserSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    onColorPaletteChange: (AppColorPalette) -> Unit,
    onRecentServingsCountChange: (Int) -> Unit,
    onExpressiveBlurChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onStickyHomeChartChange: (Boolean) -> Unit = {},
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
    val isDark = when (userSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val appearanceLocked = isPlusFeatureLocked(ProFeature.AdvancedAppearance)
    val openPlus = LocalOpenPlus.current
    val request = LocalRequestPlusFeature.current
    val onLocked = { request(ProFeature.AdvancedAppearance) }
    val themeContent: @Composable () -> Unit = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.appearance_theme_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    modifier = Modifier.clip(MaterialTheme.shapes.large),
                    leadingContent = {
                        Icon(
                            imageVector = when (userSettings.themeMode) {
                                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                            },
                            contentDescription = null,
                        )
                    },
                    supportingContent = {
                        AdaptiveConnectedButtonGroup(
                            itemCount = themeModes.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) { index, shapes ->
                                val (themeMode, label) = themeModes[index]
                                ToggleButton(
                                    checked = userSettings.themeMode == themeMode,
                                    onCheckedChange = { checked ->
                                        if (checked && userSettings.themeMode != themeMode) {
                                            haptics.toggle()
                                            onThemeModeChange(themeMode)
                                        }
                                    },
                                    shapes = shapes,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }

    }
    val homeContent: @Composable () -> Unit = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.appearance_home_view_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    modifier = Modifier.clip(MaterialTheme.shapes.large),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.DonutLarge,
                            contentDescription = null,
                        )
                    },
                    supportingContent = {
                        AdaptiveConnectedButtonGroup(
                            itemCount = homeViewModes.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) { index, shapes ->
                                val (mode, label) = homeViewModes[index]
                                ToggleButton(
                                    checked = userSettings.homeViewMode == mode,
                                    onCheckedChange = { checked ->
                                        if (checked && userSettings.homeViewMode != mode) {
                                            haptics.toggle()
                                            onHomeViewModeChange(mode)
                                        }
                                    },
                                    shapes = shapes,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }

    }
    val blurContent: @Composable (Boolean) -> Unit = { locked ->
            SegmentedListItem(
                checked = userSettings.expressiveBlur,
                onCheckedChange = { enabled ->
                    haptics.toggle()
                    if (locked && !userSettings.expressiveBlur) onLocked()
                    else onExpressiveBlurChange(enabled)
                },
                modifier = Modifier.plusLockedSemantics(locked, userSettings.expressiveBlur,
                    if (locked && userSettings.expressiveBlur) stringResource(R.string.plus_turn_off_saved,
                        stringResource(R.string.appearance_expressive_blur_label)) else stringResource(R.string.plus_locked_click_label)),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.BlurOn,
                        contentDescription = null,
                        modifier = Modifier.plusLockedContent(locked),
                    )
                },
                supportingContent = {
                    Text(text = stringResource(R.string.appearance_expressive_blur_summary), color = if (locked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.plusLockedContent(locked))
                },
                trailingContent = {
                    // The whole row toggles; the switch only mirrors the state.
                    Switch(checked = userSettings.expressiveBlur, onCheckedChange = null, modifier = Modifier.plusLockedContent(locked))
                },
                verticalAlignment = Alignment.CenterVertically,
                shapes = segmentedListItemShapes(index = 0, count = 1),
                colors = ListItemDefaults.colors(
                    containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                ),
            ) {
                Text(
                    text = stringResource(R.string.appearance_expressive_blur_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.plusLockedContent(locked),
                )
            }

    }
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
            themeContent()
            PaletteChoices(
                palettes = if (appearanceLocked) listOf(AppColorPalette.DYNAMIC) else AppColorPalette.entries,
                selectedPalette = userSettings.colorPalette,
                dark = isDark,
                onSelect = onColorPaletteChange,
            )
            homeContent()
            SettingsSwitchRow(
                title = stringResource(R.string.appearance_sticky_chart_label),
                summary = stringResource(R.string.appearance_sticky_chart_summary),
                icon = Icons.Rounded.PushPin,
                checked = userSettings.stickyHomeChart,
                onCheckedChange = onStickyHomeChartChange,
            )
            if (!appearanceLocked) blurContent(false)
            RecentServingsCountItem(
                count = userSettings.recentServingsCount,
                onCountChange = onRecentServingsCountChange,
            )
            if (appearanceLocked) {
                PlusDivider(stringResource(R.string.plus_divider_label), openPlus)
                PaletteChoices(
                    palettes = AppColorPalette.entries.filter { it != AppColorPalette.DYNAMIC },
                    selectedPalette = userSettings.colorPalette,
                    dark = isDark,
                    onSelect = onColorPaletteChange,
                    locked = true,
                    onLocked = onLocked,
                )
                blurContent(true)
            }

        }
    }
}

@Composable
private fun PaletteChoices(
    palettes: List<AppColorPalette>,
    selectedPalette: AppColorPalette,
    dark: Boolean,
    onSelect: (AppColorPalette) -> Unit,
    locked: Boolean = false,
    onLocked: () -> Unit = {},
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(if (locked) R.string.appearance_more_palettes else R.string.appearance_color_palette_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.plusLockedContent(locked),
                )
            },
            modifier = Modifier.clip(MaterialTheme.shapes.large)
                .plusLockedSemantics(locked)
                .then(if (locked) Modifier.clickable { haptics.toggle(); onLocked() } else Modifier),
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier.plusLockedContent(locked),
                )
            },
            supportingContent = {
                // Plain scrollable Row: ListItem measures slots with intrinsics,
                // which LazyRow (SubcomposeLayout) cannot provide.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = 4.dp)
                        .plusLockedContent(locked),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    palettes.forEach { palette ->
                        val previewScheme = remember(palette, dark) {
                            if (palette == AppColorPalette.DYNAMIC) {
                                if (dark) dynamicDarkColorScheme(context)
                                else dynamicLightColorScheme(context)
                            } else {
                                colorSchemeForPalette(palette, dark)
                            }
                        }
                        ColorPaletteItem(
                            colorScheme = previewScheme,
                            isSelected = selectedPalette == palette,
                            name = stringResource(palette.labelRes()),
                            locked = locked,
                            onClick = {
                                haptics.toggle()
                                if (locked) onLocked() else onSelect(palette)
                            },
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
            ),
        )
    }
}

@Composable
private fun RecentServingsCountItem(
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    // Local drag state so the DataStore is written once per gesture, not on every frame.
    var displayedCount by remember(count) { mutableIntStateOf(count) }

    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.appearance_recent_servings_label),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = Modifier.clip(MaterialTheme.shapes.large),
        leadingContent = {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
            )
        },
        trailingContent = {
            Text(
                text = if (displayedCount == 0) {
                    stringResource(R.string.appearance_recent_servings_off)
                } else {
                    displayedCount.toString()
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = stringResource(R.string.appearance_recent_servings_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ExpressiveIntSlider(
                    value = displayedCount,
                    onValueChange = { displayedCount = it },
                    onValueChangeFinished = {
                        if (displayedCount != count) onCountChange(displayedCount)
                    },
                    valueRange = 0..MAX_RECENT_SERVINGS,
                    showStepMarks = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
        ),
    )
}

@Composable
private fun ColorPaletteItem(
    colorScheme: ColorScheme,
    isSelected: Boolean,
    name: String,
    onClick: () -> Unit,
    locked: Boolean = false,
) {
    val transition = updateTransition(targetState = isSelected, label = "palette_selection")
    val corner by transition.animateDp(label = "corner") { if (it) 28.dp else 12.dp }
    val backgroundColor by transition.animateColor(label = "bg") {
        if (it) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor by transition.animateColor(label = "text") {
        if (it) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 12.dp, bottomEnd = 12.dp))
            .drawBehind { drawRect(backgroundColor) }
            .plusLockedSemantics(locked)
            .semantics { selected = isSelected }
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Box(contentAlignment = Alignment.Center) {
                ColorPaletteSample()

                Crossfade(targetState = isSelected, label = "check_overlay") { selected ->
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = name,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ColorPaletteSample(modifier: Modifier = Modifier) {
    val circleSize = 80.dp
    val gap = 2.dp
    val halfSize = (circleSize - gap) / 2

    Box(
        modifier = modifier
            .size(circleSize)
            .clip(CircleShape),
    ) {
        Spacer(
            modifier = Modifier
                .size(halfSize)
                .align(Alignment.TopStart)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                ),
        )
        Spacer(
            modifier = Modifier
                .size(halfSize)
                .align(Alignment.TopEnd)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                ),
        )
        Spacer(
            modifier = Modifier
                .size(width = circleSize, height = halfSize)
                .align(Alignment.BottomCenter)
                .background(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                ),
        )
    }
}

@StringRes
private fun AppColorPalette.labelRes(): Int = when (this) {
    AppColorPalette.DYNAMIC -> R.string.appearance_color_palette_dynamic
    AppColorPalette.ESPRESSO -> R.string.appearance_color_palette_espresso
    AppColorPalette.MATCHA -> R.string.appearance_color_palette_matcha
    AppColorPalette.OCEAN -> R.string.appearance_color_palette_ocean
    AppColorPalette.SAKURA -> R.string.appearance_color_palette_sakura
}

package com.uc.caffeine.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
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
    val isDark = when (userSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

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
                            imageVector = when (userSettings.themeMode) {
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
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
                    shapes = segmentedListItemShapes(index = 0, count = 1),
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
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                        )
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.appearance_color_palette_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            items(AppColorPalette.entries) { palette ->
                                val previewScheme = remember(palette, isDark) {
                                    if (palette == AppColorPalette.DYNAMIC) {
                                        if (isDark) dynamicDarkColorScheme(context)
                                        else dynamicLightColorScheme(context)
                                    } else {
                                        colorSchemeForPalette(palette, isDark)
                                    }
                                }
                                ColorPaletteItem(
                                    colorScheme = previewScheme,
                                    isSelected = userSettings.colorPalette == palette,
                                    name = stringResource(palette.labelRes()),
                                    onClick = {
                                        haptics.toggle()
                                        onColorPaletteChange(palette)
                                    },
                                )
                            }
                        }
                    },
                    shapes = segmentedListItemShapes(index = 0, count = 1),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
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
                    shapes = segmentedListItemShapes(index = 0, count = 1),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ColorPaletteItem(
    colorScheme: ColorScheme,
    isSelected: Boolean,
    name: String,
    onClick: () -> Unit,
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
                                imageVector = Icons.Default.Check,
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

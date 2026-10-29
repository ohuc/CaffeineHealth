package com.uc.caffeine.widget

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.color.ColorProvider
import androidx.glance.material3.ColorProviders
import com.uc.caffeine.data.AppColorPalette
import com.uc.caffeine.ui.theme.colorSchemeForPalette

/**
 * The tonal surface roles Glance's [androidx.glance.color.ColorProviders] doesn't carry. Every entry
 * is a day/night pair, so the launcher flips them with the system theme without waking the app.
 */
internal class WidgetPalette(private val light: ColorScheme, private val dark: ColorScheme) {
    private fun pair(role: (ColorScheme) -> Color) = ColorProvider(day = role(light), night = role(dark))

    val background = pair { it.surfaceContainer }
    val tile = pair { it.surfaceContainerHighest }
    val iconContainer = pair { it.primaryContainer }
    val onIconContainer = pair { it.onPrimaryContainer }
    val accent = pair { it.primary }
    val onAccent = pair { it.onPrimary }
    val text = pair { it.onSurface }
    val textMuted = pair { it.onSurfaceVariant }
    val divider = pair { it.outlineVariant }
    val ringTrack = pair { it.surfaceVariant }
    val warning = pair { it.tertiary }
    val danger = pair { it.error }
}

internal val LocalWidgetPalette = staticCompositionLocalOf<WidgetPalette> {
    error("WidgetPalette is provided by WidgetTheme")
}

/** Follows the in-app color palette (Settings → Appearance), including dynamic color. */
@Composable
internal fun WidgetTheme(colorPalette: AppColorPalette, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val (light, dark) = remember(colorPalette) { resolveWidgetColorSchemes(context, colorPalette) }
    val palette = remember(light, dark) { WidgetPalette(light, dark) }
    CompositionLocalProvider(LocalWidgetPalette provides palette) {
        GlanceTheme(colors = ColorProviders(light = light, dark = dark), content = content)
    }
}

private fun resolveWidgetColorSchemes(
    context: Context,
    colorPalette: AppColorPalette,
): Pair<ColorScheme, ColorScheme> =
    if (colorPalette == AppColorPalette.DYNAMIC) {
        dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
    } else {
        colorSchemeForPalette(colorPalette, darkTheme = false) to
            colorSchemeForPalette(colorPalette, darkTheme = true)
    }

/** Shorthand so call sites read `palette.accent` rather than `LocalWidgetPalette.current.accent`. */
internal val widgetPalette: WidgetPalette
    @Composable get() = LocalWidgetPalette.current

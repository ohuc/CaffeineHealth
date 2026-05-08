package com.uc.caffeine.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.material3.ColorProviders
import com.uc.caffeine.ui.theme.DarkColorScheme
import com.uc.caffeine.ui.theme.LightColorScheme

private val FallbackColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

@Composable
internal fun WidgetTheme(useDynamicColor: Boolean, content: @Composable () -> Unit) {
    val colors = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        ColorProviders(
            light = dynamicLightColorScheme(context),
            dark = dynamicDarkColorScheme(context),
        )
    } else {
        FallbackColors
    }
    GlanceTheme(colors = colors, content = content)
}

/**
 * Resolves the same Material 3 [ColorScheme] used by [WidgetTheme] from a non-composable
 * context — useful when a widget needs theme colors before [provideContent] runs (e.g. to
 * tint a Bitmap rendered by [WidgetChartRenderer]).
 */
internal fun resolveWidgetColorScheme(context: Context, useDynamicColor: Boolean): ColorScheme {
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    return when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }
}

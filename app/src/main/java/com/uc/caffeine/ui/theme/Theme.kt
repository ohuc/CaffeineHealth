package com.uc.caffeine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

internal val DarkColorScheme = darkColorScheme(
    primary = EspressoPrimaryDark,
    onPrimary = OnEspressoPrimaryDark,
    primaryContainer = EspressoPrimaryContainerDark,
    onPrimaryContainer = OnEspressoPrimaryContainerDark,
    secondary = EspressoSecondaryDark,
    onSecondary = OnEspressoSecondaryDark,
    secondaryContainer = EspressoSecondaryContainerDark,
    onSecondaryContainer = OnEspressoSecondaryContainerDark,
    tertiary = EspressoTertiaryDark,
    onTertiary = OnEspressoTertiaryDark,
    tertiaryContainer = EspressoTertiaryContainerDark,
    onTertiaryContainer = OnEspressoTertiaryContainerDark,
    background = CaffeineBackgroundDark,
    onBackground = OnCaffeineBackgroundDark,
    surface = CaffeineSurfaceDark,
    onSurface = OnCaffeineSurfaceDark,
    surfaceVariant = CaffeineSurfaceVariantDark,
    onSurfaceVariant = OnCaffeineSurfaceVariantDark,
    outline = CaffeineOutlineDark,
    outlineVariant = CaffeineOutlineVariantDark,
    inverseSurface = CaffeineInverseSurfaceDark,
    inverseOnSurface = CaffeineInverseOnSurfaceDark,
    inversePrimary = CaffeineInversePrimaryDark,
    surfaceDim = CaffeineSurfaceDimDark,
    surfaceBright = CaffeineSurfaceBrightDark,
    surfaceContainerLowest = CaffeineSurfaceLowestDark,
    surfaceContainerLow = CaffeineSurfaceLowDark,
    surfaceContainer = CaffeineSurfaceContainerDark,
    surfaceContainerHigh = CaffeineSurfaceHighDark,
    surfaceContainerHighest = CaffeineSurfaceHighestDark,
)

internal val LightColorScheme = lightColorScheme(
    primary = EspressoPrimaryLight,
    onPrimary = OnEspressoPrimaryLight,
    primaryContainer = EspressoPrimaryContainerLight,
    onPrimaryContainer = OnEspressoPrimaryContainerLight,
    secondary = EspressoSecondaryLight,
    onSecondary = OnEspressoSecondaryLight,
    secondaryContainer = EspressoSecondaryContainerLight,
    onSecondaryContainer = OnEspressoSecondaryContainerLight,
    tertiary = EspressoTertiaryLight,
    onTertiary = OnEspressoTertiaryLight,
    tertiaryContainer = EspressoTertiaryContainerLight,
    onTertiaryContainer = OnEspressoTertiaryContainerLight,
    background = CaffeineBackgroundLight,
    onBackground = OnCaffeineBackgroundLight,
    surface = CaffeineSurfaceLight,
    onSurface = OnCaffeineSurfaceLight,
    surfaceVariant = CaffeineSurfaceVariantLight,
    onSurfaceVariant = OnCaffeineSurfaceVariantLight,
    outline = CaffeineOutlineLight,
    outlineVariant = CaffeineOutlineVariantLight,
    inverseSurface = CaffeineInverseSurfaceLight,
    inverseOnSurface = CaffeineInverseOnSurfaceLight,
    inversePrimary = CaffeineInversePrimaryLight,
    surfaceDim = CaffeineSurfaceDimLight,
    surfaceBright = CaffeineSurfaceBrightLight,
    surfaceContainerLowest = CaffeineSurfaceLowestLight,
    surfaceContainerLow = CaffeineSurfaceLowLight,
    surfaceContainer = CaffeineSurfaceContainerLight,
    surfaceContainerHigh = CaffeineSurfaceHighLight,
    surfaceContainerHighest = CaffeineSurfaceHighestLight,
)

@Composable
fun CaffeineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = {
            WarmUpTypographyFonts()
            ProvideVicoTheme(theme = rememberM3VicoTheme()) {
                content()
            }
        }
    )
}

object CaffeineSurfaceDefaults {
    val groupedListContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val groupedListDividerColor: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    val chartContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    val iconContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

    val detailPanelContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
}

@Composable
private fun WarmUpTypographyFonts() {
    val fontFamilyResolver = LocalFontFamilyResolver.current

    LaunchedEffect(fontFamilyResolver) {
        runCatching {
            fontFamilyResolver.preload(MontserratFamily)
        }
    }
}

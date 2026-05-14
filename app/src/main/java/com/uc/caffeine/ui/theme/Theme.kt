package com.uc.caffeine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
import com.uc.caffeine.data.AppColorPalette

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

internal val MatchaLightColorScheme = lightColorScheme(
    primary = MatchaPrimaryLight,
    onPrimary = OnMatchaPrimaryLight,
    primaryContainer = MatchaPrimaryContainerLight,
    onPrimaryContainer = OnMatchaPrimaryContainerLight,
    secondary = MatchaSecondaryLight,
    onSecondary = OnMatchaSecondaryLight,
    secondaryContainer = MatchaSecondaryContainerLight,
    onSecondaryContainer = OnMatchaSecondaryContainerLight,
    tertiary = MatchaTertiaryLight,
    onTertiary = OnMatchaTertiaryLight,
    tertiaryContainer = MatchaTertiaryContainerLight,
    onTertiaryContainer = OnMatchaTertiaryContainerLight,
    background = MatchaBackgroundLight,
    onBackground = OnMatchaBackgroundLight,
    surface = MatchaSurfaceLight,
    onSurface = OnMatchaSurfaceLight,
    surfaceVariant = MatchaSurfaceVariantLight,
    onSurfaceVariant = OnMatchaSurfaceVariantLight,
    outline = MatchaOutlineLight,
    outlineVariant = MatchaOutlineVariantLight,
    inverseSurface = MatchaInverseSurfaceLight,
    inverseOnSurface = MatchaInverseOnSurfaceLight,
    inversePrimary = MatchaInversePrimaryLight,
    surfaceDim = MatchaSurfaceDimLight,
    surfaceBright = MatchaSurfaceBrightLight,
    surfaceContainerLowest = MatchaSurfaceLowestLight,
    surfaceContainerLow = MatchaSurfaceLowLight,
    surfaceContainer = MatchaSurfaceContainerLight,
    surfaceContainerHigh = MatchaSurfaceHighLight,
    surfaceContainerHighest = MatchaSurfaceHighestLight,
)

internal val MatchaDarkColorScheme = darkColorScheme(
    primary = MatchaPrimaryDark,
    onPrimary = OnMatchaPrimaryDark,
    primaryContainer = MatchaPrimaryContainerDark,
    onPrimaryContainer = OnMatchaPrimaryContainerDark,
    secondary = MatchaSecondaryDark,
    onSecondary = OnMatchaSecondaryDark,
    secondaryContainer = MatchaSecondaryContainerDark,
    onSecondaryContainer = OnMatchaSecondaryContainerDark,
    tertiary = MatchaTertiaryDark,
    onTertiary = OnMatchaTertiaryDark,
    tertiaryContainer = MatchaTertiaryContainerDark,
    onTertiaryContainer = OnMatchaTertiaryContainerDark,
    background = MatchaBackgroundDark,
    onBackground = OnMatchaBackgroundDark,
    surface = MatchaSurfaceDark,
    onSurface = OnMatchaSurfaceDark,
    surfaceVariant = MatchaSurfaceVariantDark,
    onSurfaceVariant = OnMatchaSurfaceVariantDark,
    outline = MatchaOutlineDark,
    outlineVariant = MatchaOutlineVariantDark,
    inverseSurface = MatchaInverseSurfaceDark,
    inverseOnSurface = MatchaInverseOnSurfaceDark,
    inversePrimary = MatchaInversePrimaryDark,
    surfaceDim = MatchaSurfaceDimDark,
    surfaceBright = MatchaSurfaceBrightDark,
    surfaceContainerLowest = MatchaSurfaceLowestDark,
    surfaceContainerLow = MatchaSurfaceLowDark,
    surfaceContainer = MatchaSurfaceContainerDark,
    surfaceContainerHigh = MatchaSurfaceHighDark,
    surfaceContainerHighest = MatchaSurfaceHighestDark,
)

internal val OceanLightColorScheme = lightColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = OnOceanPrimaryLight,
    primaryContainer = OceanPrimaryContainerLight,
    onPrimaryContainer = OnOceanPrimaryContainerLight,
    secondary = OceanSecondaryLight,
    onSecondary = OnOceanSecondaryLight,
    secondaryContainer = OceanSecondaryContainerLight,
    onSecondaryContainer = OnOceanSecondaryContainerLight,
    tertiary = OceanTertiaryLight,
    onTertiary = OnOceanTertiaryLight,
    tertiaryContainer = OceanTertiaryContainerLight,
    onTertiaryContainer = OnOceanTertiaryContainerLight,
    background = OceanBackgroundLight,
    onBackground = OnOceanBackgroundLight,
    surface = OceanSurfaceLight,
    onSurface = OnOceanSurfaceLight,
    surfaceVariant = OceanSurfaceVariantLight,
    onSurfaceVariant = OnOceanSurfaceVariantLight,
    outline = OceanOutlineLight,
    outlineVariant = OceanOutlineVariantLight,
    inverseSurface = OceanInverseSurfaceLight,
    inverseOnSurface = OceanInverseOnSurfaceLight,
    inversePrimary = OceanInversePrimaryLight,
    surfaceDim = OceanSurfaceDimLight,
    surfaceBright = OceanSurfaceBrightLight,
    surfaceContainerLowest = OceanSurfaceLowestLight,
    surfaceContainerLow = OceanSurfaceLowLight,
    surfaceContainer = OceanSurfaceContainerLight,
    surfaceContainerHigh = OceanSurfaceHighLight,
    surfaceContainerHighest = OceanSurfaceHighestLight,
)

internal val OceanDarkColorScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    onPrimary = OnOceanPrimaryDark,
    primaryContainer = OceanPrimaryContainerDark,
    onPrimaryContainer = OnOceanPrimaryContainerDark,
    secondary = OceanSecondaryDark,
    onSecondary = OnOceanSecondaryDark,
    secondaryContainer = OceanSecondaryContainerDark,
    onSecondaryContainer = OnOceanSecondaryContainerDark,
    tertiary = OceanTertiaryDark,
    onTertiary = OnOceanTertiaryDark,
    tertiaryContainer = OceanTertiaryContainerDark,
    onTertiaryContainer = OnOceanTertiaryContainerDark,
    background = OceanBackgroundDark,
    onBackground = OnOceanBackgroundDark,
    surface = OceanSurfaceDark,
    onSurface = OnOceanSurfaceDark,
    surfaceVariant = OceanSurfaceVariantDark,
    onSurfaceVariant = OnOceanSurfaceVariantDark,
    outline = OceanOutlineDark,
    outlineVariant = OceanOutlineVariantDark,
    inverseSurface = OceanInverseSurfaceDark,
    inverseOnSurface = OceanInverseOnSurfaceDark,
    inversePrimary = OceanInversePrimaryDark,
    surfaceDim = OceanSurfaceDimDark,
    surfaceBright = OceanSurfaceBrightDark,
    surfaceContainerLowest = OceanSurfaceLowestDark,
    surfaceContainerLow = OceanSurfaceLowDark,
    surfaceContainer = OceanSurfaceContainerDark,
    surfaceContainerHigh = OceanSurfaceHighDark,
    surfaceContainerHighest = OceanSurfaceHighestDark,
)

internal val SakuraLightColorScheme = lightColorScheme(
    primary = SakuraPrimaryLight,
    onPrimary = OnSakuraPrimaryLight,
    primaryContainer = SakuraPrimaryContainerLight,
    onPrimaryContainer = OnSakuraPrimaryContainerLight,
    secondary = SakuraSecondaryLight,
    onSecondary = OnSakuraSecondaryLight,
    secondaryContainer = SakuraSecondaryContainerLight,
    onSecondaryContainer = OnSakuraSecondaryContainerLight,
    tertiary = SakuraTertiaryLight,
    onTertiary = OnSakuraTertiaryLight,
    tertiaryContainer = SakuraTertiaryContainerLight,
    onTertiaryContainer = OnSakuraTertiaryContainerLight,
    background = SakuraBackgroundLight,
    onBackground = OnSakuraBackgroundLight,
    surface = SakuraSurfaceLight,
    onSurface = OnSakuraSurfaceLight,
    surfaceVariant = SakuraSurfaceVariantLight,
    onSurfaceVariant = OnSakuraSurfaceVariantLight,
    outline = SakuraOutlineLight,
    outlineVariant = SakuraOutlineVariantLight,
    inverseSurface = SakuraInverseSurfaceLight,
    inverseOnSurface = SakuraInverseOnSurfaceLight,
    inversePrimary = SakuraInversePrimaryLight,
    surfaceDim = SakuraSurfaceDimLight,
    surfaceBright = SakuraSurfaceBrightLight,
    surfaceContainerLowest = SakuraSurfaceLowestLight,
    surfaceContainerLow = SakuraSurfaceLowLight,
    surfaceContainer = SakuraSurfaceContainerLight,
    surfaceContainerHigh = SakuraSurfaceHighLight,
    surfaceContainerHighest = SakuraSurfaceHighestLight,
)

internal val SakuraDarkColorScheme = darkColorScheme(
    primary = SakuraPrimaryDark,
    onPrimary = OnSakuraPrimaryDark,
    primaryContainer = SakuraPrimaryContainerDark,
    onPrimaryContainer = OnSakuraPrimaryContainerDark,
    secondary = SakuraSecondaryDark,
    onSecondary = OnSakuraSecondaryDark,
    secondaryContainer = SakuraSecondaryContainerDark,
    onSecondaryContainer = OnSakuraSecondaryContainerDark,
    tertiary = SakuraTertiaryDark,
    onTertiary = OnSakuraTertiaryDark,
    tertiaryContainer = SakuraTertiaryContainerDark,
    onTertiaryContainer = OnSakuraTertiaryContainerDark,
    background = SakuraBackgroundDark,
    onBackground = OnSakuraBackgroundDark,
    surface = SakuraSurfaceDark,
    onSurface = OnSakuraSurfaceDark,
    surfaceVariant = SakuraSurfaceVariantDark,
    onSurfaceVariant = OnSakuraSurfaceVariantDark,
    outline = SakuraOutlineDark,
    outlineVariant = SakuraOutlineVariantDark,
    inverseSurface = SakuraInverseSurfaceDark,
    inverseOnSurface = SakuraInverseOnSurfaceDark,
    inversePrimary = SakuraInversePrimaryDark,
    surfaceDim = SakuraSurfaceDimDark,
    surfaceBright = SakuraSurfaceBrightDark,
    surfaceContainerLowest = SakuraSurfaceLowestDark,
    surfaceContainerLow = SakuraSurfaceLowDark,
    surfaceContainer = SakuraSurfaceContainerDark,
    surfaceContainerHigh = SakuraSurfaceHighDark,
    surfaceContainerHighest = SakuraSurfaceHighestDark,
)

internal fun colorSchemeForPalette(palette: AppColorPalette, darkTheme: Boolean): ColorScheme =
    when (palette) {
        AppColorPalette.DYNAMIC, AppColorPalette.ESPRESSO -> if (darkTheme) DarkColorScheme else LightColorScheme
        AppColorPalette.MATCHA -> if (darkTheme) MatchaDarkColorScheme else MatchaLightColorScheme
        AppColorPalette.OCEAN -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
        AppColorPalette.SAKURA -> if (darkTheme) SakuraDarkColorScheme else SakuraLightColorScheme
    }

@Composable
fun CaffeineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPalette: AppColorPalette = AppColorPalette.DYNAMIC,
    content: @Composable () -> Unit
) {
    val colorScheme = if (colorPalette == AppColorPalette.DYNAMIC) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        colorSchemeForPalette(colorPalette, darkTheme)
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

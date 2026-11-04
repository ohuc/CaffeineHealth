package com.uc.caffeine.monetization

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.AppColorPalette
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.ui.screens.analytics.AnalyticsRangePicker
import com.uc.caffeine.ui.screens.settings.AppearanceSettingsScreen
import com.uc.caffeine.ui.screens.settings.ConnectionsSettingsScreen
import com.uc.caffeine.ui.screens.settings.LockedCoachSettings
import com.uc.caffeine.ui.theme.CaffeineTheme
import com.uc.caffeine.ui.components.SettingsNavGroup
import com.uc.caffeine.ui.components.SettingsNavItem
import kotlinx.coroutines.flow.MutableStateFlow

internal class SeparationPreviewAccess(snapshot: AccessSnapshot, private val policy: Boolean) : FeatureAccess {
    override val snapshots = MutableStateFlow(snapshot)
    override fun canUse(feature: ProFeature) = FeaturePolicy.canUse(snapshots.value, feature, policy)
}

internal data class SeparationPreviewCase(val name: String, val source: AccessSource, val policy: Boolean,
    val saved: Boolean = false, val palette: AppColorPalette = AppColorPalette.ESPRESSO, val dark: Boolean = false) {
    fun access() = SeparationPreviewAccess(AccessSnapshot(
        if (source == AccessSource.FOSS) Distribution.FOSS else Distribution.PLAY, source, VerificationStatus.VERIFIED), policy)
    override fun toString() = name
}

internal class SeparationPreviewProvider : PreviewParameterProvider<SeparationPreviewCase> {
    override val values = sequenceOf(
        SeparationPreviewCase("Locked Free", AccessSource.NONE, true),
        SeparationPreviewCase("Locked saved enabled", AccessSource.NONE, true, saved = true),
        SeparationPreviewCase("FOSS", AccessSource.FOSS, true),
        SeparationPreviewCase("Policy off", AccessSource.NONE, false),
        SeparationPreviewCase("Free day", AccessSource.PREVIEW, true),
        SeparationPreviewCase("Subscribed", AccessSource.SUBSCRIPTION, true),
        SeparationPreviewCase("Lifetime", AccessSource.LIFETIME, true),
    )
}
internal class SeparationPaletteProvider : PreviewParameterProvider<SeparationPreviewCase> {
    override val values = listOf(AppColorPalette.ESPRESSO, AppColorPalette.MATCHA, AppColorPalette.OCEAN, AppColorPalette.SAKURA)
        .asSequence().flatMap { palette -> sequenceOf(false, true).map { dark ->
            SeparationPreviewCase("$palette dark=$dark", AccessSource.NONE, true, true, palette, dark)
        } }
}

@Composable
internal fun SeparationPreviewTheme(case: SeparationPreviewCase, content: @Composable () -> Unit) {
    CaffeineTheme(darkTheme = case.dark, colorPalette = case.palette) {
        Surface { ProvidePlusSettings(case.access(), {}, {}, content) }
    }
}

@Preview(name = "Divider and saved locks", widthDp = 360, showBackground = true)
@Preview(name = "Divider and saved locks large text", widthDp = 360, fontScale = 2f, showBackground = true)
@Composable
private fun SeparationKitPreview(@PreviewParameter(SeparationPaletteProvider::class) case: SeparationPreviewCase) {
    SeparationPreviewTheme(case) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlusDivider(stringResource(R.string.plus_divider_label), {})
                SettingsNavGroup(listOf(SettingsNavItem(stringResource(R.string.plus_feature_health),
                    stringResource(R.string.settings_health_connect_summary), Icons.Rounded.Favorite, {}, locked = true, savedEnabled = true)))
                LockedCoachSettings(UserSettings(caffeineCoachEnabled = true, coachToleranceEnabled = true), {}, {}, {})
            }
        }
    }
}

@Preview(name = "Appearance access states", widthDp = 360, heightDp = 800)
@Preview(name = "Appearance large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun AppearanceSeparationPreview(@PreviewParameter(SeparationPreviewProvider::class) case: SeparationPreviewCase) {
    SeparationPreviewTheme(case) {
        AppearanceSettingsScreen(UserSettings(colorPalette = if (case.saved) AppColorPalette.MATCHA else AppColorPalette.DYNAMIC,
            expressiveBlur = case.saved, themeMode = if (case.dark) ThemeMode.DARK else ThemeMode.LIGHT), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Appearance palette schemes", widthDp = 360, heightDp = 800)
@Preview(name = "Appearance palette schemes large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun AppearancePalettePreview(@PreviewParameter(SeparationPaletteProvider::class) case: SeparationPreviewCase) {
    SeparationPreviewTheme(case) {
        AppearanceSettingsScreen(UserSettings(colorPalette = case.palette, expressiveBlur = true,
            themeMode = if (case.dark) ThemeMode.DARK else ThemeMode.LIGHT), {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Connections access and saved states", widthDp = 360, heightDp = 800)
@Composable
private fun ConnectionsSeparationPreview(@PreviewParameter(SeparationPreviewProvider::class) case: SeparationPreviewCase) {
    SeparationPreviewTheme(case) {
        ConnectionsSettingsScreen(UserSettings(healthConnectEnabled = case.saved, automationEnabled = case.saved), {}, {}, {}, {})
    }
}

@Preview(name = "Analytics access states", widthDp = 360, showBackground = true)
@Preview(name = "Analytics large text", widthDp = 360, showBackground = true, fontScale = 2f)
@Composable
private fun AnalyticsSeparationPreview(@PreviewParameter(SeparationPreviewProvider::class) case: SeparationPreviewCase) {
    SeparationPreviewTheme(case) { Surface { Column(Modifier.padding(16.dp)) {
        AnalyticsRangePicker(com.uc.caffeine.util.AnalyticsRange.LAST_7_DAYS) {}
    } } }
}

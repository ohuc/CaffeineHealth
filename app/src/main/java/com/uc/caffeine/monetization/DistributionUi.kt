package com.uc.caffeine.monetization

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uc.caffeine.R

/** Name and launcher icon shown in Settings -> About. */
data class AboutBrand(val name: String, @param:DrawableRes val icon: Int)

interface DistributionUi {
    /** Highlighted entry at the very top of Settings. FOSS shows none. */
    @Composable fun SettingsEntry(onOpen: () -> Unit) {}
    /** Full-screen Plus page, slid up over the whole app (including the toolbar). */
    @Composable fun PlusPage(showcase: Boolean, onDismiss: () -> Unit) {}
    /** App-level full-screen layers owned by the edition (e.g. the app-icon switch screen). */
    @Composable fun Overlays() {}
    @Composable fun FeatureSheet(feature: ProFeature, onSeePlans: () -> Unit, onDismiss: () -> Unit) {}
    @Composable fun StatusCard(onOpen: () -> Unit) {}
    /** One-time journey sheets (free-day offer, free-day follow-up). */
    @Composable fun JourneySheet(prompt: JourneyPrompt, onSeePlans: () -> Unit) {}
    /** Caffeine Health+ introduction inside first-run setup. Only offered when [JourneyState.onboardingIntroEligible]. */
    @Composable fun OnboardingPlusIntro(onContinue: () -> Unit, onSeePlans: () -> Unit) {}
    /**
     * Cloud backup controls at the end of Settings → My data. [onRestore] hands a downloaded backup
     * to the normal import, which asks merge or replace. FOSS shows none.
     */
    @Composable fun CloudBackupSettings(onRestore: (String) -> Unit) {}
    /**
     * Restore action on the first setup screen, for returning users. [onRestore] replaces local data
     * with the backup (which also finishes setup) and reports whether it worked. FOSS shows none.
     */
    @Composable fun OnboardingRestore(onRestore: suspend (String) -> Boolean) {}
    /** App icon choice inside Look & feel. Play only. */
    @Composable fun AppIconSettings(onOpenPlus: () -> Unit) {}
    @Composable fun aboutBrand(): AboutBrand = AboutBrand(stringResource(R.string.app_name), R.mipmap.ic_launcher)
    /** Hidden developer page, opened by double-tapping the app icon in About. */
    val hasDeveloperSettings: Boolean get() = false
    @Composable fun DeveloperSettings() {}
}

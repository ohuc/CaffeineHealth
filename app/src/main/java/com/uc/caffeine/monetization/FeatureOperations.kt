package com.uc.caffeine.monetization

import com.uc.caffeine.data.AppColorPalette
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.util.AnalyticsRange

inline fun <T> FeatureAccess.withFeature(feature: ProFeature, operation: () -> T): T? =
    if (canUse(feature)) operation() else null

fun UserSettings.withEffectiveAccess(access: FeatureAccess): UserSettings {
    val health = access.canUse(ProFeature.HealthConnect)
    val appearance = access.canUse(ProFeature.AdvancedAppearance)
    return copy(
        healthConnectEnabled = healthConnectEnabled && health,
        hcSleepEnabled = hcSleepEnabled && health,
        hcSleepTimeHour = hcSleepTimeHour.takeIf { health },
        hcSleepTimeMinute = hcSleepTimeMinute.takeIf { health },
        automationEnabled = automationEnabled && access.canUse(ProFeature.Automation),
        caffeineCoachEnabled = caffeineCoachEnabled && access.canUse(ProFeature.Coach),
        colorPalette = if (appearance) colorPalette else AppColorPalette.DYNAMIC,
        expressiveBlur = expressiveBlur && appearance,
    )
}

fun AnalyticsRange.requiresPlus(): Boolean = this in setOf(AnalyticsRange.LAST_30_DAYS, AnalyticsRange.LAST_90_DAYS, AnalyticsRange.CUSTOM)

fun effectiveAnalyticsRange(range: AnalyticsRange, access: FeatureAccess): AnalyticsRange =
    if (range.requiresPlus() && !access.canUse(ProFeature.ExtendedAnalytics)) AnalyticsRange.LAST_7_DAYS else range

fun canRenderWidget(kind: String, access: FeatureAccess): Boolean =
    kind !in setOf("ring", "combined") || access.canUse(ProFeature.PremiumWidgets)

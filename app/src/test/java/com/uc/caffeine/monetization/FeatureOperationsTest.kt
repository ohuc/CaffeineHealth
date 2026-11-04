package com.uc.caffeine.monetization

import com.uc.caffeine.data.AppColorPalette
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.util.AnalyticsRange
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class FeatureOperationsTest {
    private val denied = object : FeatureAccess {
        override val snapshots = MutableStateFlow(AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED))
        override fun canUse(feature: ProFeature) = false
    }
    @Test fun `Coach gate does not execute insight generation`() {
        var generated = false
        assertNull(denied.withFeature(ProFeature.Coach) { generated = true; "private insight" })
        assertFalse(generated)
    }
    @Test fun `appearance fallback retains saved preference and free profile`() {
        val saved = UserSettings(colorPalette = AppColorPalette.MATCHA, expressiveBlur = true,
            halfLifeMinutes = 420, hcSleepEnabled = true, hcSleepTimeHour = 22, hcSleepTimeMinute = 15)
        val effective = saved.withEffectiveAccess(denied)
        assertEquals(AppColorPalette.DYNAMIC, effective.colorPalette)
        assertFalse(effective.expressiveBlur)
        assertFalse(effective.hcSleepEnabled)
        assertNull(effective.hcSleepTimeHour)
        assertEquals(420, effective.halfLifeMinutes)
        assertEquals(AppColorPalette.MATCHA, saved.colorPalette)
        assertTrue(saved.hcSleepEnabled)
    }
    @Test fun `free analytics clamps extended aggregation to seven days`() {
        assertEquals(AnalyticsRange.LAST_7_DAYS, effectiveAnalyticsRange(AnalyticsRange.LAST_90_DAYS, denied))
        assertEquals(AnalyticsRange.TODAY, effectiveAnalyticsRange(AnalyticsRange.TODAY, denied))
        assertEquals(AnalyticsRange.YESTERDAY, effectiveAnalyticsRange(AnalyticsRange.YESTERDAY, denied))
    }
    @Test fun `only Ring and Combined require paid widget access`() {
        assertFalse(canRenderWidget("ring", denied))
        assertFalse(canRenderWidget("combined", denied))
        assertTrue(canRenderWidget("now", denied))
        assertTrue(canRenderWidget("quick_add", denied))
    }
}

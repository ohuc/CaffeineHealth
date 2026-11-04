package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class PlusSettingStateTest {
    private class Access(var allowed: Boolean) : FeatureAccess {
        override val snapshots = MutableStateFlow(AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED))
        override fun canUse(feature: ProFeature) = allowed
    }

    @Test fun `presentation uses feature access even when snapshot has no paid source`() {
        val access = Access(true)
        assertFalse(access.plusSettingState(ProFeature.AdvancedAppearance).locked)
        access.allowed = false
        assertTrue(access.plusSettingState(ProFeature.AdvancedAppearance).locked)
    }
    @Test fun `locked saved Health Connect stays reachable for disconnect`() {
        val state = Access(false).plusSettingState(ProFeature.HealthConnect, savedEnabled = true)
        assertTrue(state.locked)
        assertTrue(state.canOpenSettings)
        assertTrue(state.savedEnabled)
    }
    @Test fun `locked saved Automation stays reachable for turning off`() {
        assertTrue(Access(false).plusSettingState(ProFeature.Automation, savedEnabled = true).canOpenSettings)
    }
    @Test fun `locked saved Coach retains an off action`() {
        val state = Access(false).plusSettingState(ProFeature.Coach, savedEnabled = true)
        assertTrue(state.locked)
        assertTrue(state.canTurnOff)
    }
    @Test fun `locked unset features explain access before navigating or enabling`() {
        ProFeature.entries.forEach {
            val state = Access(false).plusSettingState(it)
            assertTrue(state.locked)
            assertFalse(state.canOpenSettings)
            assertFalse(state.canTurnOff)
        }
    }
    @Test fun `available features navigate even with saved setting off`() {
        ProFeature.entries.forEach { assertTrue(Access(true).plusSettingState(it).canOpenSettings) }
    }
    @Test fun `FOSS policy-off free-day subscription and lifetime have no locked presentation`() {
        val snapshots = listOf(
            AccessSnapshot(Distribution.FOSS, AccessSource.FOSS, VerificationStatus.NOT_APPLICABLE) to true,
            AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED) to false,
            AccessSnapshot(Distribution.PLAY, AccessSource.PREVIEW, VerificationStatus.VERIFIED) to true,
            AccessSnapshot(Distribution.PLAY, AccessSource.SUBSCRIPTION, VerificationStatus.VERIFIED) to true,
            AccessSnapshot(Distribution.PLAY, AccessSource.LIFETIME, VerificationStatus.VERIFIED) to true,
        )
        snapshots.forEach { (snapshot, enforce) ->
            val access = object : FeatureAccess {
                override val snapshots = MutableStateFlow(snapshot)
                override fun canUse(feature: ProFeature) = FeaturePolicy.canUse(snapshot, feature, enforce)
            }
            ProFeature.entries.forEach { assertFalse(access.plusSettingState(it).locked) }
        }
    }
}

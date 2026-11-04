package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class PlayFoundationTest {
    @Test fun `operation readiness waits for local state rather than a network result`() = runBlocking {
        val access = PlayFeatureAccess(enforceLocks = true)
        access.beginLoading()
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { access.awaitReady(); "ready" }
        assertFalse(waiting.isCompleted)
        access.finishLoading()
        assertEquals("ready", waiting.await())
    }
    @Test fun `Play keeps current features available without claiming a purchase`() {
        val services = FlavorServices.create()
        ProFeature.entries.forEach { assertTrue(services.featureAccess.canUse(it)) }
        assertEquals(Distribution.PLAY, services.distribution)
        assertEquals(AccessSource.NONE, services.featureAccess.snapshots.value.source)
        assertEquals(VerificationStatus.UNVERIFIED, services.featureAccess.snapshots.value.verification)
        assertFalse(services.featureAccess.snapshots.value.hasVerifiedPurchase)
        assertEquals(StoreAvailability.NOT_CONFIGURED, services.store.snapshots.value.availability)
        assertTrue(services.store.snapshots.value.plans.isEmpty())
    }
}

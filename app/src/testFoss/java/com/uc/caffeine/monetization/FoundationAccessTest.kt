package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Test

class FoundationAccessTest {
    @Test fun `FOSS local access never depends on purchase verification`() {
        val services = FlavorServices.create()
        val access = services.featureAccess
        ProFeature.entries.forEach { assertTrue(access.canUse(it)) }
        assertEquals(AccessSource.FOSS, access.snapshots.value.source)
        assertFalse(access.snapshots.value.hasVerifiedPurchase)
        assertEquals(Distribution.FOSS, services.distribution)
        assertEquals(StoreAvailability.NOT_SUPPORTED, services.store.snapshots.value.availability)
        assertTrue(services.store.snapshots.value.plans.isEmpty())
    }
}

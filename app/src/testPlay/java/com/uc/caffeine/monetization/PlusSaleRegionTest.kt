package com.uc.caffeine.monetization

import com.uc.caffeine.R
import org.junit.Assert.*
import org.junit.Test

class PlusSaleRegionTest {
    @Test fun `India only sells to India`() {
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("IN", "IN"))
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("IN", "in"))
        assertEquals(SaleRegion.CLOSED, PlusSaleRegion.resolve("IN", "US"))
        assertEquals(SaleRegion.CLOSED, PlusSaleRegion.resolve("IN", "DE"))
    }

    @Test fun `unknown or malformed country is never treated as on sale`() {
        assertEquals(SaleRegion.UNKNOWN, PlusSaleRegion.resolve("IN", null))
        assertEquals(SaleRegion.UNKNOWN, PlusSaleRegion.resolve("IN", ""))
        assertEquals(SaleRegion.UNKNOWN, PlusSaleRegion.resolve("IN", "IND"))
        assertEquals(SaleRegion.UNKNOWN, PlusSaleRegion.resolve("IN", "1N"))
    }

    @Test fun `everywhere opens all countries even before the country is known`() {
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("*", "US"))
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("*", null))
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("IN, *", "BR"))
    }

    @Test fun `lists are case and space tolerant`() {
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve(" in , us ", "US"))
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("in;gb", "GB"))
        assertEquals(SaleRegion.CLOSED, PlusSaleRegion.resolve("IN,US", "GB"))
    }

    @Test fun `blank list means no restriction`() {
        assertEquals(SaleRegion.OPEN, PlusSaleRegion.resolve("", "US"))
    }

    @Test fun `remote list overrides the build default only when set`() {
        assertEquals("*", PlusSaleRegion.effectiveSpec("*", "IN"))
        assertEquals("IN", PlusSaleRegion.effectiveSpec(null, "IN"))
        assertEquals("IN", PlusSaleRegion.effectiveSpec("  ", "IN"))
    }

    @Test fun `closed country gets an honest neutral page with no plans, checkout or restore`() {
        val free = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED)
        val yearly = StorePlan("year", PlanDuration.YEARLY, "sample yearly")
        val store = StoreSnapshot(StoreAvailability.AVAILABLE, listOf(yearly), true, false)
        val model = PlusPageModel.from(free, store, JourneyState(), ProScreenState.from(free, store, yearly.id),
            showcase = false, purchasesActive = true, saleRegionOpen = false)
        assertEquals(PlusLayout.PITCH, model.layout)
        assertNull(model.plans)
        assertEquals(PlusCta.None, model.cta)
        assertFalse(model.showRestore)
        assertEquals(PlusStrip(R.string.plus_region_closed, PlusSeverity.NEUTRAL), model.strip)
    }

    @Test fun `build without purchases keeps the version message even in a closed country`() {
        val free = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED)
        val store = StoreSnapshot(StoreAvailability.AVAILABLE, emptyList(), true, false)
        val model = PlusPageModel.from(free, store, JourneyState(), ProScreenState.from(free, store, null),
            showcase = false, purchasesActive = false, saleRegionOpen = false)
        assertEquals(R.string.plus_release_off_neutral, model.strip?.text)
    }
}

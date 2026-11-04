package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Test

class ProScreenStateTest {
    private val free = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED)
    private val plans = listOf(StorePlan("annual", PlanDuration.YEARLY, "€11,99"))
    private val store = StoreSnapshot(StoreAvailability.AVAILABLE, plans, ownershipLoaded = true, purchasesEnabled = true)
    @Test fun `checkout requires a live plan and fresh ownership`() {
        assertTrue(ProScreenState.from(free, store, "annual").canPurchase)
        assertFalse(ProScreenState.from(free, store.copy(plans = emptyList()), "annual").canPurchase)
        assertFalse(ProScreenState.from(free, store.copy(ownershipLoaded = false), "annual").canPurchase)
        assertFalse(ProScreenState.from(free.copy(verification = VerificationStatus.ERROR), store, "annual").canPurchase)
    }
    @Test fun `unavailable prices never enable stale selection`() {
        assertFalse(ProScreenState.from(free, store.copy(availability = StoreAvailability.UNAVAILABLE), "annual").canPurchase)
        assertFalse(ProScreenState.from(free, store, "removed-plan").canPurchase)
    }
    @Test fun `pending and in flight operations prevent duplicate checkout`() {
        StoreOperation.entries.filter { it != StoreOperation.IDLE }.forEach {
            assertFalse(ProScreenState.from(free, store.copy(operation = it), "annual").canPurchase)
        }
    }
    @Test fun `lifetime hides plans but preserves mixed subscription management`() {
        val lifetime = free.copy(source = AccessSource.LIFETIME, manageSubscription = true)
        val state = ProScreenState.from(lifetime, store, "annual")
        assertFalse(state.showPlans)
        assertFalse(state.canPurchase)
        assertTrue(state.showManagement)
    }
    @Test fun `canceled paid period does not expose Lifetime purchase`() {
        val state = ProScreenState.from(free.copy(source = AccessSource.SUBSCRIPTION, willRenew = false), store, "annual")
        assertFalse(state.canPurchase)
    }
    @Test fun `known renewing ownership blocks purchases even if entitlement mapping is missing`() {
        val state = ProScreenState.from(free.copy(manageSubscription = true, willRenew = true), store, "annual")
        assertFalse(state.showPlans)
        assertFalse(state.canPurchase)
        assertTrue(state.showManagement)
    }
    @Test fun `active canceled subscription still blocks Lifetime when entitlement mapping is missing`() {
        val state = ProScreenState.from(free.copy(activeSubscription = true, manageSubscription = true, willRenew = false), store, "annual")
        assertFalse(state.showPlans)
        assertFalse(state.canPurchase)
        assertTrue(state.showManagement)
    }
    @Test fun `release approval switch disables checkout`() {
        assertFalse(ProScreenState.from(free, store.copy(purchasesEnabled = false), "annual").canPurchase)
    }
}

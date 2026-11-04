package com.uc.caffeine.monetization

import com.uc.caffeine.R
import org.junit.Assert.*
import org.junit.Test

class PlusPageModelTest {
    private val monthly = StorePlan("month", PlanDuration.MONTHLY, "sample monthly")
    private val yearly = StorePlan("year", PlanDuration.YEARLY, "sample yearly")
    private val lifetime = StorePlan("once", PlanDuration.LIFETIME, "sample once")
    private val free = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED)
    private val ready = StoreSnapshot(StoreAvailability.AVAILABLE, listOf(monthly, lifetime, yearly), true, true)
    private val paid = free.copy(source = AccessSource.SUBSCRIPTION, activeSubscription = true,
        manageSubscription = true, willRenew = true, paidExpiresAtMillis = 123456789L)

    private fun page(access: AccessSnapshot = free, store: StoreSnapshot = ready,
        showcase: Boolean = false, trial: Boolean = false, active: Boolean = true,
        selected: String? = yearly.id) = PlusPageModel.from(access, store,
        JourneyState(canStartTrial = trial), ProScreenState.from(access, store, selected), showcase, active)

    @Test fun `S1 showcase sorts catalog and keeps free exit without trial noise`() {
        val model = page(showcase = true, trial = true)
        assertEquals(PlusLayout.PITCH, model.layout)
        assertEquals(listOf(yearly, monthly, lifetime), (model.plans as PlusPlans.Ready).plans)
        assertEquals(PlusCta.Subscribe(yearly), model.cta)
        assertEquals(R.string.plus_continue, model.dismissLabel)
        assertFalse(model.freeDay)
        assertNull(model.chip)
        assertNull(model.strip)
    }
    @Test fun `S2 settings free user gets pitch and footer recovery`() {
        val model = page()
        assertEquals(PlusLayout.PITCH, model.layout)
        assertTrue(model.showRestore)
        assertTrue(model.restoreEnabled)
        assertFalse(model.freeDay)
        assertEquals(PlusCta.Subscribe(yearly), model.cta)
    }
    @Test fun `S3 catalog or ownership loading always has placeholders and explicit disabled reason`() {
        listOf(ready.copy(availability = StoreAvailability.LOADING), ready.copy(ownershipLoaded = false)).forEach {
            val model = page(store = it)
            assertEquals(PlusPlans.Loading, model.plans)
            assertEquals(PlusCta.Disabled(R.string.plus_cta_loading, true), model.cta)
            assertEquals(R.string.plus_continue, model.dismissLabel)
        }
    }
    @Test fun `S4 visible stale plans offer refresh rather than silently disabled checkout`() {
        listOf(VerificationStatus.CACHED, VerificationStatus.ERROR, VerificationStatus.UNVERIFIED).forEach {
            val model = page(access = free.copy(verification = it))
            assertTrue(model.plans is PlusPlans.Ready)
            assertEquals(PlusCta.RefreshToContinue, model.cta)
        }
    }
    @Test fun `S5 unavailable unsupported and unconfigured stores omit plans and keep retry`() {
        listOf(StoreAvailability.UNAVAILABLE, StoreAvailability.NOT_CONFIGURED, StoreAvailability.NOT_SUPPORTED).forEach {
            val model = page(store = ready.copy(availability = it))
            assertNull(model.plans)
            assertEquals(PlusCta.None, model.cta)
            assertEquals(PlusSeverity.NEUTRAL, model.strip?.severity)
            assertTrue(model.strip!!.actions.contains(PlusAction.REFRESH))
        }
    }
    @Test fun `S6 inactive purchases hide prices and restore without an error banner`() {
        val model = page(active = false)
        assertNull(model.plans)
        assertEquals(PlusCta.None, model.cta)
        assertFalse(model.showRestore)
        assertEquals(R.string.plus_release_off_neutral, model.strip?.text)
        assertEquals(PlusSeverity.NEUTRAL, model.strip?.severity)
    }
    @Test fun `S7 active operations explain disabled checkout and lock plan selection`() {
        listOf(StoreOperation.PURCHASING, StoreOperation.RESTORING, StoreOperation.VERIFYING).forEach {
            val model = page(store = ready.copy(operation = it))
            assertTrue(model.cta is PlusCta.Disabled)
            assertEquals(PlusSeverity.PROGRESS, model.strip?.severity)
            assertFalse((model.plans as PlusPlans.Ready).enabled)
        }
    }
    @Test fun `S8 pending payment is neutral and cannot start another purchase`() {
        val model = page(store = ready.copy(operation = StoreOperation.PENDING))
        assertEquals(PlusCta.Disabled(R.string.plus_cta_pending), model.cta)
        assertEquals(PlusSeverity.NEUTRAL, model.strip?.severity)
        assertFalse((model.plans as PlusPlans.Ready).enabled)
    }
    @Test fun `S9 real store failures are recoverable problem strips`() {
        listOf(StoreMessage.NETWORK, StoreMessage.STORE, StoreMessage.NOT_ALLOWED, StoreMessage.UNKNOWN).forEach {
            val model = page(store = ready.copy(message = it))
            assertEquals(PlusSeverity.PROBLEM, model.strip?.severity)
            assertEquals(listOf(PlusAction.REFRESH), model.strip?.actions)
        }
    }
    @Test fun `S10 restore results distinguish restored access from no purchases`() {
        assertEquals(PlusSeverity.POSITIVE, page(store = ready.copy(message = StoreMessage.RESTORED)).strip?.severity)
        assertEquals(PlusSeverity.NEUTRAL, page(store = ready.copy(message = StoreMessage.NOTHING_TO_RESTORE)).strip?.severity)
        assertEquals(PlusSeverity.POSITIVE, page(store = ready.copy(message = StoreMessage.SUCCESS)).strip?.severity)
    }
    @Test fun `S11 free day has end fact and immediate billing disclosure`() {
        val model = page(access = free.copy(source = AccessSource.PREVIEW, previewEndsAtMillis = 987L))
        assertEquals(PlusChip(R.string.plus_preview_until, 987L, true), model.chip)
        assertTrue(model.previewPurchaseNote)
        assertEquals(R.string.plus_keep_preview, model.dismissLabel)
        assertEquals(R.string.plus_continue, page(access = free.copy(source = AccessSource.PREVIEW), showcase = true).dismissLabel)
    }
    @Test fun `S12 eligible free day appears only outside showcase and without ownership`() {
        assertTrue(page(trial = true).freeDay)
        assertFalse(page(trial = true, showcase = true).freeDay)
        assertFalse(page(access = paid, trial = true).freeDay)
    }
    @Test fun `S13 expired access is a neutral pitch with full free exit`() {
        val model = page(access = free.copy(expired = true))
        assertEquals(PlusLayout.PITCH, model.layout)
        assertEquals(R.string.plus_status_expired, model.strip?.text)
        assertEquals(PlusSeverity.NEUTRAL, model.strip?.severity)
    }
    @Test fun `O1 subscribed owner has included list and management without checkout`() {
        val model = page(access = paid)
        assertEquals(PlusLayout.OWNER, model.layout)
        assertTrue(model.included)
        assertTrue(model.showManage)
        assertTrue(model.showRestore)
        assertEquals(PlusChip(R.string.plus_status_until, paid.paidExpiresAtMillis), model.chip)
        assertNull(model.plans)
        assertEquals(PlusCta.None, model.cta)
        assertNull(model.strip)
    }
    @Test fun `O2 canceled subscription shows access end without claiming renewal`() {
        val model = page(access = paid.copy(willRenew = false))
        assertEquals(R.string.plus_status_canceled_until, model.chip?.text)
        assertEquals(paid.paidExpiresAtMillis, model.chip?.timestamp)
        assertTrue(model.showManage)
    }
    @Test fun `O3 lifetime has thank you fact and no unrelated subscription management`() {
        val model = page(access = free.copy(source = AccessSource.LIFETIME))
        assertEquals(PlusLayout.OWNER, model.layout)
        assertEquals(R.string.plus_status_lifetime, model.chip?.text)
        assertTrue(model.included)
        assertFalse(model.showManage)
    }
    @Test fun `O4 mixed ownership calls attention to renewing subscription`() {
        val model = page(access = paid.copy(source = AccessSource.LIFETIME))
        assertEquals(R.string.plus_mixed_ownership, model.strip?.text)
        assertEquals(listOf(PlusAction.MANAGE), model.strip?.actions)
        assertTrue(model.showManage)
    }
    @Test fun `O5 billing problem offers manage action on owner page`() {
        val model = page(access = paid.copy(billingIssue = true))
        assertEquals(PlusLayout.OWNER, model.layout)
        assertEquals(R.string.plus_status_billing, model.strip?.text)
        assertEquals(PlusSeverity.PROBLEM, model.strip?.severity)
        assertEquals(listOf(PlusAction.MANAGE), model.strip?.actions)
    }
    @Test fun `O6 unverified owner never claims included benefits or offers duplicate checkout`() {
        val model = page(access = paid.copy(source = AccessSource.NONE, verification = VerificationStatus.ERROR))
        assertEquals(PlusLayout.OWNER, model.layout)
        assertFalse(model.included)
        assertFalse(model.iconOffer)
        assertNull(model.chip)
        assertNull(model.plans)
        assertEquals(R.string.plus_short_unverified, model.ownerSubtitle)
        assertEquals(R.string.plus_status_unverified, model.strip?.text)
        assertEquals(listOf(PlusAction.REFRESH, PlusAction.RESTORE), model.strip?.actions)
    }
    @Test fun `O7 verified ownership alone qualifies for app icon offer`() {
        assertTrue(page(access = paid).iconOffer)
        assertTrue(page(access = free.copy(source = AccessSource.LIFETIME)).iconOffer)
        assertFalse(page(access = free.copy(source = AccessSource.PREVIEW)).iconOffer)
    }
    @Test fun `O8 owner restoration uses progress strip without purchase bar`() {
        val model = page(access = paid, store = ready.copy(operation = StoreOperation.RESTORING))
        assertEquals(PlusLayout.OWNER, model.layout)
        assertEquals(PlusSeverity.PROGRESS, model.strip?.severity)
        assertEquals(PlusCta.None, model.cta)
        assertNull(model.plans)
    }
    @Test fun `strip priority resolves operation billing error result mixed unverified inactive unavailable expired`() {
        val mixedProblem = paid.copy(source = AccessSource.LIFETIME, billingIssue = true)
        assertEquals(R.string.plus_restore_loading, page(access = mixedProblem, store = ready.copy(operation = StoreOperation.RESTORING, message = StoreMessage.NETWORK)).strip?.text)
        assertEquals(R.string.plus_status_billing, page(access = mixedProblem, store = ready.copy(message = StoreMessage.NETWORK)).strip?.text)
        assertEquals(R.string.plus_network_error, page(access = mixedProblem.copy(billingIssue = false), store = ready.copy(message = StoreMessage.NETWORK)).strip?.text)
        assertEquals(R.string.plus_restored, page(access = mixedProblem.copy(billingIssue = false), store = ready.copy(message = StoreMessage.RESTORED)).strip?.text)
        assertEquals(R.string.plus_status_unverified, page(access = paid.copy(source = AccessSource.NONE), active = false).strip?.text)
        assertEquals(R.string.plus_release_off_neutral, page(access = free.copy(expired = true), store = ready.copy(availability = StoreAvailability.UNAVAILABLE), active = false).strip?.text)
        assertEquals(R.string.plus_no_configuration, page(access = free.copy(expired = true), store = ready.copy(availability = StoreAvailability.UNAVAILABLE)).strip?.text)
    }
    @Test fun `CTA precedence is loading unavailable inactive busy stale then no selected plan`() {
        assertEquals(PlusCta.Disabled(R.string.plus_cta_loading, true), page(store = ready.copy(availability = StoreAvailability.LOADING, operation = StoreOperation.PENDING)).cta)
        assertEquals(PlusCta.None, page(store = ready.copy(availability = StoreAvailability.UNAVAILABLE, operation = StoreOperation.PENDING)).cta)
        assertEquals(PlusCta.None, page(store = ready.copy(operation = StoreOperation.PENDING), active = false).cta)
        assertEquals(PlusCta.Disabled(R.string.plus_cta_pending), page(access = free.copy(verification = VerificationStatus.CACHED), store = ready.copy(operation = StoreOperation.PENDING)).cta)
        assertEquals(PlusCta.RefreshToContinue, page(access = free.copy(verification = VerificationStatus.CACHED), selected = null).cta)
        assertEquals(PlusCta.Disabled(R.string.plus_cta_choose), page(selected = "removed-plan").cta)
    }
    @Test fun `purchases disabled by catalog never renders an enabled subscribe action`() {
        assertEquals(PlusCta.None, page(store = ready.copy(purchasesEnabled = false)).cta)
        assertNull(page(store = ready.copy(purchasesEnabled = false)).plans)
    }
    @Test fun `showcase suppresses only irrelevant facts and never hides a real error`() {
        assertNull(page(access = free.copy(expired = true), showcase = true).strip)
        assertNull(page(access = free.copy(source = AccessSource.PREVIEW, previewEndsAtMillis = 99L), showcase = true).chip)
        assertEquals(PlusSeverity.PROBLEM, page(showcase = true, store = ready.copy(message = StoreMessage.NETWORK)).strip?.severity)
    }
    @Test fun `initial loading shows loading before catalog has enabled purchases`() {
        val initial = StoreSnapshot(StoreAvailability.LOADING)
        assertEquals(PlusPlans.Loading, page(store = initial).plans)
        assertEquals(PlusCta.Disabled(R.string.plus_cta_loading, true), page(store = initial).cta)
        assertNull(page(store = initial).strip)
    }
    @Test fun `unconfigured initial catalog is unavailable rather than endless ownership loading`() {
        val initial = StoreSnapshot(StoreAvailability.NOT_CONFIGURED)
        assertNull(page(store = initial).plans)
        assertEquals(PlusCta.None, page(store = initial).cta)
        assertEquals(R.string.plus_no_configuration, page(store = initial).strip?.text)
    }
    @Test fun `expired subscription retains Google Play management on the pitch layout`() {
        val model = page(access = free.copy(expired = true, manageSubscription = true, willRenew = false))
        assertEquals(PlusLayout.PITCH, model.layout)
        assertEquals(listOf(PlusAction.MANAGE), model.managementActions)
    }
    @Test fun `owner management keeps restore while pitch restore belongs in the footer`() {
        assertEquals(listOf(PlusAction.MANAGE, PlusAction.RESTORE), page(access = paid).managementActions)
        assertEquals(listOf(PlusAction.RESTORE), page(access = free.copy(source = AccessSource.LIFETIME)).managementActions)
        assertTrue(page().managementActions.isEmpty())
    }
    @Test fun `completed refresh with rejected verification is recoverable instead of loading forever`() {
        val rejected = ready.copy(ownershipLoaded = false, message = StoreMessage.VERIFYING)
        val model = page(access = free.copy(verification = VerificationStatus.ERROR), store = rejected)
        assertNull(model.plans)
        assertEquals(PlusCta.RefreshToContinue, model.cta)
        assertEquals(R.string.plus_status_unverified, model.strip?.text)
        assertEquals(listOf(PlusAction.REFRESH, PlusAction.RESTORE), model.strip?.actions)
        assertTrue(model.refreshEnabled)
    }
    @Test fun `stale verifying message does not replace an actual refresh in progress`() {
        val refreshing = ready.copy(availability = StoreAvailability.LOADING, ownershipLoaded = false, message = StoreMessage.VERIFYING)
        assertEquals(PlusPlans.Loading, page(store = refreshing).plans)
        assertEquals(PlusCta.Disabled(R.string.plus_cta_loading, true), page(store = refreshing).cta)
        assertNull(page(store = refreshing).strip)
    }
    @Test fun `lifetime ownership has a status fact distinct from its thank you subtitle`() {
        val model = page(access = free.copy(source = AccessSource.LIFETIME))
        assertNotEquals(model.ownerSubtitle, model.chip?.text)
    }
}

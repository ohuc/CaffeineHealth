package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Test

class PlusJourneyPolicyTest {
    private val day = 86_400_000L
    private val now = 100 * day
    private val today = 100L

    private fun openOn(vararg epochDays: Long, start: JourneyRecord = JourneyRecord()) =
        epochDays.fold(start) { record, epochDay -> PlusJourneyPolicy.recordOpen(record, epochDay, null, epochDay * day) }

    private fun prompt(record: JourneyRecord, trial: PreviewRecord? = null, at: Long = now, paid: Boolean = false, policy: Boolean = true) =
        PlusJourneyPolicy.prompt(record, trial, at, paid, policy)

    @Test fun `several opens on one calendar day count once`() {
        assertEquals(1, openOn(today, today, today).openDays)
    }
    @Test fun `a clock moved back a day does not count as a new day`() {
        assertEquals(2, openOn(today, today + 1, today).openDays)
    }
    @Test fun `separate days need not be consecutive`() {
        assertEquals(3, openOn(today, today + 4, today + 9).openDays)
    }
    @Test fun `the free day is offered on the third separate day, not before`() {
        assertEquals(JourneyPrompt.NONE, prompt(openOn(today, today + 1)))
        assertEquals(JourneyPrompt.TRIAL_OFFER, prompt(openOn(today, today + 1, today + 2)))
    }
    @Test fun `the free day offer appears only once`() {
        val shown = PlusJourneyPolicy.handled(openOn(today, today + 1, today + 2), JourneyPrompt.TRIAL_OFFER)
        assertEquals(JourneyPrompt.NONE, prompt(openOn(today + 3, start = shown)))
    }
    @Test fun `declining the offer still allows starting the free day later`() {
        val shown = PlusJourneyPolicy.handled(openOn(today, today + 1, today + 2), JourneyPrompt.TRIAL_OFFER)
        assertTrue(PlusJourneyPolicy.canStartTrial(shown, null, paid = false, policyEnabled = true))
    }
    @Test fun `the free day cannot start before three separate days or twice`() {
        assertFalse(PlusJourneyPolicy.canStartTrial(openOn(today, today + 1), null, false, true))
        val used = PreviewRecord(now - 2 * day, now)
        assertFalse(PlusJourneyPolicy.canStartTrial(openOn(today, today + 1, today + 2), used, false, true))
    }
    @Test fun `the free day lasts exactly one day`() {
        val trial = PreviewRecord(now, now)
        assertTrue(AccessPolicy.resolve(null, trial, now + day - 1).hasPlus)
        assertFalse(AccessPolicy.resolve(null, trial, now + day).hasPlus)
    }
    @Test fun `the follow-up waits for the next open after the free day ends and shows once`() {
        val record = openOn(today, today + 1, today + 2).let { PlusJourneyPolicy.handled(it, JourneyPrompt.TRIAL_OFFER) }
        val trial = PreviewRecord((today + 2) * day, (today + 2) * day)
        val afterEnd = (today + 3) * day + 1
        assertEquals(JourneyPrompt.NONE, prompt(record, trial, at = afterEnd))
        val reopened = PlusJourneyPolicy.recordOpen(record, today + 3, trial, afterEnd)
        assertEquals(JourneyPrompt.TRIAL_ENDED, prompt(reopened, trial, at = afterEnd))
        val handled = PlusJourneyPolicy.handled(reopened, JourneyPrompt.TRIAL_ENDED)
        val again = PlusJourneyPolicy.recordOpen(handled, today + 4, trial, afterEnd + day)
        assertEquals(JourneyPrompt.NONE, prompt(again, trial, at = afterEnd + day))
    }
    @Test fun `the showcase follows a completed setup once`() {
        val completed = PlusJourneyPolicy.onboardingCompleted(JourneyRecord(), policyEnabled = true)
        assertEquals(JourneyPrompt.SHOWCASE, prompt(completed))
        val shown = PlusJourneyPolicy.handled(completed, JourneyPrompt.SHOWCASE)
        assertEquals(JourneyPrompt.NONE, prompt(PlusJourneyPolicy.onboardingCompleted(shown, policyEnabled = true)))
    }
    @Test fun `setup finished while Plus is off never queues a late showcase`() {
        val completed = PlusJourneyPolicy.onboardingCompleted(JourneyRecord(), policyEnabled = false)
        assertEquals(JourneyPrompt.NONE, prompt(completed, policy = true))
    }
    @Test fun `paying users and disabled policy get no prompts or free day`() {
        val ready = PlusJourneyPolicy.onboardingCompleted(openOn(today, today + 1, today + 2), policyEnabled = true)
        assertEquals(JourneyPrompt.NONE, prompt(ready, paid = true))
        assertEquals(JourneyPrompt.NONE, prompt(ready, policy = false))
        assertFalse(PlusJourneyPolicy.canStartTrial(ready, null, paid = true, policyEnabled = true))
        assertFalse(PlusJourneyPolicy.canStartTrial(ready, null, paid = false, policyEnabled = false))
    }
    @Test fun `no offer interrupts an active free day`() {
        val record = openOn(today, today + 1, today + 2)
        assertEquals(JourneyPrompt.NONE, prompt(record, PreviewRecord(now, now), at = now + 1))
    }
    @Test fun `home status shows the free day and paid problems but not a finished free day`() {
        val base = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED)
        assertEquals(PlusStatusCard.TRIAL_ACTIVE, plusStatusCard(base.copy(source = AccessSource.PREVIEW)))
        assertEquals(PlusStatusCard.NONE, plusStatusCard(base.copy(expired = true, previewEndsAtMillis = now)))
        assertEquals(PlusStatusCard.PAID_ENDED, plusStatusCard(base.copy(expired = true, paidExpiresAtMillis = now)))
        assertEquals(PlusStatusCard.BILLING, plusStatusCard(base.copy(source = AccessSource.SUBSCRIPTION, billingIssue = true)))
        assertEquals(PlusStatusCard.NONE, plusStatusCard(base.copy(source = AccessSource.SUBSCRIPTION)))
    }

    private fun introEligible(
        record: JourneyRecord = JourneyRecord(),
        trial: PreviewRecord? = null,
        paid: Boolean = false,
        policy: Boolean = true,
    ) = PlusJourneyPolicy.onboardingIntroEligible(record, trial, now, paid, policy)

    @Test fun `the onboarding intro is eligible while Plus is active and not yet shown`() {
        assertTrue(introEligible())
    }
    @Test fun `the onboarding intro is absent without policy, for owners, during a free day, or once shown`() {
        assertFalse(introEligible(policy = false))
        assertFalse(introEligible(paid = true))
        assertFalse(introEligible(trial = PreviewRecord(startedAtMillis = now - 1_000, latestSeenMillis = now)))
        assertFalse(introEligible(record = JourneyRecord(showcaseShown = true)))
    }
    @Test fun `after the intro was shown the post-setup showcase never queues`() {
        val shown = PlusJourneyPolicy.handled(JourneyRecord(), JourneyPrompt.SHOWCASE)
        val afterSetup = PlusJourneyPolicy.onboardingCompleted(shown, policyEnabled = true)
        assertFalse(afterSetup.showcasePending)
        assertEquals(JourneyPrompt.NONE, prompt(afterSetup))
    }
}

package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Test

class AccessPolicyTest {
    private val day = 86_400_000L
    private val now = 10 * day
    private fun grant(lifetime: Boolean = false, active: Boolean = true) = PaidGrant(
        active = active, lifetime = lifetime, verifiedAtMillis = now,
        expiresAtMillis = if (lifetime) null else now + day, hasSubscription = !lifetime,
    )

    @Test fun `no verified ownership never grants paid access`() {
        assertFalse(AccessPolicy.resolve(null, null, now).hasVerifiedPurchase)
    }
    @Test fun `a verified empty account is Free rather than expired`() {
        val empty = PaidGrant(false, false, now, null, false)
        assertFalse(AccessPolicy.resolve(empty, null, now).expired)
    }
    @Test fun `freshly fetched ownership remains verified while the user chooses a plan`() {
        assertEquals(VerificationStatus.VERIFIED, AccessPolicy.resolve(grant(), null, now + 1_000L).verification)
    }
    @Test fun `free day includes Coach for exactly one day`() {
        val preview = PreviewRecord(startedAtMillis = now, latestSeenMillis = now)
        assertTrue(AccessPolicy.resolve(null, preview, now + day - 1).hasPlus)
        assertFalse(AccessPolicy.resolve(null, preview, now + day).hasPlus)
        assertTrue(AccessPolicy.resolve(null, preview, now + day).expired)
    }
    @Test fun `free day clock rollback cannot extend access`() {
        val preview = PreviewRecord(now, now + day)
        assertFalse(AccessPolicy.resolve(null, preview, now + day / 2).hasPlus)
    }
    @Test fun `canceled subscription retains verified paid period`() {
        val state = AccessPolicy.resolve(grant().copy(willRenew = false), null, now)
        assertTrue(state.hasVerifiedPurchase)
        assertFalse(state.expired)
    }
    @Test fun `network failure preserves valid cache but does not extend known expiry`() {
        assertTrue(AccessPolicy.resolve(grant(), null, now + day - 1, refreshFailed = true).hasPlus)
        val state = AccessPolicy.resolve(grant(), null, now + day, refreshFailed = true)
        assertFalse(state.hasPlus)
        assertFalse(state.expired)
    }
    @Test fun `offline subscription cache ends after seventy two hours`() {
        val longGrant = grant().copy(expiresAtMillis = now + 30 * day)
        assertTrue(AccessPolicy.resolve(longGrant, null, now + 3 * day - 1).hasPlus)
        assertFalse(AccessPolicy.resolve(longGrant, null, now + 3 * day).hasPlus)
    }
    @Test fun `stale paid cache is not called expired because an older preview ended`() {
        val paid = grant().copy(expiresAtMillis = now + 30 * day)
        val state = AccessPolicy.resolve(paid, PreviewRecord(now - 4 * day, now), now + 4 * day)
        assertFalse(state.hasPlus)
        assertFalse(state.expired)
    }
    @Test fun `verified lifetime works offline until authoritative revocation`() {
        assertTrue(AccessPolicy.resolve(grant(lifetime = true), null, now + 365 * day, true).hasPlus)
        assertFalse(AccessPolicy.resolve(grant(lifetime = true, active = false), null, now).hasPlus)
    }
    @Test fun `a clock rollback never expires verified Lifetime`() {
        assertTrue(AccessPolicy.resolve(grant(lifetime = true), null, now - day, refreshFailed = true).hasPlus)
    }
    @Test fun `pending does not unlock and failed verification does not replace existing grant`() {
        val existing = grant()
        assertEquals(existing, AccessPolicy.acceptGrant(existing, grant(lifetime = true), verified = false))
        assertNull(AccessPolicy.acceptGrant(null, grant(), verified = false))
    }
    @Test fun `paid access suppresses preview expiry`() {
        assertFalse(AccessPolicy.resolve(grant(), PreviewRecord(now - 4 * day, now), now).expired)
    }
    @Test fun `release rollout bypasses proposed locks without pretending ownership`() {
        val state = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.UNVERIFIED)
        assertTrue(FeaturePolicy.canUse(state, ProFeature.Coach, enforceLocks = false))
        assertFalse(FeaturePolicy.canUse(state, ProFeature.Coach, enforceLocks = true))
        assertFalse(state.hasVerifiedPurchase)
    }
    @Test fun `FOSS permits every feature independent of Play state`() {
        val state = AccessSnapshot(Distribution.FOSS, AccessSource.FOSS, VerificationStatus.NOT_APPLICABLE)
        ProFeature.entries.forEach { assertTrue(FeaturePolicy.canUse(state, it, true)) }
    }
    @Test fun `mixed lifetime and subscription ownership keeps management available`() {
        val state = AccessPolicy.resolve(grant(lifetime = true).copy(hasSubscription = true, willRenew = true), null, now)
        assertTrue(state.manageSubscription)
        assertTrue(state.hasVerifiedPurchase)
    }
    @Test fun `expiry highlight happens only on confirmed transition and respects reduced motion`() {
        assertTrue(shouldHighlightCoachExpiry(wasExpired = false, isExpired = true, reducedMotion = false))
        assertFalse(shouldHighlightCoachExpiry(true, true, false))
        assertFalse(shouldHighlightCoachExpiry(false, true, true))
        assertFalse(shouldHighlightCoachExpiry(false, false, false))
    }
}

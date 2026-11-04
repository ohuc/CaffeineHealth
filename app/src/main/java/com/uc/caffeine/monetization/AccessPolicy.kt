package com.uc.caffeine.monetization

data class PaidGrant(
    val active: Boolean,
    val lifetime: Boolean,
    val verifiedAtMillis: Long,
    val expiresAtMillis: Long?,
    val hasSubscription: Boolean,
    val willRenew: Boolean = false,
    val billingIssue: Boolean = false,
    val previouslyOwned: Boolean = active || lifetime || expiresAtMillis != null,
    val activeSubscription: Boolean = active && hasSubscription && !lifetime,
)

data class PreviewRecord(val startedAtMillis: Long, val latestSeenMillis: Long) {
    val endsAtMillis: Long get() = startedAtMillis + AccessPolicy.PREVIEW_MILLIS
}

object AccessPolicy {
    /** The one free day, started by the user after three separate days of use. */
    const val PREVIEW_MILLIS = 24L * 60 * 60 * 1000
    const val SUBSCRIPTION_CACHE_MILLIS = 72L * 60 * 60 * 1000

    fun acceptGrant(previous: PaidGrant?, incoming: PaidGrant, verified: Boolean): PaidGrant? =
        if (verified && (previous == null || incoming.verifiedAtMillis >= previous.verifiedAtMillis)) incoming else previous

    fun resolve(
        grant: PaidGrant?, preview: PreviewRecord?, nowMillis: Long, refreshFailed: Boolean = false,
    ): AccessSnapshot {
        val now = maxOf(nowMillis, preview?.latestSeenMillis ?: nowMillis)
        val cacheAge = grant?.let { now - it.verifiedAtMillis }
        val paid = grant?.active == true && (grant.lifetime ||
            (cacheAge != null && cacheAge >= 0 && grant.expiresAtMillis?.let { now < it } == true && cacheAge < SUBSCRIPTION_CACHE_MILLIS))
        val previewActive = preview != null && now >= preview.startedAtMillis && now < preview.endsAtMillis
        val knownEndPassed = grant?.expiresAtMillis?.let { now >= it } == true
        val activeSubscription = grant?.activeSubscription == true && (grant.lifetime || !knownEndPassed)
        val confirmedExpiry = !refreshFailed && !(activeSubscription && grant?.active != true) &&
            (grant?.let { !it.active && it.previouslyOwned } == true || grant?.expiresAtMillis?.let { now >= it } == true ||
                (preview?.let { now >= it.endsAtMillis } == true && grant?.active != true))
        return AccessSnapshot(
            distribution = Distribution.PLAY,
            source = when {
                paid && grant.lifetime -> AccessSource.LIFETIME
                paid -> AccessSource.SUBSCRIPTION
                previewActive -> AccessSource.PREVIEW
                else -> AccessSource.NONE
            },
            verification = when {
                refreshFailed -> VerificationStatus.ERROR
                grant != null && cacheAge != null && cacheAge in 0 until 300_000L -> VerificationStatus.VERIFIED
                grant != null -> VerificationStatus.CACHED
                else -> VerificationStatus.UNVERIFIED
            },
            paidExpiresAtMillis = grant?.expiresAtMillis,
            previewEndsAtMillis = preview?.endsAtMillis,
            verifiedAtMillis = grant?.verifiedAtMillis,
            expired = !paid && !previewActive && confirmedExpiry,
            manageSubscription = grant?.hasSubscription == true,
            willRenew = grant?.willRenew == true,
            billingIssue = grant?.billingIssue == true,
            activeSubscription = activeSubscription,
        )
    }
}

object FeaturePolicy {
    fun canUse(snapshot: AccessSnapshot, feature: ProFeature, enforceLocks: Boolean): Boolean =
        snapshot.distribution == Distribution.FOSS || !enforceLocks || snapshot.hasPlus
}

fun shouldHighlightCoachExpiry(wasExpired: Boolean, isExpired: Boolean, reducedMotion: Boolean): Boolean =
    !wasExpired && isExpired && !reducedMotion

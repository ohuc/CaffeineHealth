package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.StateFlow

enum class Distribution { FOSS, PLAY }

enum class ProFeature {
    HealthConnect, Automation, PremiumWidgets, AdvancedAppearance, ExtendedAnalytics, Coach,
}

enum class AccessSource { NONE, FOSS, PREVIEW, SUBSCRIPTION, LIFETIME }
enum class VerificationStatus { NOT_APPLICABLE, UNVERIFIED, VERIFIED, CACHED, ERROR }

data class AccessSnapshot(
    val distribution: Distribution,
    val source: AccessSource,
    val verification: VerificationStatus,
    val paidExpiresAtMillis: Long? = null,
    val previewEndsAtMillis: Long? = null,
    val verifiedAtMillis: Long? = null,
    val expired: Boolean = false,
    val manageSubscription: Boolean = false,
    val willRenew: Boolean = false,
    val billingIssue: Boolean = false,
    val paymentPending: Boolean = false,
    val activeSubscription: Boolean = false,
    /**
     * Whether Plus locks are enforced right now. Part of the snapshot so a policy change (for
     * example the debug "Plus rules on" switch loading at startup) emits a new value and every
     * `canUse` consumer re-evaluates, even when the access itself is unchanged.
     */
    val locksEnforced: Boolean = false,
    /**
     * Whether Plus can be bought here (Play sale region). When false, locks still apply but the app
     * must not advertise Plus: no locked Coach chip, no prompts, no status cards nudging a purchase.
     * The Plus page itself stays reachable and says it is not on sale in this country.
     */
    val plusOnSale: Boolean = true,
) {
    val hasVerifiedPurchase: Boolean get() = source == AccessSource.SUBSCRIPTION || source == AccessSource.LIFETIME
    val hasPlus: Boolean get() = hasVerifiedPurchase || source == AccessSource.FOSS || source == AccessSource.PREVIEW
}

interface FeatureAccess {
    val snapshots: StateFlow<AccessSnapshot>
    fun canUse(feature: ProFeature): Boolean
    suspend fun awaitReady() {}
}

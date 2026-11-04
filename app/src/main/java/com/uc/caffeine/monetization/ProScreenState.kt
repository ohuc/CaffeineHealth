package com.uc.caffeine.monetization

data class ProScreenState(
    val selectedPlan: StorePlan?,
    val showPlans: Boolean,
    val canPurchase: Boolean,
    val showManagement: Boolean,
) {
    companion object {
        fun from(access: AccessSnapshot, store: StoreSnapshot, selectedId: String?): ProScreenState {
            val selected = store.plans.firstOrNull { it.id == selectedId }
            val showPlans = store.ownershipLoaded && !access.hasVerifiedPurchase &&
                !access.activeSubscription && !(access.manageSubscription && access.willRenew)
            return ProScreenState(
                selectedPlan = selected,
                showPlans = showPlans,
                canPurchase = showPlans && selected != null && store.purchasesEnabled &&
                    store.availability == StoreAvailability.AVAILABLE &&
                    store.operation == StoreOperation.IDLE && access.verification == VerificationStatus.VERIFIED,
                showManagement = access.manageSubscription,
            )
        }
    }
}

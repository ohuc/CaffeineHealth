package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlavorServices {
    fun create(context: android.content.Context? = null): DistributionServices = object : DistributionServices {
        override val distribution = Distribution.PLAY
        override val featureAccess = PlayFeatureAccess(com.uc.caffeine.BuildConfig.PLUS_POLICY_ENABLED)
        private val repository = context?.let { RevenueCatBillingRepository(it, featureAccess) }
        override val store: StoreCatalog = repository ?: object : StoreCatalog {
            override val snapshots = MutableStateFlow(StoreSnapshot(StoreAvailability.NOT_CONFIGURED)).asStateFlow()
        }
        override val journey: PlusJourney = repository ?: PlusJourney.None
        override val distributionUi: DistributionUi = PlayDistributionUi(featureAccess, store, journey)
    }
}

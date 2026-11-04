package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlavorServices {
    fun create(context: android.content.Context? = null): DistributionServices = object : DistributionServices {
        override val distribution = Distribution.FOSS
        override val featureAccess = FossFeatureAccess()
        override val store = object : StoreCatalog {
            override val snapshots = MutableStateFlow(StoreSnapshot(StoreAvailability.NOT_SUPPORTED)).asStateFlow()
        }
        override val distributionUi = object : DistributionUi {}
    }
}

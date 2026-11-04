package com.uc.caffeine.monetization

interface DistributionServices {
    val distribution: Distribution
    val featureAccess: FeatureAccess
    val store: StoreCatalog
    val distributionUi: DistributionUi
    val journey: PlusJourney get() = PlusJourney.None
}

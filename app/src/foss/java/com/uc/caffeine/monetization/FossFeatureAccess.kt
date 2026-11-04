package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FossFeatureAccess : FeatureAccess {
    override val snapshots = MutableStateFlow(
        AccessSnapshot(Distribution.FOSS, AccessSource.FOSS, VerificationStatus.NOT_APPLICABLE),
    ).asStateFlow()

    override fun canUse(feature: ProFeature): Boolean = true
}

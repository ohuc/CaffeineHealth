package com.uc.caffeine.monetization

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.uc.caffeine.BuildConfig

internal object BillingVariantSupport {
    const val HAS_DEVELOPER_SETTINGS = false

    /**
     * Until release activation, a release build never mentions Caffeine Health+ (no Settings row,
     * no Plus app icon) to anyone who has not bought it: every feature is free and nothing is on sale.
     */
    val PLUS_SURFACES: Boolean = BuildConfig.PLUS_PURCHASES_ENABLED && BuildConfig.PLUS_POLICY_ENABLED

    fun baseOption(product: StoreProduct): SubscriptionOption? = null

    @Composable fun purchasesActive(store: StoreCatalog): Boolean =
        BuildConfig.PLUS_PURCHASES_ENABLED && BuildConfig.PLUS_POLICY_ENABLED

    /** Release builds contain no demo controls. */
    @Composable fun DemoControls(store: StoreCatalog, journey: PlusJourney) {}
}

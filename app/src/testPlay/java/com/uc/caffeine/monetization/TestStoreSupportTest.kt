package com.uc.caffeine.monetization

import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import org.junit.Assert.*
import org.junit.Test

class TestStoreSupportTest {
    @Test fun `debug Test Store selects the full price option without a Play base plan id`() {
        val product = TestStoreProduct(id = "pro:monthly", name = "Monthly", title = "Monthly",
            description = "", price = Price(formatted = "$1.99", amountMicros = 1_990_000L, currencyCode = "USD"),
            period = Period.create("P1M"),
            freeTrialPricingPhase = PricingPhase(billingPeriod = Period.create("P7D"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING, billingCycleCount = 1,
                price = Price(formatted = "$0.00", amountMicros = 0L, currencyCode = "USD")))
        val option = BillingVariantSupport.baseOption(product)
        assertNotNull(option)
        assertTrue(option!!.isBasePlan)
        assertNull(option.freePhase)
        assertEquals("$1.99", option.fullPricePhase?.price?.formatted)
    }
    @Test fun `Test Store lifetime remains a one time product without subscription options`() {
        val product = TestStoreProduct(id = "pro_lifetime", name = "Lifetime", title = "Lifetime",
            description = "", price = Price(formatted = "$29.99", amountMicros = 29_990_000L, currencyCode = "USD"))
        assertNull(BillingVariantSupport.baseOption(product))
    }
}

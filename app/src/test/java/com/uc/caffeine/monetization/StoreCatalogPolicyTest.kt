package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Test

class StoreCatalogPolicyTest {
    @Test fun `known base plans map to the same benefit tier with store prices`() {
        assertEquals(StorePlan("m", PlanDuration.MONTHLY, "₹99.00"), catalogPlan("m", "pro:monthly", "₹99.00", subscription = true, basePlan = "monthly", billingPeriod = "P1M"))
        assertEquals(StorePlan("y", PlanDuration.YEARLY, "€11,99"), catalogPlan("y", "pro:yearly", "€11,99", subscription = true, basePlan = "yearly", billingPeriod = "P1Y"))
        assertEquals(StorePlan("l", PlanDuration.LIFETIME, "$29.99"), catalogPlan("l", "pro_lifetime", "$29.99", subscription = false))
    }
    @Test fun `trial offers and unexpected products never replace the chosen base plan`() {
        assertNull(catalogPlan("x", "pro:monthly", "$1.99", true, "monthly:trial", billingPeriod = "P1M"))
        assertNull(catalogPlan("x", "other", "$1.99", false))
        assertNull(catalogPlan("x", "pro_lifetime", "$1.99", true))
        assertNull(catalogPlan("x", "pro:monthly", "", true, "monthly", billingPeriod = "P1M"))
    }
    @Test fun `catalog period must match the displayed commitment`() {
        assertNull(catalogPlan("wrong", "pro:yearly", "$1.99", true, "yearly", billingPeriod = "P1M"))
        assertNull(catalogPlan("missing", "pro:monthly", "$1.99", true, "monthly", billingPeriod = null))
    }
}

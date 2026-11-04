package com.uc.caffeine.monetization

fun catalogPlan(id: String, productId: String, localizedPrice: String, subscription: Boolean,
    basePlan: String? = null, billingPeriod: String? = null): StorePlan? {
    if (id.isBlank() || localizedPrice.isBlank()) return null
    val duration = when {
        subscription && productId == "pro:monthly" && basePlan == "monthly" && billingPeriod == "P1M" -> PlanDuration.MONTHLY
        subscription && productId == "pro:yearly" && basePlan == "yearly" && billingPeriod == "P1Y" -> PlanDuration.YEARLY
        !subscription && productId == "pro_lifetime" -> PlanDuration.LIFETIME
        else -> return null
    }
    return StorePlan(id, duration, localizedPrice)
}

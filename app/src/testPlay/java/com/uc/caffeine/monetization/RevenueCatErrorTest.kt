package com.uc.caffeine.monetization

import com.revenuecat.purchases.PurchasesErrorCode
import org.junit.Assert.assertEquals
import org.junit.Test

class RevenueCatErrorTest {
    @Test fun `network failures get retry messaging without a false payment failure`() {
        assertEquals(StoreMessage.NETWORK, messageFor(PurchasesErrorCode.NetworkError))
    }
    @Test fun `pending is distinct from a purchase failure`() {
        assertEquals(StoreMessage.PENDING, messageFor(PurchasesErrorCode.PaymentPendingError))
    }
    @Test fun `store unavailability and missing configuration are actionable`() {
        assertEquals(StoreMessage.STORE, messageFor(PurchasesErrorCode.StoreProblemError))
        assertEquals(StoreMessage.UNAVAILABLE, messageFor(PurchasesErrorCode.ConfigurationError))
        assertEquals(StoreMessage.UNAVAILABLE, messageFor(PurchasesErrorCode.InvalidCredentialsError))
        assertEquals(StoreMessage.NOT_ALLOWED, messageFor(PurchasesErrorCode.PurchaseNotAllowedError))
    }
}

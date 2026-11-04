package com.uc.caffeine.util.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationAccessTest {
    @Test fun freshAndroid13OffersAnExplicitRequest() {
        assertEquals(NotificationAccessAction.REQUEST, notificationAccessAction(33, false, false, false))
    }
    @Test fun denialWithRationaleAllowsExplicitRetry() {
        assertEquals(NotificationAccessAction.REQUEST, notificationAccessAction(36, false, true, true))
    }
    @Test fun fixedDenialOpensSettings() {
        assertEquals(NotificationAccessAction.SETTINGS, notificationAccessAction(36, false, true, false))
    }
    @Test fun grantedRuntimePermissionWithBlockedChannelsOpensSettings() {
        assertEquals(NotificationAccessAction.SETTINGS, notificationAccessAction(36, true, true, false))
    }
    @Test fun android12UsesSystemSettings() {
        assertEquals(NotificationAccessAction.SETTINGS, notificationAccessAction(31, true, false, false))
    }
}

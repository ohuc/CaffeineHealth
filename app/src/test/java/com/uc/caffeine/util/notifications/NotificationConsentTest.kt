package com.uc.caffeine.util.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationConsentTest {

    @Test
    fun notNow_turnsAllRemindersOff() {
        val current = ReminderPrefs(dailyTimes = setOf("11:00", "14:00"), inactivity = true)
        assertEquals(ReminderPrefs(emptySet(), false), reminderPrefsAfterChoice(turnOn = false, current = current))
    }

    @Test
    fun turnOn_keepsSavedReminders() {
        val current = ReminderPrefs(dailyTimes = setOf("09:30"), inactivity = false)
        assertEquals(current, reminderPrefsAfterChoice(turnOn = true, current = current))
    }

    @Test
    fun prompt_onlyOnApi33PlusAndNeverAfterAnAttempt() {
        assertTrue(notificationPromptAllowed(sdk = 33, previouslyRequested = false))
        assertFalse(notificationPromptAllowed(sdk = 33, previouslyRequested = true))
        assertFalse(notificationPromptAllowed(sdk = 32, previouslyRequested = false))
    }
}

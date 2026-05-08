package com.uc.caffeine.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsTest {

    private val defaultSettings = UserSettings(use24HourClock = false, timeZoneId = "UTC")

    @Test
    fun defaultUserSettings_hasExpectedNotificationDefaults() {
        val settings = UserSettings()
        assertTrue(settings.inactivityReminderEnabled)
        assertEquals(setOf("11:00", "14:00"), settings.dailyReminderTimes)
    }

    @Test
    fun toUserSettings_inactivityReminderEnabled_roundTrips() {
        val prefs = mutablePreferencesOf(
            SettingsKeys.INACTIVITY_REMINDER_ENABLED to false,
        )
        val settings = prefs.toUserSettings(defaultSettings)
        assertFalse(settings.inactivityReminderEnabled)
    }

    @Test
    fun toUserSettings_inactivityReminderMissing_defaultsToTrue() {
        val prefs = mutablePreferencesOf()
        val settings = prefs.toUserSettings(defaultSettings)
        assertTrue(settings.inactivityReminderEnabled)
    }

    @Test
    fun toUserSettings_dailyReminderTimes_roundTrips() {
        val times = setOf("08:00", "20:00")
        val prefs = mutablePreferencesOf(
            SettingsKeys.DAILY_REMINDER_TIMES to times,
        )
        val settings = prefs.toUserSettings(defaultSettings)
        assertEquals(times, settings.dailyReminderTimes)
    }

    @Test
    fun toUserSettings_dailyReminderTimesMissing_defaultsTo11And14() {
        val prefs = mutablePreferencesOf()
        val settings = prefs.toUserSettings(defaultSettings)
        assertEquals(setOf("11:00", "14:00"), settings.dailyReminderTimes)
    }

    @Test
    fun toUserSettings_emptyDailyReminderTimes_returnsEmpty() {
        val prefs = mutablePreferencesOf(
            SettingsKeys.DAILY_REMINDER_TIMES to emptySet<String>(),
        )
        val settings = prefs.toUserSettings(defaultSettings)
        assertTrue(settings.dailyReminderTimes.isEmpty())
    }

    @Test
    fun toUserSettings_allNotificationFields_roundTrip() {
        val times = setOf("07:30", "13:00", "18:45")
        val prefs = mutablePreferencesOf(
            SettingsKeys.INACTIVITY_REMINDER_ENABLED to true,
            SettingsKeys.DAILY_REMINDER_TIMES to times,
        )
        val settings = prefs.toUserSettings(defaultSettings)
        assertTrue(settings.inactivityReminderEnabled)
        assertEquals(times, settings.dailyReminderTimes)
    }
}

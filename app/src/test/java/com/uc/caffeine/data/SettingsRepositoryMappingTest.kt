package com.uc.caffeine.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryMappingTest {

    @Test
    fun writeOnboardingCompletion_roundTripsIntoUserSettings() {
        val profile = DerivedOnboardingProfile(
            halfLifeMinutes = 345,
            sleepThresholdMg = 72,
            sleepTimeHour = 22,
            sleepTimeMinute = 30,
        )
        val defaults = UserSettings(
            use24HourClock = true,
            timeZoneId = "UTC",
        )
        val preferences = mutablePreferencesOf()

        preferences.writeOnboardingCompletion(profile)

        val mapped = preferences.toUserSettings(defaultSettings = defaults)

        assertEquals(345, mapped.halfLifeMinutes)
        assertEquals(72, mapped.sleepThresholdMg)
        assertEquals(22, mapped.sleepTimeHour)
        assertEquals(30, mapped.sleepTimeMinute)
        assertTrue(mapped.isOnboardingComplete)
    }

    @Test
    fun whatsNewSeen_skipsNamesFromEarlierReleases() {
        val preferences = mutablePreferencesOf()
        preferences[SettingsKeys.WHATS_NEW_SEEN] = setOf(NewFeature.RingWidget.name, "BarcodeScanner")

        val mapped = preferences.toUserSettings(defaultSettings = UserSettings())

        assertEquals(setOf(NewFeature.RingWidget), mapped.whatsNewSeen)
    }
}

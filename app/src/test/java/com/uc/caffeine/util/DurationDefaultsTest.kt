package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.decodeDurationMap
import com.uc.caffeine.data.encodeDurationMap
import com.uc.caffeine.data.model.DEFAULT_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.data.model.DrinkPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationDefaultsTest {

    private val espresso = DrinkPreset(itemId = "espresso", name = "Espresso", category = "coffee")
    private val caffeinePill = DrinkPreset(itemId = "pill-200", name = "Caffeine Pill", category = "pill")

    @Test
    fun defaultsToTenMinutesWithNoOverrides() {
        assertEquals(DEFAULT_CONSUMPTION_DURATION_MINUTES, UserSettings().defaultDurationFor(espresso))
        assertEquals(DEFAULT_CONSUMPTION_DURATION_MINUTES, UserSettings().defaultDurationFor(null))
    }

    @Test
    fun pillsDefaultToOneMinuteEvenWhenGlobalDefaultChanges() {
        assertEquals(1, UserSettings().defaultDurationFor(caffeinePill))
        assertEquals(1, UserSettings(defaultDurationMinutes = 30).defaultDurationFor(caffeinePill))
    }

    @Test
    fun globalDefaultAppliesToCategoriesWithoutOverride() {
        assertEquals(4, UserSettings(defaultDurationMinutes = 4).defaultDurationFor(espresso))
    }

    @Test
    fun categoryOverrideBeatsGlobalAndBuiltIn() {
        val settings = UserSettings(
            defaultDurationMinutes = 4,
            categoryDurationMinutes = mapOf("coffee" to 20, "pill" to 2),
        )
        assertEquals(20, settings.defaultDurationFor(espresso))
        assertEquals(2, settings.defaultDurationFor(caffeinePill))
    }

    @Test
    fun drinkOverrideBeatsCategory() {
        val settings = UserSettings(
            categoryDurationMinutes = mapOf("coffee" to 20),
            drinkDurationMinutes = mapOf("espresso" to 1),
        )
        assertEquals(1, settings.defaultDurationFor(espresso))
    }

    @Test
    fun categoryIsMatchedRegardlessOfStoredCasing() {
        val customCoffee = DrinkPreset(itemId = "custom-1", name = "Mine", category = "Coffee")
        val settings = UserSettings(categoryDurationMinutes = mapOf("coffee" to 15))
        assertEquals(15, settings.defaultDurationFor(customCoffee))
    }

    @Test
    fun durationMapRoundTripsAndDropsGarbage() {
        val map = mapOf("espresso" to 3, "custom-17000" to 180)
        assertEquals(map, decodeDurationMap(encodeDurationMap(map)))
        assertEquals(
            mapOf("tea" to 5),
            decodeDurationMap(setOf("tea=5", "=4", "coffee=abc", "noequals")),
        )
    }

    @Test
    fun releaseTimeBeatsEveryFinishTimeDefault() {
        val slowRelease = DrinkPreset(
            itemId = "custom-xr",
            name = "XR pill",
            category = "Pill",
            releaseMinutes = 480,
        )
        val settings = UserSettings(
            defaultDurationMinutes = 5,
            categoryDurationMinutes = mapOf("pill" to 2),
            drinkDurationMinutes = mapOf("custom-xr" to 1),
        )
        assertEquals(480, settings.defaultDurationFor(slowRelease))
    }

    @Test
    fun releaseTimeIsClampedToItsOwnRangeNotTheFinishTimeCap() {
        assertEquals(MAX_RELEASE_MINUTES, coerceReleaseMinutes(10_000))
        assertEquals(MIN_RELEASE_MINUTES, coerceReleaseMinutes(0))
        assertEquals(600, coerceReleaseMinutes(600))
    }

    @Test
    fun resolveRecentConsumptionDuration_tracksExtendedReleaseEditsInBothDirections() {
        val settings = UserSettings()
        val editedTo8hXr = DrinkPreset(
            itemId = "custom-pill",
            name = "My Capsule",
            category = "pill",
            releaseMinutes = 480,
        )
        val editedTo12hXr = editedTo8hXr.copy(releaseMinutes = 720)
        val revertedToRegularPill = editedTo8hXr.copy(releaseMinutes = null)

        // Regular 1-minute log after enabling 8h extended release -> 480 min.
        assertEquals(480, resolveRecentConsumptionDuration(1, editedTo8hXr, settings))
        // 8h XR log after changing preset to 12h extended release -> 720 min.
        assertEquals(720, resolveRecentConsumptionDuration(480, editedTo12hXr, settings))
        // 8h XR log after turning extended release off -> falls back to regular pill default (1 min).
        assertEquals(1, resolveRecentConsumptionDuration(480, revertedToRegularPill, settings))
        // Regular drink's custom finish duration (<= 180 min) is preserved as-is.
        assertEquals(25, resolveRecentConsumptionDuration(25, espresso, settings))
        // Deleted preset (null) preserves the historical logged duration.
        assertEquals(15, resolveRecentConsumptionDuration(15, null, settings))
    }

    @Test
    fun resolveVisibleDrinkDurationOverrides_hidesDeletedAndExtendedReleasePresets() {
        val xrCustom = DrinkPreset(
            itemId = "custom-xr",
            name = "Slow Capsule",
            category = "pill",
            releaseMinutes = 480,
        )
        val regularCustom = DrinkPreset(
            itemId = "custom-latte",
            name = "Morning Latte",
            category = "coffee",
        )
        val overrides = mapOf(
            "espresso" to 3,
            "custom-latte" to 20,
            "custom-xr" to 5,
            "custom-deleted" to 12,
        )

        // While catalog is still loading, don't flash raw itemIds.
        assertTrue(resolveVisibleDrinkDurationOverrides(overrides, emptyList()).isEmpty())

        assertEquals(
            listOf(
                Triple("espresso", "Espresso", 3),
                Triple("custom-latte", "Morning Latte", 20),
            ),
            resolveVisibleDrinkDurationOverrides(
                drinkDurationMinutes = overrides,
                presets = listOf(espresso, xrCustom, regularCustom),
            ),
        )
    }
}

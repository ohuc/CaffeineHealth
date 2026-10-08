package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.decodeDurationMap
import com.uc.caffeine.data.encodeDurationMap
import com.uc.caffeine.data.model.DEFAULT_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.data.model.DrinkPreset
import org.junit.Assert.assertEquals
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
}

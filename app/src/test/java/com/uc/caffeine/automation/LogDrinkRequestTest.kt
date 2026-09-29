package com.uc.caffeine.automation

import com.uc.caffeine.data.model.DEFAULT_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogDrinkRequestTest {

    private val now = 1_750_000_000_000L

    private fun parseValid(extras: Map<String, Any?>): LogDrinkRequest {
        val result = LogDrinkRequest.parse(extras, now)
        assertTrue("expected Valid but was $result", result is LogDrinkRequest.ParseResult.Valid)
        return (result as LogDrinkRequest.ParseResult.Valid).request
    }

    private fun assertInvalid(extras: Map<String, Any?>) {
        val result = LogDrinkRequest.parse(extras, now)
        assertTrue("expected Invalid but was $result", result is LogDrinkRequest.ParseResult.Invalid)
    }

    @Test
    fun parse_appliesDefaultsWhenOnlyNameGiven() {
        val request = parseValid(mapOf("drink_name" to "Espresso"))

        assertEquals("Espresso", request.drinkName)
        assertNull(request.drinkId)
        assertNull(request.unitKey)
        assertEquals(1.0, request.quantity, 0.0)
        assertNull(request.caffeineMg)
        assertEquals(now, request.startedAtMillis)
        assertEquals(DEFAULT_CONSUMPTION_DURATION_MINUTES, request.durationMinutes)
    }

    @Test
    fun parse_acceptsNumbersSentAsStrings() {
        // Tasker and most automation apps send every extra as a String.
        val request = parseValid(
            mapOf(
                "drink_id" to "espresso",
                "quantity" to "2.5",
                "caffeine_mg" to "150",
                "duration_minutes" to "20",
                "timestamp" to (now - 60_000).toString(),
            ),
        )

        assertEquals(2.5, request.quantity, 0.0)
        assertEquals(150, request.caffeineMg)
        assertEquals(20, request.durationMinutes)
        assertEquals(now - 60_000, request.startedAtMillis)
    }

    @Test
    fun parse_treatsSmallTimestampAsEpochSeconds() {
        val request = parseValid(mapOf("drink_name" to "Tea", "timestamp" to (now / 1000 - 600)))

        assertEquals(now - 600_000, request.startedAtMillis)
    }

    @Test
    fun parse_rejectsMissingDrink() {
        assertInvalid(emptyMap())
        assertInvalid(mapOf("drink_name" to "   "))
    }

    @Test
    fun parse_rejectsOutOfRangeValues() {
        assertInvalid(mapOf("drink_name" to "Espresso", "quantity" to 0))
        assertInvalid(mapOf("drink_name" to "Espresso", "quantity" to "lots"))
        assertInvalid(mapOf("drink_name" to "Espresso", "caffeine_mg" to 5000))
        assertInvalid(mapOf("drink_name" to "Espresso", "duration_minutes" to 0))
        assertInvalid(mapOf("drink_name" to "Espresso", "timestamp" to now + 60 * 60_000))
        assertInvalid(mapOf("drink_name" to "Espresso", "timestamp" to now - 8L * 24 * 60 * 60_000))
    }

    private val espresso = DrinkPreset(
        id = 7,
        itemId = "espresso",
        name = "Espresso",
        emoji = "☕",
        absorptionRate = 40,
        defaultUnit = "shot",
        defaultCaffeineMg = 77,
    )
    private val espressoUnits = listOf(
        DrinkUnit(drinkId = 7, unitKey = "shot", caffeineMg = 77.0, milliliters = 30.0, grams = null, isDefault = true),
        DrinkUnit(drinkId = 7, unitKey = "cup(large)", caffeineMg = 154.0, milliliters = 60.0, grams = null),
    )

    @Test
    fun resolve_usesDefaultUnitForPreset() {
        val request = parseValid(mapOf("drink_name" to "espresso", "quantity" to 2))
        val result = resolveAutomationEntry(request, espresso, espressoUnits)

        result as AutomationEntryResolution.Resolved
        assertEquals("Espresso", result.entry.drinkName)
        assertEquals("espresso", result.entry.presetItemId)
        assertEquals("shot", result.entry.unitKey)
        assertEquals(154, result.entry.caffeineMg)
        assertEquals(40, result.entry.absorptionRate)
        assertEquals(60.0, result.volumeMl!!, 0.0)
    }

    @Test
    fun resolve_matchesUnitIgnoringCaseAndSpaces() {
        val request = parseValid(mapOf("drink_id" to "espresso", "unit" to "Cup (Large)"))
        val result = resolveAutomationEntry(request, espresso, espressoUnits)

        result as AutomationEntryResolution.Resolved
        assertEquals("cup(large)", result.entry.unitKey)
        assertEquals(154, result.entry.caffeineMg)
    }

    @Test
    fun resolve_failsOnUnknownUnit() {
        val request = parseValid(mapOf("drink_id" to "espresso", "unit" to "bucket"))

        assertTrue(resolveAutomationEntry(request, espresso, espressoUnits) is AutomationEntryResolution.Failed)
    }

    @Test
    fun resolve_caffeineOverrideReplacesPresetAmount() {
        val request = parseValid(mapOf("drink_id" to "espresso", "caffeine_mg" to 100))
        val result = resolveAutomationEntry(request, espresso, espressoUnits)

        result as AutomationEntryResolution.Resolved
        assertEquals(100, result.entry.caffeineMg)
        assertEquals(100.0, result.entry.unitCaffeineMg, 0.0)
    }

    @Test
    fun resolve_logsCustomDrinkWhenNoPresetButCaffeineGiven() {
        val request = parseValid(mapOf("drink_name" to "Office brew", "caffeine_mg" to 120))
        val result = resolveAutomationEntry(request, preset = null, units = emptyList())

        result as AutomationEntryResolution.Resolved
        assertEquals("Office brew", result.entry.drinkName)
        assertEquals(120, result.entry.caffeineMg)
        assertEquals("", result.entry.presetItemId)
        assertNull(result.volumeMl)
    }

    @Test
    fun resolve_failsWhenNoPresetAndNoCaffeine() {
        val request = parseValid(mapOf("drink_name" to "Mystery drink"))

        assertTrue(resolveAutomationEntry(request, preset = null, units = emptyList()) is AutomationEntryResolution.Failed)
    }
}

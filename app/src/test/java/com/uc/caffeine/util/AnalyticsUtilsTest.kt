package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsUtilsTest {

    @Test
    fun buildAnalyticsUiState_weekRangeHonorsLocalDayBoundaries() {
        val settings = UserSettings(timeZoneId = "America/Los_Angeles")
        val presets = listOf(testPreset(itemId = "coffee-1", category = "coffee"))
        val entries = listOf(
            testEntry(
                id = 1,
                startedAt = "2026-04-03T06:30:00Z",
                caffeineMg = 110,
                presetItemId = "coffee-1",
            ),
            testEntry(
                id = 2,
                startedAt = "2026-04-03T07:30:00Z",
                caffeineMg = 90,
                presetItemId = "coffee-1",
            ),
        )

        val uiState = buildAnalyticsUiState(
            entries = entries,
            presets = presets,
            settings = settings,
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = Instant.parse("2026-04-09T20:00:00Z").toEpochMilli(),
            locale = Locale.US,
        )

        assertEquals(200, uiState.totalCaffeineMg)
        assertEquals(listOf("Coffee"), uiState.sourceAxisLabels)
    }

    @Test
    fun buildAnalyticsUiState_usesOtherBucketWhenPresetMappingIsMissing() {
        val uiState = buildAnalyticsUiState(
            entries = listOf(
                testEntry(
                    id = 1,
                    startedAt = "2026-04-08T10:00:00Z",
                    caffeineMg = 80,
                    presetItemId = "",
                ),
            ),
            presets = emptyList(),
            settings = UserSettings(timeZoneId = "UTC"),
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = Instant.parse("2026-04-09T12:00:00Z").toEpochMilli(),
            locale = Locale.US,
        )

        assertEquals("Other", uiState.topSourceLabel)
        assertEquals(listOf("Other"), uiState.sourceAxisLabels)
    }

    @Test
    fun buildAnalyticsUiState_preservesSingleCategorySeries() {
        val uiState = buildAnalyticsUiState(
            entries = listOf(
                testEntry(
                    id = 1,
                    startedAt = "2026-04-08T10:00:00Z",
                    caffeineMg = 95,
                    presetItemId = "coffee-1",
                ),
            ),
            presets = listOf(testPreset(itemId = "coffee-1", category = "coffee")),
            settings = UserSettings(timeZoneId = "UTC"),
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = Instant.parse("2026-04-09T12:00:00Z").toEpochMilli(),
            locale = Locale.US,
        )

        assertTrue(uiState.hasData)
        assertEquals(listOf("Coffee"), uiState.sourceAxisLabels)
        assertEquals(listOf(95.0), uiState.sourceValues)
    }

    @Test
    fun buildAnalyticsUiState_assignsConsumptionIntoTimeOfDayBuckets() {
        val presets = listOf(testPreset(itemId = "coffee-1", category = "coffee"))
        val entries = listOf(
            testEntry(id = 1, startedAt = "2026-04-09T01:00:00Z", caffeineMg = 25, presetItemId = "coffee-1"),
            testEntry(id = 2, startedAt = "2026-04-09T08:00:00Z", caffeineMg = 50, presetItemId = "coffee-1"),
            testEntry(id = 3, startedAt = "2026-04-09T13:00:00Z", caffeineMg = 75, presetItemId = "coffee-1"),
            testEntry(id = 4, startedAt = "2026-04-09T17:00:00Z", caffeineMg = 100, presetItemId = "coffee-1"),
        )

        val uiState = buildAnalyticsUiState(
            entries = entries,
            presets = presets,
            settings = UserSettings(timeZoneId = "UTC"),
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = Instant.parse("2026-04-09T22:00:00Z").toEpochMilli(),
            locale = Locale.US,
        )

        assertEquals(listOf(25.0, 50.0, 75.0, 100.0), uiState.timeOfDayValues)
    }

    @Test
    fun buildAnalyticsUiState_buildsExpectedBedtimeSeriesLengthsForEachRange() {
        val presets = listOf(testPreset(itemId = "coffee-1", category = "coffee"))
        val entries = listOf(
            testEntry(
                id = 1,
                startedAt = "2026-04-09T09:00:00Z",
                caffeineMg = 95,
                presetItemId = "coffee-1",
            ),
        )
        val nowMillis = Instant.parse("2026-04-09T12:00:00Z").toEpochMilli()
        val settings = UserSettings(timeZoneId = "UTC")

        val todayUiState = buildAnalyticsUiState(
            entries = entries,
            presets = presets,
            settings = settings,
            selectedRange = AnalyticsRange.TODAY,
            nowMillis = nowMillis,
            locale = Locale.US,
        )
        val last30UiState = buildAnalyticsUiState(
            entries = entries,
            presets = presets,
            settings = settings,
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = nowMillis,
            locale = Locale.US,
        )
        val last90UiState = buildAnalyticsUiState(
            entries = entries,
            presets = presets,
            settings = settings,
            selectedRange = AnalyticsRange.LAST_90_DAYS,
            nowMillis = nowMillis,
            locale = Locale.US,
        )

        assertEquals(1, todayUiState.bedtimeValues.size)
        assertEquals(5, last30UiState.bedtimeValues.size)
        assertEquals(4, last90UiState.bedtimeValues.size)
    }

    @Test
    fun buildAnalyticsUiState_countsSafeNightsAgainstSleepThreshold() {
        val presets = listOf(testPreset(itemId = "energy-1", category = "energy_drink"))
        val uiState = buildAnalyticsUiState(
            entries = listOf(
                testEntry(
                    id = 1,
                    startedAt = "2026-04-08T22:30:00Z",
                    caffeineMg = 200,
                    presetItemId = "energy-1",
                    absorptionRate = 30,
                ),
            ),
            presets = presets,
            settings = UserSettings(
                halfLifeMinutes = 60,
                sleepThresholdMg = 60,
                sleepTimeHour = 23,
                sleepTimeMinute = 0,
                timeZoneId = "UTC",
            ),
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = Instant.parse("2026-04-09T12:00:00Z").toEpochMilli(),
            locale = Locale.US,
        )

        assertEquals(29, uiState.safeNights)
        assertEquals(30, uiState.totalNights)
    }

    @Test
    fun buildAnalyticsUiState_returnsEmptyStateForZeroDataRange() {
        val uiState = buildAnalyticsUiState(
            entries = emptyList(),
            presets = emptyList(),
            settings = UserSettings(timeZoneId = "UTC"),
            selectedRange = AnalyticsRange.LAST_30_DAYS,
            nowMillis = Instant.parse("2026-04-09T12:00:00Z").toEpochMilli(),
            locale = Locale.US,
        )

        assertFalse(uiState.hasData)
        assertEquals(0, uiState.totalCaffeineMg)
        assertTrue(uiState.sourceAxisLabels.isEmpty())
        assertTrue(uiState.bedtimeAxisLabels.isEmpty())
    }

    private fun testPreset(
        itemId: String,
        category: String,
    ): DrinkPreset {
        return DrinkPreset(
            itemId = itemId,
            name = itemId,
            category = category,
            defaultCaffeineMg = 95,
        )
    }

    private fun testEntry(
        id: Int,
        startedAt: String,
        caffeineMg: Int = 95,
        presetItemId: String,
        absorptionRate: Int = 45,
    ): ConsumptionEntry {
        return ConsumptionEntry(
            id = id,
            drinkName = "Drink $id",
            caffeineMg = caffeineMg,
            emoji = "\u2615",
            presetItemId = presetItemId,
            absorptionRate = absorptionRate,
            startedAtMillis = Instant.parse(startedAt).toEpochMilli(),
        )
    }
}

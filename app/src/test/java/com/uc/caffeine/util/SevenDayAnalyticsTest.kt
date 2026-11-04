package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SevenDayAnalyticsTest {
    @Test fun `seven dates include local midnight and exclude the eighth day`() {
        val settings = UserSettings(useSystemTimeZone = false, timeZoneId = "Asia/Kolkata")
        fun entry(time: String) = ConsumptionEntry(drinkName = "Coffee", caffeineMg = 50, emoji = "", startedAtMillis = Instant.parse(time).toEpochMilli())
        val result = buildAnalyticsUiState(listOf(
            entry("2026-10-24T18:29:59Z"), // Oct 24, excluded
            entry("2026-10-24T18:30:00Z"), // Oct 25, included
            entry("2026-10-31T18:29:59Z"), // Oct 31, included
            entry("2026-10-31T18:30:00Z"), // Nov 1, excluded
        ), emptyList(), settings, AnalyticsRange.LAST_7_DAYS, Instant.parse("2026-10-31T10:00:00Z").toEpochMilli())
        assertEquals(100, result.totalCaffeineMg)
        assertEquals(7, result.totalNights)
        assertEquals(7, result.bedtimeValues.size)
    }
    @Test fun `daylight saving week counts calendar dates rather than 168 hours`() {
        val settings = UserSettings(useSystemTimeZone = false, timeZoneId = "America/New_York")
        val entry = ConsumptionEntry(drinkName = "Tea", caffeineMg = 30, emoji = "", startedAtMillis = Instant.parse("2026-10-27T04:00:00Z").toEpochMilli())
        val result = buildAnalyticsUiState(listOf(entry), emptyList(), settings, AnalyticsRange.LAST_7_DAYS, Instant.parse("2026-11-02T23:00:00Z").toEpochMilli())
        assertEquals(30, result.totalCaffeineMg)
        assertEquals(7, result.totalNights)
    }
    @Test fun `free aggregation does not retain an extended bedtime analysis map`() {
        val settings = UserSettings(useSystemTimeZone = false, timeZoneId = "UTC")
        val old = ConsumptionEntry(drinkName = "Tea", caffeineMg = 30, emoji = "", startedAtMillis = Instant.parse("2026-09-01T10:00:00Z").toEpochMilli())
        val result = buildAnalyticsUiState(listOf(old), emptyList(), settings, AnalyticsRange.LAST_7_DAYS,
            Instant.parse("2026-10-31T10:00:00Z").toEpochMilli(), includeExtendedHistory = false)
        assertTrue(result.bedtimeDailyStats.isEmpty())
        assertEquals(0, result.currentSleepStreak)
    }
}

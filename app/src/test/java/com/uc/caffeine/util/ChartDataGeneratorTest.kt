package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataGeneratorTest {

    private val utcSettings = UserSettings(
        sleepTimeHour = 23,
        sleepTimeMinute = 0,
        timeZoneId = "UTC", useSystemTimeZone = false,
    )

    @Test
    fun generateChartData_covers_allHistory_withAnchorAndFutureWindow() {
        val currentTime = Instant.parse("2026-04-05T12:00:00Z").toEpochMilli()
        val oldestEntry = testEntry(
            id = 1,
            emoji = "\u2615",
            startedAt = "2026-02-20T09:10:00Z",
        )

        val data = ChartDataGenerator.generateChartData(
            entries = listOf(oldestEntry),
            settings = utcSettings,
            currentTime = currentTime,
        )

        val expectedStart = roundDownToInterval(oldestEntry.startedAtMillis, SIX_HOURS_MILLIS) - SIX_HOURS_MILLIS
        val expectedEnd = Instant.parse("2026-04-06T00:00:00Z").toEpochMilli()

        assertEquals(expectedStart, data.domainStartMillis)
        assertEquals(expectedStart, data.dataPoints.first().timestampMillis)
        assertEquals(expectedEnd, data.dataPoints.last().timestampMillis)
    }

    @Test
    fun generateChartData_usesAdaptiveSamplingAtThirtySevenAndOneDayBoundaries() {
        val currentTime = Instant.parse("2026-04-05T12:00:00Z").toEpochMilli()
        val data = ChartDataGenerator.generateChartData(
            entries = listOf(testEntry(id = 1, startedAt = "2026-02-20T09:10:00Z")),
            settings = utcSettings,
            currentTime = currentTime,
        )

        val timestamps = data.dataPoints.map { it.timestampMillis }
        val boundary30Days = Instant.parse("2026-03-06T12:00:00Z").toEpochMilli()
        val boundary7Days = Instant.parse("2026-03-29T12:00:00Z").toEpochMilli()
        val boundary1Day = Instant.parse("2026-04-04T12:00:00Z").toEpochMilli()

        assertTrue(timestamps.contains(boundary30Days))
        assertTrue(timestamps.contains(boundary7Days))
        assertTrue(timestamps.contains(boundary1Day))

        assertEquals(THREE_HOURS_MILLIS, diffAfter(boundary30Days, timestamps))
        assertEquals(ONE_HOUR_MILLIS, diffAfter(boundary7Days, timestamps))
        assertEquals(FIFTEEN_MINUTES_MILLIS, diffAfter(boundary1Day, timestamps))

        val allDiffs = timestamps.zipWithNext { a, b -> b - a }.toSet()
        assertTrue(allDiffs.contains(SIX_HOURS_MILLIS))
        assertTrue(allDiffs.contains(THREE_HOURS_MILLIS))
        assertTrue(allDiffs.contains(ONE_HOUR_MILLIS))
        assertTrue(allDiffs.contains(FIFTEEN_MINUTES_MILLIS))
    }

    @Test
    fun generateChartData_extendsToBedtimeWhenBedtimeIsLaterThanTwelveHoursAhead() {
        val currentTime = Instant.parse("2026-04-05T01:00:00Z").toEpochMilli()
        val data = ChartDataGenerator.generateChartData(
            entries = emptyList(),
            settings = utcSettings,
            currentTime = currentTime,
        )

        val expectedBedtime = Instant.parse("2026-04-05T23:00:00Z").toEpochMilli()

        assertEquals(expectedBedtime, data.bedtimeMillis)
        assertEquals(expectedBedtime, data.dataPoints.last().timestampMillis)
    }

    @Test
    fun generateChartData_groupsMarkersAcrossMixedResolutionBuckets() {
        val currentTime = Instant.parse("2026-04-05T12:00:00Z").toEpochMilli()
        val entries = listOf(
            testEntry(id = 1, emoji = "\u2615", startedAt = "2026-02-20T01:10:00Z"),
            testEntry(id = 2, emoji = "\uD83C\uDF75", startedAt = "2026-02-20T02:40:00Z"),
            testEntry(id = 3, emoji = "\uD83E\uDD64", startedAt = "2026-03-24T01:10:00Z"),
            testEntry(id = 4, emoji = "\u26A1", startedAt = "2026-04-05T00:10:00Z"),
        )

        val data = ChartDataGenerator.generateChartData(
            entries = entries,
            settings = utcSettings,
            currentTime = currentTime,
        )

        assertEquals(3, data.consumptionMarkers.size)
        assertEquals(
            listOf("\u2615", "\uD83C\uDF75"),
            data.consumptionMarkers.first().entries.map { it.emoji },
        )
        assertEquals(
            ChartDataGenerator.timestampToDomainX(
                data.domainStartMillis,
                roundDownToInterval(entries.first().startedAtMillis, SIX_HOURS_MILLIS),
            ),
            data.consumptionMarkers.first().xValue,
            0.0,
        )
        assertTrue(data.consumptionMarkers[0].xValue < data.consumptionMarkers[1].xValue)
        assertTrue(data.consumptionMarkers[1].xValue < data.consumptionMarkers[2].xValue)
    }

    @Test
    fun generateChartData_extendsTailToZeroForRecentEntries() {
        val currentTime = Instant.parse("2026-04-05T12:00:00Z").toEpochMilli()
        val data = ChartDataGenerator.generateChartData(
            entries = listOf(
                testEntry(
                    id = 1,
                    startedAt = "2026-04-05T11:40:00Z",
                    caffeineMg = 120,
                    durationMinutes = 20,
                )
            ),
            settings = utcSettings,
            currentTime = currentTime,
        )

        assertEquals(0.0, data.dataPoints.last().caffeineLevel, 0.0)
        assertTrue(data.dataPoints.last().timestampMillis > currentTime)
        assertTrue(data.dataPoints.dropLast(1).any { it.caffeineLevel > 0.0 })
    }

    private fun diffAfter(
        timestamp: Long,
        timestamps: List<Long>,
    ): Long {
        val index = timestamps.indexOf(timestamp)
        check(index >= 0 && index < timestamps.lastIndex)
        return timestamps[index + 1] - timestamps[index]
    }

    private fun testEntry(
        id: Int,
        emoji: String = "\u2615",
        startedAt: String,
        caffeineMg: Int = 90,
        durationMinutes: Int = 10,
    ): ConsumptionEntry {
        return ConsumptionEntry(
            id = id,
            drinkName = "Drink $id",
            caffeineMg = caffeineMg,
            emoji = emoji,
            startedAtMillis = Instant.parse(startedAt).toEpochMilli(),
            durationMinutes = durationMinutes,
        )
    }

    private fun roundDownToInterval(
        timestamp: Long,
        intervalMillis: Long,
    ): Long {
        return timestamp - (timestamp % intervalMillis)
    }

    private companion object {
        const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000L
        const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
        const val THREE_HOURS_MILLIS = 3 * ONE_HOUR_MILLIS
        const val SIX_HOURS_MILLIS = 6 * ONE_HOUR_MILLIS
    }
}

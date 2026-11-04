package com.uc.caffeine.util

import com.uc.caffeine.data.AppDateFormat
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UserDateTimePreferencesTest {

    @Test
    fun resolvedZoneId_followsSystemWhenMatchingEnabled() {
        val settings = UserSettings(useSystemTimeZone = true, timeZoneId = "America/Los_Angeles")
        // Matching the system ignores the stored override and tracks the device zone live.
        assertEquals(ZoneId.systemDefault(), settings.resolvedZoneId())
    }

    @Test
    fun resolvedZoneId_usesManualOverrideWhenMatchingDisabled() {
        val settings = UserSettings(useSystemTimeZone = false, timeZoneId = "America/Los_Angeles")
        assertEquals(ZoneId.of("America/Los_Angeles"), settings.resolvedZoneId())
    }

    @Test
    fun resolvedZoneId_fallsBackToSystemWhenManualOverrideIsInvalid() {
        val settings = UserSettings(useSystemTimeZone = false, timeZoneId = "Not/AZone")
        assertEquals(ZoneId.systemDefault(), settings.resolvedZoneId())
    }

    @Test
    fun formatConsumptionDateHeader_returnsTodayAndYesterdayLabels() {
        val settings = UserSettings(timeZoneId = "UTC", useSystemTimeZone = false)
        val referenceTimeMillis = Instant.parse("2025-01-07T10:15:00Z").toEpochMilli()

        assertEquals(
            "TODAY",
            formatConsumptionDateHeader(
                date = LocalDate.of(2025, 1, 7),
                settings = settings,
                referenceTimeMillis = referenceTimeMillis,
                locale = Locale.US
            )
        )
        assertEquals(
            "YESTERDAY",
            formatConsumptionDateHeader(
                date = LocalDate.of(2025, 1, 6),
                settings = settings,
                referenceTimeMillis = referenceTimeMillis,
                locale = Locale.US
            )
        )
    }

    @Test
    fun formatConsumptionDateHeader_usesSelectedDateFormatForHistoricalDates() {
        val referenceTimeMillis = Instant.parse("2025-01-08T12:00:00Z").toEpochMilli()
        val historicalDate = LocalDate.of(2025, 1, 6)

        assertEquals(
            "MONDAY, 01/06/2025",
            formatConsumptionDateHeader(
                date = historicalDate,
                settings = UserSettings(
                    dateFormat = AppDateFormat.MONTH_DAY_YEAR,
                    timeZoneId = "UTC", useSystemTimeZone = false
                ),
                referenceTimeMillis = referenceTimeMillis,
                locale = Locale.US
            )
        )
        assertEquals(
            "MONDAY, 06/01/2025",
            formatConsumptionDateHeader(
                date = historicalDate,
                settings = UserSettings(
                    dateFormat = AppDateFormat.DAY_MONTH_YEAR,
                    timeZoneId = "UTC", useSystemTimeZone = false
                ),
                referenceTimeMillis = referenceTimeMillis,
                locale = Locale.US
            )
        )
        assertEquals(
            "MONDAY, 2025-01-06",
            formatConsumptionDateHeader(
                date = historicalDate,
                settings = UserSettings(
                    dateFormat = AppDateFormat.YEAR_MONTH_DAY,
                    timeZoneId = "UTC", useSystemTimeZone = false
                ),
                referenceTimeMillis = referenceTimeMillis,
                locale = Locale.US
            )
        )
    }

    @Test
    fun groupConsumptionEntriesByLocalDate_sortsNewestDaysAndEntriesFirst() {
        val jan5Morning = testEntry(id = 1, startedAt = "2025-01-05T09:00:00Z")
        val jan6Noon = testEntry(id = 2, startedAt = "2025-01-06T12:00:00Z")
        val jan5Evening = testEntry(id = 3, startedAt = "2025-01-05T18:00:00Z")

        val grouped = groupConsumptionEntriesByLocalDate(
            entries = listOf(jan5Morning, jan6Noon, jan5Evening),
            settings = UserSettings(timeZoneId = "UTC", useSystemTimeZone = false)
        )

        assertEquals(
            listOf(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 5)),
            grouped.keys.toList()
        )
        assertEquals(listOf(jan5Evening, jan5Morning), grouped.getValue(LocalDate.of(2025, 1, 5)))
    }

    @Test
    fun groupConsumptionEntriesByLocalDate_honorsTimezoneBoundaries() {
        val laterUtcEntry = testEntry(id = 1, startedAt = "2025-01-07T02:30:00Z")
        val earlierUtcEntry = testEntry(id = 2, startedAt = "2025-01-06T23:30:00Z")

        val grouped = groupConsumptionEntriesByLocalDate(
            entries = listOf(earlierUtcEntry, laterUtcEntry),
            settings = UserSettings(timeZoneId = "America/Los_Angeles", useSystemTimeZone = false)
        )

        assertEquals(listOf(LocalDate.of(2025, 1, 6)), grouped.keys.toList())
        assertEquals(
            listOf(laterUtcEntry, earlierUtcEntry),
            grouped.getValue(LocalDate.of(2025, 1, 6))
        )
    }

    @Test
    fun groupConsumptionEntriesByLocalDate_regroupsWhenTimezoneChanges() {
        val laterUtcEntry = testEntry(id = 1, startedAt = "2025-01-07T02:30:00Z")
        val earlierUtcEntry = testEntry(id = 2, startedAt = "2025-01-06T23:30:00Z")
        val entries = listOf(earlierUtcEntry, laterUtcEntry)

        val utcGrouped = groupConsumptionEntriesByLocalDate(
            entries = entries,
            settings = UserSettings(timeZoneId = "UTC", useSystemTimeZone = false)
        )
        val losAngelesGrouped = groupConsumptionEntriesByLocalDate(
            entries = entries,
            settings = UserSettings(timeZoneId = "America/Los_Angeles", useSystemTimeZone = false)
        )

        assertEquals(
            listOf(LocalDate.of(2025, 1, 7), LocalDate.of(2025, 1, 6)),
            utcGrouped.keys.toList()
        )
        assertEquals(listOf(LocalDate.of(2025, 1, 6)), losAngelesGrouped.keys.toList())
    }

    @Test
    fun calculateNextBedtimeMillis_singleBedtimeBehavesAsBefore() {
        val settings = UserSettings(sleepTimeHour = 1, sleepTimeMinute = 0, timeZoneId = "UTC", useSystemTimeZone = false)

        assertEquals(
            Instant.parse("2025-01-07T01:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-07T00:30:00Z").toEpochMilli(), settings),
        )
        assertEquals(
            Instant.parse("2025-01-08T01:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-07T02:00:00Z").toEpochMilli(), settings),
        )
        val evening = settings.copy(sleepTimeHour = 23)
        assertEquals(
            Instant.parse("2025-01-07T23:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-07T10:00:00Z").toEpochMilli(), evening),
        )
    }

    @Test
    fun calculateNextBedtimeMillis_rotaAfterMidnightBelongsToThatDaysNight() {
        // 2025-01-10 is a Friday. Friday's rota bedtime 1:00 AM = early Saturday morning;
        // every other night is 11 PM.
        val settings = UserSettings(
            sleepTimeHour = 23,
            sleepTimeMinute = 0,
            weeklySleepRotaEnabled = true,
            weeklySleepRota = mapOf(DayOfWeek.FRIDAY to LocalTime.of(1, 0)),
            timeZoneId = "UTC",
            useSystemTimeZone = false,
        )

        // Friday evening: next bedtime is Friday night's 1 AM (on Saturday), not 11 PM.
        assertEquals(
            Instant.parse("2025-01-11T01:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-10T20:00:00Z").toEpochMilli(), settings),
        )
        // Friday 00:30: Thursday's 11 PM has passed; Friday morning is not a bedtime any more.
        assertEquals(
            Instant.parse("2025-01-11T01:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-10T00:30:00Z").toEpochMilli(), settings),
        )
        // Saturday 00:30: Friday night's bedtime is still ahead.
        assertEquals(
            Instant.parse("2025-01-11T01:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-11T00:30:00Z").toEpochMilli(), settings),
        )
        // Saturday 02:00: next is Saturday's 11 PM.
        assertEquals(
            Instant.parse("2025-01-11T23:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(Instant.parse("2025-01-11T02:00:00Z").toEpochMilli(), settings),
        )
    }

    @Test
    fun formatConsumptionDateHeader_usesProvidedLabels() {
        val settings = UserSettings(timeZoneId = "UTC", useSystemTimeZone = false)
        val reference = Instant.parse("2025-01-07T10:15:00Z").toEpochMilli()

        assertEquals(
            "HOY",
            formatConsumptionDateHeader(
                date = LocalDate.of(2025, 1, 7),
                settings = settings,
                referenceTimeMillis = reference,
                locale = Locale.US,
                todayLabel = "Hoy",
                yesterdayLabel = "Ayer",
            ),
        )
    }

    private fun testEntry(
        id: Int,
        startedAt: String,
    ): ConsumptionEntry {
        return ConsumptionEntry(
            id = id,
            drinkName = "Coffee $id",
            caffeineMg = 95,
            emoji = "\u2615",
            startedAtMillis = Instant.parse(startedAt).toEpochMilli()
        )
    }
}

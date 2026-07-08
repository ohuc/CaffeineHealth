package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySleepRotaTest {

    // 2026-06-10 is a Wednesday.
    private val wednesdayNoon = Instant.parse("2026-06-10T12:00:00Z").toEpochMilli()

    private val baseSettings = UserSettings(
        sleepTimeHour = 23,
        sleepTimeMinute = 0,
        timeZoneId = "UTC", useSystemTimeZone = false,
    )

    @Test
    fun nextBedtime_usesTypicalTime_whenRotaDisabled() {
        val settings = baseSettings.copy(
            weeklySleepRotaEnabled = false,
            weeklySleepRota = mapOf(DayOfWeek.WEDNESDAY to LocalTime.of(21, 30)),
        )

        assertEquals(
            Instant.parse("2026-06-10T23:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(wednesdayNoon, settings),
        )
    }

    @Test
    fun nextBedtime_usesTodaysRotaTime_whenRotaEnabled() {
        val settings = baseSettings.copy(
            weeklySleepRotaEnabled = true,
            weeklySleepRota = mapOf(DayOfWeek.WEDNESDAY to LocalTime.of(21, 30)),
        )

        assertEquals(
            Instant.parse("2026-06-10T21:30:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(wednesdayNoon, settings),
        )
    }

    @Test
    fun nextBedtime_rollsToTomorrowsRotaTime_whenTodaysBedtimeHasPassed() {
        val wednesdayLateEvening = Instant.parse("2026-06-10T22:00:00Z").toEpochMilli()
        val settings = baseSettings.copy(
            weeklySleepRotaEnabled = true,
            weeklySleepRota = mapOf(
                DayOfWeek.WEDNESDAY to LocalTime.of(21, 30),
                DayOfWeek.THURSDAY to LocalTime.of(22, 15),
            ),
        )

        assertEquals(
            Instant.parse("2026-06-11T22:15:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(wednesdayLateEvening, settings),
        )
    }

    @Test
    fun nextBedtime_fallsBackToTypicalTime_forDaysWithoutRotaEntry() {
        val settings = baseSettings.copy(
            weeklySleepRotaEnabled = true,
            weeklySleepRota = mapOf(DayOfWeek.MONDAY to LocalTime.of(21, 0)),
        )

        assertEquals(
            Instant.parse("2026-06-10T23:00:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(wednesdayNoon, settings),
        )
    }

    @Test
    fun nextBedtime_healthConnectOverride_beatsRota() {
        val settings = baseSettings.copy(
            weeklySleepRotaEnabled = true,
            weeklySleepRota = mapOf(DayOfWeek.WEDNESDAY to LocalTime.of(21, 30)),
            hcSleepEnabled = true,
            hcSleepTimeHour = 22,
            hcSleepTimeMinute = 10,
        )

        assertEquals(
            Instant.parse("2026-06-10T22:10:00Z").toEpochMilli(),
            calculateNextBedtimeMillis(wednesdayNoon, settings),
        )
    }

    @Test
    fun effectiveSleepTimeFor_resolvesEachDayIndependently() {
        val settings = baseSettings.copy(
            weeklySleepRotaEnabled = true,
            weeklySleepRota = mapOf(
                DayOfWeek.WEDNESDAY to LocalTime.of(21, 30),
                DayOfWeek.FRIDAY to LocalTime.of(1, 0),
            ),
        )

        assertEquals(LocalTime.of(21, 30), settings.effectiveSleepTimeFor(DayOfWeek.WEDNESDAY))
        assertEquals(LocalTime.of(1, 0), settings.effectiveSleepTimeFor(DayOfWeek.FRIDAY))
        assertEquals(LocalTime.of(23, 0), settings.effectiveSleepTimeFor(DayOfWeek.SATURDAY))
    }
}

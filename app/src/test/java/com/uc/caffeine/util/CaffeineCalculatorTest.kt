package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineCalculatorTest {

    @Test
    fun calculatePeakTime_longerDurationProducesLaterFlatterPeak() {
        val quickDrink = testEntry(
            id = 1,
            startedAt = "2026-04-05T12:00:00Z",
            durationMinutes = 1,
        )
        val slowDrink = testEntry(
            id = 2,
            startedAt = "2026-04-05T12:00:00Z",
            durationMinutes = 10,
        )

        val quickPeakTime = CaffeineCalculator.calculatePeakTime(quickDrink, halfLifeMinutes = 300)
        val slowPeakTime = CaffeineCalculator.calculatePeakTime(slowDrink, halfLifeMinutes = 300)
        val quickPeakLevel = CaffeineCalculator.calculateEntryContribution(quickDrink, quickPeakTime, 300)
        val slowPeakLevel = CaffeineCalculator.calculateEntryContribution(slowDrink, slowPeakTime, 300)

        assertTrue(slowPeakTime > quickPeakTime)
        assertTrue(slowPeakLevel < quickPeakLevel)
    }

    @Test
    fun calculatePeakTime_preservesAbsorptionRateDifferences() {
        val fastAbsorption = testEntry(
            id = 1,
            startedAt = "2026-04-05T12:00:00Z",
            absorptionRate = 20,
        )
        val slowAbsorption = testEntry(
            id = 2,
            startedAt = "2026-04-05T12:00:00Z",
            absorptionRate = 60,
        )

        val fastPeakTime = CaffeineCalculator.calculatePeakTime(fastAbsorption, halfLifeMinutes = 300)
        val slowPeakTime = CaffeineCalculator.calculatePeakTime(slowAbsorption, halfLifeMinutes = 300)

        assertTrue(slowPeakTime > fastPeakTime)
    }

    @Test
    fun predictTimeUntilLevel_returnsFutureTimestampForDistributedEntry() {
        val entry = testEntry(
            id = 1,
            startedAt = "2026-04-05T12:00:00Z",
            caffeineMg = 220,
            durationMinutes = 30,
        )
        val now = Instant.parse("2026-04-05T12:20:00Z").toEpochMilli()

        val predicted = CaffeineCalculator.predictTimeUntilLevel(
            entries = listOf(entry),
            targetLevelMg = 5.0,
            currentTimeMillis = now,
            halfLifeMinutes = 300,
        )

        assertNotNull(predicted)
        assertTrue(predicted!! > now)
    }

    @Test
    fun calculatePeakTime_neverLiesPastItsSearchWindow() {
        // timeUntilPeak skips entries whose search window has closed, which is only sound if the
        // peak is always inside that window — including for a long extended release (#58).
        listOf(1, 10, 180, 480, 1440).forEach { minutes ->
            val entry = testEntry(id = minutes, startedAt = "2026-04-05T08:00:00Z", durationMinutes = minutes)
            val peak = CaffeineCalculator.calculatePeakTime(entry, halfLifeMinutes = 300)
            assertTrue(peak <= CaffeineCalculator.peakSearchEndMillis(entry))
            assertTrue(peak >= entry.startedAtMillis)
        }
    }

    @Test
    fun extendedRelease_peaksLowerAndLaterThanTheSameDoseAtOnce() {
        val atOnce = testEntry(id = 1, startedAt = "2026-04-05T08:00:00Z", caffeineMg = 200, durationMinutes = 1)
        val released = testEntry(id = 2, startedAt = "2026-04-05T08:00:00Z", caffeineMg = 200, durationMinutes = 480)

        val atOncePeak = CaffeineCalculator.calculatePeakTime(atOnce, halfLifeMinutes = 300)
        val releasedPeak = CaffeineCalculator.calculatePeakTime(released, halfLifeMinutes = 300)

        assertTrue(releasedPeak > atOncePeak)
        assertTrue(
            CaffeineCalculator.calculateEntryContribution(released, releasedPeak, 300) <
                CaffeineCalculator.calculateEntryContribution(atOnce, atOncePeak, 300),
        )
    }

    private fun testEntry(
        id: Int,
        startedAt: String,
        caffeineMg: Int = 100,
        durationMinutes: Int = 10,
        absorptionRate: Int = 45,
    ): ConsumptionEntry {
        return ConsumptionEntry(
            id = id,
            drinkName = "Drink $id",
            caffeineMg = caffeineMg,
            emoji = "\u2615",
            absorptionRate = absorptionRate,
            startedAtMillis = Instant.parse(startedAt).toEpochMilli(),
            durationMinutes = durationMinutes,
        )
    }
}

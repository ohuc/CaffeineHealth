package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineToleranceTest {

    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-06-30T15:00:00Z").toEpochMilli()

    private fun entry(isoStart: String, mg: Int) = ConsumptionEntry(
        drinkName = "Coffee",
        caffeineMg = mg,
        emoji = "☕",
        startedAtMillis = Instant.parse(isoStart).toEpochMilli(),
    )

    /** One [mg] drink at 9am on each of the [days] days before [now]'s day. */
    private fun dailyIntake(days: Int, mg: Int) = (1..days).map { daysAgo ->
        entry("2026-06-${"%02d".format(30 - daysAgo)}T09:00:00Z", mg)
    }

    private fun tolerance(entries: List<ConsumptionEntry>, seed: Int = 0) =
        CaffeineTolerance.toleranceMgPerDay(entries, now, zone, seed)

    @Test
    fun noHistoryKeepsTheSeed() {
        assertEquals(200.0, tolerance(emptyList(), seed = 200), 0.0)
    }

    @Test
    fun steadyIntakeConvergesOnIt() {
        val result = tolerance(dailyIntake(days = 28, mg = 300))
        assertTrue("expected close to 300, was $result", result in 290.0..300.0)
    }

    @Test
    fun intakeAboveTheBaselineRaisesItAndBelowLowersIt() {
        val rising = tolerance(dailyIntake(days = 3, mg = 400), seed = 100)
        val falling = tolerance(dailyIntake(days = 3, mg = 0) + entry("2026-06-26T09:00:00Z", 0), seed = 300)
        assertTrue(rising > 100.0 && rising < 400.0)
        assertTrue(falling < 300.0)
    }

    @Test
    fun unloggedDaysBeforeTheFirstLogDoNotDecayTheSeed() {
        // Logged only yesterday, at exactly the seed: nothing should have pulled it down.
        val result = tolerance(listOf(entry("2026-06-29T09:00:00Z", 250)), seed = 250)
        assertEquals(250.0, result, 0.0001)
    }

    @Test
    fun todayIsIgnoredUntilItIsOver() {
        val history = dailyIntake(days = 10, mg = 200)
        val withBigToday = history + entry("2026-06-30T08:00:00Z", 600)
        assertEquals(tolerance(history), tolerance(withBigToday), 0.0)
    }

    @Test
    fun toleranceMakesTheCoachRecommendAtLeastAsMuch() {
        val base = CoachInput(
            nowMillis = Instant.parse("2026-06-16T14:00:00Z").toEpochMilli(),
            entries = emptyList(),
            halfLifeMinutes = 300,
            absorptionRateMinutes = 45,
            sleepThresholdMg = 100,
            bedtimeMillis = Instant.parse("2026-06-17T00:30:00Z").toEpochMilli(),
            wakeMillis = Instant.parse("2026-06-16T06:00:00Z").toEpochMilli(),
            lastNightSleepMinutes = 300,
            zoneId = zone,
        )
        val t = base.nowMillis
        val naiveBoost = CaffeineCoach.alertnessAt(t, base, 0, 100.0) - CaffeineCoach.alertnessAt(t, base, 0, 0.0)
        val tolerant = base.copy(toleranceMgPerDay = 400.0)
        val tolerantBoost =
            CaffeineCoach.alertnessAt(t, tolerant, 0, 100.0) - CaffeineCoach.alertnessAt(t, tolerant, 0, 0.0)
        assertTrue("tolerance should blunt the boost", tolerantBoost < naiveBoost)

        val naiveDose = (CaffeineCoach.recommend(base) as? CoachRecommendation.Recommend)?.doseMg ?: 0
        val tolerantDose = (CaffeineCoach.recommend(tolerant) as? CoachRecommendation.Recommend)?.doseMg ?: 0
        assertTrue("tolerant dose $tolerantDose < naive $naiveDose", tolerantDose >= naiveDose)
    }
}

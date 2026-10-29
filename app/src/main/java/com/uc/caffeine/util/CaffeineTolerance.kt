package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.exp

/**
 * Caffeine tolerance for the Coach (#59): a daily-intake baseline that rises when you take more
 * than it and falls when you take less — an exponential moving average of complete days.
 *
 * Tolerance to caffeine's alerting effect builds within days of regular use and fades over a week
 * or two of abstinence (Evans & Griffiths 1992; James & Rogers 2005), so the baseline adapts with a
 * one-week time constant. Like the rest of the Coach this is a heuristic, not a validated model.
 *
 * It only shifts how much caffeine the Coach thinks you need to feel the lift. It deliberately
 * leaves the sleep threshold alone: the evidence for tolerance to caffeine's effect on sleep is
 * weak, so raising the threshold with tolerance would just green-light more caffeine before bed.
 */
object CaffeineTolerance {

    /** Days for the baseline to cover ~63% of the gap to a new steady intake. */
    const val TIME_CONSTANT_DAYS = 7.0

    /**
     * Days of history replayed. After six time constants the starting value weighs e^-6 (~0.25%),
     * so older history wouldn't change the result.
     */
    const val LOOKBACK_DAYS = 42L

    const val MAX_SEED_MG_PER_DAY = 1000
    const val SEED_STEP_MG = 25

    private val dailyWeight = 1.0 - exp(-1.0 / TIME_CONSTANT_DAYS)

    /**
     * The tolerance baseline in mg/day as of [nowMillis]. [seedMgPerDay] is the user's habitual
     * intake before they started logging, which the baseline starts from on the first logged day
     * (or the start of the lookback). Today is left out until it's over, so the baseline — and the
     * Coach's advice — doesn't drift through the day as drinks are logged.
     */
    fun toleranceMgPerDay(
        entries: List<ConsumptionEntry>,
        nowMillis: Long,
        zoneId: ZoneId,
        seedMgPerDay: Int,
    ): Double {
        val seed = seedMgPerDay.coerceIn(0, MAX_SEED_MG_PER_DAY).toDouble()
        val today = localDate(nowMillis, zoneId)
        val intakeByDay = entries
            .groupBy { localDate(it.startedAtMillis, zoneId) }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.caffeineMg }.toDouble() }
        // Days before the first log aren't "zero caffeine", they're just unlogged.
        val firstLogged = intakeByDay.keys.minOrNull() ?: return seed
        var day = maxOf(firstLogged, today.minusDays(LOOKBACK_DAYS))

        var tolerance = seed
        while (day.isBefore(today)) {
            val intake = intakeByDay[day] ?: 0.0
            tolerance += dailyWeight * (intake - tolerance)
            day = day.plusDays(1)
        }
        return tolerance
    }

    private fun localDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
}

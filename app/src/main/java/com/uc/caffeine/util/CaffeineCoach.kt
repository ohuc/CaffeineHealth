package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import java.time.ZoneId
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Caffeine Coach — recommends the *minimum effective, optimally-timed* caffeine dose.
 *
 * Inspired by the US Department of Defense Unified Model of Performance / 2B-Alert line
 * of research (Ramakrishnan et al. 2016, SLEEP; Vital-Lopez et al. 2018, J Sleep Res),
 * which jointly integrates a two-process sleep model (homeostatic sleep pressure +
 * circadian rhythm) with a caffeine pharmacodynamic factor to predict alertness, then
 * optimises caffeine *timing and dose* — cutting total caffeine while sustaining target
 * alertness.
 *
 * This is a deliberately **simplified, heuristic** adaptation for a consumer app:
 *  - [Alertness] is a unitless 0-100 index, NOT a validated psychomotor-vigilance (PVT)
 *    prediction. The homeostatic/circadian/debt constants are literature-inspired but
 *    tuned for plausible behaviour (a ~2-3pm dip, a sleep-debt penalty), not clinically
 *    validated.
 *  - Caffeine "load" is the active body load (mg) from [CaffeineCalculator] — the app's
 *    existing one-compartment PK model — used as a proxy for plasma concentration in a
 *    saturating (Hill) dose-response.
 *
 * The optimiser returns the smallest dose, at the best time, that keeps predicted
 * alertness at or above [TARGET_ALERTNESS] through the user's focus window, WITHOUT
 * pushing residual caffeine at bedtime above their sleep threshold (so it never fights
 * the sleep-forecast feature).
 */
object CaffeineCoach {

    // ── Alertness model (heuristic; see class doc) ────────────────────────────
    private const val BASE_ALERTNESS = 95.0
    private const val TARGET_ALERTNESS = 75.0

    /** Homeostatic process: alertness falls the longer you stay awake, saturating. */
    private const val HOMEOSTATIC_AMPLITUDE = 30.0
    private const val WAKE_TIME_CONSTANT_HOURS = 12.0

    /** A full night's sleep debt removes roughly this many alertness points. */
    private const val SLEEP_DEBT_AMPLITUDE = 22.0

    /** Circadian post-lunch dip, modelled as a Gaussian centred mid-afternoon. */
    private const val POST_LUNCH_DIP_DEPTH = 8.0
    private const val POST_LUNCH_DIP_HOUR = 14.5
    private const val POST_LUNCH_DIP_WIDTH_HOURS = 2.0

    /** Caffeine pharmacodynamics: saturating boost vs active load (mg). */
    private const val CAFFEINE_EMAX = 35.0
    private const val CAFFEINE_EC50_MG = 60.0

    /**
     * Tolerance (#59) shifts the dose-response right: a habitual intake of this many mg/day
     * doubles the load needed for half the boost. Heuristic, like the constants above.
     */
    private const val TOLERANCE_EC50_DOUBLING_MG_PER_DAY = 300.0

    // ── Optimiser ─────────────────────────────────────────────────────────────
    /** Don't try to keep the user alert in the final wind-down before bed. */
    private const val WIND_DOWN_MINUTES = 120L
    /** Caffeine takes ~30-45 min to bite, so aim a dose slightly ahead of the dip. */
    private const val PRE_DIP_LEAD_MINUTES = 30L
    /**
     * A single dose is only sized to cover the next few hours (~one half-life of useful
     * lift). This keeps recommendations realistic — one coffee, not a day's worth at once —
     * and a later re-evaluation handles any subsequent dip.
     */
    private const val COVERAGE_HORIZON_MINUTES = 180L
    private const val SAMPLE_STEP_MINUTES = 15L
    private const val MAX_SAMPLES = 120
    private const val DOSE_STEP_MG = 10
    private const val MAX_SINGLE_DOSE_MG = 200
    private const val MIN_MEANINGFUL_DOSE_MG = 20
    /** A recommended dose within this of "now" is presented as "have it now". */
    private const val NOW_TOLERANCE_MINUTES = 45L
    /** Below this debt we don't frame the nudge as sleep-recovery. */
    private const val SLEEP_DEBT_REASON_THRESHOLD_MINUTES = 60

    /** Typical adult nightly sleep need, used for the sleep-debt term. */
    const val DEFAULT_SLEEP_NEED_MINUTES = 480

    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 3_600_000.0

    /**
     * Produce a recommendation, or `null` only when there is genuinely nothing to model
     * (e.g. no usable bedtime). Callers gate display on their own enabled flag.
     */
    fun recommend(input: CoachInput): CoachRecommendation {
        val personalized = input.lastNightSleepMinutes != null
        val sleepDebt = input.lastNightSleepMinutes
            ?.let { (input.sleepNeedMinutes - it).coerceAtLeast(0) }
            ?: 0

        val focusEnd = input.bedtimeMillis - WIND_DOWN_MINUTES * MINUTE_MILLIS

        // Evening / past the focus window → nothing to optimise; defer to sleep forecast.
        if (input.nowMillis >= focusEnd) {
            return CoachRecommendation.Hold(CoachReason.BEDTIME_PROTECTION, personalized, sleepDebt)
        }

        val sampleTimes = buildSampleTimes(input.nowMillis, focusEnd)
        val baselineLoad = sampleTimes.map { t ->
            CaffeineCalculator.calculateCurrentLevel(input.entries, t, input.halfLifeMinutes)
        }
        val baselineAlert = sampleTimes.indices.map { i ->
            alertnessAt(sampleTimes[i], input, sleepDebt, baselineLoad[i])
        }

        val minBaseline = baselineAlert.min()
        if (minBaseline >= TARGET_ALERTNESS) {
            return CoachRecommendation.Hold(CoachReason.ALREADY_ALERT, personalized, sleepDebt)
        }

        // The slump starts at the first sub-target sample; dose slightly ahead of it.
        val onsetIndex = baselineAlert.indexOfFirst { it < TARGET_ALERTNESS }
        val onsetTime = sampleTimes[onsetIndex]
        val doseTime = (onsetTime - PRE_DIP_LEAD_MINUTES * MINUTE_MILLIS)
            .coerceIn(input.nowMillis, onsetTime)

        val existingResidualAtBedtime =
            CaffeineCalculator.calculateCurrentLevel(input.entries, input.bedtimeMillis, input.halfLifeMinutes)
        val maxSafe = maxSafeDose(input, doseTime, existingResidualAtBedtime)

        // Can't add a meaningful dose without risking sleep → protect bedtime.
        if (maxSafe < MIN_MEANINGFUL_DOSE_MG) {
            return CoachRecommendation.Hold(CoachReason.BEDTIME_PROTECTION, personalized, sleepDebt)
        }

        // Only size the dose to cover the next few hours (see COVERAGE_HORIZON_MINUTES).
        val coverageEnd = minOf(doseTime + COVERAGE_HORIZON_MINUTES * MINUTE_MILLIS, focusEnd)

        // Smallest dose that keeps the covered window at target; capped by sleep safety.
        val neededDose = (
            minDoseToHitTarget(input, sampleTimes, baselineLoad, sleepDebt, doseTime, coverageEnd, maxSafe)
                ?: maxSafe // even the safe max can't fully cover the dip — give the best safe lift
            ).coerceAtLeast(MIN_MEANINGFUL_DOSE_MG)

        val residual = (existingResidualAtBedtime +
            hypotheticalContribution(input, neededDose, doseTime, input.bedtimeMillis)).roundToInt()

        val isNow = doseTime - input.nowMillis <= NOW_TOLERANCE_MINUTES * MINUTE_MILLIS
        val reason = when {
            !isNow -> CoachReason.AFTERNOON_DIP
            personalized && sleepDebt >= SLEEP_DEBT_REASON_THRESHOLD_MINUTES -> CoachReason.SLEEP_DEBT
            else -> CoachReason.STAY_SHARP
        }

        return CoachRecommendation.Recommend(
            doseMg = neededDose,
            atMillis = doseTime,
            isNow = isNow,
            reason = reason,
            residualAtBedtimeMg = residual,
            sleepDebtMinutes = sleepDebt,
            personalized = personalized,
        )
    }

    // ── Alertness ─────────────────────────────────────────────────────────────

    /**
     * Predicted alertness (unitless, ~0-100, higher = sharper) at [tMillis] given a
     * caffeine body load of [caffeineLoadMg]. Exposed for testing.
     */
    fun alertnessAt(
        tMillis: Long,
        input: CoachInput,
        sleepDebtMinutes: Int,
        caffeineLoadMg: Double,
    ): Double {
        val hoursAwake = ((tMillis - input.wakeMillis) / HOUR_MILLIS).coerceAtLeast(0.0)
        val homeostatic = HOMEOSTATIC_AMPLITUDE * (1.0 - exp(-hoursAwake / WAKE_TIME_CONSTANT_HOURS))

        val debtFraction = (sleepDebtMinutes.toDouble() / input.sleepNeedMinutes).coerceIn(0.0, 1.0)
        val debtPenalty = SLEEP_DEBT_AMPLITUDE * debtFraction

        val clockHour = localClockHour(tMillis, input.zoneId)
        val dipDistance = circularHourDistance(clockHour, POST_LUNCH_DIP_HOUR)
        val circadianPenalty = POST_LUNCH_DIP_DEPTH * gaussian(dipDistance, POST_LUNCH_DIP_WIDTH_HOURS)

        val ec50 = CAFFEINE_EC50_MG *
            (1.0 + input.toleranceMgPerDay.coerceAtLeast(0.0) / TOLERANCE_EC50_DOUBLING_MG_PER_DAY)
        val caffeineBoost = CAFFEINE_EMAX * caffeineLoadMg / (ec50 + caffeineLoadMg)

        return BASE_ALERTNESS - homeostatic - debtPenalty - circadianPenalty + caffeineBoost
    }

    private fun maxSafeDose(input: CoachInput, doseTime: Long, existingResidualAtBedtime: Double): Int {
        val budget = input.sleepThresholdMg - existingResidualAtBedtime
        if (budget <= 0) return 0
        var dose = MAX_SINGLE_DOSE_MG
        while (dose >= DOSE_STEP_MG) {
            if (hypotheticalContribution(input, dose, doseTime, input.bedtimeMillis) <= budget) return dose
            dose -= DOSE_STEP_MG
        }
        return 0
    }

    private fun minDoseToHitTarget(
        input: CoachInput,
        sampleTimes: List<Long>,
        baselineLoad: List<Double>,
        sleepDebtMinutes: Int,
        doseTime: Long,
        coverageEnd: Long,
        maxDose: Int,
    ): Int? {
        var dose = DOSE_STEP_MG
        while (dose <= maxDose) {
            if (windowMeetsTarget(input, sampleTimes, baselineLoad, sleepDebtMinutes, doseTime, coverageEnd, dose)) {
                return dose
            }
            dose += DOSE_STEP_MG
        }
        return null
    }

    private fun windowMeetsTarget(
        input: CoachInput,
        sampleTimes: List<Long>,
        baselineLoad: List<Double>,
        sleepDebtMinutes: Int,
        doseTime: Long,
        coverageEnd: Long,
        doseMg: Int,
    ): Boolean {
        for (i in sampleTimes.indices) {
            val t = sampleTimes[i]
            if (t < doseTime) continue   // a dose can't help moments that have already passed
            if (t > coverageEnd) break   // only responsible for the near-term coverage window
            val load = baselineLoad[i] + hypotheticalContribution(input, doseMg, doseTime, t)
            if (alertnessAt(t, input, sleepDebtMinutes, load) < TARGET_ALERTNESS) return false
        }
        return true
    }

    /** Active mg remaining at [atMillis] from a single hypothetical [doseMg] taken at [doseTime]. */
    private fun hypotheticalContribution(
        input: CoachInput,
        doseMg: Int,
        doseTime: Long,
        atMillis: Long,
    ): Double {
        if (atMillis < doseTime) return 0.0
        return CaffeineCalculator.calculateDecayedAmount(
            caffeineMg = doseMg.toDouble(),
            consumedAtMillis = doseTime,
            currentTimeMillis = atMillis,
            absorptionMinutes = input.absorptionRateMinutes.coerceAtLeast(1),
            halfLifeMinutes = input.halfLifeMinutes,
        )
    }

    private fun buildSampleTimes(start: Long, end: Long): List<Long> {
        val step = SAMPLE_STEP_MINUTES * MINUTE_MILLIS
        val result = ArrayList<Long>()
        var t = start
        while (t <= end && result.size < MAX_SAMPLES) {
            result += t
            t += step
        }
        if (result.isEmpty()) result += start
        return result
    }

    private fun localClockHour(tMillis: Long, zoneId: ZoneId): Double {
        val zdt = Instant.ofEpochMilli(tMillis).atZone(zoneId)
        return zdt.hour + zdt.minute / 60.0
    }

    /** Smallest distance in hours between two clock hours, accounting for 24 h wrap. */
    private fun circularHourDistance(a: Double, b: Double): Double {
        val raw = kotlin.math.abs(a - b) % 24.0
        return if (raw > 12.0) 24.0 - raw else raw
    }

    private fun gaussian(distance: Double, width: Double): Double {
        return exp(-(distance * distance) / (2.0 * width * width))
    }
}

/** Inputs for a single coach evaluation. Build from settings + Health Connect sleep. */
data class CoachInput(
    val nowMillis: Long,
    val entries: List<ConsumptionEntry>,
    val halfLifeMinutes: Int,
    val absorptionRateMinutes: Int,
    val sleepThresholdMg: Int,
    val bedtimeMillis: Long,
    val wakeMillis: Long,
    /** Minutes actually slept last night, or null when no wearable sleep data is available. */
    val lastNightSleepMinutes: Int?,
    val sleepNeedMinutes: Int = CaffeineCoach.DEFAULT_SLEEP_NEED_MINUTES,
    val zoneId: ZoneId,
    /** Habitual-intake baseline from [CaffeineTolerance]; 0 = tolerance not accounted for. */
    val toleranceMgPerDay: Double = 0.0,
)

enum class CoachReason {
    /** Under-slept → a dose now aids recovery (only when sleep data is available). */
    SLEEP_DEBT,

    /** A predicted alertness dip lies ahead → time caffeine into it. */
    AFTERNOON_DIP,

    /** A dose now keeps you above target through the focus window. */
    STAY_SHARP,

    /** Predicted alertness is already fine → no caffeine needed. */
    ALREADY_ALERT,

    /** Too close to bed (or existing load too high) to add caffeine without risking sleep. */
    BEDTIME_PROTECTION,
}

sealed interface CoachRecommendation {
    val personalized: Boolean
    val sleepDebtMinutes: Int

    /** Suggest a concrete dose at a concrete time. */
    data class Recommend(
        val doseMg: Int,
        val atMillis: Long,
        val isNow: Boolean,
        val reason: CoachReason,
        val residualAtBedtimeMg: Int,
        override val sleepDebtMinutes: Int,
        override val personalized: Boolean,
    ) : CoachRecommendation

    /** No dose suggested; [reason] explains why. */
    data class Hold(
        val reason: CoachReason,
        override val personalized: Boolean,
        override val sleepDebtMinutes: Int,
    ) : CoachRecommendation
}

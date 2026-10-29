package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import kotlin.math.abs
import kotlin.math.exp

/**
 * Caffeine metabolism calculator using scientifically accurate half-life model.
 * 
 * Key concepts:
 * - Absorption phase (0-45min): Linear ramp up to peak blood concentration
 * - Elimination phase (45min+): Exponential decay with ~5 hour half-life
 * - Half-life: Time for caffeine level to reduce by 50% (average 300min = 5h)
 */
object CaffeineCalculator {
    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val MIN_PEAK_SEARCH_PADDING_MINUTES = 120L
    private const val PEAK_SEARCH_PADDING_MULTIPLIER = 4L
    private const val MAX_PREDICTION_WINDOW_HOURS = 168L
    private const val BINARY_SEARCH_PRECISION_MILLIS = 60_000L
    // Once min(ka, ke) * elapsedMinutes >= 40 (~58 half-lives; exp(-40) ≈ 4.2e-18), even a multi-gram
    // entry contributes < 1e-11 mg, allowing long historical logs to short-circuit in O(1).
    private const val NEGLIGIBLE_DECAY_EXPONENT = 40.0

    /**
     * Calculate current active caffeine level from all logged drinks.
     * 
     * @param entries All consumption entries (filter to relevant time range before calling)
     * @param currentTimeMillis Current timestamp in milliseconds
     * @param halfLifeMinutes Caffeine half-life in minutes (default 300 = 5 hours)
     * @return Current caffeine level in mg (Double for precision)
     */
    fun calculateCurrentLevel(
        entries: List<ConsumptionEntry>,
        currentTimeMillis: Long = System.currentTimeMillis(),
        halfLifeMinutes: Int = 300
    ): Double {
        return entries.sumOf { entry ->
            calculateEntryContribution(
                entry = entry,
                currentTimeMillis = currentTimeMillis,
                halfLifeMinutes = halfLifeMinutes,
            )
        }
    }

    fun calculateEntryContribution(
        entry: ConsumptionEntry,
        currentTimeMillis: Long = System.currentTimeMillis(),
        halfLifeMinutes: Int = 300,
    ): Double {
        if (entry.caffeineMg <= 0 || currentTimeMillis < entry.startedAtMillis) return 0.0

        val doseCount = entry.normalizedDurationMinutes
        val ke = LN2 / halfLifeMinutes
        val ka = LN10 / entry.absorptionRate

        val lastSubDoseMillis = entry.startedAtMillis + ((doseCount - 1L) * MINUTE_MILLIS)
        if (currentTimeMillis > lastSubDoseMillis) {
            val minElapsedMinutes = (currentTimeMillis - lastSubDoseMillis) / (1000.0 * 60.0)
            if (minOf(ke, ka) * minElapsedMinutes >= NEGLIGIBLE_DECAY_EXPONENT) {
                return 0.0
            }
        }

        val elapsedMinutes = (currentTimeMillis - entry.startedAtMillis) / MINUTE_MILLIS
        val activeDoseCount = minOf(doseCount.toLong(), elapsedMinutes + 1L).toInt()
        val doseMg = entry.caffeineMg.toDouble() / doseCount.toDouble()

        var sum = 0.0
        for (minuteIndex in 0 until activeDoseCount) {
            val consumedAtMillis = entry.startedAtMillis + (minuteIndex * MINUTE_MILLIS)
            val t = (currentTimeMillis - consumedAtMillis) / (1000.0 * 60.0)
            sum += decayedAmountForElapsedMinutes(
                caffeineMg = doseMg,
                t = t,
                ka = ka,
                ke = ke,
            )
        }
        return sum
    }

    /**
     * Calculate how much caffeine remains from a single drink.
     * 
     * Models two phases:
     * 1. Absorption (0 to absorptionMinutes): Linear increase to peak
     * 2. Elimination (after absorptionMinutes): Exponential decay by half-life
     * 
     * @param caffeineMg Initial caffeine amount in the drink
     * @param consumedAtMillis When the drink was consumed (timestamp)
     * @param currentTimeMillis Current time (timestamp)
     * @param absorptionMinutes Time to reach peak blood concentration (typically 45min)
     * @param halfLifeMinutes Half-life duration (typically 300min = 5h)
     * @return Remaining active caffeine in mg
     */
    private const val LN2  = 0.6931471805599453
    private const val LN10 = 2.302585092994046

    fun calculateDecayedAmount(
        caffeineMg: Double,
        consumedAtMillis: Long,
        currentTimeMillis: Long,
        absorptionMinutes: Int,
        halfLifeMinutes: Int
    ): Double {
        val t = (currentTimeMillis - consumedAtMillis) / (1000.0 * 60.0)
        if (t < 0) return 0.0

        val ke = LN2 / halfLifeMinutes
        val ka = LN10 / absorptionMinutes
        return decayedAmountForElapsedMinutes(
            caffeineMg = caffeineMg,
            t = t,
            ka = ka,
            ke = ke,
        )
    }

    private fun decayedAmountForElapsedMinutes(
        caffeineMg: Double,
        t: Double,
        ka: Double,
        ke: Double,
    ): Double {
        if (t < 0.0) return 0.0

        val expKe = exp(-ke * t)
        val expKa = exp(-ka * t)

        // 1. Calculate the standard Bateman equation
        val standardBateman = if (abs(ka - ke) < 1e-9) {
            caffeineMg * ka * t * expKe
        } else {
            caffeineMg * (ka / (ka - ke)) * (expKe - expKa)
        }

        // 2. THE FIX: Apply a "Gastric Emptying" smoothing factor.
        // This forces the equation to start at a slope of 0 and gently curve upwards,
        // simulating the delay before the liquid reaches the intestines.
        val gastricSmoothing = 1.0 - expKa

        return standardBateman * gastricSmoothing
    }

    fun calculateDecayedAmount(
        caffeineMg: Int,
        consumedAtMillis: Long,
        currentTimeMillis: Long,
        absorptionMinutes: Int,
        halfLifeMinutes: Int,
    ): Double {
        return calculateDecayedAmount(
            caffeineMg = caffeineMg.toDouble(),
            consumedAtMillis = consumedAtMillis,
            currentTimeMillis = currentTimeMillis,
            absorptionMinutes = absorptionMinutes,
            halfLifeMinutes = halfLifeMinutes,
        )
    }
    
    /**
     * Predict when caffeine level will drop to a target amount.
     * 
     * Uses binary search to find the timestamp when active caffeine reaches
     * the target level (e.g., 50mg for safe sleep).
     * 
     * @param entries All consumption entries
     * @param targetLevelMg Target caffeine level (e.g., 50mg for sleep)
     * @param currentTimeMillis Current time
     * @param halfLifeMinutes Half-life duration
     * @return Timestamp when level reaches target, or null if already below
     */
    /**
     * Calculate the timestamp at which a single drink reaches peak blood concentration.
     *
     * Derived analytically from the one-compartment oral PK model:
     *   tmax = ln(ka / ke) / (ka - ke)
     *
     * @param consumedAtMillis When the drink was consumed
     * @param absorptionMinutes Time for 90% absorption (from JSON "90_absorption_rate")
     * @param halfLifeMinutes Elimination half-life (default 300 = 5h)
     * @return Timestamp in milliseconds when the drink peaks in the bloodstream
     */
    fun calculatePeakTime(
        entry: ConsumptionEntry,
        halfLifeMinutes: Int = 300,
    ): Long {
        val searchEndTime = peakSearchEndMillis(entry)
        val totalSearchMinutes = ((searchEndTime - entry.startedAtMillis) / MINUTE_MILLIS).toInt()
        if (totalSearchMinutes <= 0) return entry.startedAtMillis
        if (entry.caffeineMg <= 0) return searchEndTime

        val doseCount = entry.normalizedDurationMinutes
        val doseMg = entry.caffeineMg.toDouble() / doseCount.toDouble()
        val ke = LN2 / halfLifeMinutes
        val ka = LN10 / entry.absorptionRate

        var bestTime = entry.startedAtMillis
        var bestLevel = Double.NEGATIVE_INFINITY

        if (doseCount == 1) {
            for (minute in 0..totalSearchMinutes) {
                val level = decayedAmountForElapsedMinutes(
                    caffeineMg = doseMg,
                    t = minute.toDouble(),
                    ka = ka,
                    ke = ke,
                )
                if (level >= bestLevel) {
                    bestLevel = level
                    bestTime = entry.startedAtMillis + (minute.toLong() * MINUTE_MILLIS)
                }
            }
            return bestTime
        }

        // At integer minute m from startedAtMillis, active sub-doses i in 0..min(m, doseCount - 1)
        // have elapsed minutes k = m - i in max(0, m - doseCount + 1)..m. Prefix-summing the
        // single-sub-dose response evaluates the PK equation once per minute (O(M)) instead of
        // re-summing all sub-doses at every probe step (O(M * D)).
        val prefix = DoubleArray(totalSearchMinutes + 2)
        for (minute in 0..totalSearchMinutes) {
            val unitResponse = decayedAmountForElapsedMinutes(
                caffeineMg = doseMg,
                t = minute.toDouble(),
                ka = ka,
                ke = ke,
            )
            prefix[minute + 1] = prefix[minute] + unitResponse
            val windowStart = maxOf(0, minute - doseCount + 1)
            val level = prefix[minute + 1] - prefix[windowStart]
            if (level >= bestLevel) {
                bestLevel = level
                bestTime = entry.startedAtMillis + (minute.toLong() * MINUTE_MILLIS)
            }
        }

        return bestTime
    }

    /** The latest moment [calculatePeakTime] considers, so its result never lies past it. */
    fun peakSearchEndMillis(entry: ConsumptionEntry): Long {
        val searchPaddingMinutes = maxOf(
            MIN_PEAK_SEARCH_PADDING_MINUTES,
            entry.absorptionRate.toLong() * PEAK_SEARCH_PADDING_MULTIPLIER,
        )
        return entry.finishedAtMillis + (searchPaddingMinutes * MINUTE_MILLIS)
    }

    fun predictTimeUntilLevel(
        entries: List<ConsumptionEntry>,
        targetLevelMg: Double,
        currentTimeMillis: Long = System.currentTimeMillis(),
        halfLifeMinutes: Int = 300
    ): Long? {
        // Check if already below target
        val currentLevel = calculateCurrentLevel(entries, currentTimeMillis, halfLifeMinutes)
        if (currentLevel <= targetLevelMg) {
            return null
        }

        var low = currentTimeMillis
        val maxHigh = currentTimeMillis + (MAX_PREDICTION_WINDOW_HOURS * HOUR_MILLIS)
        var high = currentTimeMillis + HOUR_MILLIS

        while (
            high < maxHigh &&
            calculateCurrentLevel(entries, high, halfLifeMinutes) > targetLevelMg
        ) {
            val currentWindow = high - currentTimeMillis
            high = currentTimeMillis + (currentWindow * 2).coerceAtMost(maxHigh - currentTimeMillis)
        }

        if (calculateCurrentLevel(entries, high, halfLifeMinutes) > targetLevelMg) {
            return null
        }

        while (high - low > BINARY_SEARCH_PRECISION_MILLIS) {
            val mid = (low + high) / 2
            val levelAtMid = calculateCurrentLevel(entries, mid, halfLifeMinutes)

            if (levelAtMid > targetLevelMg) {
                low = mid
            } else {
                high = mid
            }
        }

        return high
    }
}

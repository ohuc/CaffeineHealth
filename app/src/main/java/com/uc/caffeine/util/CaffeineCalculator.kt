package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import kotlin.math.pow

/**
 * Caffeine metabolism calculator using scientifically accurate half-life model.
 * 
 * Key concepts:
 * - Absorption phase (0-45min): Linear ramp up to peak blood concentration
 * - Elimination phase (45min+): Exponential decay with ~5 hour half-life
 * - Half-life: Time for caffeine level to reduce by 50% (average 300min = 5h)
 */
object CaffeineCalculator {
    
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
            calculateDecayedAmount(
                caffeineMg = entry.caffeineMg,
                consumedAtMillis = entry.timestamp,
                currentTimeMillis = currentTimeMillis,
                absorptionMinutes = entry.absorptionRate,
                halfLifeMinutes = halfLifeMinutes
            )
        }
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
    fun calculateDecayedAmount(
        caffeineMg: Int,
        consumedAtMillis: Long,
        currentTimeMillis: Long,
        absorptionMinutes: Int,
        halfLifeMinutes: Int
    ): Double {
        val elapsedMinutes = (currentTimeMillis - consumedAtMillis) / (1000.0 * 60.0)
        
        return when {
            elapsedMinutes < 0 -> {
                // Future drink (shouldn't happen)
                0.0
            }
            elapsedMinutes < absorptionMinutes -> {
                // Absorption phase - linear ramp up to peak
                caffeineMg * (elapsedMinutes / absorptionMinutes)
            }
            else -> {
                // Elimination phase - exponential decay
                val timeInElimination = elapsedMinutes - absorptionMinutes
                val halfLives = timeInElimination / halfLifeMinutes
                caffeineMg * (0.5.pow(halfLives))
            }
        }
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
        
        // Binary search for the time when level drops to target
        // Search up to 24 hours in the future (should be more than enough)
        var low = currentTimeMillis
        var high = currentTimeMillis + (24 * 60 * 60 * 1000L)
        
        // Search with 1-minute precision
        while (high - low > 60_000) {
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

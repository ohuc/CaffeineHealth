package com.uc.caffeine.data

/**
 * User preferences for personalized caffeine tracking.
 * 
 * These settings affect half-life calculations and sleep recommendations.
 */
data class UserSettings(
    /**
     * Caffeine half-life in minutes.
     * Default: 300 (5 hours) - typical for most adults
     * Range: 180-420 (3-7 hours) based on genetics and metabolism
     */
    val halfLifeMinutes: Int = 300,
    
    /**
     * Threshold for safe sleep level in mg.
     * Default: 60mg - research shows 60mg better threshold
     * Users can adjust based on personal sensitivity
     */
    val sleepThresholdMg: Int = 60,
    
    /**
     * Time to reach peak blood concentration in minutes.
     * Default: 45 minutes on empty stomach
     * Range: 15-60 minutes depending on food intake
     */
    val absorptionRateMinutes: Int = 45,
    
    /**
     * User's typical bedtime hour (24-hour format).
     * Default: 23 (11pm)
     * Range: 0-23
     */
    val sleepTimeHour: Int = 23,
    
    /**
     * User's typical bedtime minute.
     * Default: 0 (on the hour)
     * Range: 0-59
     */
    val sleepTimeMinute: Int = 0
)

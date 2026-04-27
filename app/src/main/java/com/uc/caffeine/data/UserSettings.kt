package com.uc.caffeine.data

import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * CYP1A2 rs762551 genotype — affects caffeine metabolism speed.
 * A/A is the "fast metabolizer" reference genotype.
 */
enum class Cyp1a2Genotype(val label: String, val clearanceFactor: Double) {
    NOT_SET("Not set", 1.0),
    AA("A/A — fast metabolizer", 1.0),
    AC("A/C — intermediate", 0.78),
    CC("C/C — slow metabolizer", 0.63),
    ;

    companion object {
        fun fromStorage(value: String?): Cyp1a2Genotype =
            entries.find { it.name == value } ?: NOT_SET
    }
}

/**
 * AHR rs2066853 genotype — affects CYP1A2 expression level.
 * G/G is the normal-expression reference genotype.
 */
enum class AhrGenotype(val label: String, val clearanceFactor: Double) {
    NOT_SET("Not set", 1.0),
    GG("G/G — normal", 1.0),
    A_CARRIER("G/A or A/A — reduced", 0.78),
    ;

    companion object {
        fun fromStorage(value: String?): AhrGenotype =
            entries.find { it.name == value } ?: NOT_SET
    }
}

/**
 * Hormonal status that modulates caffeine clearance.
 * Pregnancy progressively suppresses CYP1A2; oral contraceptives inhibit it.
 */
enum class HormonalStatus(val label: String, val clearanceFactor: Double) {
    NONE("None", 1.0),
    ORAL_CONTRACEPTIVES("Hormonal contraceptives", 0.48),
    PREGNANT_FIRST("Pregnant — 1st trimester", 0.73),
    PREGNANT_SECOND("Pregnant — 2nd trimester", 0.48),
    PREGNANT_THIRD("Pregnant — 3rd trimester", 0.28),
    ;

    companion object {
        fun fromStorage(value: String?): HormonalStatus =
            entries.find { it.name == value } ?: NONE
    }
}

enum class HcSleepMode {
    PREVIOUS_DAY,
    SEVEN_DAY_AVERAGE;

    companion object {
        fun fromStorage(value: String?): HcSleepMode =
            entries.find { it.name == value } ?: PREVIOUS_DAY
    }
}

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
    val sleepTimeMinute: Int = 0,

    /**
     * App theme mode preference.
     * SYSTEM follows the device theme. LIGHT and DARK override it.
     */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /**
     * Whether Material You dynamic color should be used when available.
     */
    val useDynamicColor: Boolean = true,

    /**
     * Whether times should be shown in 24-hour format.
     */
    val use24HourClock: Boolean = false,

    /**
     * Preferred date format for user-facing dates.
     */
    val dateFormat: AppDateFormat = AppDateFormat.MONTH_DAY_YEAR,

    /**
     * Preferred timezone for calculations and user-facing times.
     * Stored as a full IANA timezone ID.
     */
    val timeZoneId: String = ZoneId.systemDefault().id,

    /**
     * Whether the first-run onboarding flow has been completed or intentionally skipped.
     */
    val isOnboardingComplete: Boolean = false,

    /**
     * Raw onboarding profile factors, stored for later editing from Settings.
     */
    val profileFactors: ProfileFactors = ProfileFactors(),

    /**
     * Optional CYP1A2 rs762551 genotype for more accurate metabolism modeling.
     */
    val cyp1a2Genotype: Cyp1a2Genotype = Cyp1a2Genotype.NOT_SET,

    /**
     * Optional AHR rs2066853 genotype for more accurate metabolism modeling.
     */
    val ahrGenotype: AhrGenotype = AhrGenotype.NOT_SET,

    /**
     * Optional hormonal status (pregnancy or oral contraceptives).
     */
    val hormonalStatus: HormonalStatus = HormonalStatus.NONE,
    val healthConnectEnabled: Boolean = false,
    val hcSleepEnabled: Boolean = false,
    val hcSleepMode: HcSleepMode = HcSleepMode.PREVIOUS_DAY,
    val hcSleepTimeHour: Int? = null,
    val hcSleepTimeMinute: Int? = null,
) {
    /**
     * Combined clearance factor from optional genetic and hormonal modifiers.
     * Values < 1.0 mean slower clearance (longer half-life).
     */
    val clearanceFactor: Double
        get() = cyp1a2Genotype.clearanceFactor *
                ahrGenotype.clearanceFactor *
                hormonalStatus.clearanceFactor

    /**
     * Effective half-life after applying genetic and hormonal modifiers.
     * Use this for all PK calculations instead of [halfLifeMinutes] directly.
     */
    val effectiveHalfLifeMinutes: Int
        get() = (halfLifeMinutes / clearanceFactor).roundToInt()

    val effectiveSleepTimeHour: Int
        get() = if (hcSleepEnabled && hcSleepTimeHour != null) hcSleepTimeHour else sleepTimeHour

    val effectiveSleepTimeMinute: Int
        get() = if (hcSleepEnabled && hcSleepTimeMinute != null) hcSleepTimeMinute else sleepTimeMinute
}

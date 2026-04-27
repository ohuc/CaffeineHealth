package com.uc.caffeine.data

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.util.Locale

// Extension property to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

object SettingsKeys {
    val HALF_LIFE_MINUTES = intPreferencesKey("half_life_minutes")
    val SLEEP_THRESHOLD_MG = intPreferencesKey("sleep_threshold_mg")
    val ABSORPTION_RATE_MINUTES = intPreferencesKey("absorption_rate_minutes")
    val SLEEP_TIME_HOUR = intPreferencesKey("sleep_time_hour")
    val SLEEP_TIME_MINUTE = intPreferencesKey("sleep_time_minute")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
    val DATE_FORMAT = stringPreferencesKey("date_format")
    val TIME_ZONE_ID = stringPreferencesKey("time_zone_id")
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    val CYP1A2_GENOTYPE = stringPreferencesKey("cyp1a2_genotype")
    val AHR_GENOTYPE = stringPreferencesKey("ahr_genotype")
    val HORMONAL_STATUS = stringPreferencesKey("hormonal_status")
    val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
    val HC_SLEEP_ENABLED = booleanPreferencesKey("hc_sleep_enabled")
    val HC_SLEEP_MODE = stringPreferencesKey("hc_sleep_mode")
    val HC_SLEEP_TIME_HOUR = intPreferencesKey("hc_sleep_time_hour")
    val HC_SLEEP_TIME_MINUTE = intPreferencesKey("hc_sleep_time_minute")

    // Raw onboarding profile factors
    val PROFILE_AGE_BUCKET = stringPreferencesKey("profile_age_bucket")
    val PROFILE_WEIGHT_VALUE = intPreferencesKey("profile_weight_value")
    val PROFILE_WEIGHT_UNIT = stringPreferencesKey("profile_weight_unit")
    val PROFILE_HAS_INSOMNIA = stringPreferencesKey("profile_has_insomnia")
    val PROFILE_SMOKING_HABIT = stringPreferencesKey("profile_smoking_habit")
    val PROFILE_HEAVY_ALCOHOL = stringPreferencesKey("profile_heavy_alcohol")
    val PROFILE_HEAVY_CAFFEINE = stringPreferencesKey("profile_heavy_caffeine")
    val PROFILE_LIVER_DISEASE = stringPreferencesKey("profile_liver_disease")
    val PROFILE_MEDICATIONS = stringSetPreferencesKey("profile_medications")
}

/**
 * Repository for user preferences using DataStore.
 * 
 * Provides persistent storage for caffeine tracking personalization:
 * - Half-life (varies by person: 3-7 hours)
 * - Sleep threshold (how much caffeine is "safe" for sleep)
 * - Absorption rate (how fast caffeine enters bloodstream)
 */
class SettingsRepository(private val context: Context) {

    val defaultSettings = UserSettings(
        use24HourClock = DateFormat.is24HourFormat(context),
        dateFormat = AppDateFormat.fromLocale(Locale.getDefault()),
        timeZoneId = ZoneId.systemDefault().id,
    )
    
    /**
     * Flow of current user settings.
     * Emits UserSettings with defaults if not yet configured.
     */
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        prefs.toUserSettings(defaultSettings)
    }
    
    /**
     * Update caffeine half-life setting.
     * @param minutes Half-life in minutes (typically 180-420 for 3-7 hours)
     */
    suspend fun updateHalfLife(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HALF_LIFE_MINUTES] = minutes
        }
    }
    
    /**
     * Update sleep threshold setting.
     * @param mg Caffeine level in mg below which sleep is considered safe
     */
    suspend fun updateSleepThreshold(mg: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.SLEEP_THRESHOLD_MG] = mg
        }
    }
    
    /**
     * Update absorption rate setting.
     * @param minutes Time to peak blood concentration (typically 15-60 minutes)
     */
    suspend fun updateAbsorptionRate(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.ABSORPTION_RATE_MINUTES] = minutes
        }
    }
    
    /**
     * Update sleep time setting.
     * @param hour Hour in 24-hour format (0-23)
     * @param minute Minute (0-59)
     */
    suspend fun updateSleepTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.SLEEP_TIME_HOUR] = hour
            prefs[SettingsKeys.SLEEP_TIME_MINUTE] = minute
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateUse24HourClock(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.USE_24_HOUR_CLOCK] = enabled
        }
    }

    suspend fun updateDateFormat(dateFormat: AppDateFormat) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.DATE_FORMAT] = dateFormat.name
        }
    }

    suspend fun updateTimeZoneId(timeZoneId: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.TIME_ZONE_ID] = timeZoneId
        }
    }

    suspend fun updateCyp1a2Genotype(genotype: Cyp1a2Genotype) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.CYP1A2_GENOTYPE] = genotype.name
        }
    }

    suspend fun updateAhrGenotype(genotype: AhrGenotype) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.AHR_GENOTYPE] = genotype.name
        }
    }

    suspend fun updateHormonalStatus(status: HormonalStatus) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HORMONAL_STATUS] = status.name
        }
    }

    suspend fun updateProfileFactor(block: MutablePreferences.() -> Unit) {
        context.dataStore.edit { prefs ->
            prefs.block()
        }
    }

    suspend fun completeOnboarding(
        profile: DerivedOnboardingProfile,
        factors: ProfileFactors? = null,
    ) {
        context.dataStore.edit { prefs ->
            prefs.writeOnboardingCompletion(profile)
            factors?.let { prefs.writeProfileFactors(it) }
        }
    }

    suspend fun markOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun updateHealthConnectEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HEALTH_CONNECT_ENABLED] = enabled
        }
    }

    suspend fun updateHcSleepEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HC_SLEEP_ENABLED] = enabled
        }
    }

    suspend fun updateHcSleepMode(mode: HcSleepMode) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HC_SLEEP_MODE] = mode.name
        }
    }

    suspend fun saveHcSleepTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HC_SLEEP_TIME_HOUR] = hour
            prefs[SettingsKeys.HC_SLEEP_TIME_MINUTE] = minute
        }
    }

    suspend fun importSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HALF_LIFE_MINUTES] = settings.halfLifeMinutes
            prefs[SettingsKeys.SLEEP_THRESHOLD_MG] = settings.sleepThresholdMg
            prefs[SettingsKeys.ABSORPTION_RATE_MINUTES] = settings.absorptionRateMinutes
            prefs[SettingsKeys.SLEEP_TIME_HOUR] = settings.sleepTimeHour
            prefs[SettingsKeys.SLEEP_TIME_MINUTE] = settings.sleepTimeMinute
            prefs[SettingsKeys.THEME_MODE] = settings.themeMode.name
            prefs[SettingsKeys.USE_DYNAMIC_COLOR] = settings.useDynamicColor
            prefs[SettingsKeys.USE_24_HOUR_CLOCK] = settings.use24HourClock
            prefs[SettingsKeys.DATE_FORMAT] = settings.dateFormat.name
            prefs[SettingsKeys.TIME_ZONE_ID] = settings.timeZoneId
            prefs[SettingsKeys.CYP1A2_GENOTYPE] = settings.cyp1a2Genotype.name
            prefs[SettingsKeys.AHR_GENOTYPE] = settings.ahrGenotype.name
            prefs[SettingsKeys.HORMONAL_STATUS] = settings.hormonalStatus.name
            prefs[SettingsKeys.ONBOARDING_COMPLETE] = true
            val pf = settings.profileFactors
            pf.ageBucket?.let { prefs[SettingsKeys.PROFILE_AGE_BUCKET] = it }
            prefs[SettingsKeys.PROFILE_WEIGHT_VALUE] = pf.weightValue
            prefs[SettingsKeys.PROFILE_WEIGHT_UNIT] = pf.weightUnit
            pf.hasInsomnia?.let { prefs[SettingsKeys.PROFILE_HAS_INSOMNIA] = it.toString() }
            pf.smokingHabit?.let { prefs[SettingsKeys.PROFILE_SMOKING_HABIT] = it }
            pf.heavyAlcohol?.let { prefs[SettingsKeys.PROFILE_HEAVY_ALCOHOL] = it.toString() }
            pf.heavyCaffeine?.let { prefs[SettingsKeys.PROFILE_HEAVY_CAFFEINE] = it.toString() }
            pf.liverDisease?.let { prefs[SettingsKeys.PROFILE_LIVER_DISEASE] = it }
            if (pf.medications.isNotEmpty()) {
                prefs[SettingsKeys.PROFILE_MEDICATIONS] = pf.medications
            }
        }
    }
}

internal fun Preferences.toUserSettings(defaultSettings: UserSettings): UserSettings {
    val hasLegacyProfilePrefs = this.hasLegacyProfilePrefs()

    return UserSettings(
        halfLifeMinutes = this[SettingsKeys.HALF_LIFE_MINUTES] ?: defaultSettings.halfLifeMinutes,
        sleepThresholdMg = this[SettingsKeys.SLEEP_THRESHOLD_MG] ?: defaultSettings.sleepThresholdMg,
        absorptionRateMinutes = this[SettingsKeys.ABSORPTION_RATE_MINUTES] ?: defaultSettings.absorptionRateMinutes,
        sleepTimeHour = this[SettingsKeys.SLEEP_TIME_HOUR] ?: defaultSettings.sleepTimeHour,
        sleepTimeMinute = this[SettingsKeys.SLEEP_TIME_MINUTE] ?: defaultSettings.sleepTimeMinute,
        themeMode = ThemeMode.fromStorage(this[SettingsKeys.THEME_MODE]),
        useDynamicColor = this[SettingsKeys.USE_DYNAMIC_COLOR] ?: defaultSettings.useDynamicColor,
        use24HourClock = this[SettingsKeys.USE_24_HOUR_CLOCK] ?: defaultSettings.use24HourClock,
        dateFormat = AppDateFormat.fromStorage(this[SettingsKeys.DATE_FORMAT]),
        timeZoneId = this[SettingsKeys.TIME_ZONE_ID] ?: defaultSettings.timeZoneId,
        isOnboardingComplete = this[SettingsKeys.ONBOARDING_COMPLETE] ?: hasLegacyProfilePrefs,
        profileFactors = this.toProfileFactors(),
        cyp1a2Genotype = Cyp1a2Genotype.fromStorage(this[SettingsKeys.CYP1A2_GENOTYPE]),
        ahrGenotype = AhrGenotype.fromStorage(this[SettingsKeys.AHR_GENOTYPE]),
        hormonalStatus = HormonalStatus.fromStorage(this[SettingsKeys.HORMONAL_STATUS]),
        healthConnectEnabled = this[SettingsKeys.HEALTH_CONNECT_ENABLED] ?: false,
        hcSleepEnabled = this[SettingsKeys.HC_SLEEP_ENABLED] ?: false,
        hcSleepMode = HcSleepMode.fromStorage(this[SettingsKeys.HC_SLEEP_MODE]),
        hcSleepTimeHour = this[SettingsKeys.HC_SLEEP_TIME_HOUR],
        hcSleepTimeMinute = this[SettingsKeys.HC_SLEEP_TIME_MINUTE],
    )
}

fun Preferences.toProfileFactors(): ProfileFactors {
    return ProfileFactors(
        ageBucket = this[SettingsKeys.PROFILE_AGE_BUCKET],
        weightValue = this[SettingsKeys.PROFILE_WEIGHT_VALUE] ?: 60,
        weightUnit = this[SettingsKeys.PROFILE_WEIGHT_UNIT] ?: "Kilograms",
        hasInsomnia = this[SettingsKeys.PROFILE_HAS_INSOMNIA]?.toBooleanStrictOrNull(),
        smokingHabit = this[SettingsKeys.PROFILE_SMOKING_HABIT],
        heavyAlcohol = this[SettingsKeys.PROFILE_HEAVY_ALCOHOL]?.toBooleanStrictOrNull(),
        heavyCaffeine = this[SettingsKeys.PROFILE_HEAVY_CAFFEINE]?.toBooleanStrictOrNull(),
        liverDisease = this[SettingsKeys.PROFILE_LIVER_DISEASE],
        medications = this[SettingsKeys.PROFILE_MEDICATIONS] ?: emptySet(),
    )
}

internal fun MutablePreferences.writeProfileFactors(factors: ProfileFactors) {
    factors.ageBucket?.let { this[SettingsKeys.PROFILE_AGE_BUCKET] = it }
    this[SettingsKeys.PROFILE_WEIGHT_VALUE] = factors.weightValue
    this[SettingsKeys.PROFILE_WEIGHT_UNIT] = factors.weightUnit
    factors.hasInsomnia?.let { this[SettingsKeys.PROFILE_HAS_INSOMNIA] = it.toString() }
    factors.smokingHabit?.let { this[SettingsKeys.PROFILE_SMOKING_HABIT] = it }
    factors.heavyAlcohol?.let { this[SettingsKeys.PROFILE_HEAVY_ALCOHOL] = it.toString() }
    factors.heavyCaffeine?.let { this[SettingsKeys.PROFILE_HEAVY_CAFFEINE] = it.toString() }
    factors.liverDisease?.let { this[SettingsKeys.PROFILE_LIVER_DISEASE] = it }
    if (factors.medications.isNotEmpty()) {
        this[SettingsKeys.PROFILE_MEDICATIONS] = factors.medications
    }

    // When OC is in the medication set, also set hormonal status
    if (factors.medications.contains("OralContraceptives")) {
        this[SettingsKeys.HORMONAL_STATUS] = HormonalStatus.ORAL_CONTRACEPTIVES.name
    }
}

internal fun MutablePreferences.writeOnboardingCompletion(profile: DerivedOnboardingProfile) {
    this[SettingsKeys.HALF_LIFE_MINUTES] = profile.halfLifeMinutes
    this[SettingsKeys.SLEEP_THRESHOLD_MG] = profile.sleepThresholdMg
    this[SettingsKeys.SLEEP_TIME_HOUR] = profile.sleepTimeHour
    this[SettingsKeys.SLEEP_TIME_MINUTE] = profile.sleepTimeMinute
    this[SettingsKeys.ONBOARDING_COMPLETE] = true
}

internal fun Preferences.hasLegacyProfilePrefs(): Boolean {
    return this[SettingsKeys.HALF_LIFE_MINUTES] != null ||
        this[SettingsKeys.SLEEP_THRESHOLD_MG] != null ||
        this[SettingsKeys.SLEEP_TIME_HOUR] != null ||
        this[SettingsKeys.SLEEP_TIME_MINUTE] != null
}

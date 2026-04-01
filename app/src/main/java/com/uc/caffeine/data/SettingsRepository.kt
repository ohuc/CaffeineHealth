package com.uc.caffeine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Repository for user preferences using DataStore.
 * 
 * Provides persistent storage for caffeine tracking personalization:
 * - Half-life (varies by person: 3-7 hours)
 * - Sleep threshold (how much caffeine is "safe" for sleep)
 * - Absorption rate (how fast caffeine enters bloodstream)
 */
class SettingsRepository(private val context: Context) {
    
    private object Keys {
        val HALF_LIFE_MINUTES = intPreferencesKey("half_life_minutes")
        val SLEEP_THRESHOLD_MG = intPreferencesKey("sleep_threshold_mg")
        val ABSORPTION_RATE_MINUTES = intPreferencesKey("absorption_rate_minutes")
        val SLEEP_TIME_HOUR = intPreferencesKey("sleep_time_hour")
        val SLEEP_TIME_MINUTE = intPreferencesKey("sleep_time_minute")
    }
    
    /**
     * Flow of current user settings.
     * Emits UserSettings with defaults if not yet configured.
     */
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            halfLifeMinutes = prefs[Keys.HALF_LIFE_MINUTES] ?: 300,
            sleepThresholdMg = prefs[Keys.SLEEP_THRESHOLD_MG] ?: 60,
            absorptionRateMinutes = prefs[Keys.ABSORPTION_RATE_MINUTES] ?: 45,
            sleepTimeHour = prefs[Keys.SLEEP_TIME_HOUR] ?: 23,
            sleepTimeMinute = prefs[Keys.SLEEP_TIME_MINUTE] ?: 0
        )
    }
    
    /**
     * Update caffeine half-life setting.
     * @param minutes Half-life in minutes (typically 180-420 for 3-7 hours)
     */
    suspend fun updateHalfLife(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HALF_LIFE_MINUTES] = minutes
        }
    }
    
    /**
     * Update sleep threshold setting.
     * @param mg Caffeine level in mg below which sleep is considered safe
     */
    suspend fun updateSleepThreshold(mg: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SLEEP_THRESHOLD_MG] = mg
        }
    }
    
    /**
     * Update absorption rate setting.
     * @param minutes Time to peak blood concentration (typically 15-60 minutes)
     */
    suspend fun updateAbsorptionRate(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ABSORPTION_RATE_MINUTES] = minutes
        }
    }
    
    /**
     * Update sleep time setting.
     * @param hour Hour in 24-hour format (0-23)
     * @param minute Minute (0-59)
     */
    suspend fun updateSleepTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SLEEP_TIME_HOUR] = hour
            prefs[Keys.SLEEP_TIME_MINUTE] = minute
        }
    }
}

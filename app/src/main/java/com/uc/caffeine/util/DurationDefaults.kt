package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.DrinkPreset

const val MIN_CONSUMPTION_DURATION_MINUTES = 1
const val MAX_CONSUMPTION_DURATION_MINUTES = 180

/**
 * Release time for extended-release custom drinks (#58). Unlike "time to finish" it isn't capped
 * at 3 hours: slow-release pills spread their dose over most of a day.
 */
const val RELEASE_STEP_MINUTES = 15
const val MIN_RELEASE_MINUTES = RELEASE_STEP_MINUTES
const val MAX_RELEASE_MINUTES = 24 * 60
const val DEFAULT_RELEASE_MINUTES = 8 * 60

/**
 * Built-in "time to finish" per category, used until the user sets their own.
 * Pills (and gummies, which live in the pill category) are swallowed, not sipped.
 */
val builtInCategoryDurationMinutes: Map<String, Int> = mapOf("pill" to 1)

/**
 * Default "time to finish" for a category key from [CategoryUtils.getCategoryOrder]:
 * the user's category override → the built-in category default → the global default.
 */
fun UserSettings.defaultDurationForCategory(categoryKey: String?): Int {
    if (categoryKey == null) return defaultDurationMinutes
    return categoryDurationMinutes[categoryKey]
        ?: builtInCategoryDurationMinutes[categoryKey]
        ?: defaultDurationMinutes
}

/**
 * Default "time to finish" pre-filled when logging [preset]: the drink's own override →
 * its category default → the global default.
 */
fun UserSettings.defaultDurationFor(preset: DrinkPreset?): Int {
    if (preset == null) return defaultDurationMinutes
    // An extended-release drink is taken in one go; its release time is the window.
    preset.releaseMinutes?.let { return it }
    preset.itemId.takeIf { it.isNotBlank() }
        ?.let { drinkDurationMinutes[it] }
        ?.let { return it }
    return defaultDurationForCategory(CategoryUtils.normalizeCategoryKey(preset.category))
}

/**
 * Resolves the consumption/release window when re-logging a recent serving combo (from the
 * Add screen's Recent servings list or the Quick Log widget).
 *
 * - If [preset] currently has an extended-release window ([DrinkPreset.releaseMinutes]), that
 *   window always wins so enabling or editing extended release takes effect immediately.
 * - If [preset] is a regular drink ([DrinkPreset.releaseMinutes] == null) and the previously
 *   logged duration exceeded [MAX_CONSUMPTION_DURATION_MINUTES] (because extended release was
 *   later turned off), fall back to [defaultDurationFor].
 * - Otherwise preserve the exact finish duration the user logged for that serving.
 */
fun resolveRecentConsumptionDuration(
    loggedDurationMinutes: Int,
    preset: DrinkPreset?,
    settings: UserSettings,
): Int {
    if (preset == null) return loggedDurationMinutes
    preset.releaseMinutes?.let { return it }
    if (loggedDurationMinutes > MAX_CONSUMPTION_DURATION_MINUTES) {
        return settings.defaultDurationFor(preset)
    }
    return loggedDurationMinutes
}

/**
 * Filters saved per-drink "Time to finish" overrides to presets that currently exist in the
 * catalog and do not use an extended-release window (where finish-time overrides are a no-op).
 * Returns an empty list while [presets] has not loaded yet so raw itemIds never flash on screen.
 */
fun resolveVisibleDrinkDurationOverrides(
    drinkDurationMinutes: Map<String, Int>,
    presets: List<DrinkPreset>,
): List<Triple<String, String, Int>> {
    if (presets.isEmpty() || drinkDurationMinutes.isEmpty()) return emptyList()
    val adjustableNamesByItemId = presets
        .asSequence()
        .filter { it.itemId.isNotBlank() && it.releaseMinutes == null }
        .associate { it.itemId to it.name }
    return drinkDurationMinutes.entries
        .mapNotNull { (itemId, minutes) ->
            adjustableNamesByItemId[itemId]?.let { name -> Triple(itemId, name, minutes) }
        }
        .sortedBy { it.second.lowercase() }
}

fun coerceConsumptionDuration(minutes: Int): Int =
    minutes.coerceIn(MIN_CONSUMPTION_DURATION_MINUTES, MAX_CONSUMPTION_DURATION_MINUTES)

fun coerceReleaseMinutes(minutes: Int): Int =
    minutes.coerceIn(MIN_RELEASE_MINUTES, MAX_RELEASE_MINUTES)

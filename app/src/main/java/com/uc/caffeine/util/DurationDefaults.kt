package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.DrinkPreset

const val MIN_CONSUMPTION_DURATION_MINUTES = 1
const val MAX_CONSUMPTION_DURATION_MINUTES = 180

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
    preset.itemId.takeIf { it.isNotBlank() }
        ?.let { drinkDurationMinutes[it] }
        ?.let { return it }
    return defaultDurationForCategory(CategoryUtils.normalizeCategoryKey(preset.category))
}

fun coerceConsumptionDuration(minutes: Int): Int =
    minutes.coerceIn(MIN_CONSUMPTION_DURATION_MINUTES, MAX_CONSUMPTION_DURATION_MINUTES)

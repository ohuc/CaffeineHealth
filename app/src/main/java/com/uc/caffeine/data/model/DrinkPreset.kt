package com.uc.caffeine.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drink_presets")
data class DrinkPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val itemId: String = "",
    val name: String,
    val brand: String = "",
    val description: String? = null,
    val category: String = "Coffee",
    val imageName: String = "",
    val emoji: String = "☕",
    val absorptionRate: Int = 45,
    val relevance: Int = 0,
    val defaultUnit: String = "cup",

    // Default caffeine for the default unit — denormalised for display
    // The full per-unit values live in drink_units table
    val defaultCaffeineMg: Int = 0,

    val isCustom: Boolean = false
)

val defaultDrinkPresets = listOf(
    DrinkPreset(itemId = "drip-coffee", name = "Drip Coffee", defaultCaffeineMg = 95,  category = "Coffee",  emoji = "☕", absorptionRate = 45, relevance = 1876),
    DrinkPreset(itemId = "espresso",    name = "Espresso",    defaultCaffeineMg = 77,  category = "Coffee",  emoji = "☕", absorptionRate = 45, relevance = 1148),
    DrinkPreset(itemId = "tea-black",   name = "Black Tea",   defaultCaffeineMg = 48,  category = "Tea",     emoji = "🍵", absorptionRate = 45, relevance = 655),
    DrinkPreset(itemId = "red-bull",    name = "Red Bull",    defaultCaffeineMg = 80,  category = "Energy",  emoji = "⚡", absorptionRate = 45, relevance = 214),
)

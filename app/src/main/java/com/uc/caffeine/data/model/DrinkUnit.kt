package com.uc.caffeine.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// This is the second table — one row per serving size per drink
// e.g. Americano has: cup (77mg, 250ml) AND shot (77mg)
// ForeignKey links every unit back to its parent DrinkPreset
@Entity(
    tableName = "drink_units",
    foreignKeys = [ForeignKey(
        entity = DrinkPreset::class,
        parentColumns = ["id"],
        childColumns = ["drinkId"],
        onDelete = ForeignKey.CASCADE  // delete drink → delete all its units too
    )],
    indices = [Index("drinkId")]  // speeds up "give me all units for drink X" queries
)
data class DrinkUnit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val drinkId: Int,           // FK → drink_presets.id

    // e.g. "cup", "shot", "can", "pod", "teaspoon", "ml", "fl oz"
    val unitKey: String,

    // caffeine in this serving — stored as Double because JSON has values like 77.0
    // We'll display as rounded Int in the UI
    val caffeineMg: Double,

    val milliliters: Double?,   // null for solid items (pills, chocolate pieces)
    val grams: Double?,         // null for liquid items

    val isDefault: Boolean = false  // which unit to show by default for this drink
)

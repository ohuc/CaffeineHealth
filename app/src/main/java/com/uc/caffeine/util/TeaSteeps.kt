package com.uc.caffeine.util

/**
 * Re-steeping the same leaves or bag gives less caffeine each time (#51). Factors are relative to a
 * normal first steep and come from long-steep (3–5 minute) studies, where about two-thirds of the
 * leaf's caffeine comes out in the first steep and a quarter in the second. Short gongfu-style
 * steeps extract more evenly, so for those these estimates err low.
 */
enum class TeaSteep(val caffeineFactor: Double) {
    First(1.0),
    Second(0.35),
    ThirdOrLater(0.12),
}

private val brewedUnits = setOf("teabag", "g", "cup", "mug", "ml", "liter")

// Canned and bottled teas aren't re-steeped, and powders like matcha are whisked in whole.
private val unsteepableUnits = setOf("can", "bottle", "teaspoon")

/** Whether a tea served in [selectedUnitKey] can sensibly be on its second or later steep. */
fun canBeResteeped(category: String, unitKeys: Collection<String>, selectedUnitKey: String?): Boolean =
    category.equals("tea", ignoreCase = true) &&
        selectedUnitKey in brewedUnits &&
        unitKeys.none { it in unsteepableUnits }

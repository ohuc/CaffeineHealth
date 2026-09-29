package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToInt

fun formatUnitLabel(unitKey: String): String {
    return unitKey.replace("(", " (")
}

// Renders a possibly-fractional quantity without a trailing ".0" for whole numbers.
// 1.0 -> "1", 1.5 -> "1.5", 30.0 -> "30", 12.25 -> "12.25"
fun formatQuantity(quantity: Double): String {
    return DecimalFormat("0.##").format(quantity)
}

fun formatServingSummary(
    quantity: Double,
    unitKey: String,
): String {
    val safeQuantity = quantity.coerceAtLeast(MIN_SERVING_QUANTITY)
    val label = pluralizeUnitKey(unitKey, safeQuantity)
    return "${formatQuantity(safeQuantity)} ${formatUnitLabel(label)}"
}

fun calculateServingTotalCaffeine(
    quantity: Double,
    unitCaffeineMg: Double,
): Int {
    return (quantity.coerceAtLeast(MIN_SERVING_QUANTITY) * unitCaffeineMg).roundToInt()
}

/** Builds the log entry for [quantity] × [unit] of a catalog or custom [preset]. */
fun buildPresetConsumptionEntry(
    preset: DrinkPreset,
    quantity: Double,
    unit: DrinkUnit,
    startedAtMillis: Long,
    durationMinutes: Int,
): ConsumptionEntry {
    val safeQuantity = quantity.coerceAtLeast(MIN_SERVING_QUANTITY)
    return ConsumptionEntry(
        drinkName = preset.name,
        caffeineMg = calculateServingTotalCaffeine(safeQuantity, unit.caffeineMg),
        emoji = preset.emoji,
        presetItemId = preset.itemId,
        quantity = safeQuantity,
        unitKey = unit.unitKey,
        unitCaffeineMg = unit.caffeineMg,
        imageName = preset.imageName,
        absorptionRate = preset.absorptionRate,
        startedAtMillis = startedAtMillis,
        durationMinutes = durationMinutes.coerceAtLeast(1),
    )
}

fun formatCaffeineAmount(value: Double): String {
    val formatter = when {
        abs(value) < 1.0 -> DecimalFormat("0.##")
        abs(value - value.roundToInt()) < 0.001 -> DecimalFormat("0")
        else -> DecimalFormat("0.#")
    }
    return formatter.format(value)
}

fun findMatchingUnit(
    units: List<DrinkUnit>,
    unitKey: String,
    unitCaffeineMg: Double,
): DrinkUnit? {
    return units.firstOrNull { it.unitKey == unitKey }
        ?: units.firstOrNull { abs(it.caffeineMg - unitCaffeineMg) < 0.001 }
        ?: units.firstOrNull { it.isDefault }
        ?: units.firstOrNull()
}

// Smallest serving we allow; also the floor the stepper decrements to.
const val MIN_SERVING_QUANTITY = 0.25

// How much the +/- stepper moves per tap, tuned per unit so volume/weight units
// don't require hundreds of taps (issue: "click 330 times to input a small drink").
fun quantityStepFor(unitKey: String): Double = when (unitKey) {
    "ml" -> 10.0
    "g" -> 5.0
    "liter" -> 1.0
    else -> 1.0
}

// One-tap "classic size" shortcuts shown beneath the stepper for volume/weight units.
fun quickPickQuantitiesFor(unitKey: String): List<Double> = when (unitKey) {
    "ml" -> listOf(100.0, 250.0, 330.0, 500.0)
    "g" -> listOf(10.0, 25.0, 50.0, 100.0)
    else -> emptyList()
}

private fun pluralizeUnitKey(
    unitKey: String,
    quantity: Double,
): String {
    if (quantity == 1.0) return unitKey

    val suffix = unitKey.substringAfter('(', missingDelimiterValue = "")
    val base = unitKey.substringBefore('(')

    val pluralBase = when (base) {
        "g" -> "g"
        "ml" -> "ml"
        "fl oz" -> "fl oz"
        "piece" -> "pieces"
        "gummy" -> "gummies"
        else -> "${base}s"
    }

    return if (suffix.isEmpty()) {
        pluralBase
    } else {
        "$pluralBase($suffix"
    }
}

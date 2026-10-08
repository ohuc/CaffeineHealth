package com.uc.caffeine.automation

import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.util.MIN_SERVING_QUANTITY
import com.uc.caffeine.util.buildPresetConsumptionEntry
import com.uc.caffeine.util.defaultDurationFor

/**
 * Public broadcast API for automation apps (Tasker, MacroDroid, Automate, NFC tag triggers…).
 *
 * Send an explicit broadcast to package `com.uc.caffeine` with [ACTION_LOG_DRINK] and:
 *  - [EXTRA_DRINK_NAME] or [EXTRA_DRINK_ID] (required) — catalog/custom drink to log
 *  - [EXTRA_UNIT] — serving name as shown in the app ("shot", "can", "cup (large)"); default serving if omitted
 *  - [EXTRA_QUANTITY] — number of servings, default 1
 *  - [EXTRA_CAFFEINE_MG] — total caffeine; overrides the catalog amount, required for drinks not in the catalog
 *  - [EXTRA_TIMESTAMP] — start time as epoch millis (or seconds), default now; up to 7 days back
 *  - [EXTRA_DURATION_MINUTES] — time taken to finish the drink; defaults to the user's
 *    "time to finish" setting for that drink (Settings → Time to finish)
 *
 * Extras may be sent as strings or numbers. Nothing is logged unless the user has enabled
 * automation in Settings → Automation.
 */
object AutomationContract {
    const val ACTION_LOG_DRINK = "com.uc.caffeine.action.LOG_DRINK"

    const val EXTRA_DRINK_ID = "drink_id"
    const val EXTRA_DRINK_NAME = "drink_name"
    const val EXTRA_UNIT = "unit"
    const val EXTRA_QUANTITY = "quantity"
    const val EXTRA_CAFFEINE_MG = "caffeine_mg"
    const val EXTRA_TIMESTAMP = "timestamp"
    const val EXTRA_DURATION_MINUTES = "duration_minutes"

    const val MAX_QUANTITY = 100.0
    const val MAX_CAFFEINE_MG = 1000
    const val MAX_DURATION_MINUTES = 240
    const val MAX_BACKDATE_MILLIS = 7L * 24 * 60 * 60 * 1000
    const val MAX_FUTURE_SKEW_MILLIS = 5L * 60 * 1000
}

data class LogDrinkRequest(
    val drinkId: String?,
    val drinkName: String?,
    val unitKey: String?,
    val quantity: Double,
    val caffeineMg: Int?,
    val startedAtMillis: Long,
    /** Null when the sender didn't specify one — the user's default applies. */
    val durationMinutes: Int?,
) {
    sealed interface ParseResult {
        data class Valid(val request: LogDrinkRequest) : ParseResult
        data class Invalid(val reason: String) : ParseResult
    }

    companion object {
        // Anything below this can't be epoch millis for a plausible date, so it's seconds.
        private const val SECONDS_THRESHOLD = 100_000_000_000L

        fun parse(extras: Map<String, Any?>, nowMillis: Long): ParseResult {
            with(AutomationContract) {
                val drinkId = extras[EXTRA_DRINK_ID]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                val drinkName = extras[EXTRA_DRINK_NAME]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                if (drinkId == null && drinkName == null) {
                    return invalid("Missing $EXTRA_DRINK_NAME or $EXTRA_DRINK_ID")
                }
                val unitKey = extras[EXTRA_UNIT]?.toString()?.trim()?.takeIf { it.isNotEmpty() }

                val quantity = when (val raw = extras[EXTRA_QUANTITY]) {
                    null -> 1.0
                    else -> raw.asDouble()
                        ?.takeIf { it >= MIN_SERVING_QUANTITY && it <= MAX_QUANTITY }
                        ?: return invalid("$EXTRA_QUANTITY must be between $MIN_SERVING_QUANTITY and $MAX_QUANTITY")
                }

                val caffeineMg = when (val raw = extras[EXTRA_CAFFEINE_MG]) {
                    null -> null
                    else -> raw.asDouble()
                        ?.takeIf { it > 0 && it <= MAX_CAFFEINE_MG }
                        ?.let { Math.round(it).toInt() }
                        ?: return invalid("$EXTRA_CAFFEINE_MG must be between 1 and $MAX_CAFFEINE_MG")
                }

                val durationMinutes = when (val raw = extras[EXTRA_DURATION_MINUTES]) {
                    null -> null
                    else -> raw.asDouble()
                        ?.takeIf { it >= 1 && it <= MAX_DURATION_MINUTES }
                        ?.toInt()
                        ?: return invalid("$EXTRA_DURATION_MINUTES must be between 1 and $MAX_DURATION_MINUTES")
                }

                val startedAtMillis = when (val raw = extras[EXTRA_TIMESTAMP]) {
                    null -> nowMillis
                    else -> {
                        val value = raw.asDouble()?.toLong()
                            ?: return invalid("$EXTRA_TIMESTAMP must be a number")
                        val millis = if (value < SECONDS_THRESHOLD) value * 1000 else value
                        if (millis > nowMillis + MAX_FUTURE_SKEW_MILLIS || millis < nowMillis - MAX_BACKDATE_MILLIS) {
                            return invalid("$EXTRA_TIMESTAMP must be within the last 7 days")
                        }
                        millis
                    }
                }

                return ParseResult.Valid(
                    LogDrinkRequest(
                        drinkId = drinkId,
                        drinkName = drinkName,
                        unitKey = unitKey,
                        quantity = quantity,
                        caffeineMg = caffeineMg,
                        startedAtMillis = startedAtMillis,
                        durationMinutes = durationMinutes,
                    ),
                )
            }
        }

        private fun invalid(reason: String) = ParseResult.Invalid(reason)

        private fun Any.asDouble(): Double? = when (this) {
            is Number -> toDouble()
            is String -> trim().toDoubleOrNull()
            else -> null
        }?.takeIf { it.isFinite() }
    }
}

sealed interface AutomationEntryResolution {
    /** [volumeMl] is forwarded to Health Connect as hydration when the serving has a volume. */
    data class Resolved(val entry: ConsumptionEntry, val volumeMl: Double?) : AutomationEntryResolution
    data class Failed(val reason: String) : AutomationEntryResolution
}

/**
 * Turns a parsed request into a log entry. [preset] is the drink the request's id/name
 * matched (null when nothing matched) and [units] its serving sizes.
 */
fun resolveAutomationEntry(
    request: LogDrinkRequest,
    preset: DrinkPreset?,
    units: List<DrinkUnit>,
    settings: UserSettings = UserSettings(),
): AutomationEntryResolution {
    val durationMinutes = request.durationMinutes ?: settings.defaultDurationFor(preset)
    if (preset == null) {
        val name = request.drinkName
        val caffeineMg = request.caffeineMg
        if (name == null || caffeineMg == null) {
            val requested = request.drinkName ?: request.drinkId
            return AutomationEntryResolution.Failed(
                "No drink named \"$requested\". Add ${AutomationContract.EXTRA_CAFFEINE_MG} to log it as a custom drink.",
            )
        }
        return AutomationEntryResolution.Resolved(
            entry = ConsumptionEntry(
                drinkName = name,
                caffeineMg = caffeineMg,
                emoji = "☕",
                unitCaffeineMg = caffeineMg.toDouble(),
                startedAtMillis = request.startedAtMillis,
                durationMinutes = durationMinutes,
            ),
            volumeMl = null,
        )
    }

    val unit = if (request.unitKey != null) {
        val wanted = normalizeUnitKey(request.unitKey)
        units.firstOrNull { normalizeUnitKey(it.unitKey) == wanted }
            ?: return AutomationEntryResolution.Failed(
                "${preset.name} has no \"${request.unitKey}\" serving. Options: " +
                    units.joinToString { it.unitKey },
            )
    } else {
        units.firstOrNull { it.isDefault } ?: units.firstOrNull() ?: DrinkUnit(
            drinkId = preset.id,
            unitKey = preset.defaultUnit,
            caffeineMg = preset.defaultCaffeineMg.toDouble(),
            milliliters = null,
            grams = null,
            isDefault = true,
        )
    }

    val presetEntry = buildPresetConsumptionEntry(
        preset = preset,
        quantity = request.quantity,
        unit = unit,
        startedAtMillis = request.startedAtMillis,
        durationMinutes = durationMinutes,
    )
    val entry = request.caffeineMg?.let { total ->
        presetEntry.copy(caffeineMg = total, unitCaffeineMg = total / presetEntry.quantity)
    } ?: presetEntry

    return AutomationEntryResolution.Resolved(
        entry = entry,
        volumeMl = unit.milliliters?.let { it * entry.quantity },
    )
}

private fun normalizeUnitKey(unitKey: String): String =
    unitKey.lowercase().filterNot { it.isWhitespace() }

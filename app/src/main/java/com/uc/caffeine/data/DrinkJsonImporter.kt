package com.uc.caffeine.data

import android.content.Context
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import org.json.JSONObject

object DrinkJsonImporter {

    data class ImportResult(
        val preset: DrinkPreset,
        val units: List<DrinkUnit>
    )

    fun importFromAssets(context: Context): List<ImportResult> {
        val results = mutableListOf<ImportResult>()
        try {
            val jsonString = context.assets.open("consumable_items.json")
                .bufferedReader().readText()
            val root = JSONObject(jsonString)
            root.keys().forEach { itemId ->
                val parsed = parseItem(itemId, root.getJSONObject(itemId))
                if (parsed != null) results.add(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results.sortedByDescending { it.preset.relevance }
    }

    private fun parseItem(itemId: String, item: JSONObject): ImportResult? {
        return try {
            val name        = item.optString("name", itemId)
            val category    = item.optString("category", "coffee")
            val imageSrc    = item.optString("image_src", "")
            val imageName   = imageSrc.substringBeforeLast(".")
            val absorption  = item.optInt("90_absorption_rate", 45)
            val relevance   = item.optInt("relevance", 0)
            val defaultUnit = item.optString("default_unit", "cup")
            val description = if (item.isNull("description")) null else item.optString("description")

            // Find caffeine value for the default unit
            val unitsJson = item.optJSONArray("units") ?: return null
            var defaultCaffeine = 0
            for (i in 0 until unitsJson.length()) {
                val u = unitsJson.getJSONObject(i)
                if (u.optString("unit_text_key") == defaultUnit && !u.optBoolean("archived", false)) {
                    defaultCaffeine = u.optDouble("caffeine_content", 0.0).toInt()
                    break
                }
            }
            if (defaultCaffeine == 0 && unitsJson.length() > 0) {
                defaultCaffeine = unitsJson.getJSONObject(0).optDouble("caffeine_content", 0.0).toInt()
            }

            val preset = DrinkPreset(
                itemId            = itemId,
                name              = name,
                brand             = inferBrand(itemId),
                description       = description,
                category          = category,
                imageName         = imageName,
                emoji             = categoryEmoji(category),
                absorptionRate    = absorption,
                relevance         = relevance,
                defaultUnit       = defaultUnit,
                defaultCaffeineMg = defaultCaffeine,
                isCustom          = false
            )

            // Build serving size units — skip pure conversion units (ml, fl oz, liter)
            val units = mutableListOf<DrinkUnit>()
            for (i in 0 until unitsJson.length()) {
                val u = unitsJson.getJSONObject(i)
                if (u.optBoolean("archived", false)) continue
                val unitKey = u.optString("unit_text_key", "cup")
                if (unitKey in listOf("ml", "fl oz", "liter")) continue

                val caffeine = u.optDouble("caffeine_content", 0.0)
                val ml       = if (u.isNull("milliliters")) null else u.optDouble("milliliters")
                val grams    = if (u.isNull("grams")) null else u.optDouble("grams")

                units.add(DrinkUnit(
                    drinkId     = 0,
                    unitKey     = unitKey,
                    caffeineMg  = caffeine,
                    milliliters = ml,
                    grams       = grams,
                    isDefault   = unitKey == defaultUnit
                ))
            }

            if (units.isEmpty()) return null
            if (units.none { it.isDefault }) {
                units[0] = units[0].copy(isDefault = true)
            }

            ImportResult(preset, units)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun inferBrand(itemId: String): String {
        val brands = mapOf(
            "starbucks"  to "Starbucks",
            "costa"      to "Costa",
            "dunkin"     to "Dunkin'",
            "mccafe"     to "McCafé",
            "nescafe"    to "Nescafé",
            "nespresso"  to "Nespresso",
            "red-bull"   to "Red Bull",
            "monster"    to "Monster",
            "caffe-nero" to "Caffé Nero"
        )
        return brands.entries.firstOrNull { itemId.startsWith(it.key) }?.value ?: ""
    }

    private fun categoryEmoji(category: String): String = when (category) {
        "coffee"       -> "☕"
        "tea"          -> "🍵"
        "energy_drink" -> "⚡"
        "soft_drink"   -> "🥤"
        "chocolate"    -> "🍫"
        "pill"         -> "💊"
        else           -> "☕"
    }
}

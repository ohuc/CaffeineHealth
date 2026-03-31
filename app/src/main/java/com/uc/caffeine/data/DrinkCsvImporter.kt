package com.uc.caffeine.data

import android.content.Context
import com.uc.caffeine.data.model.DrinkPreset

/**
 * Reads drinks.csv from the assets folder and parses it into DrinkPreset objects.
 *
 * Why assets? Because it's the standard Android place for bundled read-only files.
 * Think of it like a config file shipped with the app — users can't see or edit it,
 * but YOU can update it easily just by editing the CSV and rebuilding.
 *
 * Adding new drinks in future = just add a row to drinks.csv. No Kotlin changes needed.
 */
object DrinkCsvImporter {

    fun importFromAssets(context: Context): List<DrinkPreset> {
        val drinks = mutableListOf<DrinkPreset>()

        try {
            // context.assets.open() reads a file from the assets/ folder
            // useLines = automatically closes the file when done (like Python's 'with open()')
            context.assets.open("drinks.csv").bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->

                    // Skip the header row
                    if (index == 0) return@forEachIndexed

                    // Skip blank lines
                    if (line.isBlank()) return@forEachIndexed

                    val parsed = parseLine(line)
                    if (parsed != null) drinks.add(parsed)
                }
            }
        } catch (e: Exception) {
            // If the CSV is missing or malformed, log it and return empty
            // The app will fall back to defaultDrinkPresets
            e.printStackTrace()
        }

        return drinks
    }

    // Parses one CSV line into a DrinkPreset
    // Returns null if the line is malformed — we skip bad rows rather than crashing
    private fun parseLine(line: String): DrinkPreset? {
        return try {
            // Split by comma — this works for our CSV since we don't have commas in values
            // If values ever contain commas, we'd need a proper CSV library
            val parts = line.split(",")

            // We expect exactly 7 columns matching the header:
            // name, brand, caffeineMg, servingSize, category, imageName, emoji
            if (parts.size < 7) return null

            DrinkPreset(
                name              = parts[0].trim(),
                brand             = parts[1].trim(),
                defaultCaffeineMg = parts[2].trim().toIntOrNull() ?: 0,
                defaultUnit       = parts[3].trim().ifBlank { "cup" },
                category          = parts[4].trim(),
                imageName         = parts[5].trim(),
                emoji             = parts[6].trim(),
                isCustom          = false
            )
        } catch (e: Exception) {
            null  // Skip this row if anything goes wrong
        }
    }
}

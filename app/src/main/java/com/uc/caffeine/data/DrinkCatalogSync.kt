package com.uc.caffeine.data

import android.content.Context
import com.uc.caffeine.data.dao.DrinkPresetDao
import com.uc.caffeine.data.dao.DrinkUnitDao

/**
 * The drink catalog is seeded from assets only when the database is first created
 * ([CaffeineDatabase.onCreate]). When we ship new catalog items or new serving units
 * for existing items, users who installed earlier would never see them.
 *
 * This performs a one-time, idempotent merge of the bundled catalog into an existing
 * database: it adds presets that don't exist yet (matched by [itemId]) and adds any
 * serving units missing from presets that do exist. It never edits or removes user
 * data, so it's safe to run on top of edited/custom drinks.
 *
 * Fresh installs are stamped as already-current inside [CaffeineDatabase.onCreate],
 * so this is a no-op for them (and avoids racing the initial seed).
 */
object DrinkCatalogSync {

    private const val PREFS = "caffeine_catalog_sync"
    private const val KEY_VERSION = "catalog_version"

    // Bump whenever consumable_items.json gains new items or units that existing
    // users should receive. v1: added Moka Pot Coffee + "g" unit on dark chocolate.
    // v2: Arizona Arnold Palmer + Stacker 2 pills, shot and gummies (#44).
    const val CURRENT_VERSION = 2

    /** Marks the catalog as current without doing any work — used right after a fresh seed. */
    fun markCurrent(context: Context) {
        prefs(context).edit().putInt(KEY_VERSION, CURRENT_VERSION).apply()
    }

    suspend fun syncIfNeeded(
        context: Context,
        presetDao: DrinkPresetDao,
        unitDao: DrinkUnitDao,
    ) {
        val prefs = prefs(context)
        if (prefs.getInt(KEY_VERSION, 0) >= CURRENT_VERSION) return
        // Empty DB means the initial seed hasn't populated yet; it will, and it stamps
        // the version itself. Don't merge into an empty table (would race the seed).
        if (presetDao.getCount() == 0) return

        runCatching {
            val catalog = DrinkJsonImporter.importFromAssets(context)
            for (result in catalog) {
                val existing = presetDao.getPresetByItemId(result.preset.itemId)
                if (existing == null) {
                    val presetId = presetDao.insertAndGetId(result.preset).toInt()
                    unitDao.insertAll(result.units.map { it.copy(drinkId = presetId) })
                } else {
                    val existingKeys = unitDao.getUnitsForDrink(existing.id)
                        .map { it.unitKey }
                        .toSet()
                    val missing = result.units
                        .filter { it.unitKey !in existingKeys }
                        .map { it.copy(drinkId = existing.id) }
                    if (missing.isNotEmpty()) unitDao.insertAll(missing)
                }
            }
        }.onSuccess {
            prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

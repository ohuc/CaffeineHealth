package com.uc.caffeine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uc.caffeine.data.dao.ConsumptionLogDao
import com.uc.caffeine.data.dao.DrinkPresetDao
import com.uc.caffeine.data.dao.DrinkUnitDao
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.data.model.defaultDrinkPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DrinkPreset::class, DrinkUnit::class, ConsumptionEntry::class],
    version = 9,
    exportSchema = false
)
abstract class CaffeineDatabase : RoomDatabase() {

    abstract fun drinkPresetDao(): DrinkPresetDao
    abstract fun drinkUnitDao(): DrinkUnitDao
    abstract fun consumptionLogDao(): ConsumptionLogDao

    companion object {
        @Volatile
        private var INSTANCE: CaffeineDatabase? = null

        fun getDatabase(context: Context): CaffeineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaffeineDatabase::class.java,
                    "caffeine_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed in background when DB is first created
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedDatabase(context, database)
                                }
                            }
                        }
                    })
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDatabase(context: Context, db: CaffeineDatabase) {
            // Try JSON first — 220 items with full data
            val jsonItems = DrinkJsonImporter.importFromAssets(context)

            if (jsonItems.isNotEmpty()) {
                for (result in jsonItems) {
                    // Insert preset and get its auto-generated ID back
                    val presetId = db.drinkPresetDao().insertAndGetId(result.preset).toInt()

                    // Now insert all its units with the real drinkId
                    val unitsWithId = result.units.map { it.copy(drinkId = presetId) }
                    db.drinkUnitDao().insertAll(unitsWithId)
                }
            } else {
                // Fallback: hardcoded minimal presets if JSON is missing
                defaultDrinkPresets.forEach { preset ->
                    val id = db.drinkPresetDao().insertAndGetId(preset).toInt()
                    // Create one default unit per preset
                    db.drinkUnitDao().insert(
                        DrinkUnit(
                            drinkId     = id,
                            unitKey     = preset.defaultUnit,
                            caffeineMg  = 80.0,
                            milliliters = 240.0,
                            grams       = null,
                            isDefault   = true
                        )
                    )
                }
            }
        }
    }
}
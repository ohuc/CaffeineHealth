package com.uc.caffeine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uc.caffeine.data.model.DrinkPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkPresetDao {

    // Returns the auto-generated row ID — needed so we can link DrinkUnits to this preset
    @Insert
    suspend fun insertAndGetId(preset: DrinkPreset): Long

    @Insert
    suspend fun insertAll(presets: List<DrinkPreset>)

    // Sorted by relevance DESC so popular drinks appear first in the list
    @Query("SELECT * FROM drink_presets ORDER BY relevance DESC, name ASC")
    fun getAllPresets(): Flow<List<DrinkPreset>>

    // Filter by category — for the category chips on Add screen
    @Query("SELECT * FROM drink_presets WHERE category = :category ORDER BY relevance DESC, name ASC")
    fun getPresetsByCategory(category: String): Flow<List<DrinkPreset>>

    // Search by name — for the search bar
    @Query("SELECT * FROM drink_presets WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY relevance DESC")
    fun searchPresets(query: String): Flow<List<DrinkPreset>>

    @Query("SELECT * FROM drink_presets WHERE itemId = :itemId LIMIT 1")
    suspend fun getPresetByItemId(itemId: String): DrinkPreset?

    // Case-insensitive exact name match for automation intents; a user's own custom drink
    // wins over a catalog drink with the same name.
    @Query("SELECT * FROM drink_presets WHERE name = :name COLLATE NOCASE ORDER BY isCustom DESC, relevance DESC LIMIT 1")
    suspend fun getPresetByName(name: String): DrinkPreset?

    @Query("SELECT COUNT(*) FROM drink_presets")
    suspend fun getCount(): Int

    @Query("SELECT * FROM drink_presets WHERE isCustom = 1 ORDER BY name ASC")
    suspend fun getCustomPresets(): List<DrinkPreset>

    @Update
    suspend fun update(preset: DrinkPreset)

    @Query("DELETE FROM drink_presets WHERE isCustom = 1")
    suspend fun deleteCustomPresets()
}

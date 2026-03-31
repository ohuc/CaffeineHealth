package com.uc.caffeine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.uc.caffeine.data.model.DrinkUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkUnitDao {

    @Insert
    suspend fun insert(unit: DrinkUnit)

    @Insert
    suspend fun insertAll(units: List<DrinkUnit>)

    // Get all units for a specific drink — shown when user picks a serving size
    @Query("SELECT * FROM drink_units WHERE drinkId = :drinkId ORDER BY isDefault DESC")
    suspend fun getUnitsForDrink(drinkId: Int): List<DrinkUnit>

    // Get just the default unit for a drink — used when quick-adding
    @Query("SELECT * FROM drink_units WHERE drinkId = :drinkId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultUnit(drinkId: Int): DrinkUnit?

    @Query("SELECT COUNT(*) FROM drink_units")
    suspend fun getCount(): Int
}

package com.uc.caffeine.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.RecentDrink
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumptionLogDao {

    // Log a drink — inserts one row into consumption_log
    @Insert
    suspend fun logDrink(entry: ConsumptionEntry)

    // Get ALL entries from today only
    // :startOfDay is a parameter we pass in — Room replaces it with the actual value
    // startOfDay = midnight today in milliseconds
    @Query("SELECT * FROM consumption_log WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayEntries(startOfDay: Long): Flow<List<ConsumptionEntry>>

    // Sum of caffeine logged today — the big number on the home screen
    // COALESCE means "if SUM returns null (no rows), return 0 instead"
    @Query("SELECT COALESCE(SUM(caffeineMg), 0) FROM consumption_log WHERE timestamp >= :startOfDay")
    fun getTodayTotal(startOfDay: Long): Flow<Int>

    // All entries ever — for the History screen
    @Query("SELECT * FROM consumption_log ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<ConsumptionEntry>>

    // Delete all entries from today — the "Reset Today" button
    @Query("DELETE FROM consumption_log WHERE timestamp >= :startOfDay")
    suspend fun clearToday(startOfDay: Long)

    // The 2 most recently logged DISTINCT drinks — used for Quick Add
    // GROUP BY drinkName collapses duplicate drinks into one row each
    // MAX(timestamp) picks the most recent tap of each drink
    // ORDER BY that timestamp DESC = most recent first
    // LIMIT 2 = only the top 2
    @Query("""
        SELECT drinkName, caffeineMg, emoji, MAX(timestamp) as lastUsed
        FROM consumption_log
        GROUP BY drinkName
        ORDER BY lastUsed DESC
        LIMIT 2
    """)
    fun getRecentlyUsedDrinks(): Flow<List<RecentDrink>>
}

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
    // This query gets the full row data for the most recent entry of each distinct drink
    // The subquery finds the max timestamp for each drinkName
    // The outer query joins back to get all column values from that specific row
    @Query("""
        SELECT c1.drinkName, c1.caffeineMg, c1.emoji, c1.timestamp as lastUsed
        FROM consumption_log c1
        INNER JOIN (
            SELECT drinkName, MAX(timestamp) as maxTimestamp
            FROM consumption_log
            GROUP BY drinkName
        ) c2 ON c1.drinkName = c2.drinkName AND c1.timestamp = c2.maxTimestamp
        ORDER BY c1.timestamp DESC
        LIMIT 2
    """)
    fun getRecentlyUsedDrinks(): Flow<List<RecentDrink>>
}

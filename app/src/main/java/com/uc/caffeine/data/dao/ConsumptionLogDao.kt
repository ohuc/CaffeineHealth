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

    @Query(
        """
        UPDATE consumption_log
        SET caffeineMg = :caffeineMg,
            quantity = :quantity,
            unitKey = :unitKey,
            unitCaffeineMg = :unitCaffeineMg,
            startedAtMillis = :startedAtMillis,
            durationMinutes = :durationMinutes
        WHERE id = :entryId
        """
    )
    suspend fun updateEntryById(
        entryId: Int,
        caffeineMg: Int,
        quantity: Int,
        unitKey: String,
        unitCaffeineMg: Double,
        startedAtMillis: Long,
        durationMinutes: Int,
    )

    @Query("DELETE FROM consumption_log WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Int)

    // Get ALL entries from today only
    // :startOfDay is a parameter we pass in — Room replaces it with the actual value
    // startOfDay = midnight today in milliseconds
    @Query("SELECT * FROM consumption_log WHERE startedAtMillis >= :startOfDay ORDER BY startedAtMillis DESC")
    fun getTodayEntries(startOfDay: Long): Flow<List<ConsumptionEntry>>

    // Sum of caffeine logged today — the big number on the home screen
    // COALESCE means "if SUM returns null (no rows), return 0 instead"
    @Query("SELECT COALESCE(SUM(caffeineMg), 0) FROM consumption_log WHERE startedAtMillis >= :startOfDay")
    fun getTodayTotal(startOfDay: Long): Flow<Int>

    // All entries ever — for the History screen
    @Query("SELECT * FROM consumption_log ORDER BY startedAtMillis DESC")
    fun getAllEntries(): Flow<List<ConsumptionEntry>>

    // One-shot read of all entries — used for Health Connect bulk sync
    @Query("SELECT * FROM consumption_log ORDER BY startedAtMillis DESC")
    suspend fun getAllEntriesOnce(): List<ConsumptionEntry>

    // Delete all entries from today — the "Reset Today" button
    @Query("DELETE FROM consumption_log WHERE startedAtMillis >= :startOfDay")
    suspend fun clearToday(startOfDay: Long)

    @Query("DELETE FROM consumption_log")
    suspend fun deleteAll()

    // The 2 most recently logged DISTINCT serving combos — used for Add screen quick add
    // The subquery finds the max timestamp for each saved serving selection
    // The outer query joins back to get all column values from that specific row
    @Query("""
        SELECT
            c1.drinkName,
            c1.caffeineMg,
            c1.emoji,
            c1.presetItemId,
            c1.quantity,
            c1.unitKey,
            c1.unitCaffeineMg,
            c1.imageName,
            c1.absorptionRate,
            c1.durationMinutes,
            c1.startedAtMillis as lastUsed
        FROM consumption_log c1
        INNER JOIN (
            SELECT presetItemId, drinkName, quantity, unitKey, durationMinutes, MAX(startedAtMillis) as maxStartedAtMillis
            FROM consumption_log
            GROUP BY presetItemId, drinkName, quantity, unitKey, durationMinutes
        ) c2
            ON c1.presetItemId = c2.presetItemId
            AND c1.drinkName = c2.drinkName
            AND c1.quantity = c2.quantity
            AND c1.unitKey = c2.unitKey
            AND c1.durationMinutes = c2.durationMinutes
            AND c1.startedAtMillis = c2.maxStartedAtMillis
        ORDER BY c1.startedAtMillis DESC
        LIMIT 2
    """)
    fun getRecentlyUsedDrinks(): Flow<List<RecentDrink>>
}

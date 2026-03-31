package com.uc.caffeine.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// This table stores EVERY drink the user logs — one row per drink consumed
// This is how we get:
//   - Today's total  (sum WHERE timestamp is today)
//   - History        (group by day)
//   - Half-life math (we know exact time each drink was consumed)
@Entity(tableName = "consumption_log")
data class ConsumptionEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // We store the drink name + mg directly here (not a foreign key)
    // Why? Because if the user later deletes a preset, history should still show correctly
    val drinkName: String,
    val caffeineMg: Int,
    val emoji: String,

    // Timestamp = milliseconds since epoch (System.currentTimeMillis())
    // Easy to compare, sort, and filter by date
    // e.g. "today" = timestamp > (start of today in ms)
    val timestamp: Long = System.currentTimeMillis()
)

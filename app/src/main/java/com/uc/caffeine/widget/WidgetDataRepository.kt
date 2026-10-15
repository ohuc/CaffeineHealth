package com.uc.caffeine.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.uc.caffeine.data.CaffeineDatabase
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.util.CaffeineCalculator
import com.uc.caffeine.util.calculateNextBedtimeMillis
import com.uc.caffeine.util.formatTimestampToTime
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

data class WidgetDrink(val info: RecentDrink, val icon: Bitmap?) {
    /** Identifies this drink across refreshes, so a tile can show the "logged" confirmation. */
    val key: String get() = "${info.drinkName}|${info.quantity}|${info.unitKey}|${info.caffeineMg}"
}

/** How the projected level at bedtime compares with the user's sleep threshold (same bands as Home). */
enum class SleepOutlook { Clear, Caution, Disruptive }

data class WidgetData(
    val currentCaffeineMg: Double,
    val caffeineAtBedtimeMg: Double,
    val bedtimeMillis: Long,
    val bedtimeLabel: String,
    val recentDrinks: List<WidgetDrink>,
    val allEntries: List<ConsumptionEntry>,
    val settings: UserSettings,
) {
    val sleepOutlook: SleepOutlook
        get() = when {
            caffeineAtBedtimeMg < settings.sleepThresholdMg -> SleepOutlook.Clear
            caffeineAtBedtimeMg < settings.sleepThresholdMg * 1.5 -> SleepOutlook.Caution
            else -> SleepOutlook.Disruptive
        }
}

class WidgetDataRepository(private val context: Context) {
    suspend fun load(): WidgetData {
        val settings = SettingsRepository(context).settingsFlow.first()
        val db = CaffeineDatabase.getDatabase(context)
        val entries = db.consumptionLogDao().getAllEntriesOnce()
        // Larger widget sizes show more tiles; each layout takes as many of these as fit.
        val recentDrinks = db.consumptionLogDao().getRecentlyUsedDrinksOnce(limit = MAX_RECENT_DRINKS)
            .map { WidgetDrink(info = it, icon = loadDrinkIcon(it.imageName)) }
        val now = System.currentTimeMillis()
        val bedtimeMillis = calculateNextBedtimeMillis(now, settings)
        return WidgetData(
            currentCaffeineMg = CaffeineCalculator.calculateCurrentLevel(
                entries, now, settings.effectiveHalfLifeMinutes
            ),
            caffeineAtBedtimeMg = CaffeineCalculator.calculateCurrentLevel(
                entries, bedtimeMillis, settings.effectiveHalfLifeMinutes
            ),
            bedtimeMillis = bedtimeMillis,
            bedtimeLabel = formatTimestampToTime(bedtimeMillis, settings),
            recentDrinks = recentDrinks,
            allEntries = entries,
            settings = settings,
        )
    }

    /** Deterministic mock data for [GlanceAppWidget.providePreview] renders, with real drink art. */
    fun mockData(): WidgetData {
        val now = System.currentTimeMillis()
        val mockEntries = listOf(
            ConsumptionEntry(
                drinkName = "Drip coffee",
                caffeineMg = 95,
                emoji = "☕",
                startedAtMillis = now - TimeUnit.HOURS.toMillis(1),
            ),
            ConsumptionEntry(
                drinkName = "Espresso",
                caffeineMg = 63,
                emoji = "☕",
                startedAtMillis = now - TimeUnit.HOURS.toMillis(3),
            ),
        )
        val mockRecent = listOf(
            mockDrink("Drip coffee", 95, "cup", "drip-coffee", now),
            mockDrink("Espresso", 63, "shot", "espresso", now - TimeUnit.HOURS.toMillis(3)),
            mockDrink("Green tea", 30, "cup", "green-tea", now - TimeUnit.HOURS.toMillis(20)),
            mockDrink("Coca-Cola", 34, "can", "coca-cola", now - TimeUnit.HOURS.toMillis(30)),
        )
        val settings = UserSettings()
        val bedtimeMillis = calculateNextBedtimeMillis(now, settings)
        return WidgetData(
            currentCaffeineMg = 142.6,
            caffeineAtBedtimeMg = 41.0,
            bedtimeMillis = bedtimeMillis,
            bedtimeLabel = formatTimestampToTime(bedtimeMillis, settings),
            recentDrinks = mockRecent.map { WidgetDrink(info = it, icon = loadDrinkIcon(it.imageName)) },
            allEntries = mockEntries,
            settings = settings,
        )
    }

    companion object {
        const val MAX_RECENT_DRINKS = 4

        // Drink art is shown at 40dp at most; decoding the full asset would put several MB of
        // pixels into every RemoteViews parcel for nothing.
        private const val ICON_MAX_PX = 144

        private fun mockDrink(
            name: String,
            mg: Int,
            unitKey: String,
            imageName: String,
            lastUsed: Long,
        ) = RecentDrink(
            drinkName = name,
            caffeineMg = mg,
            emoji = "☕",
            presetItemId = "",
            quantity = 1.0,
            unitKey = unitKey,
            unitCaffeineMg = mg.toDouble(),
            imageName = imageName,
            absorptionRate = 45,
            durationMinutes = 10,
            lastUsed = lastUsed,
        )
    }

    private fun loadDrinkIcon(imageName: String): Bitmap? {
        if (imageName.isBlank()) return null
        val decoded = try {
            when {
                imageName.startsWith("content://") ->
                    context.contentResolver.openInputStream(Uri.parse(imageName))
                        ?.use { BitmapFactory.decodeStream(it) }
                imageName.startsWith("/") ->
                    BitmapFactory.decodeFile(imageName)
                else ->
                    context.assets.open("items/$imageName.png")
                        .use { BitmapFactory.decodeStream(it) }
            }
        } catch (_: Exception) { null } ?: return null
        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= ICON_MAX_PX) return decoded
        val ratio = ICON_MAX_PX.toFloat() / longest
        return decoded.scale(
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
        ).also { if (it !== decoded) decoded.recycle() }
    }
}

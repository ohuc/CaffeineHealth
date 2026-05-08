package com.uc.caffeine.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.uc.caffeine.data.CaffeineDatabase
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.util.CaffeineCalculator
import com.uc.caffeine.util.calculateNextBedtimeMillis
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

data class WidgetDrink(val info: RecentDrink, val icon: Bitmap?)

data class WidgetData(
    val currentCaffeineMg: Double,
    val caffeineAtBedtimeMg: Double,
    val bedtimeMillis: Long,
    val recentDrinks: List<WidgetDrink>,
    val allEntries: List<ConsumptionEntry>,
    val settings: UserSettings,
)

class WidgetDataRepository(private val context: Context) {
    suspend fun load(): WidgetData {
        val settings = SettingsRepository(context).settingsFlow.first()
        val db = CaffeineDatabase.getDatabase(context)
        val entries = db.consumptionLogDao().getAllEntriesOnce()
        val recentDrinks = db.consumptionLogDao().getRecentlyUsedDrinksOnce()
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
            recentDrinks = recentDrinks,
            allEntries = entries,
            settings = settings,
        )
    }

    companion object {
        /** Deterministic mock data for [GlanceAppWidget.providePreview] renders. */
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
                    drinkName = "Costa Espresso",
                    caffeineMg = 100,
                    emoji = "☕",
                    startedAtMillis = now - TimeUnit.HOURS.toMillis(3),
                ),
            )
            val mockRecent = listOf(
                RecentDrink(
                    drinkName = "Drip coffee",
                    caffeineMg = 95,
                    emoji = "☕",
                    presetItemId = "",
                    quantity = 1,
                    unitKey = "cup",
                    unitCaffeineMg = 95.0,
                    imageName = "",
                    absorptionRate = 45,
                    durationMinutes = 10,
                    lastUsed = now,
                ),
                RecentDrink(
                    drinkName = "Costa Espresso",
                    caffeineMg = 100,
                    emoji = "☕",
                    presetItemId = "",
                    quantity = 1,
                    unitKey = "shot",
                    unitCaffeineMg = 100.0,
                    imageName = "",
                    absorptionRate = 45,
                    durationMinutes = 5,
                    lastUsed = now - TimeUnit.HOURS.toMillis(3),
                ),
            )
            val settings = UserSettings()
            return WidgetData(
                currentCaffeineMg = 358.6,
                caffeineAtBedtimeMg = 185.0,
                bedtimeMillis = calculateNextBedtimeMillis(now, settings),
                recentDrinks = mockRecent.map { WidgetDrink(info = it, icon = null) },
                allEntries = mockEntries,
                settings = settings,
            )
        }
    }

    private fun loadDrinkIcon(imageName: String): Bitmap? {
        if (imageName.isBlank()) return null
        return try {
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
        } catch (_: Exception) { null }
    }
}

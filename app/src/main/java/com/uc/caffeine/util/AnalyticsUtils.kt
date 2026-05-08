package com.uc.caffeine.util

import androidx.annotation.StringRes
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class AnalyticsRange(@param:StringRes val labelRes: Int) {
    TODAY(R.string.analytics_range_today),
    YESTERDAY(R.string.analytics_range_yesterday),
    LAST_30_DAYS(R.string.analytics_range_30_days),
    LAST_90_DAYS(R.string.analytics_range_90_days),
    CUSTOM(R.string.analytics_range_custom),
}

data class SourceItemEntry(
    val name: String,
    val emoji: String,
    val imageName: String,
    val totalCaffeineMg: Int,
    val count: Int,
)

data class AnalyticsUiState(
    val selectedRange: AnalyticsRange = AnalyticsRange.LAST_30_DAYS,
    val hasData: Boolean = false,
    val totalCaffeineMg: Int = 0,
    val averageCaffeinePerDayMg: Int = 0,
    val safeNights: Int = 0,
    val totalNights: Int = 0,
    val topSourceLabel: String = "No data yet",
    val sourceAxisLabels: List<String> = emptyList(),
    val sourceValues: List<Double> = emptyList(),
    val sourceItemEntries: List<SourceItemEntry> = emptyList(),
    val bedtimeAxisLabels: List<String> = emptyList(),
    val bedtimeValues: List<Double> = emptyList(),
    val timeOfDayAxisLabels: List<String> = TimeOfDayBucket.axisLabels,
    val timeOfDayValues: List<Double> = List(TimeOfDayBucket.entries.size) { 0.0 },
    val sleepThresholdMg: Double = 0.0,
    val customStartDate: java.time.LocalDate? = null,
    val customEndDate: java.time.LocalDate? = null,
)

private const val OtherCategoryKey = "__other__"
private const val OtherCategoryLabel = "Other"

private data class AnalyticsWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

private data class DailyBedtimeStat(
    val date: LocalDate,
    val caffeineMg: Double,
    val isSafe: Boolean,
)

private enum class TimeOfDayBucket(val label: String) {
    NIGHT("Night"),
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    ;

    companion object {
        val axisLabels: List<String> = entries.map(TimeOfDayBucket::label)

        fun fromHour(hour: Int): TimeOfDayBucket = when {
            hour in 5..11 -> MORNING
            hour in 12..14 -> AFTERNOON
            hour in 15..18 -> EVENING
            else -> NIGHT  // 0..4 and 19..23
        }
    }
}

fun buildAnalyticsUiState(
    entries: List<ConsumptionEntry>,
    presets: List<DrinkPreset>,
    settings: UserSettings,
    selectedRange: AnalyticsRange,
    nowMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault(),
    customStartDate: LocalDate? = null,
    customEndDate: LocalDate? = null,
): AnalyticsUiState {
    val zoneId = settings.resolvedZoneId()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val window = resolveAnalyticsWindow(range = selectedRange, today = today, customStartDate = customStartDate, customEndDate = customEndDate)
    val datesInWindow = datesInWindow(window.startDate, window.endDate)
    val entriesInRange = entries.filter { entry ->
        val localDate = Instant.ofEpochMilli(entry.startedAtMillis).atZone(zoneId).toLocalDate()
        !localDate.isBefore(window.startDate) && !localDate.isAfter(window.endDate)
    }
    val dailyBedtimeStats = datesInWindow.map { date ->
        buildDailyBedtimeStat(
            date = date,
            entries = entries,
            settings = settings,
        )
    }
    val sourceTotals = buildSourceTotals(entriesInRange = entriesInRange, presets = presets)
    val sortedSourceTotals = sourceTotals.entries.sortedWith(
        compareBy<Map.Entry<String, Double>>(
            { categorySortOrder(it.key) },
            { displayLabelForCategoryKey(it.key) },
        ),
    )
    val sourceItemEntries = buildItemTotals(entriesInRange = entriesInRange)
    val bedtimeSeries = buildBedtimeSeries(
        selectedRange = selectedRange,
        dailyBedtimeStats = dailyBedtimeStats,
        locale = locale,
    )
    val totalCaffeineMg = entriesInRange.sumOf(ConsumptionEntry::caffeineMg)
    val averageCaffeinePerDayMg = if (datesInWindow.isNotEmpty()) {
        (totalCaffeineMg.toDouble() / datesInWindow.size.toDouble()).roundToInt()
    } else {
        0
    }
    val timeOfDayValues = buildTimeOfDayValues(entriesInRange = entriesInRange, zoneId = zoneId)
    val hasData = entriesInRange.isNotEmpty()

    return AnalyticsUiState(
        selectedRange = selectedRange,
        hasData = hasData,
        totalCaffeineMg = totalCaffeineMg,
        averageCaffeinePerDayMg = averageCaffeinePerDayMg,
        safeNights = dailyBedtimeStats.count(DailyBedtimeStat::isSafe),
        totalNights = dailyBedtimeStats.size,
        topSourceLabel = sortedSourceTotals
            .maxByOrNull { it.value }
            ?.let { displayLabelForCategoryKey(it.key) }
            ?: "No data yet",
        sourceAxisLabels = if (hasData) {
            sortedSourceTotals.map { entry ->
                chartLabelForCategoryKey(entry.key)
            }
        } else {
            emptyList()
        },
        sourceValues = if (hasData) {
            sortedSourceTotals.map(Map.Entry<String, Double>::value)
        } else {
            emptyList()
        },
        sourceItemEntries = if (hasData) sourceItemEntries else emptyList(),
        bedtimeAxisLabels = if (hasData) bedtimeSeries.first else emptyList(),
        bedtimeValues = if (hasData) bedtimeSeries.second else emptyList(),
        timeOfDayAxisLabels = TimeOfDayBucket.axisLabels,
        timeOfDayValues = timeOfDayValues,
        sleepThresholdMg = settings.sleepThresholdMg.toDouble(),
        customStartDate = customStartDate,
        customEndDate = customEndDate,
    )
}

private fun buildItemTotals(
    entriesInRange: List<ConsumptionEntry>,
): List<SourceItemEntry> {
    if (entriesInRange.isEmpty()) return emptyList()
    return entriesInRange
        .groupBy { it.drinkName }
        .map { (name, group) ->
            SourceItemEntry(
                name = name,
                emoji = group.first().emoji,
                imageName = group.first().imageName,
                totalCaffeineMg = group.sumOf { it.caffeineMg },
                count = group.size,
            )
        }
        .sortedByDescending { it.totalCaffeineMg }
}

private fun buildSourceTotals(
    entriesInRange: List<ConsumptionEntry>,
    presets: List<DrinkPreset>,
): Map<String, Double> {
    if (entriesInRange.isEmpty()) return emptyMap()

    val presetByItemId = presets
        .filter { it.itemId.isNotBlank() }
        .associateBy(DrinkPreset::itemId)

    return entriesInRange
        .groupBy { entry ->
            val preset = presetByItemId[entry.presetItemId]
            preset?.category?.let(::normalizeCategoryKey) ?: OtherCategoryKey
        }
        .mapValues { (_, groupedEntries) ->
            groupedEntries.sumOf { it.caffeineMg.toDouble() }
        }
}

private fun buildTimeOfDayValues(
    entriesInRange: List<ConsumptionEntry>,
    zoneId: java.time.ZoneId,
): List<Double> {
    val totals = DoubleArray(TimeOfDayBucket.entries.size)
    entriesInRange.forEach { entry ->
        val hour = Instant.ofEpochMilli(entry.startedAtMillis).atZone(zoneId).hour
        val bucket = TimeOfDayBucket.fromHour(hour)
        totals[bucket.ordinal] += entry.caffeineMg.toDouble()
    }
    return totals.toList()
}

private fun buildBedtimeSeries(
    selectedRange: AnalyticsRange,
    dailyBedtimeStats: List<DailyBedtimeStat>,
    locale: Locale,
): Pair<List<String>, List<Double>> {
    if (dailyBedtimeStats.isEmpty()) return emptyList<String>() to emptyList()

    val dayCount = dailyBedtimeStats.size
    return when {
        dayCount <= 2 -> {
            val formatter = DateTimeFormatter.ofPattern("EEE", locale)
            dailyBedtimeStats.map { it.date.format(formatter) } to
                dailyBedtimeStats.map(DailyBedtimeStat::caffeineMg)
        }
        dayCount <= 35 -> {
            if (dayCount <= 7) {
                val formatter = DateTimeFormatter.ofPattern("EEE", locale)
                dailyBedtimeStats.map { it.date.format(formatter) } to
                    dailyBedtimeStats.map(DailyBedtimeStat::caffeineMg)
            } else {
                val formatter = DateTimeFormatter.ofPattern("MMM d", locale)
                val chunks = dailyBedtimeStats.chunked(size = 7)
                chunks.map { bucket -> bucket.last().date.format(formatter) } to
                    chunks.map { bucket -> bucket.map(DailyBedtimeStat::caffeineMg).average() }
            }
        }
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM", locale)
            val months = dailyBedtimeStats.groupBy { stat -> YearMonth.from(stat.date) }
                .toSortedMap()
            months.keys.map { month -> month.format(formatter) } to
                months.values.map { bucket -> bucket.map(DailyBedtimeStat::caffeineMg).average() }
        }
    }
}

private fun buildDailyBedtimeStat(
    date: LocalDate,
    entries: List<ConsumptionEntry>,
    settings: UserSettings,
): DailyBedtimeStat {
    val zoneId = settings.resolvedZoneId()
    val middayAnchorMillis = ZonedDateTime.of(date, LocalTime.NOON, zoneId)
        .toInstant()
        .toEpochMilli()
    val bedtimeMillis = calculateNextBedtimeMillis(
        currentTimeMillis = middayAnchorMillis,
        settings = settings,
    )
    val caffeineMg = CaffeineCalculator.calculateCurrentLevel(
        entries = entries,
        currentTimeMillis = bedtimeMillis,
        halfLifeMinutes = settings.effectiveHalfLifeMinutes,
    )

    return DailyBedtimeStat(
        date = date,
        caffeineMg = caffeineMg,
        isSafe = caffeineMg <= settings.sleepThresholdMg.toDouble(),
    )
}

private fun resolveAnalyticsWindow(
    range: AnalyticsRange,
    today: LocalDate,
    customStartDate: LocalDate? = null,
    customEndDate: LocalDate? = null,
): AnalyticsWindow {
    return when (range) {
        AnalyticsRange.TODAY -> AnalyticsWindow(
            startDate = today,
            endDate = today,
        )
        AnalyticsRange.YESTERDAY -> AnalyticsWindow(
            startDate = today.minusDays(1),
            endDate = today.minusDays(1),
        )
        AnalyticsRange.LAST_30_DAYS -> AnalyticsWindow(
            startDate = today.minusDays(29),
            endDate = today,
        )
        AnalyticsRange.LAST_90_DAYS -> AnalyticsWindow(
            startDate = today.minusDays(89),
            endDate = today,
        )
        AnalyticsRange.CUSTOM -> AnalyticsWindow(
            startDate = customStartDate ?: today.minusDays(29),
            endDate = customEndDate ?: today,
        )
    }
}

private fun datesInWindow(
    startDate: LocalDate,
    endDate: LocalDate,
): List<LocalDate> {
    return buildList {
        var date = startDate
        while (!date.isAfter(endDate)) {
            add(date)
            date = date.plusDays(1)
        }
    }
}

private fun normalizeCategoryKey(category: String): String {
    return when (category.trim().lowercase().replace(" ", "_").replace("-", "_")) {
        "coffee" -> "coffee"
        "energy", "energy_drink", "energy_drinks" -> "energy_drink"
        "soft_drink", "soft_drinks", "softdrink", "soda" -> "soft_drink"
        "tea" -> "tea"
        "chocolate" -> "chocolate"
        "pill", "pills" -> "pill"
        else -> OtherCategoryKey
    }
}

private fun categorySortOrder(categoryKey: String): Int {
    return if (categoryKey == OtherCategoryKey) {
        Int.MAX_VALUE
    } else {
        CategoryUtils.getCategorySortOrder(categoryKey)
    }
}

private fun displayLabelForCategoryKey(categoryKey: String): String {
    return if (categoryKey == OtherCategoryKey) {
        OtherCategoryLabel
    } else {
        CategoryUtils.getCategoryDisplayName(categoryKey)
    }
}

private fun chartLabelForCategoryKey(categoryKey: String): String {
    return CategoryUtils.getCategoryButtonLabel(displayLabelForCategoryKey(categoryKey))
}

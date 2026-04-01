package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.UserSettings
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ChartDataGenerator {
    
    private const val INTERVAL_MINUTES = 15
    private const val HOURS_BEFORE = 12
    private const val HOURS_AFTER = 12
    private const val TOTAL_POINTS = ((HOURS_BEFORE + HOURS_AFTER) * 60 / INTERVAL_MINUTES) + 1
    private const val MINI_CHART_TAIL_HOURS = 2
    private const val MAX_DETAIL_WINDOW_HOURS = 24
    
    /**
     * Generate 24-hour caffeine curve data for charting.
     * 
     * @param entries All consumption entries (will filter to relevant time range)
     * @param settings User settings (half-life, threshold, bedtime)
     * @param currentTime Current timestamp (usually System.currentTimeMillis())
     * @return ChartData with 97 data points, including both ends of the 24-hour window
     */
    fun generateChartData(
        entries: List<ConsumptionEntry>,
        settings: UserSettings,
        currentTime: Long = System.currentTimeMillis()
    ): ChartData {
        val startTime = currentTime - (HOURS_BEFORE * 60 * 60 * 1000L)
        val endTime = currentTime + (HOURS_AFTER * 60 * 60 * 1000L)
        
        val dataPoints = mutableListOf<CaffeineDataPoint>()
        
        // Generate points at 15-minute intervals across the full 24-hour window.
        for (i in 0 until TOTAL_POINTS) {
            val pointTime = startTime + (i * INTERVAL_MINUTES * 60 * 1000L)
            val isHistorical = pointTime <= currentTime
            
            // Calculate caffeine level at this point in time
            val caffeineLevel = CaffeineCalculator.calculateCurrentLevel(
                entries = entries,
                currentTimeMillis = pointTime,
                halfLifeMinutes = settings.halfLifeMinutes
            )
            
            dataPoints.add(
                CaffeineDataPoint(
                    timestampMillis = pointTime,
                    caffeineLevel = caffeineLevel,
                    isHistorical = isHistorical
                )
            )
        }
        
        // Calculate the bedtime occurrence that falls inside the visible 24-hour window.
        val bedtime = calculateBedtimeWithinWindow(startTime, endTime, currentTime, settings)
        
        // Mark bedtime point
        val bedtimeIndex = dataPoints.indexOfFirst { 
            it.timestampMillis >= bedtime 
        }
        if (bedtimeIndex >= 0) {
            dataPoints[bedtimeIndex] = dataPoints[bedtimeIndex].copy(isBedtime = true)
        }
        
        // Mark peak absorption points for recent drinks
        markPeakPoints(dataPoints, entries, startTime, endTime)
        
        return ChartData(
            dataPoints = dataPoints,
            consumptionMarkers = buildConsumptionMarkers(entries, startTime, endTime),
            thresholdLevel = settings.sleepThresholdMg.toDouble(),
            bedtimeMillis = bedtime,
            currentTimeMillis = currentTime
        )
    }
    
    fun generateContributionDetail(
        entry: ConsumptionEntry,
        settings: UserSettings,
        currentTime: Long = System.currentTimeMillis()
    ): ConsumptionContributionDetail {
        val intervalMillis = INTERVAL_MINUTES * 60 * 1000L
        val peakTime = entry.timestamp + (entry.absorptionRate * 60 * 1000L)
        val startTime = roundDownToInterval(entry.timestamp, intervalMillis)
        val maxWindowEnd = startTime + (MAX_DETAIL_WINDOW_HOURS * 60 * 60 * 1000L)
        val desiredEnd = max(currentTime, peakTime) + (MINI_CHART_TAIL_HOURS * 60 * 60 * 1000L)
        val endTime = max(
            startTime + intervalMillis,
            min(roundUpToInterval(desiredEnd, intervalMillis), maxWindowEnd)
        )

        val dataPoints = buildList {
            var pointTime = startTime
            while (pointTime <= endTime) {
                add(
                    ConsumptionContributionPoint(
                        timestampMillis = pointTime,
                        caffeineContributionMg = CaffeineCalculator.calculateDecayedAmount(
                            caffeineMg = entry.caffeineMg,
                            consumedAtMillis = entry.timestamp,
                            currentTimeMillis = pointTime,
                            absorptionMinutes = entry.absorptionRate,
                            halfLifeMinutes = settings.halfLifeMinutes
                        )
                    )
                )
                pointTime += intervalMillis
            }
        }

        val peakContribution = CaffeineCalculator.calculateDecayedAmount(
            caffeineMg = entry.caffeineMg,
            consumedAtMillis = entry.timestamp,
            currentTimeMillis = peakTime,
            absorptionMinutes = entry.absorptionRate,
            halfLifeMinutes = settings.halfLifeMinutes
        )
        val currentContribution = CaffeineCalculator.calculateDecayedAmount(
            caffeineMg = entry.caffeineMg,
            consumedAtMillis = entry.timestamp,
            currentTimeMillis = currentTime,
            absorptionMinutes = entry.absorptionRate,
            halfLifeMinutes = settings.halfLifeMinutes
        )
        val peakMarkerIndex = dataPoints.indices.minByOrNull { index ->
            abs(dataPoints[index].timestampMillis - peakTime)
        } ?: 0
        val currentX = ((currentTime - startTime).toDouble() / intervalMillis)
            .coerceIn(0.0, dataPoints.lastIndex.toDouble())

        return ConsumptionContributionDetail(
            entryId = entry.id,
            drinkName = entry.drinkName,
            emoji = entry.emoji,
            loggedAtMillis = entry.timestamp,
            caffeineMg = entry.caffeineMg,
            dataPoints = dataPoints,
            peakTimestampMillis = peakTime,
            peakContributionMg = peakContribution,
            currentContributionMg = currentContribution,
            totalContributionMg = entry.caffeineMg.toDouble(),
            thresholdLevel = settings.sleepThresholdMg.toDouble(),
            currentTimeMillis = currentTime,
            currentX = currentX,
            peakMarkerIndex = peakMarkerIndex
        )
    }

    private fun calculateBedtimeWithinWindow(
        startTime: Long,
        endTime: Long,
        currentTime: Long,
        settings: UserSettings
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(Calendar.HOUR_OF_DAY, settings.sleepTimeHour)
        calendar.set(Calendar.MINUTE, settings.sleepTimeMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis < startTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        } else if (calendar.timeInMillis > endTime) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return calendar.timeInMillis
    }
    
    private fun markPeakPoints(
        dataPoints: MutableList<CaffeineDataPoint>,
        entries: List<ConsumptionEntry>,
        startTime: Long,
        endTime: Long
    ) {
        // Find entries within our time range
        val relevantEntries = entries.filter { entry ->
            val peakTime = entry.timestamp + (entry.absorptionRate * 60 * 1000L)
            peakTime in startTime..endTime
        }
        
        // Mark the nearest data point to each peak
        relevantEntries.forEach { entry ->
            val peakTime = entry.timestamp + (entry.absorptionRate * 60 * 1000L)
            val nearestIndex = dataPoints.indexOfFirst { 
                it.timestampMillis >= peakTime 
            }
            if (nearestIndex >= 0) {
                dataPoints[nearestIndex] = dataPoints[nearestIndex].copy(isPeak = true)
            }
        }
    }

    private fun buildConsumptionMarkers(
        entries: List<ConsumptionEntry>,
        startTime: Long,
        endTime: Long
    ): List<ChartConsumptionMarker> {
        val intervalMillis = INTERVAL_MINUTES * 60 * 1000L

        return entries
            .asSequence()
            .filter { it.timestamp in startTime..endTime }
            .sortedBy { it.timestamp }
            .groupBy { entry ->
                ((entry.timestamp - startTime).toDouble() / intervalMillis)
                    .roundToInt()
                    .coerceIn(0, TOTAL_POINTS - 1)
            }
            .map { (xIndex, groupedEntries) ->
                ChartConsumptionMarker(
                    xIndex = xIndex,
                    emojiLabel = groupedEntries.toEmojiLabel(),
                    timestampMillis = groupedEntries.first().timestamp
                )
            }
            .sortedBy { it.xIndex }
    }

    private fun List<ConsumptionEntry>.toEmojiLabel(): String {
        if (isEmpty()) return ""

        val emojis = map { it.emoji }
        return when {
            emojis.size <= 3 -> emojis.joinToString(separator = "")
            else -> emojis.take(2).joinToString(separator = "") + "+${emojis.size - 2}"
        }
    }

    private fun roundDownToInterval(timestamp: Long, intervalMillis: Long): Long {
        return timestamp - (timestamp % intervalMillis)
    }

    private fun roundUpToInterval(timestamp: Long, intervalMillis: Long): Long {
        val remainder = timestamp % intervalMillis
        return if (remainder == 0L) timestamp else timestamp + (intervalMillis - remainder)
    }
}

data class CaffeineDataPoint(
    val timestampMillis: Long,
    val caffeineLevel: Double,
    val isHistorical: Boolean,
    val isBedtime: Boolean = false,
    val isPeak: Boolean = false
)

data class ChartData(
    val dataPoints: List<CaffeineDataPoint>,
    val consumptionMarkers: List<ChartConsumptionMarker>,
    val thresholdLevel: Double,
    val bedtimeMillis: Long,
    val currentTimeMillis: Long
)

data class ChartConsumptionMarker(
    val xIndex: Int,
    val emojiLabel: String,
    val timestampMillis: Long
)

data class ConsumptionContributionPoint(
    val timestampMillis: Long,
    val caffeineContributionMg: Double
)

data class ConsumptionContributionDetail(
    val entryId: Int,
    val drinkName: String,
    val emoji: String,
    val loggedAtMillis: Long,
    val caffeineMg: Int,
    val dataPoints: List<ConsumptionContributionPoint>,
    val peakTimestampMillis: Long,
    val peakContributionMg: Double,
    val currentContributionMg: Double,
    val totalContributionMg: Double,
    val thresholdLevel: Double,
    val currentTimeMillis: Long,
    val currentX: Double,
    val peakMarkerIndex: Int
)

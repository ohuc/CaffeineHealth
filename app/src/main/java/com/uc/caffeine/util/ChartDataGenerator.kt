package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.UserSettings
import kotlin.math.max

object ChartDataGenerator {
    
    private const val INTERVAL_MINUTES = 15
    private const val HOURS_BEFORE = 12
    private const val HOURS_AFTER = 12
    private const val TOTAL_POINTS = 96 // (24 * 60) / 15
    
    /**
     * Generate 24-hour caffeine curve data for charting.
     * 
     * @param entries All consumption entries (will filter to relevant time range)
     * @param settings User settings (half-life, threshold, bedtime)
     * @param currentTime Current timestamp (usually System.currentTimeMillis())
     * @return ChartData with 96 data points
     */
    fun generateChartData(
        entries: List<ConsumptionEntry>,
        settings: UserSettings,
        currentTime: Long = System.currentTimeMillis()
    ): ChartData {
        val startTime = currentTime - (HOURS_BEFORE * 60 * 60 * 1000L)
        val endTime = currentTime + (HOURS_AFTER * 60 * 60 * 1000L)
        
        val dataPoints = mutableListOf<CaffeineDataPoint>()
        
        // Generate 96 points at 15-minute intervals
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
        
        // Calculate next bedtime
        val bedtime = calculateNextBedtime(currentTime, settings)
        
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
            thresholdLevel = settings.sleepThresholdMg.toDouble(),
            bedtimeMillis = bedtime,
            currentTimeMillis = currentTime
        )
    }
    
    private fun calculateNextBedtime(currentTime: Long, settings: UserSettings): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, settings.sleepTimeHour)
        calendar.set(java.util.Calendar.MINUTE, settings.sleepTimeMinute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // If bedtime already passed today, move to tomorrow
        if (calendar.timeInMillis <= currentTime) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
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
    val thresholdLevel: Double,
    val bedtimeMillis: Long,
    val currentTimeMillis: Long
)

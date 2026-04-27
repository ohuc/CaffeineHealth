package com.uc.caffeine.util

import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.UserSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object ChartDataGenerator {

    private const val BASE_INTERVAL_MINUTES = 15
    private const val BASE_INTERVAL_MILLIS = BASE_INTERVAL_MINUTES * 60 * 1000L
    private const val RECENT_HISTORY_HOURS = 24
    private const val FUTURE_WINDOW_HOURS = 12
    private const val MAX_FUTURE_BASELINE_WINDOW_HOURS = 168
    private const val DEFAULT_EMPTY_HISTORY_LOOKBACK_HOURS = 12
    private const val HOURLY_HISTORY_DAYS = 7
    private const val THREE_HOURLY_HISTORY_DAYS = 30
    private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    private const val THIRTY_MINUTES_MILLIS = 30 * 60 * 1000L
    private const val THREE_HOURS_MILLIS = 3 * ONE_HOUR_MILLIS
    private const val SIX_HOURS_MILLIS = 6 * ONE_HOUR_MILLIS
    private const val ONE_DAY_MILLIS = 24 * ONE_HOUR_MILLIS
    private const val THREE_DAYS_MILLIS = 3 * ONE_DAY_MILLIS
    private const val MAX_FUTURE_BASELINE_WINDOW_MILLIS = MAX_FUTURE_BASELINE_WINDOW_HOURS * ONE_HOUR_MILLIS
    private const val SEVEN_DAYS_MILLIS = HOURLY_HISTORY_DAYS * ONE_DAY_MILLIS
    private const val THIRTY_DAYS_MILLIS = THREE_HOURLY_HISTORY_DAYS * ONE_DAY_MILLIS
    private const val MINI_CHART_TAIL_HOURS = 2
    private const val BASELINE_TARGET_MG = 1.0
    // Once caffeine drops below 1 mg it is pharmacologically negligible.
    // Clamping to 0.0 makes the line descend cleanly to the x-axis and
    // prevents the cubic point-connector from creating spline-overshoot
    // artifacts that keep the line floating above the axis.
    private const val DISPLAY_ZERO_THRESHOLD_MG = BASELINE_TARGET_MG

    internal fun toDisplayCaffeineLevel(
        actualCaffeineLevel: Double,
        hasEntries: Boolean,
    ): Double {
        return when {
            !hasEntries -> actualCaffeineLevel
            actualCaffeineLevel < DISPLAY_ZERO_THRESHOLD_MG -> 0.0
            else -> actualCaffeineLevel
        }
    }

    /**
     * Generate an adaptive all-history caffeine curve for charting.
     * 
     * @param entries All consumption entries (will filter to relevant time range)
     * @param settings User settings (half-life, threshold, bedtime)
     * @param currentTime Current timestamp (usually System.currentTimeMillis())
     * @return ChartData covering all logged history plus the active/future window
     */
    fun generateChartData(
        entries: List<ConsumptionEntry>,
        settings: UserSettings,
        currentTime: Long = System.currentTimeMillis()
    ): ChartData {
        val bedtime = calculateNextBedtimeMillis(currentTime, settings)
        val baselineReturnTime = predictFutureBaselineReturnTime(
            entries = entries,
            targetLevelMg = BASELINE_TARGET_MG,
            currentTimeMillis = currentTime,
            halfLifeMinutes = settings.effectiveHalfLifeMinutes,
        )
        val baselineTailEndTime = baselineReturnTime?.plus(BASE_INTERVAL_MILLIS) ?: currentTime
        val endTime = roundUpToInterval(
            max(
                max(currentTime + (FUTURE_WINDOW_HOURS * ONE_HOUR_MILLIS), bedtime),
                baselineTailEndTime,
            ),
            BASE_INTERVAL_MILLIS,
        )
        val domainStartTime = resolveChartStartTime(entries, currentTime)
        val dataPoints = buildAdaptiveTimelinePoints(
            entries = entries,
            settings = settings,
            currentTime = currentTime,
            startTime = domainStartTime,
            endTime = endTime,
        )

        val bedtimeIndex = dataPoints.indexOfFirst { it.timestampMillis >= bedtime }
        if (bedtimeIndex >= 0) {
            dataPoints[bedtimeIndex] = dataPoints[bedtimeIndex].copy(isBedtime = true)
        }

        markPeakPoints(
            dataPoints = dataPoints,
            entries = entries,
            startTime = domainStartTime,
            endTime = endTime,
            halfLifeMinutes = settings.effectiveHalfLifeMinutes,
        )

        return ChartData(
            dataPoints = dataPoints,
            consumptionMarkers = buildConsumptionMarkers(
                entries = entries,
                domainStartTime = domainStartTime,
                endTime = endTime,
                currentTime = currentTime,
            ),
            thresholdLevel = settings.sleepThresholdMg.toDouble(),
            bedtimeMillis = bedtime,
            currentTimeMillis = currentTime,
            domainStartMillis = domainStartTime,
        )
    }

    private fun predictFutureBaselineReturnTime(
        entries: List<ConsumptionEntry>,
        targetLevelMg: Double,
        currentTimeMillis: Long,
        halfLifeMinutes: Int,
    ): Long? {
        val maxPredictionTime = currentTimeMillis + MAX_FUTURE_BASELINE_WINDOW_MILLIS

        val currentReturnTime = CaffeineCalculator.predictTimeUntilLevel(
            entries = entries,
            targetLevelMg = targetLevelMg,
            currentTimeMillis = currentTimeMillis,
            halfLifeMinutes = halfLifeMinutes,
        )
        if (currentReturnTime != null) {
            return currentReturnTime.coerceAtMost(maxPredictionTime)
        }

        val latestFuturePeakTime = entries
            .asSequence()
            .map { entry ->
                CaffeineCalculator.calculatePeakTime(
                    entry = entry,
                    halfLifeMinutes = halfLifeMinutes,
                )
            }
            .filter { peakTime -> peakTime > currentTimeMillis }
            .maxOrNull()
            ?: return null

        val searchStartTime = min(latestFuturePeakTime, maxPredictionTime)
        val peakLevel = CaffeineCalculator.calculateCurrentLevel(
            entries = entries,
            currentTimeMillis = searchStartTime,
            halfLifeMinutes = halfLifeMinutes,
        )
        if (peakLevel <= targetLevelMg) {
            return null
        }

        return CaffeineCalculator.predictTimeUntilLevel(
            entries = entries,
            targetLevelMg = targetLevelMg,
            currentTimeMillis = searchStartTime,
            halfLifeMinutes = halfLifeMinutes,
        )?.coerceAtMost(maxPredictionTime)
    }

    fun generateContributionDetail(
        entry: ConsumptionEntry,
        settings: UserSettings,
        currentTime: Long = System.currentTimeMillis()
    ): ConsumptionContributionDetail {
        val intervalMillis = BASE_INTERVAL_MILLIS
        val peakTime = CaffeineCalculator.calculatePeakTime(
            entry = entry,
            halfLifeMinutes = settings.effectiveHalfLifeMinutes,
        )
        val startTime = roundDownToInterval(entry.startedAtMillis, intervalMillis)
        val maxWindowEnd = startTime + MAX_FUTURE_BASELINE_WINDOW_MILLIS
        val baselineReturnTime = predictEntryBaselineReturnTime(
            entry = entry,
            targetLevelMg = BASELINE_TARGET_MG,
            peakTimeMillis = peakTime,
            maxPredictionTimeMillis = maxWindowEnd,
            halfLifeMinutes = settings.effectiveHalfLifeMinutes,
        )
        val desiredEnd = baselineReturnTime?.plus(intervalMillis)
            ?: max(currentTime, peakTime) + (MINI_CHART_TAIL_HOURS * 60 * 60 * 1000L)
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
                        caffeineContributionMg = toDisplayCaffeineLevel(
                            actualCaffeineLevel = CaffeineCalculator.calculateEntryContribution(
                                entry = entry,
                                currentTimeMillis = pointTime,
                                halfLifeMinutes = settings.effectiveHalfLifeMinutes,
                            ),
                            hasEntries = true,
                        )
                    )
                )
                pointTime += intervalMillis
            }
        }

        val peakContribution = CaffeineCalculator.calculateEntryContribution(
            entry = entry,
            currentTimeMillis = peakTime,
            halfLifeMinutes = settings.effectiveHalfLifeMinutes,
        )
        val currentContribution = CaffeineCalculator.calculateEntryContribution(
            entry = entry,
            currentTimeMillis = currentTime,
            halfLifeMinutes = settings.effectiveHalfLifeMinutes,
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
            imageName = entry.imageName,
            loggedAtMillis = entry.startedAtMillis,
            caffeineMg = entry.caffeineMg,
            durationMinutes = entry.normalizedDurationMinutes,
            finishedAtMillis = entry.finishedAtMillis,
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

    private fun predictEntryBaselineReturnTime(
        entry: ConsumptionEntry,
        targetLevelMg: Double,
        peakTimeMillis: Long,
        maxPredictionTimeMillis: Long,
        halfLifeMinutes: Int,
    ): Long? {
        val peakContribution = CaffeineCalculator.calculateEntryContribution(
            entry = entry,
            currentTimeMillis = peakTimeMillis,
            halfLifeMinutes = halfLifeMinutes,
        )
        if (peakContribution <= targetLevelMg) {
            return null
        }

        return CaffeineCalculator.predictTimeUntilLevel(
            entries = listOf(entry),
            targetLevelMg = targetLevelMg,
            currentTimeMillis = peakTimeMillis,
            halfLifeMinutes = halfLifeMinutes,
        )?.coerceAtMost(maxPredictionTimeMillis)
    }

    fun timestampToDomainX(
        domainStartMillis: Long,
        targetTimestampMillis: Long,
    ): Double {
        return (targetTimestampMillis - domainStartMillis).toDouble() / BASE_INTERVAL_MILLIS.toDouble()
    }

    fun domainXToTimestamp(
        domainStartMillis: Long,
        xValue: Double,
    ): Long {
        return domainStartMillis + (xValue * BASE_INTERVAL_MILLIS).roundToLong()
    }

    private fun resolveChartStartTime(
        entries: List<ConsumptionEntry>,
        currentTime: Long,
    ): Long {
        if (entries.isEmpty()) {
            return roundDownToInterval(
                currentTime - (DEFAULT_EMPTY_HISTORY_LOOKBACK_HOURS * ONE_HOUR_MILLIS),
                BASE_INTERVAL_MILLIS,
            )
        }

        val oldestEntryTime = entries.minOf { it.startedAtMillis }
        val initialBucketSize = samplingIntervalFor(oldestEntryTime, currentTime)
        return roundDownToInterval(oldestEntryTime, initialBucketSize) - initialBucketSize
    }

    private fun buildAdaptiveTimelinePoints(
        entries: List<ConsumptionEntry>,
        settings: UserSettings,
        currentTime: Long,
        startTime: Long,
        endTime: Long,
    ): MutableList<CaffeineDataPoint> {
        val dataPoints = mutableListOf<CaffeineDataPoint>()
        val hasEntries = entries.isNotEmpty()
        var pointTime = startTime

        while (pointTime <= endTime) {
            val actualCaffeineLevel = CaffeineCalculator.calculateCurrentLevel(
                entries = entries,
                currentTimeMillis = pointTime,
                halfLifeMinutes = settings.effectiveHalfLifeMinutes,
            )
            val displayCaffeineLevel = toDisplayCaffeineLevel(
                actualCaffeineLevel = actualCaffeineLevel,
                hasEntries = hasEntries,
            )

            dataPoints += CaffeineDataPoint(
                timestampMillis = pointTime,
                caffeineLevel = displayCaffeineLevel,
                isHistorical = pointTime <= currentTime,
            )

            if (pointTime == endTime) break

            val nextPointTime = nextAdaptivePointTime(
                pointTime = pointTime,
                currentTime = currentTime,
                endTime = endTime,
            )
            if (nextPointTime <= pointTime) break
            pointTime = nextPointTime
        }

        return dataPoints
    }

    private fun nextAdaptivePointTime(
        pointTime: Long,
        currentTime: Long,
        endTime: Long,
    ): Long {
        val intervalMillis = samplingIntervalFor(pointTime, currentTime)
        val nextBoundary = nextSamplingBoundaryAfter(pointTime, currentTime)
        return minOf(pointTime + intervalMillis, nextBoundary, endTime)
    }

    private fun nextSamplingBoundaryAfter(
        pointTime: Long,
        currentTime: Long,
    ): Long {
        val boundaries = listOf(
            alignedResolutionBoundary(currentTime - THIRTY_DAYS_MILLIS),
            alignedResolutionBoundary(currentTime - SEVEN_DAYS_MILLIS),
            alignedResolutionBoundary(currentTime - RECENT_HISTORY_HOURS * ONE_HOUR_MILLIS),
        )

        return boundaries.filter { it > pointTime }.minOrNull() ?: Long.MAX_VALUE
    }

    private fun alignedResolutionBoundary(
        rawBoundaryTime: Long,
    ): Long {
        return roundDownToInterval(rawBoundaryTime, BASE_INTERVAL_MILLIS)
    }

    private fun samplingIntervalFor(
        timestampMillis: Long,
        currentTime: Long,
    ): Long {
        val futureOffsetMillis = timestampMillis - currentTime
        val ageMillis = currentTime - timestampMillis
        return when {
            futureOffsetMillis > THREE_DAYS_MILLIS -> THREE_HOURS_MILLIS
            futureOffsetMillis > ONE_DAY_MILLIS -> ONE_HOUR_MILLIS
            futureOffsetMillis > SIX_HOURS_MILLIS -> THIRTY_MINUTES_MILLIS
            ageMillis > THIRTY_DAYS_MILLIS -> SIX_HOURS_MILLIS
            ageMillis > SEVEN_DAYS_MILLIS -> THREE_HOURS_MILLIS
            ageMillis > ONE_DAY_MILLIS -> ONE_HOUR_MILLIS
            else -> BASE_INTERVAL_MILLIS
        }
    }

    private fun markPeakPoints(
        dataPoints: MutableList<CaffeineDataPoint>,
        entries: List<ConsumptionEntry>,
        startTime: Long,
        endTime: Long,
        halfLifeMinutes: Int,
    ) {
        // Find entries within our time range
        val relevantEntries = entries.filter { entry ->
            val peakTime = CaffeineCalculator.calculatePeakTime(
                entry = entry,
                halfLifeMinutes = halfLifeMinutes,
            )
            peakTime in startTime..endTime
        }

        // Mark the nearest data point to each peak
        relevantEntries.forEach { entry ->
            val peakTime = CaffeineCalculator.calculatePeakTime(
                entry = entry,
                halfLifeMinutes = halfLifeMinutes,
            )
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
        domainStartTime: Long,
        endTime: Long,
        currentTime: Long,
    ): List<ChartConsumptionMarker> {
        return entries
            .asSequence()
            .filter { it.startedAtMillis in domainStartTime..endTime }
            .sortedBy { it.startedAtMillis }
            .groupBy { entry ->
                val samplingBucket = samplingIntervalFor(entry.startedAtMillis, currentTime)
                roundDownToInterval(entry.startedAtMillis, samplingBucket)
            }
            .map { (bucketStartTime, groupedEntries) ->
                ChartConsumptionMarker(
                    xValue = timestampToDomainX(domainStartTime, bucketStartTime),
                    emojiLabel = groupedEntries.toEmojiLabel(),
                    timestampMillis = groupedEntries.first().startedAtMillis
                )
            }
            .sortedBy { it.xValue }
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
    val caffeineLevel: Double,  // Back to normal Double
    val isHistorical: Boolean,
    val isBedtime: Boolean = false,
    val isPeak: Boolean = false
)

data class ChartData(
    val dataPoints: List<CaffeineDataPoint>,
    val consumptionMarkers: List<ChartConsumptionMarker>,
    val thresholdLevel: Double,
    val bedtimeMillis: Long,
    val currentTimeMillis: Long,
    val domainStartMillis: Long,
)

data class ChartConsumptionMarker(
    val xValue: Double,
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
    val imageName: String,
    val loggedAtMillis: Long,
    val caffeineMg: Int,
    val durationMinutes: Int,
    val finishedAtMillis: Long,
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

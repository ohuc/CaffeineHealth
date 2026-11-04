package com.uc.caffeine.ui.components

import com.uc.caffeine.util.CaffeineDataPoint
import com.uc.caffeine.util.ChartConsumptionMarker
import com.uc.caffeine.util.ChartData
import com.uc.caffeine.util.ChartMarkerEntry
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineChartTest {

    @Test
    fun buildHomeChartDisplaySeries_keepsRegularSampleGridForLineSeries() {
        val series = buildHomeChartDisplaySeries(testChartData())

        assertEquals(listOf(0.0, 1.0, 2.0), series.xValues)
        assertEquals(listOf(40.0, 20.0, 0.0), series.yValues)
    }

    @Test
    fun interpolateSeriesLevel_isLinearBetweenSamplesAndClampsOutside() {
        val xs = listOf(0.0, 1.0, 3.0)
        val ys = listOf(40.0, 20.0, 0.0)

        assertEquals(30.0, interpolateSeriesLevel(xs, ys, 0.5), 1e-9)
        assertEquals(10.0, interpolateSeriesLevel(xs, ys, 2.0), 1e-9)
        assertEquals(20.0, interpolateSeriesLevel(xs, ys, 1.0), 1e-9)
        assertEquals(40.0, interpolateSeriesLevel(xs, ys, -5.0), 1e-9)
        assertEquals(0.0, interpolateSeriesLevel(xs, ys, 9.0), 1e-9)
        assertEquals(0.0, interpolateSeriesLevel(emptyList(), emptyList(), 1.0), 1e-9)
    }

    @Test
    fun snapScrubX_snapsToWholeMinutes() {
        // One domain unit is 15 minutes, so a minute is 1/15 of a unit.
        assertEquals(5.0 / 15.0, snapScrubX(0.33), 1e-9)
        assertEquals(2.0, snapScrubX(1.99), 1e-9)
        assertEquals(-11.0 / 15.0, snapScrubX(-0.71), 1e-9)
    }

    @Test
    fun crossedQuarterHour_ticksOncePerFifteenMinuteMark() {
        assertTrue(crossedQuarterHour(0.9, 1.1))
        assertTrue(crossedQuarterHour(1.1, 0.9))
        assertTrue(crossedQuarterHour(-0.1, 0.1))
        assertFalse(crossedQuarterHour(1.1, 1.9))
    }

    @Test
    fun visibleSeriesMax_usesOnlyTheWindowPlusInterpolatedEdges() {
        val xs = listOf(0.0, 10.0, 20.0, 30.0, 40.0)
        val ys = listOf(0.0, 900.0, 100.0, 50.0, 0.0)

        assertEquals(100.0, visibleSeriesMax(xs, ys, 20.0, 40.0), 1e-9)
        assertEquals(900.0, visibleSeriesMax(xs, ys, 5.0, 25.0), 1e-9)
        // Window between samples: the interpolated edge (x = 15 -> 500) is the max.
        assertEquals(500.0, visibleSeriesMax(xs, ys, 15.0, 18.0), 1e-9)
        // Reversed bounds (right-to-left layouts) work the same.
        assertEquals(100.0, visibleSeriesMax(xs, ys, 40.0, 20.0), 1e-9)
    }

    @Test
    fun chartAxisMaxFor_keepsFourStepsOfAtLeastOneHundred() {
        assertEquals(400.0, chartAxisMaxFor(0.0), 0.0)
        assertEquals(400.0, chartAxisMaxFor(300.0), 0.0)
        assertEquals(800.0, chartAxisMaxFor(350.0), 0.0)
        assertEquals(1200.0, chartAxisMaxFor(900.0), 0.0)
    }

    @Test
    fun chartXMapping_roundTripsInBothLayoutDirections() {
        listOf(true, false).forEach { isLtr ->
            val mapping = testMapping(isLtr)
            val scroll = 1234f
            val canvasX = mapping.xToCanvas(42.5, scroll)
            assertEquals(42.5, mapping.canvasToX(canvasX, scroll), 1e-4)
        }
    }

    @Test
    fun chartXMapping_scrollingForwardMovesContentLeft() {
        val mapping = testMapping(isLtr = true)
        assertTrue(mapping.xToCanvas(10.0, scroll = 100f) < mapping.xToCanvas(10.0, scroll = 0f))
    }

    @Test
    fun edgeAutoScrollVelocity_isZeroInsideAndEasesInTowardsEdges() {
        fun velocity(x: Float) = edgeAutoScrollVelocity(
            pointerX = x,
            layerLeft = 0f,
            layerRight = 1000f,
            edgeZonePx = 100f,
            maxSpeedPxPerSecond = 400f,
        )

        assertEquals(0f, velocity(500f), 0f)
        assertEquals(0f, velocity(900f), 0f)
        assertEquals(100f, velocity(950f), 1e-3f)   // half depth -> quarter speed
        assertEquals(400f, velocity(1000f), 1e-3f)
        assertEquals(400f, velocity(1200f), 1e-3f)  // past the edge stays at max
        assertEquals(-100f, velocity(50f), 1e-3f)
        assertEquals(-400f, velocity(-30f), 1e-3f)
    }

    @Test
    fun edgeAutoScrollVelocity_shrinksZoneOnNarrowPlots() {
        // A 200px plot only gets a 50px zone, so the centre never auto-scrolls.
        val velocity = edgeAutoScrollVelocity(100f, 0f, 200f, edgeZonePx = 100f, maxSpeedPxPerSecond = 1f)
        assertEquals(0f, velocity, 0f)
    }

    @Test
    fun crossedValue_detectsCrossingOnceInEitherDirection() {
        assertTrue(crossedValue(1.0, 3.0, 2.0))
        assertTrue(crossedValue(3.0, 1.0, 2.0))
        assertTrue(crossedValue(1.0, 2.0, 2.0))
        assertFalse(crossedValue(2.0, 3.0, 2.0)) // leaving the line after landing on it is not a second tick
        assertFalse(crossedValue(3.0, 4.0, 2.0))
    }

    @Test
    fun scrubDayLabel_namesRelativeDays() {
        val zone = ZoneId.of("Europe/London")
        fun millis(day: Int, hour: Int, minute: Int = 0) =
            LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
        val now = millis(day = 2, hour = 21, minute = 1) + 20_000L

        assertEquals(ScrubDayLabel.NOW, scrubDayLabel(millis(2, 21, 1), now, zone))
        assertEquals(ScrubDayLabel.TODAY, scrubDayLabel(millis(2, 21, 0), now, zone))
        assertEquals(ScrubDayLabel.TODAY, scrubDayLabel(millis(2, 23, 45), now, zone))
        assertEquals(ScrubDayLabel.TOMORROW, scrubDayLabel(millis(3, 0, 15), now, zone))
        assertEquals(ScrubDayLabel.YESTERDAY, scrubDayLabel(millis(1, 23, 0), now, zone))
        assertEquals(ScrubDayLabel.OTHER, scrubDayLabel(millis(5, 9, 0), now, zone))
    }

    private fun testMapping(isLtr: Boolean) = ChartXMapping(
        layerLeft = 40f,
        layerRight = 1040f,
        layerTop = 10f,
        layerBottom = 500f,
        startPadding = 12f,
        xSpacing = 10.4f,
        minX = -960.0,
        xStep = 1.0,
        isLtr = isLtr,
    )

    private fun testChartData(): ChartData {
        return ChartData(
            dataPoints = listOf(
                CaffeineDataPoint(timestampMillis = 0L, caffeineLevel = 40.0),
                CaffeineDataPoint(timestampMillis = FIFTEEN_MINUTES_MILLIS, caffeineLevel = 20.0),
                CaffeineDataPoint(timestampMillis = THIRTY_MINUTES_MILLIS, caffeineLevel = 0.0),
            ),
            consumptionMarkers = listOf(
                ChartConsumptionMarker(
                    xValue = 0.0,
                    yValue = 40.0,
                    entries = listOf(
                        ChartMarkerEntry(
                            entryId = 1,
                            drinkName = "Coffee",
                            emoji = "☕",
                            imageName = "",
                            caffeineMg = 90,
                        )
                    ),
                    timestampMillis = 0L,
                )
            ),
            thresholdLevel = 60.0,
            bedtimeMillis = THIRTY_MINUTES_MILLIS,
            currentTimeMillis = FIFTEEN_MINUTES_MILLIS,
            domainStartMillis = 0L,
        )
    }

    private companion object {
        const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000L
        const val THIRTY_MINUTES_MILLIS = 30 * 60 * 1000L
    }
}

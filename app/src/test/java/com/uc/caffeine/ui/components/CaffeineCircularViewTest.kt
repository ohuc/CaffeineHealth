package com.uc.caffeine.ui.components

import androidx.compose.ui.unit.dp
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.ui.viewmodel.CaffeineTrend
import com.uc.caffeine.ui.viewmodel.determineCaffeineTrend
import com.uc.caffeine.util.CaffeineCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineCircularViewTest {

    @Test
    fun waveSpeedForTrend_reversesDirectionWhenFalling() {
        val risingSpeed = waveSpeedForTrend(CaffeineTrend.RISING, speedMagnitude = 60.dp)
        val fallingSpeed = waveSpeedForTrend(CaffeineTrend.FALLING, speedMagnitude = 60.dp)
        val steadySpeed = waveSpeedForTrend(CaffeineTrend.STEADY, speedMagnitude = 60.dp)

        assertEquals(60.dp, risingSpeed)
        assertEquals((-60).dp, fallingSpeed)
        assertEquals(0.dp, steadySpeed)
        assertTrue(risingSpeed > 0.dp)
        assertTrue(fallingSpeed < 0.dp)
    }

    @Test
    fun wrapWaveOffset_wrapsNegativeOffsetsContinuouslyWhenReversing() {
        assertEquals(0.35f, wrapWaveOffset(0.35f), 0.0001f)
        assertEquals(0.25f, wrapWaveOffset(1.25f), 0.0001f)
        assertEquals(0.90f, wrapWaveOffset(-0.10f), 0.0001f)
        assertEquals(0.35f, wrapWaveOffset(0.35f - 1.0f), 0.0001f)
    }

    @Test
    fun determineCaffeineTrend_detectsFallingAtLowCaffeineLevelsLike7Mg() {
        // Reproduces issue #56: at ~7 mg with a 300-minute half-life, 5 minutes of decay drops
        // caffeine by ~0.08 mg (< 1.0 mg), which must still report FALLING rather than STEADY.
        val entry = ConsumptionEntry(
            id = 1,
            drinkName = "Espresso",
            caffeineMg = 63,
            emoji = "☕",
            quantity = 1.0,
            startedAtMillis = 0L,
            durationMinutes = 5,
            absorptionRate = 45,
        )
        val halfLifeMinutes = 300
        // ~15.8 hours after consumption, remaining caffeine is ~7 mg.
        val nowMillis = 950L * 60_000L
        val currentLevel = CaffeineCalculator.calculateCurrentLevel(
            entries = listOf(entry),
            currentTimeMillis = nowMillis,
            halfLifeMinutes = halfLifeMinutes,
        )
        val futureLevel = CaffeineCalculator.calculateCurrentLevel(
            entries = listOf(entry),
            currentTimeMillis = nowMillis + 5 * 60_000L,
            halfLifeMinutes = halfLifeMinutes,
        )

        assertTrue("Expected ~7 mg level, got $currentLevel", currentLevel in 6.5..7.5)
        assertEquals(
            CaffeineTrend.FALLING,
            determineCaffeineTrend(currentLevel = currentLevel, futureLevel = futureLevel),
        )
    }

    @Test
    fun determineCaffeineTrend_detectsRisingDuringAbsorptionAndSteadyNearZero() {
        assertEquals(
            CaffeineTrend.RISING,
            determineCaffeineTrend(currentLevel = 12.0, futureLevel = 20.5),
        )
        assertEquals(
            CaffeineTrend.STEADY,
            determineCaffeineTrend(currentLevel = 0.2, futureLevel = 0.19),
        )
        assertEquals(
            CaffeineTrend.STEADY,
            determineCaffeineTrend(currentLevel = 0.0, futureLevel = 0.0),
        )
    }
}

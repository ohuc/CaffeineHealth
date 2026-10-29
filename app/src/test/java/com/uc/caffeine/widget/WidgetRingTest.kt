package com.uc.caffeine.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRingTest {

    @Test
    fun ringProgress_fillsAgainstTheSleepThresholdAndClamps() {
        assertEquals(0.5f, ringProgress(currentMg = 50.0, thresholdMg = 100), 0.0001f)
        assertEquals(1f, ringProgress(currentMg = 250.0, thresholdMg = 100), 0.0001f)
        assertEquals(0f, ringProgress(currentMg = -3.0, thresholdMg = 100), 0.0001f)
    }

    @Test
    fun ringProgress_isEmptyWithoutAThreshold() {
        assertEquals(0f, ringProgress(currentMg = 80.0, thresholdMg = 0), 0.0001f)
    }
}

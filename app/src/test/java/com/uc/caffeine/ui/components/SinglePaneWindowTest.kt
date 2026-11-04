package com.uc.caffeine.ui.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class SinglePaneWindowTest {
    @Test fun verticalHingeUsesTheLargerSideInWindowCoordinates() {
        assertEquals(Rect(430f, 30f, 900f, 700f), largestUnobstructedPane(
            Rect(100f, 30f, 900f, 700f), listOf(Rect(410f, 0f, 430f, 720f)),
        ))
    }

    @Test fun tabletopFoldWithNoOcclusionStillSeparatesContent() {
        assertEquals(Rect(0f, 0f, 800f, 400f), largestUnobstructedPane(
            Rect(0f, 0f, 800f, 800f), listOf(Rect(0f, 400f, 800f, 400f)),
        ))
    }

    @Test fun hingeOutsideTheCurrentWindowDoesNotRestrictIt() {
        val window = Rect(0f, 0f, 360f, 720f)
        assertEquals(window, largestUnobstructedPane(window, listOf(Rect(400f, 0f, 420f, 720f))))
    }
}

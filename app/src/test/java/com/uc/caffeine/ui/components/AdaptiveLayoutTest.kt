package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test fun compactWindowsStackAddAboveTheToolbarWithOnePane() {
        val policy = windowLayoutPolicy(0)
        assertFalse(policy.wideToolbar)
        assertFalse(policy.twoPane)
    }

    @Test fun mediumWindowsPutAddBesideTheToolbarButKeepOnePane() {
        val policy = windowLayoutPolicy(MediumWidthDp)
        assertTrue(policy.wideToolbar)
        assertEquals(1, policy.paneCount)
        assertFalse(windowLayoutPolicy(MediumWidthDp - 1).wideToolbar)
    }

    @Test fun twoPanesStartAtTheExpandedBreakpointWithTheWideToolbar() {
        for (width in listOf(0, MediumWidthDp - 1, MediumWidthDp, ExpandedWidthDp - 1, ExpandedWidthDp,
            LargeWidthDp - 1, LargeWidthDp, 1600)) {
            val policy = windowLayoutPolicy(width)
            if (policy.twoPane) assertTrue("two panes keep Add beside the toolbar at $width", policy.wideToolbar)
        }
        assertFalse(windowLayoutPolicy(ExpandedWidthDp - 1).twoPane)
        assertTrue(windowLayoutPolicy(ExpandedWidthDp).twoPane)
    }

    @Test fun largeWindowsWidenTheSupportingPane() {
        val expanded = windowLayoutPolicy(ExpandedWidthDp)
        val large = windowLayoutPolicy(LargeWidthDp)
        assertEquals(2, large.paneCount)
        assertTrue(large.supportingPaneWidth > expanded.supportingPaneWidth)
        assertEquals(412.dp, large.supportingPaneWidth)
    }

    @Test fun tabletopHingeSplitsTheLayoutAboveAndBelowInWindowCoordinates() {
        val panes = tabletopPanes(Rect(80f, 40f, 880f, 1040f), listOf(Rect(0f, 520f, 1000f, 540f)))
        assertEquals(Rect(80f, 40f, 880f, 520f), panes?.top)
        assertEquals(Rect(80f, 540f, 880f, 1040f), panes?.bottom)
    }

    @Test fun zeroHeightFoldStillSplits() {
        val panes = tabletopPanes(Rect(0f, 0f, 800f, 800f), listOf(Rect(0f, 400f, 800f, 400f)))
        assertEquals(400f, panes?.top?.bottom)
        assertEquals(400f, panes?.bottom?.top)
    }

    @Test fun hingeOutsideOrNotAcrossTheLayoutDoesNotSplit() {
        val bounds = Rect(0f, 0f, 800f, 800f)
        assertNull(tabletopPanes(bounds, listOf(Rect(0f, 900f, 800f, 920f))))
        assertNull(tabletopPanes(bounds, listOf(Rect(200f, 400f, 600f, 410f))))
        assertNull(tabletopPanes(bounds, emptyList()))
        assertNull(tabletopPanes(Rect.Zero, listOf(Rect(0f, 400f, 800f, 400f))))
    }

    @Test fun panesKeepClearOfOnlyTheEdgesTheyTouch() {
        assertEquals(WindowInsetsSides.Top + WindowInsetsSides.Start, PanePlacement.Start.headerInsetSides)
        assertEquals(WindowInsetsSides.Top + WindowInsetsSides.End, PanePlacement.End.headerInsetSides)
        assertEquals(WindowInsetsSides.Horizontal, PanePlacement.Bottom.headerInsetSides)
        assertFalse(PanePlacement.Top.touchesBottom)
        assertTrue(PanePlacement.Bottom.touchesBottom)
    }
}

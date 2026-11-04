package com.uc.caffeine.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedButtonPackingTest {
    @Test
    fun `normal phone fits four short timing options together`() {
        assertEquals(listOf(0..3), packConnectedButtonRows(listOf(58, 76, 78, 80), 360, 2))
    }

    @Test
    fun `narrow supporting content gives full width to long theme label`() {
        assertEquals(listOf(0..0, 1..2), packConnectedButtonRows(listOf(172, 76, 78), 216, 2))
    }

    @Test
    fun `larger font repacks options instead of squeezing each label`() {
        val normal = packConnectedButtonRows(listOf(80, 90, 92), 320, 2)
        val largeFont = packConnectedButtonRows(listOf(140, 158, 164), 320, 2)
        assertEquals(listOf(0..2), normal)
        assertEquals(listOf(0..1, 2..2), largeFont)
    }

    @Test
    fun `translation longer than window receives standalone wrapping row`() {
        assertEquals(listOf(0..0, 1..2), packConnectedButtonRows(listOf(540, 110, 120), 320, 2))
    }

    @Test
    fun `wide window rejoins options and keeps their original order`() {
        val widths = listOf(90, 126, 130, 108, 124)
        val rows = packConnectedButtonRows(widths, 700, 2)
        assertEquals(listOf(0..4), rows)
        assertEquals(widths.indices.toList(), rows.flatMap { it.toList() })
    }

    @Test
    fun `every multi option row fits its widest preferred label`() {
        val widths = listOf(60, 130, 160, 70, 90, 410, 100)
        for (availableWidth in listOf(0, 160, 216, 320, 600, 840)) {
            val rows = packConnectedButtonRows(widths, availableWidth, 2)
            assertEquals(widths.indices.toList(), rows.flatMap { it.toList() })
            rows.filter { it.count() > 1 }.forEach { row ->
                val cellWidth = (availableWidth - 2 * (row.count() - 1)) / row.count()
                assertTrue(row.all { widths[it] <= cellWidth })
            }
        }
    }

    @Test
    fun `no options need no rows`() {
        assertTrue(packConnectedButtonRows(emptyList(), 320, 2).isEmpty())
    }

    @Test
    fun `unbounded width fits all options without arithmetic overflow`() {
        assertEquals(listOf(0..2), packConnectedButtonRows(listOf(80, 150, 220), Int.MAX_VALUE, 2))
        assertEquals(listOf(0..0, 1..1), packConnectedButtonRows(listOf(Int.MAX_VALUE, Int.MAX_VALUE), Int.MAX_VALUE, 2))
    }

    @Test
    fun `gap larger than window keeps each option reachable`() {
        assertEquals(listOf(0..0, 1..1), packConnectedButtonRows(listOf(48, 48), 96, Int.MAX_VALUE))
    }

    @Test
    fun positionsFollowRows() {
        assertEquals(
            listOf(0 to 2, 1 to 2, 0 to 1),
            connectedButtonPositions(listOf(0..1, 2..2), 3),
        )
    }

    @Test
    fun emptyHasNoPositions() {
        assertTrue(connectedButtonPositions(emptyList(), 0).isEmpty())
    }
}

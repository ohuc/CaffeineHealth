package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight

/**
 * Packs connected controls using their actual unwrapped content width, including button padding.
 * Each row fills the available width and gives every control the same width and height. An option
 * wider than the whole group gets a full row and can wrap without losing any of its label.
 *
 * Supply a single control for each index, using [ToggleButtonShapes] from [content]. The layout
 * supports intrinsic measurement, so it can also be used in Material ListItem supporting content.
 */
@Composable
fun AdaptiveConnectedButtonGroup(
    itemCount: Int,
    modifier: Modifier = Modifier,
    minimumHeight: Dp = 48.dp,
    singleButtonShapes: ToggleButtonShapes? = null,
    content: @Composable (index: Int, shapes: ToggleButtonShapes) -> Unit,
) {
    require(itemCount >= 0)
    val placement = remember(itemCount) { ConnectedButtonPlacement(itemCount) }
    var positions by remember(itemCount) { mutableStateOf(placement.positions) }
    // Until the first placement every button would be drawn with standalone shapes and then change
    // one frame later. Keep the group invisible for that frame; the lambda below reads `placed`
    // during drawing only, so it never triggers a recomposition of its own.
    var placed by remember(itemCount) { mutableStateOf(false) }
    val measurePolicy = remember(placement, minimumHeight) {
        ConnectedButtonMeasurePolicy(placement, minimumHeight)
    }
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (placed) 1f else 0f }
            .semantics { isTraversalGroup = true }
            .onPlaced {
                // Publish after placement, never during a real or intrinsic measurement. Shapes
                // do not affect cell size, so this converges after one shape-only recomposition.
                if (positions != placement.positions) positions = placement.positions
                placed = true
            },
        content = {
            repeat(itemCount) { index ->
                val position = positions[index]
                val shapes = if (position.count == 1 && singleButtonShapes != null) {
                    singleButtonShapes
                } else {
                    connectedButtonShapes(position.index, position.count)
                }
                Box(
                    modifier = Modifier.semantics { traversalIndex = index.toFloat() },
                    propagateMinConstraints = true,
                ) {
                    content(index, shapes)
                }
            }
        },
        measurePolicy = measurePolicy,
    )
}

private data class ConnectedButtonPosition(val index: Int, val count: Int)

private class ConnectedButtonPlacement(count: Int) {
    var positions = List(count) { ConnectedButtonPosition(0, 1) }
}

private class ConnectedButtonMeasurePolicy(
    private val placement: ConnectedButtonPlacement,
    private val minimumHeight: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val gap = ButtonGroupDefaults.ConnectedSpaceBetween.roundToPx()
        val preferredWidths = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else {
            constraints.constrainWidth(totalWidth(preferredWidths, gap))
        }
        val rows = packConnectedButtonRows(preferredWidths, width, gap)
        val rowHeights = rows.map { row ->
            val cellWidth = cellWidth(width, gap, row.count())
            row.maxOf { measurables[it].maxIntrinsicHeight(cellWidth) }.coerceAtLeast(minimumHeight.roundToPx())
        }
        placement.positions = connectedButtonPositions(rows, measurables.size)
            .map { (index, count) -> ConnectedButtonPosition(index, count) }
        val placeables = rows.mapIndexed { rowIndex, row ->
            val cellWidth = cellWidth(width, gap, row.count())
            row.map { item -> measurables[item].measure(Constraints.fixed(cellWidth, rowHeights[rowIndex])) }
        }
        val height = rowHeights.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        return layout(width, constraints.constrainHeight(height)) {
            var y = 0
            placeables.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + gap
                }
                y += rowHeights[rowIndex] + gap
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        totalWidth(measurables.map { it.maxIntrinsicWidth(height) }, ButtonGroupDefaults.ConnectedSpaceBetween.roundToPx())

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        intrinsicHeight(measurables, width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        intrinsicHeight(measurables, width)

    private fun IntrinsicMeasureScope.intrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int {
        val gap = ButtonGroupDefaults.ConnectedSpaceBetween.roundToPx()
        val rows = packConnectedButtonRows(measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }, width, gap)
        return rows.sumOf { row ->
            val cellWidth = cellWidth(width, gap, row.count())
            row.maxOf { measurables[it].maxIntrinsicHeight(cellWidth) }.coerceAtLeast(minimumHeight.roundToPx())
        } + gap * (rows.size - 1).coerceAtLeast(0)
    }
}

private fun cellWidth(width: Int, gap: Int, count: Int): Int =
    ((width - gap * (count - 1)) / count).coerceAtLeast(0)

private fun totalWidth(widths: List<Int>, gap: Int): Int =
    ((widths.maxOrNull()?.toLong() ?: 0L) * widths.size + gap.toLong() * (widths.size - 1).coerceAtLeast(0))
        .coerceAtMost(Constraints.Infinity.toLong()).toInt()

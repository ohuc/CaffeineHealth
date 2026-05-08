package com.uc.caffeine.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ShimmerDurationMillis = 1100L
private const val ShimmerStart = -1.5f
private const val ShimmerEnd = 1.5f
private const val ShimmerRange = ShimmerEnd - ShimmerStart

@Composable
fun Modifier.shimmerEffect(shape: Shape): Modifier {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    return this then ShimmerElement(
        shape = shape,
        baseColor = baseColor,
        highlightColor = highlightColor,
    )
}

private data class ShimmerElement(
    val shape: Shape,
    val baseColor: Color,
    val highlightColor: Color,
) : ModifierNodeElement<ShimmerNode>() {
    override fun create(): ShimmerNode = ShimmerNode(shape, baseColor, highlightColor)

    override fun update(node: ShimmerNode) {
        node.shape = shape
        node.baseColor = baseColor
        node.highlightColor = highlightColor
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmerEffect"
        properties["shape"] = shape
    }
}

private class ShimmerNode(
    var shape: Shape,
    var baseColor: Color,
    var highlightColor: Color,
) : Modifier.Node(), DrawModifierNode {

    private var progress: Float = ShimmerStart

    private var cachedOutline: Outline? = null
    private var cachedOutlineSize: Size = Size.Zero
    private var cachedOutlineLayoutDirection: LayoutDirection? = null
    private var cachedOutlineShape: Shape? = null

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            var startMillis = -1L
            while (isActive) {
                withFrameMillis { frameMillis ->
                    if (startMillis < 0L) startMillis = frameMillis
                    val elapsed = (frameMillis - startMillis) % ShimmerDurationMillis
                    val next = ShimmerStart +
                        (elapsed.toFloat() / ShimmerDurationMillis) * ShimmerRange
                    if (next != progress) {
                        progress = next
                        invalidateDraw()
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (size.width > 0f && size.height > 0f) {
            val outline = obtainOutline(this, layoutDirection, size)
            val brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(x = size.width * progress - size.width, y = 0f),
                end = Offset(x = size.width * progress, y = size.height),
            )
            drawOutline(outline = outline, brush = brush)
        }
        drawContent()
    }

    private fun obtainOutline(
        density: Density,
        layoutDirection: LayoutDirection,
        size: Size,
    ): Outline {
        val cached = cachedOutline
        if (
            cached != null &&
            cachedOutlineSize == size &&
            cachedOutlineLayoutDirection == layoutDirection &&
            cachedOutlineShape === shape
        ) {
            return cached
        }
        val newOutline = shape.createOutline(size, layoutDirection, density)
        cachedOutline = newOutline
        cachedOutlineSize = size
        cachedOutlineLayoutDirection = layoutDirection
        cachedOutlineShape = shape
        return newOutline
    }
}

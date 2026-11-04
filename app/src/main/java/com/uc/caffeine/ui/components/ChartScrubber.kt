package com.uc.caffeine.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Horizontal pixel geometry Vico used for the last drawn frame. Captured from a decoration so a
 * touch position can be mapped back to a chart x value (and the reverse) outside the draw pass.
 * The scroll value is passed in separately so the mapping stays valid while the chart scrolls.
 */
@Immutable
internal data class ChartXMapping(
    val layerLeft: Float,
    val layerRight: Float,
    val layerTop: Float,
    val layerBottom: Float,
    val startPadding: Float,
    val xSpacing: Float,
    val minX: Double,
    val xStep: Double,
    val isLtr: Boolean,
) {
    val layerWidth: Float get() = layerRight - layerLeft

    fun canvasToX(canvasX: Float, scroll: Float): Double {
        val offsetPx = if (isLtr) {
            canvasX - layerLeft - startPadding + scroll
        } else {
            layerRight - startPadding - scroll - canvasX
        }
        return minX + offsetPx / xSpacing * xStep
    }

    fun xToCanvas(x: Double, scroll: Float): Float {
        val offsetPx = startPadding + ((x - minX) / xStep).toFloat() * xSpacing
        return if (isLtr) layerLeft + offsetPx - scroll else layerRight - offsetPx - scroll
    }

    companion object {
        fun from(context: CartesianDrawingContext): ChartXMapping? = with(context) {
            if (layerDimensions.xSpacing == 0f || ranges.xStep == 0.0) return null
            ChartXMapping(
                layerLeft = layerBounds.left,
                layerRight = layerBounds.right,
                layerTop = layerBounds.top,
                layerBottom = layerBounds.bottom,
                startPadding = layerDimensions.startPadding,
                xSpacing = layerDimensions.xSpacing,
                minX = ranges.minX,
                xStep = ranges.xStep,
                isLtr = isLtr,
            )
        }
    }
}

/** The point the scrubber currently reads: chart x value and the displayed caffeine level there. */
@Immutable
internal data class ChartScrubPoint(
    val x: Double,
    val level: Double,
)

/**
 * Scrubber state for the home chart. [pointerX] is in chart-host canvas coordinates and changes
 * on every move, so only read it from derived state, layout or draw lambdas — never directly in
 * a large composable body. [isActive] only flips on start/end and is safe to read in composition.
 */
@Stable
internal class ChartScrubState {
    var isActive by mutableStateOf(false)
        private set
    var pointerX by mutableStateOf<Float?>(null)
        private set
    var mapping by mutableStateOf<ChartXMapping?>(null)
        private set

    /** Edge auto-scroll waits until the finger has moved, so a hold near an edge stays put. */
    var autoScrollArmed = false
        private set
    private var activationX = 0f

    fun start(x: Float) {
        activationX = x
        autoScrollArmed = false
        pointerX = x
        isActive = true
    }

    fun move(x: Float, armDistancePx: Float) {
        if (!autoScrollArmed && kotlin.math.abs(x - activationX) > armDistancePx) {
            autoScrollArmed = true
        }
        pointerX = x
    }

    fun end() {
        isActive = false
        pointerX = null
        autoScrollArmed = false
    }

    /** Scroll value of the last drawn frame (plain field: only read outside composition). */
    private var drawnScroll = 0f

    fun onFrameDrawn(newMapping: ChartXMapping, scroll: Float) {
        drawnScroll = scroll
        // Written from the draw pass: only touch the state when the geometry really changed.
        if (mapping != newMapping) mapping = newMapping
    }

    /** Whether chart x [x] was inside the plot in the last frame. True before the first frame. */
    fun isXOnScreen(x: Double): Boolean {
        val current = mapping ?: return true
        return current.xToCanvas(x, drawnScroll) in current.layerLeft..current.layerRight
    }
}

/** Domain units are 15 minutes; the readout snaps to whole clock minutes. */
private const val SCRUB_SNAP_STEPS_PER_UNIT = 15.0

internal fun snapScrubX(x: Double): Double =
    (x * SCRUB_SNAP_STEPS_PER_UNIT).roundToLong() / SCRUB_SNAP_STEPS_PER_UNIT

/**
 * Linear interpolation of the plotted series at [x]. Outside the sampled range the nearest end
 * value is used (the curve starts before the first drink and ends after the return to baseline).
 */
internal fun interpolateSeriesLevel(
    xValues: List<Double>,
    yValues: List<Double>,
    x: Double,
): Double {
    if (xValues.isEmpty() || xValues.size != yValues.size) return 0.0
    if (x <= xValues.first()) return yValues.first()
    if (x >= xValues.last()) return yValues.last()
    var low = 0
    var high = xValues.lastIndex
    while (high - low > 1) {
        val mid = (low + high) ushr 1
        if (xValues[mid] <= x) low = mid else high = mid
    }
    val x0 = xValues[low]
    val x1 = xValues[high]
    if (x1 <= x0) return yValues[low]
    val fraction = (x - x0) / (x1 - x0)
    return yValues[low] + (yValues[high] - yValues[low]) * fraction
}

/**
 * Edge auto-scroll speed in px/s for a scrub pointer at [pointerX] (positive reveals content to
 * the right). The speed eases in quadratically with how deep the finger is inside the edge zone,
 * so brushing the edge creeps and pushing into the corner moves faster.
 */
internal fun edgeAutoScrollVelocity(
    pointerX: Float,
    layerLeft: Float,
    layerRight: Float,
    edgeZonePx: Float,
    maxSpeedPxPerSecond: Float,
): Float {
    val width = layerRight - layerLeft
    if (width <= 0f || edgeZonePx <= 0f) return 0f
    val zone = min(edgeZonePx, width / 4f)
    val rightDepth = ((pointerX - (layerRight - zone)) / zone).coerceIn(0f, 1f)
    val leftDepth = (((layerLeft + zone) - pointerX) / zone).coerceIn(0f, 1f)
    return when {
        rightDepth > 0f -> maxSpeedPxPerSecond * rightDepth * rightDepth
        leftDepth > 0f -> -maxSpeedPxPerSecond * leftDepth * leftDepth
        else -> 0f
    }
}

/**
 * Hold-to-scrub. Quick swipes and taps are left alone (the chart scrolls as before); a press that
 * stays within touch slop for the long-press timeout activates the scrubber. From then on every
 * event is consumed in the Initial pass, before Vico's scroll handler and the parent list see it,
 * until the finger lifts. A second finger before activation cancels it (no accidental scrub).
 */
internal fun Modifier.longPressScrubGesture(
    onStart: (x: Float) -> Unit,
    onMove: (x: Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        var lastX = down.position.x
        val touchSlop = viewConfiguration.touchSlop

        val resolvedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                if (event.changes.size > 1 || !change.pressed || change.isConsumed) break
                if ((change.position - down.position).getDistance() > touchSlop) break
                lastX = change.position.x
            }
        }
        if (resolvedBeforeTimeout != null) return@awaitEachGesture

        try {
            onStart(lastX)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId }
                event.changes.forEach { it.consume() }
                if (change == null || !change.pressed) break
                onMove(change.position.x)
            }
        } finally {
            onEnd()
        }
    }
}

/**
 * Line fill that fades the curve out as it approaches 0 mg, so the tail glides into the axis
 * instead of running along the border as a flat line (before the first drink, after the return
 * to baseline, and on near-zero days). Fully opaque above [opaqueFraction] of the plot height,
 * fully transparent below [clearFraction]. Positions are computed from the live y range.
 */
internal class FadeToBaselineLineFill(
    private val color: Color,
    private val opaqueFraction: Float = 0.04f,
    private val clearFraction: Float = 0.006f,
) : com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer.LineFill {
    private val paint = androidx.compose.ui.graphics.Paint()
    private var cachedTop = Float.NaN
    private var cachedBottom = Float.NaN

    override fun draw(
        context: CartesianDrawingContext,
        halfLineThickness: Float,
        verticalAxisPosition: com.patrykandpatrick.vico.compose.cartesian.axis.Axis.Position.Vertical?,
    ) {
        with(context) {
            val bottom = layerBounds.bottom
            val height = layerBounds.height
            val opaqueY = bottom - height * opaqueFraction
            val clearY = bottom - height * clearFraction
            if (opaqueY != cachedTop || clearY != cachedBottom) {
                paint.shader = androidx.compose.ui.graphics.LinearGradientShader(
                    from = androidx.compose.ui.geometry.Offset(0f, opaqueY),
                    to = androidx.compose.ui.geometry.Offset(0f, clearY),
                    colors = listOf(color, color.copy(alpha = 0f)),
                )
                cachedTop = opaqueY
                cachedBottom = clearY
            }
            canvas.drawRect(
                layerBounds.left,
                layerBounds.top - halfLineThickness,
                layerBounds.right,
                bottom + halfLineThickness,
                paint,
            )
        }
    }
}

/**
 * Draws the scrub line and the dot on the curve, and publishes the frame geometry to
 * [ChartScrubState] every frame. Must be the last decoration so it sits above the markers.
 * [scrubPoint] is read during draw, so moving the scrubber only invalidates drawing.
 */
internal class ChartScrubDecoration(
    private val state: ChartScrubState,
    private val scrubPoint: () -> ChartScrubPoint?,
    private val lineColor: Color,
    private val dotColor: Color,
    private val haloColor: Color,
    private val dotRingColor: Color,
) : Decoration {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    override fun drawOverLayers(context: CartesianDrawingContext) {
        val mapping = ChartXMapping.from(context) ?: return
        state.onFrameDrawn(mapping, context.scroll)
        val point = scrubPoint() ?: return
        with(context) {
            // The 5-minute snap can land a fraction of a dp past the plot edge; keep it inside.
            val canvasX = mapping.clampToLayer(mapping.xToCanvas(point.x, scroll))
            // Read the y range Vico is drawing with, so the dot sits on the line even in the
            // frame where a new axis scale is being applied.
            val yMax = ranges.getYRange(null).maxY
            val fraction = if (yMax <= 0.0) 0f else (point.level / yMax).coerceIn(0.0, 1.0).toFloat()
            val canvasY = layerBounds.bottom - fraction * layerBounds.height
            val nativeCanvas = canvas.nativeCanvas

            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeCap = android.graphics.Paint.Cap.ROUND
            paint.strokeWidth = 2.dp.pixels
            paint.color = lineColor.toArgb()
            nativeCanvas.drawLine(canvasX, layerBounds.top, canvasX, layerBounds.bottom, paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = haloColor.toArgb()
            nativeCanvas.drawCircle(canvasX, canvasY, 12.dp.pixels, paint)
            paint.color = dotRingColor.toArgb()
            nativeCanvas.drawCircle(canvasX, canvasY, 7.dp.pixels, paint)
            paint.color = dotColor.toArgb()
            nativeCanvas.drawCircle(canvasX, canvasY, 5.dp.pixels, paint)
        }
    }
}

internal fun ChartXMapping.clampToLayer(canvasX: Float): Float = canvasX.coerceIn(layerLeft, layerRight)

/** True when [target] lies between two consecutive scrub positions (a detent was crossed). */
internal fun crossedValue(previous: Double, current: Double, target: Double): Boolean =
    (previous < target) != (current < target)

/** True when the scrubber moved across a whole domain unit (a 15-minute clock mark). */
internal fun crossedQuarterHour(previous: Double, current: Double): Boolean =
    kotlin.math.floor(previous) != kotlin.math.floor(current)

/** Highest plotted level in [fromX]..[toX], including the interpolated values at both ends. */
internal fun visibleSeriesMax(
    xValues: List<Double>,
    yValues: List<Double>,
    fromX: Double,
    toX: Double,
): Double {
    if (xValues.isEmpty() || xValues.size != yValues.size) return 0.0
    val start = min(fromX, toX)
    val end = kotlin.math.max(fromX, toX)
    var result = kotlin.math.max(
        interpolateSeriesLevel(xValues, yValues, start),
        interpolateSeriesLevel(xValues, yValues, end),
    )
    // First sample at or after `start` (binary search), then scan the window.
    var low = 0
    var high = xValues.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (xValues[mid] < start) low = mid + 1 else high = mid
    }
    var index = low
    while (index < xValues.size && xValues[index] <= end) {
        if (yValues[index] > result) result = yValues[index]
        index++
    }
    return result
}

/**
 * Y-axis top for a visible peak of [maxLevelMg]: steps of at least 100 mg, four steps tall, so
 * the curve keeps headroom under the counter and the drink icons.
 */
internal fun chartAxisMaxFor(maxLevelMg: Double): Double {
    val step = kotlin.math.max(100.0, kotlin.math.ceil(maxLevelMg / 3.0 / 100.0) * 100.0)
    return step * 4.0
}

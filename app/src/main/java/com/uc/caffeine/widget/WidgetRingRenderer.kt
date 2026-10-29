package com.uc.caffeine.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws the Home screen's circular view (#57) for the ring widget: a wavy progress arc for the
 * active level against the sleep threshold, and a flat track for the rest. RemoteViews can't draw,
 * so like [WidgetChartRenderer] each part is a white alpha mask the widget tints with a day/night
 * color. The two parts take different tints, hence two bitmaps.
 *
 * The geometry follows the in-app indicator's proportions (16dp stroke, 8dp gap, ~60dp wavelength
 * on a ~300dp ring) rather than its exact Material shapes, which a static image doesn't need.
 */
internal object WidgetRingRenderer {

    class RingBitmaps(val progress: Bitmap?, val track: Bitmap?)

    private const val STROKE_FRACTION = 0.055f
    private const val GAP_TO_STROKE = 0.5f
    private const val AMPLITUDE_TO_STROKE = 0.45f
    private const val WAVELENGTH_FRACTION = 0.2f
    private const val MIN_WAVES = 8

    /** Same cut-off as the in-app ring: below 1% of the threshold the arc is drawn flat. */
    private const val FLAT_BELOW_PROGRESS = 0.01f

    fun render(progress: Float, sizePx: Int): RingBitmaps {
        if (sizePx <= 0) return RingBitmaps(null, null)
        val p = progress.coerceIn(0f, 1f)
        val stroke = max(2f, sizePx * STROKE_FRACTION)
        val amplitude = if (p < FLAT_BELOW_PROGRESS) 0f else stroke * AMPLITUDE_TO_STROKE
        val center = sizePx / 2f
        // The crest of the wave and the round cap both have to stay inside the bitmap.
        val radius = center - stroke / 2f - stroke * AMPLITUDE_TO_STROKE
        val waves = max(MIN_WAVES, (2 * PI * radius / (sizePx * WAVELENGTH_FRACTION)).roundToInt())
        // Round caps reach half a stroke past each end, so the visible gap needs a stroke more.
        val gapDegrees = Math.toDegrees(((stroke * GAP_TO_STROKE + stroke) / radius).toDouble()).toFloat()
        val sweep = 360f * p

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val progressBitmap = if (sweep > 0f) {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { bitmap ->
                Canvas(bitmap).drawPath(wavyArc(center, radius, amplitude, waves, sweep), paint)
            }
        } else {
            null
        }

        // The track fills what the arc leaves, with a gap either side of the arc's caps.
        val trackSweep = if (sweep <= 0f) 360f else 360f - sweep - 2 * gapDegrees
        val trackBitmap = if (trackSweep > 0f) {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { bitmap ->
                val oval = RectF(center - radius, center - radius, center + radius, center + radius)
                val canvas = Canvas(bitmap)
                if (sweep <= 0f) {
                    canvas.drawOval(oval, paint)
                } else {
                    canvas.drawArc(oval, -90f + sweep + gapDegrees, trackSweep, false, paint)
                }
            }
        } else {
            null
        }

        return RingBitmaps(progressBitmap, trackBitmap)
    }

    /** A clockwise arc from 12 o'clock whose radius oscillates [waves] times per full turn. */
    private fun wavyArc(center: Float, radius: Float, amplitude: Float, waves: Int, sweepDegrees: Float): Path {
        val path = Path()
        // Two samples per degree keeps each segment well under a pixel on any widget size.
        val steps = max(2, (sweepDegrees * 2).roundToInt())
        for (i in 0..steps) {
            val turn = (sweepDegrees / 360f) * i / steps
            val angle = 2 * PI * turn - PI / 2
            val r = radius + amplitude * cos(2 * PI * waves * turn).toFloat()
            val x = center + r * cos(angle).toFloat()
            val y = center + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (sweepDegrees >= 360f) path.close()
        return path
    }
}

/** Fraction of the ring filled: the active level against the sleep threshold, as on Home. */
internal fun ringProgress(currentMg: Double, thresholdMg: Int): Float =
    if (thresholdMg > 0) (currentMg / thresholdMg).toFloat().coerceIn(0f, 1f) else 0f

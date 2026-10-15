package com.uc.caffeine.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.util.CaffeineCalculator

/**
 * Draws the widget's decay curve as a white alpha mask. The widget tints it with a day/night
 * color, so the curve follows the system theme without a re-render (a baked-in color would stay
 * light-on-dark until the next refresh after the theme flips).
 */
object WidgetChartRenderer {
    // 1-minute sampling. With ~720 points across a typical 600px-wide widget bitmap, each segment
    // is sub-pixel, so even simple linear/Bezier segments produce a perfectly smooth curve — the
    // staircasing on steep rises (visible at the original 5-minute sampling) collapses to noise
    // below the pixel grid.
    private const val INTERVAL_MS = 60 * 1000L

    /** The chart spans this long either side of now, so "now" is always the horizontal centre. */
    private const val HALF_WINDOW_MS = 6 * 60 * 60 * 1000L

    private const val FILL_TOP_ALPHA = 0x3D
    private const val LINE_ALPHA = 0xE0
    private const val BEDTIME_ALPHA = 0xB0

    fun render(
        entries: List<ConsumptionEntry>,
        settings: UserSettings,
        widthPx: Int,
        heightPx: Int,
        bedtimeMillis: Long? = null,
    ): Bitmap? {
        if (widthPx <= 0 || heightPx <= 0) return null

        val now = System.currentTimeMillis()
        val rangeStart = now - HALF_WINDOW_MS
        val rangeEnd = now + HALF_WINDOW_MS
        val totalMs = (HALF_WINDOW_MS * 2).toFloat()

        val timestamps = mutableListOf<Long>()
        val levels = mutableListOf<Double>()
        var t = rangeStart
        while (t <= rangeEnd) {
            timestamps.add(t)
            levels.add(
                CaffeineCalculator.calculateCurrentLevel(entries, t, settings.effectiveHalfLifeMinutes)
            )
            t += INTERVAL_MS
        }

        val maxLevel = levels.maxOrNull() ?: 0.0
        if (maxLevel < 1.0) return null

        val strokeWidth = (heightPx / 40f).coerceIn(3f, 7f)
        val padTop = strokeWidth * 2f
        fun timeToX(ts: Long): Float = ((ts - rangeStart).toFloat() / totalMs) * widthPx
        // The curve runs down to the bottom edge so the fill reads as part of the card, not a box.
        fun levelToY(level: Double): Float =
            heightPx - (level.toFloat() / maxLevel.toFloat()) * (heightPx - padTop)

        val points = timestamps.mapIndexed { i, ts -> PointF(timeToX(ts), levelToY(levels[i])) }

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val curvePath = buildSmoothPath(points)

        // Gradient area fill
        val minY = points.minOf { it.y }
        val fillPath = Path(curvePath).also { fp ->
            fp.lineTo(points.last().x, heightPx.toFloat())
            fp.lineTo(points.first().x, heightPx.toFloat())
            fp.close()
        }
        canvas.drawPath(fillPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, minY, 0f, heightPx.toFloat(),
                intArrayOf(white(FILL_TOP_ALPHA), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
        })

        // Dotted bedtime line. Snapped to the half-pixel grid with an integer stroke width —
        // DashPathEffect on fractional dash lengths pixel-snaps unevenly into jagged segments.
        if (bedtimeMillis != null && bedtimeMillis in rangeStart..rangeEnd) {
            val dotStroke = (strokeWidth * 0.6f).toInt().coerceAtLeast(2).toFloat()
            val bedtimeX = timeToX(bedtimeMillis).toInt() + 0.5f
            canvas.drawLine(
                bedtimeX, padTop, bedtimeX, heightPx.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = white(BEDTIME_ALPHA)
                    style = Paint.Style.STROKE
                    this.strokeWidth = dotStroke
                    strokeCap = Paint.Cap.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(0f, dotStroke * 2.5f), 0f)
                },
            )
        }

        // Curve line
        canvas.drawPath(curvePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white(LINE_ALPHA)
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })

        // Consumption markers: a solid dot with a knocked-out ring, so each drink reads as a bead
        // on the curve in a single tint instead of needing a second outline color.
        val markerRadius = strokeWidth * 1.15f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        entries.forEach { entry ->
            val ts = entry.startedAtMillis
            if (ts in rangeStart..rangeEnd) {
                val x = timeToX(ts)
                val level = CaffeineCalculator.calculateCurrentLevel(
                    entries, ts, settings.effectiveHalfLifeMinutes,
                )
                val y = levelToY(level).coerceIn(padTop, heightPx - markerRadius * 2f)
                canvas.drawCircle(x, y, markerRadius + strokeWidth * 0.8f, ringPaint)
                canvas.drawCircle(x, y, markerRadius, dotPaint)
            }
        }

        return bitmap
    }

    private fun white(alpha: Int) = Color.argb(alpha, 255, 255, 255)

    private fun buildSmoothPath(points: List<PointF>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val cpX = (prev.x + curr.x) / 2f
            path.cubicTo(cpX, prev.y, cpX, curr.y, curr.x, curr.y)
        }
        return path
    }
}

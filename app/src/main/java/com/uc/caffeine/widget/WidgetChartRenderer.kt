package com.uc.caffeine.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.util.CaffeineCalculator

object WidgetChartRenderer {
    // 1-minute sampling. With ~720 points across a typical 600px-wide widget bitmap, each segment
    // is sub-pixel, so even simple linear/Bezier segments produce a perfectly smooth curve — the
    // staircasing on steep rises (visible at the original 5-minute sampling) collapses to noise
    // below the pixel grid.
    private const val INTERVAL_MS = 60 * 1000L
    private const val BACK_MS = 6 * 60 * 60 * 1000L
    private const val AHEAD_MS = 6 * 60 * 60 * 1000L

    fun render(
        entries: List<ConsumptionEntry>,
        settings: UserSettings,
        widthPx: Int,
        heightPx: Int,
        lineColor: Int,
        fillTopColor: Int,
        bedtimeMillis: Long? = null,
        bedtimeLineColor: Int = lineColor,
        currentTimeColor: Int = Color.TRANSPARENT,
        markerColor: Int = lineColor,
        markerOutlineColor: Int = Color.TRANSPARENT,
    ): Bitmap? {
        if (widthPx <= 0 || heightPx <= 0) return null

        val now = System.currentTimeMillis()
        val rangeStart = now - BACK_MS
        val rangeEnd = now + AHEAD_MS
        val totalMs = (BACK_MS + AHEAD_MS).toFloat()

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

        val padV = heightPx * 0.08f
        fun timeToX(ts: Long): Float = ((ts - rangeStart).toFloat() / totalMs) * widthPx
        fun levelToY(level: Double): Float =
            heightPx - padV - (level.toFloat() / maxLevel.toFloat()) * (heightPx - 2 * padV)

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
                intArrayOf(fillTopColor, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
        })

        val refStrokeWidth = (heightPx / 80f).coerceAtLeast(2f)
        // Vertical guide lines look cleanest when snapped to the half-pixel grid
        // with an integer stroke width — DashPathEffect on fractional dash lengths
        // pixel-snaps unevenly, producing the jagged-segment artifact.
        val verticalStroke = refStrokeWidth.toInt().coerceAtLeast(2).toFloat()

        // Solid current-time line
        if (currentTimeColor != Color.TRANSPARENT) {
            val nowX = timeToX(now).toInt() + 0.5f
            canvas.drawLine(
                nowX, padV, nowX, heightPx - padV,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = currentTimeColor
                    style = Paint.Style.STROKE
                    strokeWidth = verticalStroke
                },
            )
        }

        // Dotted bedtime line
        if (bedtimeMillis != null && bedtimeMillis in rangeStart..rangeEnd) {
            val bedtimeX = timeToX(bedtimeMillis).toInt() + 0.5f
            canvas.drawLine(
                bedtimeX, padV, bedtimeX, heightPx - padV,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bedtimeLineColor
                    style = Paint.Style.STROKE
                    strokeWidth = verticalStroke
                    strokeCap = Paint.Cap.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(0f, verticalStroke * 2f), 0f)
                },
            )
        }

        // Curve line
        val strokeWidth = (heightPx / 50f).coerceAtLeast(2f)
        canvas.drawPath(curvePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })

        // Consumption markers — small subtle circle on the curve at each consumption start,
        // matching the in-app `rememberConsumptionMarker` (surfaceContainerHighest + outlineVariant).
        // Y is clamped to the chart interior so a marker on a flat baseline still reads.
        val markerRadius = (heightPx / 32f).coerceAtLeast(3f)
        val outlineWidth = (heightPx / 240f).coerceAtLeast(1f)
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = markerColor
            style = Paint.Style.FILL
        }
        val markerOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = markerOutlineColor
            style = Paint.Style.STROKE
            this.strokeWidth = outlineWidth
        }
        val yMin = padV + markerRadius * 1.4f
        val yMax = heightPx - padV - markerRadius * 1.4f
        entries.forEach { entry ->
            val ts = entry.startedAtMillis
            if (ts in rangeStart..rangeEnd) {
                val x = timeToX(ts)
                val level = CaffeineCalculator.calculateCurrentLevel(
                    entries, ts, settings.effectiveHalfLifeMinutes,
                )
                val y = levelToY(level).coerceIn(yMin, yMax)
                if (markerOutlineColor != Color.TRANSPARENT) {
                    canvas.drawCircle(x, y, markerRadius + outlineWidth / 2f, markerOutlinePaint)
                }
                canvas.drawCircle(x, y, markerRadius, markerPaint)
            }
        }

        return bitmap
    }

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

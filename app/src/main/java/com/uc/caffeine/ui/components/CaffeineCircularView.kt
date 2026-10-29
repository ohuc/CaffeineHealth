package com.uc.caffeine.ui.components

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import com.uc.caffeine.R
import com.uc.caffeine.ui.viewmodel.CaffeineTrend
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlinx.coroutines.isActive

// Progress here is caffeine relative to the sleep threshold and routinely sits at 100%,
// where the M3 default amplitude flattens the wave — hiding the squiggle exactly when
// there is the most caffeine to show. Keep the wave at full amplitude whenever there is
// meaningful caffeine, flattening only near zero.
private val CaffeineWaveAmplitude: (Float) -> Float = { progress ->
    if (progress < 0.01f) 0f else 1f
}

internal val DefaultCircularWaveSpeed = 60.dp

internal fun waveSpeedForTrend(
    trend: CaffeineTrend,
    speedMagnitude: Dp = DefaultCircularWaveSpeed,
): Dp = when (trend) {
    CaffeineTrend.RISING -> speedMagnitude
    CaffeineTrend.FALLING -> -speedMagnitude
    CaffeineTrend.STEADY -> 0.dp
}

internal fun wrapWaveOffset(rawOffset: Float): Float = rawOffset.mod(1f)

@Composable
fun CaffeineCircularView(
    currentMg: Double,
    maxMg: Double,
    trend: CaffeineTrend,
    modifier: Modifier = Modifier,
) {
    val targetProgress = if (maxMg > 0.0) (currentMg / maxMg).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "circular_view_progress",
    )
    val strokeWidthPx = with(LocalDensity.current) { 16.dp.toPx() }
    val progressProvider = remember { { animatedProgress.value } }
    val indicatorStroke = remember(strokeWidthPx) { Stroke(width = strokeWidthPx, cap = StrokeCap.Round) }
    val waveSpeed = waveSpeedForTrend(trend)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val indicatorSize = min(maxWidth * 0.88f, maxHeight * 0.78f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                DirectionalCircularWavyProgressIndicator(
                    progress = progressProvider,
                    modifier = Modifier.size(indicatorSize),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    stroke = indicatorStroke,
                    trackStroke = indicatorStroke,
                    gapSize = 8.dp,
                    amplitude = CaffeineWaveAmplitude,
                    wavelength = 60.dp,
                    waveSpeed = waveSpeed,
                )

                // Center: chip + number + unit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    RollingNumberText(
                        text = "%.0f".format(currentMg),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        labelPrefix = "circular_view",
                    )
                    Text(
                        text = stringResource(R.string.home_circular_mg_unit),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    AnimatedContent(
                        targetState = trend,
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                        label = "trend_chip",
                    ) { currentTrend ->
                        TrendChip(trend = currentTrend)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_circular_threshold_format, maxMg.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Determinate circular wavy progress indicator that supports signed [waveSpeed] values:
 * positive [waveSpeed] animates the wave in the rising direction, negative [waveSpeed]
 * reverses the wave to indicate falling levels (#56), and `0.dp` pauses the wave in place.
 *
 * Material 3's built-in `CircularWavyProgressIndicator` ignores negative `waveSpeed` (`waveSpeed > 0.dp`).
 */
@Composable
private fun DirectionalCircularWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    amplitude: (progress: Float) -> Float,
    wavelength: Dp,
    waveSpeed: Dp,
) {
    val circularShapes = remember { CircularShapes() }
    val progressDrawingCache = remember { CircularProgressDrawingCache() }
    val waveOffsetState = remember { mutableFloatStateOf(0f) }

    val currentProgress = progress().fastCoerceIn(0f, 1f)
    val targetAmplitude = amplitude(currentProgress).fastCoerceIn(0f, 1f)
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = if (targetAmplitude > 0f) {
            IncreasingAmplitudeAnimationSpec
        } else {
            DecreasingAmplitudeAnimationSpec
        },
        label = "circular_wave_amplitude",
    )

    val isWaveActive = targetAmplitude > 0f || animatedAmplitude > 0f

    LaunchedEffect(waveSpeed, wavelength, isWaveActive) {
        val speedMagnitudeDp = abs(waveSpeed.value).dp
        val direction = waveSpeed.value.sign
        if (!isWaveActive || speedMagnitudeDp <= 0.dp || wavelength <= 0.dp || direction == 0f) {
            return@LaunchedEffect
        }
        var lastFrameNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameNanos ->
                val deltaSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos
                val vertexCount =
                    circularShapes.currentVertexCount.intValue.coerceAtLeast(MinCircularVertexCount)
                val cycleDurationSeconds =
                    ((wavelength / speedMagnitudeDp) * vertexCount)
                        .coerceAtLeast(MinAnimationDuration / 1000f)
                val deltaOffset = (deltaSeconds / cycleDurationSeconds) * direction
                waveOffsetState.floatValue = wrapWaveOffset(waveOffsetState.floatValue + deltaOffset)
            }
        }
    }

    Spacer(
        modifier = modifier
            .drawWithCache {
                val currentGapPx = gapSize.toPx()
                val currentWavelengthPx = wavelength.toPx()
                val requiresMorph = animatedAmplitude > 0f && animatedAmplitude < 1f

                circularShapes.update(
                    size = size,
                    wavelength = currentWavelengthPx,
                    strokeWidth = stroke.width,
                    requiresMorph = requiresMorph,
                )

                onDrawWithContent {
                    val drawProgress = progress().fastCoerceIn(0f, 1f)
                    val drawAmplitude = animatedAmplitude.fastCoerceIn(0f, 1f)
                    progressDrawingCache.updatePaths(
                        size = size,
                        progressPathProvider = { amp, _, _, _, supportMotion, path ->
                            circularShapes.getProgressPath(
                                amplitude = amp,
                                path = path,
                                repeatPath = supportMotion,
                            )
                        },
                        trackPathProvider = { _, _, _, _, path ->
                            circularShapes.getTrackPath(path = path)
                        },
                        enableProgressMotion = true,
                        startProgress = 0f,
                        endProgress = drawProgress,
                        amplitude = drawAmplitude,
                        waveOffset = if (drawAmplitude > 0f) waveOffsetState.floatValue else 0f,
                        wavelength = currentWavelengthPx,
                        gapSize = currentGapPx,
                        stroke = stroke,
                        trackStroke = trackStroke,
                    )
                    drawCircularIndicator(
                        color = color,
                        trackColor = trackColor,
                        stroke = stroke,
                        trackStroke = trackStroke,
                        drawingCache = progressDrawingCache,
                    )
                }
            }
            .semantics(mergeDescendants = true) {
                val progressValue = progress()
                val clampedProgress = progressValue.coerceIn(0f, 1f)
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = if (clampedProgress.isNaN()) 0f else clampedProgress,
                    range = 0f..1f,
                )
            }
    )
}

private fun DrawScope.drawCircularIndicator(
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    drawingCache: CircularProgressDrawingCache,
) {
    if (trackColor != Color.Transparent && trackColor != Color.Unspecified) {
        drawPath(path = drawingCache.trackPathToDraw, color = trackColor, style = trackStroke)
    }
    if (color != Color.Transparent && color != Color.Unspecified) {
        drawPath(path = drawingCache.progressPathToDraw, color = color, style = stroke)
    }
}

private class CircularProgressDrawingCache {
    private var currentAmplitude = -1f
    private var currentWavelength = -1f
    private var currentSize: Size = Size.Unspecified

    private var currentStartProgress = 0f
    private var currentEndProgress = 0f
    private var currentIndicatorTrackGapSize = 0f
    private var currentWaveOffset = -1f
    private var currentStroke = Stroke()
    private var currentTrackStroke = currentStroke

    private var progressPathLength = 0f
    private var trackPathLength = 0f
    private var currentProgressMotionEnabled = false

    private val scaleMatrix = Matrix()
    private val transformMatrix = Matrix()

    val fullProgressPath: Path = Path()
    val fullTrackPath: Path = Path()
    val progressPathToDraw: Path = Path()
    val trackPathToDraw: Path = Path()
    val progressPathMeasure: PathMeasure = PathMeasure()
    val trackPathMeasure: PathMeasure = PathMeasure()
    var currentStrokeCapWidth = 0f

    fun updatePaths(
        size: Size,
        progressPathProvider:
            (
                amplitude: Float,
                wavelength: Float,
                strokeWidth: Float,
                size: Size,
                supportsMotion: Boolean,
                path: Path,
            ) -> Path,
        trackPathProvider:
            (
                amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path,
            ) -> Path?,
        enableProgressMotion: Boolean,
        @FloatRange(from = 0.0, to = 1.0) startProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) endProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke,
    ) {
        val pathsUpdated =
            updateFullPaths(
                size = size,
                progressPathProvider = progressPathProvider,
                trackPathProvider = trackPathProvider,
                enableProgressMotion = enableProgressMotion,
                amplitude = amplitude,
                wavelength = wavelength,
                gapSize = gapSize,
                stroke = stroke,
                trackStroke = trackStroke,
            )
        updateDrawPaths(
            forceUpdate = pathsUpdated,
            startProgress = startProgress,
            endProgress = endProgress,
            waveOffset = waveOffset,
        )
    }

    private fun updateFullPaths(
        size: Size,
        progressPathProvider:
            (
                amplitude: Float,
                wavelength: Float,
                strokeWidth: Float,
                size: Size,
                supportsMotion: Boolean,
                path: Path,
            ) -> Path,
        trackPathProvider:
            (
                amplitude: Float, wavelength: Float, strokeWidth: Float, size: Size, path: Path,
            ) -> Path?,
        enableProgressMotion: Boolean,
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0) gapSize: Float,
        stroke: Stroke,
        trackStroke: Stroke,
    ): Boolean {
        if (
            currentSize == size &&
                currentAmplitude == amplitude &&
                currentWavelength == wavelength &&
                currentStroke == stroke &&
                currentTrackStroke == trackStroke &&
                currentIndicatorTrackGapSize == gapSize &&
                currentProgressMotionEnabled == enableProgressMotion
        ) {
            return false
        }

        val height = size.height
        val width = size.width

        currentStrokeCapWidth =
            if (
                (stroke.cap == StrokeCap.Butt && trackStroke.cap == StrokeCap.Butt) ||
                    height > width
            ) {
                0f
            } else {
                max(stroke.width / 2, trackStroke.width / 2)
            }

        scaleMatrix.reset()
        scaleMatrix.apply { scale(x = width - stroke.width, y = height - stroke.width) }

        fullProgressPath.rewind()
        progressPathProvider(
            amplitude,
            wavelength,
            stroke.width,
            size,
            enableProgressMotion,
            fullProgressPath,
        )
        processPath(fullProgressPath, size, scaleMatrix)
        progressPathMeasure.setPath(path = fullProgressPath, forceClosed = true)
        progressPathLength =
            if (enableProgressMotion) {
                progressPathMeasure.length / 2
            } else {
                progressPathMeasure.length
            }

        fullTrackPath.rewind()
        val trackPathForAmplitude =
            trackPathProvider(amplitude, wavelength, stroke.width, size, fullTrackPath)
        if (trackPathForAmplitude != null) {
            processPath(fullTrackPath, size, scaleMatrix)
            trackPathMeasure.setPath(path = fullTrackPath, forceClosed = true)
            trackPathLength = trackPathMeasure.length
        } else {
            trackPathLength = 0f
        }

        currentSize = size
        currentAmplitude = amplitude
        currentWavelength = wavelength
        currentStroke = stroke
        currentTrackStroke = trackStroke
        currentIndicatorTrackGapSize = gapSize
        currentProgressMotionEnabled = enableProgressMotion

        return true
    }

    private fun processPath(path: Path, size: Size, scaleMatrix: Matrix) {
        path.transform(scaleMatrix)
        val progressPathBounds = path.getBounds()
        path.translate(size.center - progressPathBounds.center)
    }

    private fun updateDrawPaths(
        forceUpdate: Boolean,
        @FloatRange(from = 0.0, to = 1.0) startProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) endProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) waveOffset: Float,
    ) {
        require(currentSize != Size.Unspecified) {
            "updateDrawPaths was called before updateFullPaths"
        }
        if (
            !forceUpdate &&
                currentStartProgress == startProgress &&
                currentEndProgress == endProgress &&
                currentWaveOffset == waveOffset
        ) {
            return
        }

        trackPathToDraw.rewind()
        progressPathToDraw.rewind()

        val pStart = startProgress * progressPathLength
        val pStop = endProgress * progressPathLength

        val trackGapSize = min(pStop, currentIndicatorTrackGapSize)
        val horizontalInsets = min(pStop, currentStrokeCapWidth)
        val trackSpacing = horizontalInsets * 2 + trackGapSize

        if (currentProgressMotionEnabled) {
            val coercedWaveOffset = waveOffset.fastCoerceIn(0f, 1f)
            val startStopShift = coercedWaveOffset * progressPathLength

            progressPathMeasure.getSegment(
                startDistance = pStart + startStopShift,
                stopDistance = pStop + startStopShift,
                destination = progressPathToDraw,
            )

            val offsetAngle = coercedWaveOffset * 360 % 360
            if (offsetAngle != 0f) {
                val fullProgressBounds = fullProgressPath.getBounds()
                progressPathToDraw.translate(
                    Offset(-fullProgressBounds.center.x, -fullProgressBounds.center.y)
                )
                transformMatrix.reset()
                progressPathToDraw.transform(
                    transformMatrix.apply { rotateZ(degrees = -offsetAngle) }
                )
                progressPathToDraw.translate(
                    Offset(fullProgressBounds.center.x, fullProgressBounds.center.y)
                )
            }
        } else {
            progressPathMeasure.getSegment(
                startDistance = pStart,
                stopDistance = pStop,
                destination = progressPathToDraw,
            )
        }
        if (trackPathLength > 0) {
            val tStart = endProgress * trackPathLength + trackSpacing
            val tStop = trackPathLength - trackSpacing
            trackPathMeasure.getSegment(
                startDistance = tStart,
                stopDistance = tStop,
                destination = trackPathToDraw,
            )
        }

        currentStartProgress = startProgress
        currentEndProgress = endProgress
        currentWaveOffset = waveOffset
    }
}

private class CircularShapes {
    private var currentSize: Size? = null
    private var currentWavelength: Float = -1f
    private var trackPolygon: RoundedPolygon? = null
    private var activeIndicatorPolygon: RoundedPolygon? = null
    private var activeIndicatorMorph: Morph? = null
    val currentVertexCount = mutableIntStateOf(-1)

    fun update(
        size: Size,
        @FloatRange(from = 0.0, fromInclusive = false) wavelength: Float,
        @FloatRange(from = 0.0, fromInclusive = false) strokeWidth: Float,
        requiresMorph: Boolean,
    ) {
        require(wavelength > 0f) { "Wavelength should be greater than zero" }
        if (size == currentSize && wavelength == currentWavelength) {
            if (requiresMorph && activeIndicatorMorph == null) {
                activeIndicatorMorph = Morph(start = trackPolygon!!, end = activeIndicatorPolygon!!)
            }
            return
        }
        val r = size.minDimension / 2 - strokeWidth / 2
        val numVertices = max(MinCircularVertexCount, (2 * PI * r / wavelength).fastRoundToInt())

        if (numVertices != currentVertexCount.intValue) {
            trackPolygon = RoundedPolygon.circle(numVertices = numVertices).normalized()
            activeIndicatorPolygon =
                RoundedPolygon.star(
                        numVerticesPerRadius = numVertices,
                        innerRadius = 0.75f,
                        rounding = CornerRounding(radius = 0.35f, smoothing = 0.4f),
                        innerRounding = CornerRounding(radius = 0.5f),
                    )
                    .normalized()
            if (requiresMorph) {
                activeIndicatorMorph = Morph(start = trackPolygon!!, end = activeIndicatorPolygon!!)
            }
        }

        currentSize = size
        currentWavelength = wavelength
        currentVertexCount.intValue = numVertices
    }

    fun getTrackPath(path: Path): Path {
        trackPolygon?.toIndicatorPath(path = path)
        return path
    }

    fun getProgressPath(
        @FloatRange(from = 0.0, to = 1.0) amplitude: Float,
        path: Path,
        repeatPath: Boolean,
        rotationPivotX: Float = 0.5f,
        rotationPivotY: Float = 0.5f,
    ): Path {
        when {
            amplitude >= 1f && activeIndicatorPolygon != null -> {
                activeIndicatorPolygon!!.toIndicatorPath(path = path, repeatPath = repeatPath)
            }
            amplitude <= 0f || activeIndicatorMorph == null -> {
                trackPolygon?.toIndicatorPath(path = path, repeatPath = repeatPath)
            }
            else -> {
                activeIndicatorMorph!!.toIndicatorPath(
                    progress = amplitude,
                    path = path,
                    repeatPath = repeatPath,
                    rotationPivotX = rotationPivotX,
                    rotationPivotY = rotationPivotY,
                )
            }
        }
        return path
    }
}

private fun RoundedPolygon.toIndicatorPath(
    path: Path = Path(),
    startAngle: Int = 270,
    repeatPath: Boolean = false,
    closePath: Boolean = true,
): Path {
    pathFromCubics(
        path = path,
        startAngle = startAngle,
        repeatPath = repeatPath,
        closePath = closePath,
        cubics = cubics,
        rotationPivotX = centerX,
        rotationPivotY = centerY,
    )
    return path
}

private fun Morph.toIndicatorPath(
    progress: Float,
    path: Path = Path(),
    startAngle: Int = 270,
    repeatPath: Boolean = false,
    closePath: Boolean = true,
    rotationPivotX: Float = 0f,
    rotationPivotY: Float = 0f,
): Path {
    pathFromCubics(
        path = path,
        startAngle = startAngle,
        repeatPath = repeatPath,
        closePath = closePath,
        cubics = asCubics(progress),
        rotationPivotX = rotationPivotX,
        rotationPivotY = rotationPivotY,
    )
    return path
}

private fun pathFromCubics(
    path: Path,
    startAngle: Int,
    repeatPath: Boolean,
    closePath: Boolean,
    cubics: List<Cubic>,
    rotationPivotX: Float,
    rotationPivotY: Float,
) {
    var first = true
    var firstCubic: Cubic? = null
    path.rewind()
    cubics.fastForEach {
        if (first) {
            path.moveTo(it.anchor0X, it.anchor0Y)
            if (startAngle != 0) {
                firstCubic = it
            }
            first = false
        }
        path.cubicTo(
            it.control0X,
            it.control0Y,
            it.control1X,
            it.control1Y,
            it.anchor1X,
            it.anchor1Y,
        )
    }
    if (repeatPath) {
        var firstInRepeat = true
        cubics.fastForEach {
            if (firstInRepeat) {
                path.lineTo(it.anchor0X, it.anchor0Y)
                firstInRepeat = false
            }
            path.cubicTo(
                it.control0X,
                it.control0Y,
                it.control1X,
                it.control1Y,
                it.anchor1X,
                it.anchor1Y,
            )
        }
    }

    if (closePath) path.close()

    if (startAngle != 0 && firstCubic != null) {
        val angleToFirstCubic =
            ((atan2(
                y = cubics[0].anchor0Y - rotationPivotY,
                x = cubics[0].anchor0X - rotationPivotX,
            ) * 180.0) / PI).toFloat()
        path.transform(Matrix().apply { rotateZ(-angleToFirstCubic + startAngle) })
    }
}

private const val MinCircularVertexCount = 5
private const val MinAnimationDuration = 50

private val IncreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = 500,
        easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    )

private val DecreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = 500,
        easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f),
    )

@Composable
private fun TrendChip(trend: CaffeineTrend) {
    val isRising = trend == CaffeineTrend.RISING
    val chipColor = if (isRising) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (isRising) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = chipColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (trend) {
                    CaffeineTrend.RISING -> Icons.Rounded.ArrowUpward
                    CaffeineTrend.FALLING -> Icons.Rounded.ArrowDownward
                    CaffeineTrend.STEADY -> Icons.Rounded.Remove
                },
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = contentColor,
            )
            Text(
                text = stringResource(
                    when (trend) {
                        CaffeineTrend.RISING -> R.string.home_trend_rising
                        CaffeineTrend.FALLING -> R.string.home_trend_falling
                        CaffeineTrend.STEADY -> R.string.home_trend_steady
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

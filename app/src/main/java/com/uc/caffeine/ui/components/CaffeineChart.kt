package com.uc.caffeine.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontWeight
import com.uc.caffeine.data.AppDateFormat
import com.uc.caffeine.ui.theme.MontserratFamily
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.resolveAsTypeface
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.content.ContextCompat
import com.uc.caffeine.R
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.util.ChartData
import com.uc.caffeine.util.ChartDataGenerator
import com.uc.caffeine.util.ConsumptionContributionDetail
import com.uc.caffeine.util.chartTimeFormatter
import com.uc.caffeine.util.resolvedZoneId
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.launch

private const val POINTS_PER_2_HOURS = 8
private const val POINTS_PER_3_HOURS = 12
private const val POINTS_PER_4_HOURS = 16
private const val TIMELINE_BASE_INTERVAL_MINUTES = 15
private const val TIMELINE_AXIS_SPACING_UNITS = POINTS_PER_3_HOURS
private const val TIMELINE_SCROLL_BUFFER_DAYS = 10
private const val TIMELINE_SCROLL_BUFFER_UNITS =
    (TIMELINE_SCROLL_BUFFER_DAYS * 24 * 60) / TIMELINE_BASE_INTERVAL_MINUTES
private const val HOME_VISIBLE_WINDOW_HOURS = 24
private const val HOME_VISIBLE_WINDOW_UNITS =
    (HOME_VISIBLE_WINDOW_HOURS * 60) / TIMELINE_BASE_INTERVAL_MINUTES
private const val CONTRIBUTION_VISIBLE_WINDOW_HOURS = 12
private const val CONTRIBUTION_VISIBLE_WINDOW_UNITS =
    (CONTRIBUTION_VISIBLE_WINDOW_HOURS * 60) / TIMELINE_BASE_INTERVAL_MINUTES
private const val HOME_INITIAL_NOW_BIAS = 0.68f
private const val VERTICAL_AXIS_LABEL_COUNT = 5
private const val CURVE_SMOOTHING = 0.35f
private val BedtimeIconTopOffset = 56.dp
private val BedtimeIconSize = 12.dp
private val SleepReferenceColor = Color(0xFF6FC06B)

internal data class HomeChartDisplayPoint(
    val x: Double,
    val y: Double,
)

internal data class HomeChartDisplaySeries(
    val xValues: List<Double>,
    val yValues: List<Double>,
    val currentTimeX: Double,
)

internal fun buildHomeChartDisplaySeries(
    chartData: ChartData,
    liveNowMillis: Long,
): HomeChartDisplaySeries {
    val displayPoints = chartData.dataPoints.map { point ->
        HomeChartDisplayPoint(
            x = ChartDataGenerator.timestampToDomainX(
                domainStartMillis = chartData.domainStartMillis,
                targetTimestampMillis = point.timestampMillis,
            ),
            y = point.caffeineLevel,
        )
    }
    val currentTimeX = ChartDataGenerator.timestampToDomainX(
        domainStartMillis = chartData.domainStartMillis,
        targetTimestampMillis = liveNowMillis,
    )
    return HomeChartDisplaySeries(
        xValues = displayPoints.map(HomeChartDisplayPoint::x),
        yValues = displayPoints.map(HomeChartDisplayPoint::y),
        currentTimeX = currentTimeX,
    )
}

private fun CartesianDrawingContext.xToCanvas(xValue: Double): Float {
    val fullRangeStart = ranges.minX - layerDimensions.startPadding / layerDimensions.xSpacing * ranges.xStep
    val offsetPx = ((xValue - fullRangeStart) / ranges.xStep).toFloat() * layerDimensions.xSpacing
    val unscrolledCanvasX = if (isLtr) layerBounds.left + offsetPx else layerBounds.right - offsetPx
    return unscrolledCanvasX - scroll
}

@Composable
fun CaffeineChart(
    chartData: ChartData,
    modelProducer: CartesianChartModelProducer,
    userSettings: UserSettings,
    liveNowMillis: Long,
    currentCaffeineLevel: Double,
    predictedBedtimeCaffeineLevel: Double,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val displaySeries = remember(chartData, liveNowMillis) {
        buildHomeChartDisplaySeries(
            chartData = chartData,
            liveNowMillis = liveNowMillis,
        )
    }
    val maxCaffeine = remember(displaySeries.yValues) {
        displaySeries.yValues.maxOrNull() ?: 0.0
    }

    val yAxisStep = remember(maxCaffeine) {
        maxOf(100.0, kotlin.math.ceil(maxCaffeine / 3.0 / 100.0) * 100.0)
    }
    
    val yAxisMax = remember(yAxisStep) {
        yAxisStep * 4.0
    }

    val dataMinX = remember(displaySeries.xValues) {
        displaySeries.xValues.firstOrNull() ?: 0.0
    }
    val dataMaxX = remember(displaySeries.xValues) {
        displaySeries.xValues.lastOrNull() ?: 0.0
    }
    val bufferedMinX = remember(dataMinX) {
        dataMinX - TIMELINE_SCROLL_BUFFER_UNITS
    }
    val bufferedMaxX = remember(dataMaxX) {
        dataMaxX + TIMELINE_SCROLL_BUFFER_UNITS
    }

    val rangeProvider = remember(bufferedMinX, bufferedMaxX, yAxisMax) {
        CartesianLayerRangeProvider.fixed(
            minX = bufferedMinX,
            maxX = bufferedMaxX,
            minY = 0.0,
            maxY = yAxisMax
        )
    }

    // ── Sync model data with chart configuration ──────────────────────────
    // The model producer must always use the SAME domainStartMillis that
    // the rangeProvider and axis formatters use.  Updating the producer
    // here (instead of in the ViewModel collector) guarantees that the
    // line-series x-values and the axis x-range are never from different
    // snapshots of chartData, which previously caused one-frame visual
    // glitches ("broken" look) whenever domainStartMillis shifted.
    var isModelReady by remember { mutableStateOf(false) }
    LaunchedEffect(displaySeries.xValues, displaySeries.yValues) {
        if (displaySeries.xValues.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(displaySeries.xValues, displaySeries.yValues)
                }
            }
        }
        isModelReady = true
    }

    val yAxisItemPlacer = remember(yAxisStep) {
        VerticalAxis.ItemPlacer.step(step = { yAxisStep })
    }

    val bottomAxisFormatter = rememberTimelineAxisValueFormatter(
        domainStartMillis = chartData.domainStartMillis,
        userSettings = userSettings,
    )
    val bottomAxisOffset = rememberTimelineAxisOffset(
        domainStartMillis = chartData.domainStartMillis,
        spacing = TIMELINE_AXIS_SPACING_UNITS,
        userSettings = userSettings,
    )
    val yAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            if (value.roundToInt() == 0) "\u200B" else "${value.roundToInt()}"
        }
    }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = MontserratFamily,
        color = colorScheme.onSurfaceVariant,
    )
    val labelFontRefreshKey = rememberFontRefreshKey(labelStyle)
    val labelComponent = key(labelFontRefreshKey) {
        rememberTextComponent(style = labelStyle)
    }

    val defaultLineColor = vicoTheme.lineCartesianLayerColors.firstOrNull() ?: colorScheme.primary
    val lineColor = remember(maxCaffeine, defaultLineColor) {
        if (maxCaffeine <= 0.0) Color.Transparent else defaultLineColor
    }
    val lineFill = remember(lineColor) {
        LineCartesianLayer.LineFill.single(Fill(lineColor))
    }
    val areaFill = remember(lineColor) {
        val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.45f), androidx.compose.ui.graphics.Color.Transparent)
        )
        LineCartesianLayer.AreaFill.single(com.patrykandpatrick.vico.compose.common.Fill(brush))
    }
    val line = remember(lineFill, areaFill) {
        LineCartesianLayer.Line(
            fill = lineFill,
            areaFill = areaFill,
            pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = CURVE_SMOOTHING)
        )
    }
    val lineProvider = remember(line) {
        LineCartesianLayer.LineProvider.series(line)
    }
    val thresholdDecoration = rememberThresholdLineDecoration(
        thresholdLevel = chartData.thresholdLevel,
        label = "Sleep threshold"
    )
    val currentTimeX = remember(displaySeries.currentTimeX) {
        displaySeries.currentTimeX
    }
    val currentTimeDecoration = rememberVerticalReferenceLineDecoration(
        xValue = currentTimeX,
        lineColor = colorScheme.onSurface.copy(alpha = 0.48f),
        labelColor = colorScheme.onSurfaceVariant,
        dashed = false,
        labelAtTop = false,
    )
    val bedtimeDecorations = rememberDailyBedtimeDecorations(
        domainStartMillis = chartData.domainStartMillis,
        minX = bufferedMinX,
        maxX = bufferedMaxX,
        userSettings = userSettings,
        lineColor = colorScheme.primary.copy(alpha = 0.8f),
        labelColor = colorScheme.primary,
        icon = R.drawable.ic_moon,
        dashed = true,
        labelAtTop = true,
        iconTopOffset = BedtimeIconTopOffset,
        iconSize = BedtimeIconSize,
        iconOnRightSide = true,
    )

    val consumptionMarkers = remember(chartData.consumptionMarkers) {
        chartData.consumptionMarkers.map { marker -> marker.xValue to marker.emojiLabel }
    }
    val markerPairs = consumptionMarkers.map { (x, label) ->
        x to rememberConsumptionMarker(label)
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = lineProvider,
            rangeProvider = rangeProvider
        ),
        startAxis = VerticalAxis.rememberStart(
            itemPlacer = yAxisItemPlacer,
            valueFormatter = yAxisFormatter,
            label = labelComponent,
            guideline = null
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = bottomAxisFormatter,
            label = labelComponent,
            itemPlacer = remember(bottomAxisOffset) {
                HorizontalAxis.ItemPlacer.aligned(
                    spacing = { TIMELINE_AXIS_SPACING_UNITS },
                    offset = { bottomAxisOffset },
                    addExtremeLabelPadding = true,
                )
            },
            guideline = null
        ),
        decorations =
            listOfNotNull(
                thresholdDecoration,
                currentTimeDecoration,
            ) + bedtimeDecorations,
        persistentMarkers = {
            markerPairs.forEach { (x, marker) ->
                marker at x
            }
        }
    )

    val currentTimeXState = rememberUpdatedState(currentTimeX)
    val zoneId = remember(userSettings.timeZoneId) {
        userSettings.resolvedZoneId()
    }
    val timelineAnchorScrollValue = rememberSaveable { mutableFloatStateOf(Float.NaN) }
    val timelineAnchorXSpacing = rememberSaveable { mutableFloatStateOf(Float.NaN) }
    val timelineAnchorXStep = rememberSaveable { mutableFloatStateOf(Float.NaN) }
    val timelineAnchorScroll = remember {
        Scroll.Absolute { context, layerDimensions, bounds, maxValue ->
            val anchorScrollValue = Scroll.Absolute.x(
                x = currentTimeXState.value,
                // Keep "now" slightly right of center so both swipe directions have room immediately.
                bias = HOME_INITIAL_NOW_BIAS,
            ).getValue(context, layerDimensions, bounds, maxValue)
            if (timelineAnchorScrollValue.floatValue != anchorScrollValue) {
                timelineAnchorScrollValue.floatValue = anchorScrollValue
            }
            if (timelineAnchorXSpacing.floatValue != layerDimensions.xSpacing) {
                timelineAnchorXSpacing.floatValue = layerDimensions.xSpacing
            }
            val xStep = context.ranges.xStep.toFloat()
            if (timelineAnchorXStep.floatValue != xStep) {
                timelineAnchorXStep.floatValue = xStep
            }
            anchorScrollValue
        }
    }
    val homeViewportZoom = remember {
        Zoom.x(HOME_VISIBLE_WINDOW_UNITS.toDouble())
    }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = timelineAnchorScroll,
        autoScroll = timelineAnchorScroll,
        autoScrollCondition = AutoScrollCondition.OnModelGrowth,
    )
    val zoomState = rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = homeViewportZoom,
        minZoom = homeViewportZoom,
        maxZoom = homeViewportZoom,
    )
    val scrollDeltaFromToday by remember {
        derivedStateOf {
            val anchorValue = timelineAnchorScrollValue.floatValue
            if (anchorValue.isNaN()) 0f else scrollState.value - anchorValue
        }
    }
    val currentDay = remember(liveNowMillis, zoneId) {
        Instant.ofEpochMilli(liveNowMillis).atZone(zoneId).toLocalDate()
    }
    val dayOffsetFromToday by remember(currentDay, liveNowMillis, zoneId) {
        derivedStateOf {
            val xSpacing = timelineAnchorXSpacing.floatValue
            val xStep = timelineAnchorXStep.floatValue
            if (xSpacing.isNaN() || xSpacing == 0f || xStep.isNaN() || xStep == 0f) {
                0L
            } else {
                val deltaUnits = scrollDeltaFromToday / xSpacing * xStep
                val viewedTimestamp = liveNowMillis +
                    (deltaUnits.toDouble() * TIMELINE_BASE_INTERVAL_MINUTES * 60_000.0).roundToLong()
                ChronoUnit.DAYS.between(
                    currentDay,
                    Instant.ofEpochMilli(viewedTimestamp).atZone(zoneId).toLocalDate(),
                )
            }
        }
    }
    val showLeftReturnButton by remember {
        derivedStateOf { dayOffsetFromToday > 0 }
    }
    val showRightReturnButton by remember {
        derivedStateOf { dayOffsetFromToday < 0 }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isModelReady) {
            CartesianChartHost(
                chart = chart,
                modelProducer = modelProducer,
                scrollState = scrollState,
                zoomState = zoomState,
                animationSpec = null,
                animateIn = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        CaffeineChartStatus(
            currentCaffeineLevel = currentCaffeineLevel,
            predictedBedtimeCaffeineLevel = predictedBedtimeCaffeineLevel,
            sleepThresholdMg = userSettings.sleepThresholdMg.toDouble(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )

        AnimatedVisibility(
            visible = showLeftReturnButton,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = (-24).dp)
                .padding(start = 2.dp),
        ) {
            TimelineJumpButton(
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateScroll(timelineAnchorScroll)
                    }
                },
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Return to current day",
            )
        }

        AnimatedVisibility(
            visible = showRightReturnButton,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-24).dp)
                .padding(end = 2.dp),
        ) {
            TimelineJumpButton(
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateScroll(timelineAnchorScroll)
                    }
                },
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Return to current day",
            )
        }
    }
}

@Composable
private fun CaffeineChartStatus(
    currentCaffeineLevel: Double,
    predictedBedtimeCaffeineLevel: Double,
    sleepThresholdMg: Double,
    modifier: Modifier = Modifier,
) {
    val sleepShouldBeUnaffected = predictedBedtimeCaffeineLevel <= sleepThresholdMg

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        RollingCaffeineCounter(
            value = currentCaffeineLevel,
            modifier = Modifier.align(Alignment.End),
        )
        Text(
            text = if (sleepShouldBeUnaffected) {
                "Sleep should be unaffected"
            } else {
                "Sleep may be affected"
            },
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MontserratFamily),
            color = if (sleepShouldBeUnaffected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.error
            },
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TimelineJumpButton(
    onClick: () -> Unit,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun RollingCaffeineCounter(
    value: Double,
    modifier: Modifier = Modifier,
) {
    val formattedString = String.format(Locale.US, "%.1f", value)

    Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        formattedString.forEachIndexed { index, char ->
            if (char.isDigit()) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "chart_digit_roll_$index",
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MontserratFamily,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Text(
                    text = char.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFamily,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "mg",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MontserratFamily,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}


@Composable
fun ConsumptionContributionChart(
    detail: ConsumptionContributionDetail,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val modelProducer = remember { CartesianChartModelProducer() }
    val timestamps = remember(detail.dataPoints) {
        detail.dataPoints.map { it.timestampMillis }
    }
    val xValues = remember(detail.dataPoints) {
        detail.dataPoints.indices.map(Int::toDouble)
    }
    val yValues = remember(detail.dataPoints) {
        detail.dataPoints.map { it.caffeineContributionMg }
    }
    val contributionVisibleUnits = remember(xValues) {
        val span = ((xValues.lastOrNull() ?: 0.0) - (xValues.firstOrNull() ?: 0.0))
            .coerceAtLeast(0.0)
        max(CONTRIBUTION_VISIBLE_WINDOW_UNITS.toDouble(), span + 1.0)
    }
    val contributionAxisSpacing = remember(contributionVisibleUnits) {
        when {
            contributionVisibleUnits > (24 * 60 / TIMELINE_BASE_INTERVAL_MINUTES) -> POINTS_PER_4_HOURS
            contributionVisibleUnits > CONTRIBUTION_VISIBLE_WINDOW_UNITS -> POINTS_PER_3_HOURS
            else -> POINTS_PER_2_HOURS
        }
    }
    
    val maxContribution = remember(detail.dataPoints) {
        detail.dataPoints.maxOfOrNull { it.caffeineContributionMg } ?: 0.0
    }
    
    val yAxisStep = remember(maxContribution) {
        maxOf(25.0, kotlin.math.ceil(maxContribution / 4.0 / 25.0) * 25.0)
    }

    val yAxisMax = remember(yAxisStep) {
        yAxisStep * 4.0
    }
    
    val contributionMinX = remember(xValues) {
        xValues.firstOrNull() ?: 0.0
    }
    val contributionMaxX = remember(xValues) {
        xValues.lastOrNull() ?: 0.0
    }

    val rangeProvider = remember(contributionMinX, contributionMaxX, yAxisMax) {
        CartesianLayerRangeProvider.fixed(
            minX = contributionMinX - TIMELINE_SCROLL_BUFFER_UNITS,
            maxX = contributionMaxX + TIMELINE_SCROLL_BUFFER_UNITS,
            minY = 0.0,
            maxY = yAxisMax
        )
    }

    val bottomAxisFormatter = rememberClockValueFormatter(
        domainStartMillis = timestamps.firstOrNull() ?: detail.currentTimeMillis,
        userSettings = userSettings,
    )
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = MontserratFamily,
        color = colorScheme.onSurfaceVariant,
    )
    val labelFontRefreshKey = rememberFontRefreshKey(labelStyle)
    val labelComponent = key(labelFontRefreshKey) {
        rememberTextComponent(style = labelStyle)
    }

    val lineColor = vicoTheme.lineCartesianLayerColors.firstOrNull() ?: colorScheme.primary
    val lineFill = remember(lineColor) {
        LineCartesianLayer.LineFill.single(Fill(lineColor))
    }
    val areaFill = remember(lineColor) {
        val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.45f), androidx.compose.ui.graphics.Color.Transparent)
        )
        LineCartesianLayer.AreaFill.single(com.patrykandpatrick.vico.compose.common.Fill(brush))
    }
    val line = remember(lineFill, areaFill) {
        LineCartesianLayer.Line(
            fill = lineFill,
            areaFill = areaFill,
            pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = CURVE_SMOOTHING)
        )
    }
    val lineProvider = remember(line) {
        LineCartesianLayer.LineProvider.series(line)
    }
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = lineProvider,
            rangeProvider = rangeProvider
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = bottomAxisFormatter,
            label = labelComponent,
            itemPlacer = remember(contributionAxisSpacing) {
                HorizontalAxis.ItemPlacer.aligned(
                    spacing = { contributionAxisSpacing },
                    addExtremeLabelPadding = true // <-- ADD THIS
                )
            },
            guideline = null
        ),
    )

    RenderContributionChart(
        chart = chart,
        modelProducer = modelProducer,
        xValues = xValues,
        yValues = yValues,
        visibleUnits = contributionVisibleUnits,
        modifier = modifier
    )
}

@Composable
private fun RenderContributionChart(
    chart: com.patrykandpatrick.vico.compose.cartesian.CartesianChart,
    modelProducer: CartesianChartModelProducer,
    xValues: List<Double>,
    yValues: List<Double>,
    visibleUnits: Double,
    modifier: Modifier = Modifier
) {
    val latestDataXState = rememberUpdatedState(xValues.lastOrNull() ?: 0.0)
    val latestDataScroll = remember {
        Scroll.Absolute { context, layerDimensions, bounds, maxValue ->
            Scroll.Absolute.x(
                x = latestDataXState.value,
                bias = 1f,
            ).getValue(context, layerDimensions, bounds, maxValue)
        }
    }
    val contributionZoom = remember(visibleUnits) {
        Zoom.x(visibleUnits)
    }
    val scrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = latestDataScroll,
        autoScroll = latestDataScroll,
        autoScrollCondition = AutoScrollCondition.OnModelGrowth,
    )
    val zoomState = rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = contributionZoom,
        minZoom = contributionZoom,
        maxZoom = contributionZoom,
    )

    LaunchedEffect(xValues, yValues) {
        modelProducer.runTransaction {
            lineSeries {
                series(xValues, yValues)
            }
        }
    }

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        scrollState = scrollState,
        zoomState = zoomState,
        animationSpec = null,
        animateIn = false,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}


@Composable
private fun EmptyChartState(
    modifier: Modifier,
    message: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun rememberClockValueFormatter(
    domainStartMillis: Long,
    userSettings: UserSettings
): CartesianValueFormatter {
    val locale = remember { Locale.getDefault() }
    val zoneId = remember(userSettings.timeZoneId) {
        userSettings.resolvedZoneId()
    }
    val timeFormatter = remember(userSettings.use24HourClock, locale) {
        userSettings.chartTimeFormatter(locale)
    }
    val dateFormatter = remember(userSettings.dateFormat, locale) {
        compactTimelineDateFormatter(userSettings.dateFormat, locale)
    }

    return remember(domainStartMillis, zoneId, timeFormatter, dateFormatter) {
        CartesianValueFormatter { _, value, _ ->
            val timestampMillis = ChartDataGenerator.domainXToTimestamp(
                domainStartMillis = domainStartMillis,
                xValue = value,
            )
            val zonedDateTime = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)

            if (zonedDateTime.hour == 0 && zonedDateTime.minute == 0) {
                zonedDateTime.format(dateFormatter)
            } else {
                zonedDateTime.format(timeFormatter)
            }
        }
    }
}

@Composable
private fun rememberTimelineAxisValueFormatter(
    domainStartMillis: Long,
    userSettings: UserSettings,
): CartesianValueFormatter {
    val locale = remember { Locale.getDefault() }
    val zoneId = remember(userSettings.timeZoneId) {
        userSettings.resolvedZoneId()
    }
    val timeFormatter = remember(userSettings.use24HourClock, locale) {
        userSettings.chartTimeFormatter(locale)
    }

    return remember(domainStartMillis, zoneId, timeFormatter) {
        CartesianValueFormatter { _, value, _ ->
            val timestampMillis = ChartDataGenerator.domainXToTimestamp(
                domainStartMillis = domainStartMillis,
                xValue = value,
            )
            Instant.ofEpochMilli(timestampMillis).atZone(zoneId).format(timeFormatter)
        }
    }
}

@Composable
private fun rememberTimelineAxisOffset(
    domainStartMillis: Long,
    spacing: Int,
    userSettings: UserSettings,
): Int {
    val zoneId = remember(userSettings.timeZoneId) {
        userSettings.resolvedZoneId()
    }

    return remember(domainStartMillis, spacing, zoneId) {
        if (spacing <= 0) return@remember 0

        val startZonedDateTime = Instant.ofEpochMilli(domainStartMillis).atZone(zoneId)
        val unitsSinceLocalMidnight =
            ((startZonedDateTime.hour * 60) + startZonedDateTime.minute) / TIMELINE_BASE_INTERVAL_MINUTES
        (spacing - (unitsSinceLocalMidnight % spacing)) % spacing
    }
}

private fun compactTimelineDateFormatter(
    dateFormat: AppDateFormat,
    locale: Locale,
): DateTimeFormatter {
    val pattern = when (dateFormat) {
        AppDateFormat.MONTH_DAY_YEAR -> "MMM d"
        AppDateFormat.DAY_MONTH_YEAR -> "d MMM"
        AppDateFormat.YEAR_MONTH_DAY -> "MMM d"
    }
    return DateTimeFormatter.ofPattern(pattern, locale)
}

@Composable
private fun rememberThresholdLineDecoration(
    thresholdLevel: Double,
    label: String
): Decoration {
    val line = remember {
        LineComponent(
            fill = Fill(SleepReferenceColor.copy(alpha = 0.65f)),
            thickness = 1.dp
        )
    }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = MontserratFamily,
        color = SleepReferenceColor,
    )
    val labelFontRefreshKey = rememberFontRefreshKey(labelStyle)
    val labelComponent = key(labelFontRefreshKey) {
        rememberTextComponent(style = labelStyle)
    }

    return remember(thresholdLevel, line, labelComponent, label) {
        HorizontalLine(
            y = { thresholdLevel },
            line = line,
            labelComponent = labelComponent,
            label = { label },
            horizontalLabelPosition = Position.Horizontal.Start,
            verticalLabelPosition = Position.Vertical.Bottom
        )
    }
}

@Composable
private fun rememberDailyBedtimeDecorations(
    domainStartMillis: Long,
    minX: Double,
    maxX: Double,
    userSettings: UserSettings,
    lineColor: Color,
    labelColor: Color,
    icon: Int? = null,
    dashed: Boolean,
    labelAtTop: Boolean,
    iconTopOffset: Dp = 12.dp,
    iconSize: Dp = 16.dp,
    iconOnRightSide: Boolean = false,
): List<Decoration> {
    val zoneId = remember(userSettings.timeZoneId) {
        userSettings.resolvedZoneId()
    }
    val bedtimeXValues = remember(
        domainStartMillis,
        minX,
        maxX,
        zoneId,
        userSettings.sleepTimeHour,
        userSettings.sleepTimeMinute,
    ) {
        val bufferedStartMillis = ChartDataGenerator.domainXToTimestamp(
            domainStartMillis = domainStartMillis,
            xValue = minX,
        )
        val bufferedEndMillis = ChartDataGenerator.domainXToTimestamp(
            domainStartMillis = domainStartMillis,
            xValue = maxX,
        )
        val startDate = Instant.ofEpochMilli(bufferedStartMillis).atZone(zoneId).toLocalDate().minusDays(1)
        val endDate = Instant.ofEpochMilli(bufferedEndMillis).atZone(zoneId).toLocalDate().plusDays(1)
        val bedtimeTime = LocalTime.of(userSettings.sleepTimeHour, userSettings.sleepTimeMinute)

        buildList {
            var date = startDate
            while (!date.isAfter(endDate)) {
                val bedtimeMillis = ZonedDateTime.of(date, bedtimeTime, zoneId)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant()
                    .toEpochMilli()
                val bedtimeX = ChartDataGenerator.timestampToDomainX(
                    domainStartMillis = domainStartMillis,
                    targetTimestampMillis = bedtimeMillis,
                )
                if (bedtimeX in minX..maxX) {
                    add(bedtimeX)
                }
                date = date.plusDays(1)
            }
        }
    }

    val decorations = mutableListOf<Decoration>()
    for (bedtimeX in bedtimeXValues) {
        key(bedtimeX) {
            val decoration = rememberVerticalReferenceLineDecoration(
                xValue = bedtimeX,
                lineColor = lineColor,
                labelColor = labelColor,
                dashed = dashed,
                labelAtTop = labelAtTop,
                icon = icon,
                iconTopOffset = iconTopOffset,
                iconSize = iconSize,
                iconOnRightSide = iconOnRightSide,
            )
            if (decoration != null) {
                decorations += decoration
            }
        }
    }
    return decorations
}

@Composable
private fun rememberVerticalReferenceLineDecoration(
    xValue: Double?,
    label: String = "",
    lineColor: Color,
    labelColor: Color,
    dashed: Boolean,
    labelAtTop: Boolean,
    icon: Int? = null,
    iconTopOffset: Dp = 12.dp,
    iconSize: Dp = 16.dp,
    iconOnRightSide: Boolean = false,
): Decoration? {
    if (xValue == null) return null

    val line = remember(lineColor) {
        LineComponent(
            fill = Fill(lineColor),
            thickness = 1.dp
        )
    }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = MontserratFamily,
        color = labelColor,
    )
    val labelFontRefreshKey = rememberFontRefreshKey(labelStyle)
    val labelComponent = key(labelFontRefreshKey) {
        rememberTextComponent(style = labelStyle)
    }
    
    val context = LocalContext.current

    return remember(
        xValue,
        label,
        line,
        labelComponent,
        dashed,
        labelAtTop,
        icon,
        iconTopOffset,
        iconSize,
        iconOnRightSide,
    ) {
        VerticalReferenceLineDecoration(
            xValue = xValue,
            line = line,
            label = label,
            labelComponent = labelComponent,
            dashed = dashed,
            labelAtTop = labelAtTop,
            iconDrawableId = icon,
            iconColor = labelColor,
            iconTopOffset = iconTopOffset,
            iconSize = iconSize,
            iconOnRightSide = iconOnRightSide,
            context = context
        )
    }
}

@Composable
private fun rememberConsumptionMarker(emojiLabel: String): CartesianMarker {
    return rememberBadgeMarker(
        labelText = emojiLabel,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun rememberBadgeMarker(
    labelText: String,
    containerColor: Color,
    contentColor: Color
): CartesianMarker {
    val bubbleBackground = rememberShapeComponent(
        fill = Fill(containerColor),
        shape = CircleShape,
        strokeFill = Fill(MaterialTheme.colorScheme.outlineVariant),
        strokeThickness = 1.dp
    )
    val labelStyle = MaterialTheme.typography.labelLarge.copy(
        fontFamily = MontserratFamily,
        color = contentColor,
    )
    val labelFontRefreshKey = rememberFontRefreshKey(labelStyle)
    val label = key(labelFontRefreshKey) {
        rememberTextComponent(
            style = labelStyle,
            lineCount = 1,
            padding = Insets(horizontal = 10.dp, vertical = 6.dp),
            background = bubbleBackground,
            minWidth = TextComponent.MinWidth.fixed(36.dp)
        )
    }
    val valueFormatter = remember(labelText) {
        DefaultCartesianMarker.ValueFormatter { _, _ -> labelText }
    }

    return rememberDefaultCartesianMarker(
        label = label,
        valueFormatter = valueFormatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        guideline = null,
        indicator = null
    )
}

@Composable
private fun rememberFontRefreshKey(style: TextStyle): Any {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val resolvedTypeface by fontFamilyResolver.resolveAsTypeface(
        fontFamily = style.fontFamily ?: FontFamily.Default,
        fontWeight = style.fontWeight ?: FontWeight.Normal,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
    )

    return resolvedTypeface
}

private data class VerticalReferenceLineDecoration(
    private val xValue: Double,
    private val line: LineComponent,
    private val label: String,
    private val labelComponent: TextComponent,
    private val dashed: Boolean,
    private val labelAtTop: Boolean,
    private val iconDrawableId: Int? = null,
    private val iconColor: Color = Color.Black,
    private val context: android.content.Context,
    private val dashLength: Dp = 6.dp,
    private val gapLength: Dp = 6.dp,
    private val iconSize: Dp = 16.dp,
    private val iconHorizontalPadding: Dp = 8.dp,
    private val iconOnRightSide: Boolean = false,
    private val iconTopOffset: Dp = 12.dp,
    private val iconBottomOffset: Dp = 12.dp
) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            if (layerDimensions.xSpacing == 0f || ranges.xStep == 0.0) return

            val canvasX = xToCanvas(xValue)

            if (canvasX < layerBounds.left || canvasX > layerBounds.right) return

            if (dashed) {
                var segmentTop = layerBounds.top
                val dashHeightPx = dashLength.pixels
                val gapHeightPx = gapLength.pixels
                while (segmentTop < layerBounds.bottom) {
                    val segmentBottom = min(segmentTop + dashHeightPx, layerBounds.bottom)
                    line.drawVertical(this, canvasX, segmentTop, segmentBottom)
                    segmentTop = segmentBottom + gapHeightPx
                }
            } else {
                line.drawVertical(this, canvasX, layerBounds.top, layerBounds.bottom)
            }

            if (iconDrawableId != null) {
                val iconSizePx = iconSize.pixels
                val iconLeft = if (iconOnRightSide) {
                    canvasX + iconHorizontalPadding.pixels
                } else {
                    canvasX - iconSizePx - iconHorizontalPadding.pixels
                }
                val iconTop = if (labelAtTop) {
                    layerBounds.top + iconTopOffset.pixels
                } else {
                    layerBounds.bottom - iconBottomOffset.pixels - iconSizePx
                }
                
                // Load and draw the drawable
                val drawable = ContextCompat.getDrawable(this@VerticalReferenceLineDecoration.context, iconDrawableId)
                drawable?.let {
                    it.setBounds(
                        iconLeft.toInt(),
                        iconTop.toInt(),
                        (iconLeft + iconSizePx).toInt(),
                        (iconTop + iconSizePx).toInt()
                    )
                    it.setTint(iconColor.toArgb())
                    
                    canvas.nativeCanvas.save()
                    it.draw(canvas.nativeCanvas)
                    canvas.nativeCanvas.restore()
                }
            } else {
                // Draw text label centered on the line (original behavior)
                labelComponent.draw(
                    context = this,
                    text = label,
                    x = canvasX,
                    y = if (labelAtTop) layerBounds.top + 6.dp.pixels else layerBounds.bottom - 6.dp.pixels,
                    horizontalPosition = Position.Horizontal.Center,
                    verticalPosition = if (labelAtTop) Position.Vertical.Bottom else Position.Vertical.Top,
                    maxWidth = layerBounds.width.toInt()
                )
            }
        }
    }
}

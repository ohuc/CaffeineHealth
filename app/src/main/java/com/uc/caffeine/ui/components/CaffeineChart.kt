package com.uc.caffeine.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
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
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.uc.caffeine.R
import com.uc.caffeine.data.AppDateFormat
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.theme.MontserratFamily
import com.uc.caffeine.util.ChartConsumptionMarker
import com.uc.caffeine.util.ChartData
import com.uc.caffeine.util.ChartDataGenerator
import com.uc.caffeine.util.ConsumptionContributionDetail
import com.uc.caffeine.util.bedtimeForNightOf
import com.uc.caffeine.util.chartTimeFormatter
import com.uc.caffeine.util.resolvedZoneId
import com.uc.caffeine.util.timeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
private const val CURVE_SMOOTHING = 0.35f

/** At full depth in the edge zone the scrubber scrolls this many plot widths per second. */
private const val SCRUB_MAX_AUTO_SCROLL_WIDTHS_PER_SECOND = 0.35f
private val ScrubEdgeZone = 56.dp
private const val MIN_LIGHT_TICK_INTERVAL_NANOS = 35_000_000L
private const val Y_SCALE_SETTLE_MILLIS = 300L
private val ChartInset = 4.dp
private val BedtimeIconTopOffset = 56.dp
private val BedtimeIconSize = 12.dp
private val SleepReferenceColor = Color(0xFF6FC06B)

internal data class HomeChartDisplaySeries(
    val xValues: List<Double>,
    val yValues: List<Double>,
)

/** Plotted series for [chartData]. Depends only on the chart snapshot (refreshed every minute). */
internal fun buildHomeChartDisplaySeries(chartData: ChartData): HomeChartDisplaySeries {
    val xValues = ArrayList<Double>(chartData.dataPoints.size)
    val yValues = ArrayList<Double>(chartData.dataPoints.size)
    chartData.dataPoints.forEach { point ->
        xValues += ChartDataGenerator.timestampToDomainX(
            domainStartMillis = chartData.domainStartMillis,
            targetTimestampMillis = point.timestampMillis,
        )
        yValues += point.caffeineLevel
    }
    return HomeChartDisplaySeries(xValues = xValues, yValues = yValues)
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
    modifier: Modifier = Modifier,
    onEntryClick: ((entryId: Int) -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    val density = LocalDensity.current
    val context = LocalContext.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val chartInsetPx = with(density) { ChartInset.toPx() }

    // While the scrubber is open the chart keeps the snapshot it opened on, so the curve under the
    // finger (and the model) cannot change mid-gesture when the 60-second chart ticker fires.
    val scrubState = remember { ChartScrubState() }
    var frozenChartData by remember { mutableStateOf<ChartData?>(null) }
    val latestChartData by rememberUpdatedState(chartData)
    val shownChartData = frozenChartData ?: chartData
    val domainStartMillis = shownChartData.domainStartMillis

    val displaySeries = remember(shownChartData) { buildHomeChartDisplaySeries(shownChartData) }
    val latestDisplaySeries = rememberUpdatedState(displaySeries)
    // The live "now" line moves every second; keep it out of the chart/decoration keys so that
    // tick only redraws instead of rebuilding the series and the CartesianChart.
    val currentTimeXState = rememberUpdatedState(
        ChartDataGenerator.timestampToDomainX(
            domainStartMillis = domainStartMillis,
            targetTimestampMillis = liveNowMillis,
        )
    )
    val maxCaffeine = remember(displaySeries) { displaySeries.yValues.maxOrNull() ?: 0.0 }
    val thresholdMg = shownChartData.thresholdLevel
    // The y scale follows what is on screen (re-evaluated when scrolling settles), not the
    // all-time peak: one 900 mg day months ago no longer flattens every other day. The sleep
    // threshold is always kept in range. Start from the window the initial scroll will show.
    var yAxisMax by remember {
        val nowX = currentTimeXState.value
        mutableDoubleStateOf(
            chartAxisMaxFor(
                max(
                    visibleSeriesMax(
                        displaySeries.xValues,
                        displaySeries.yValues,
                        fromX = nowX - HOME_VISIBLE_WINDOW_UNITS * HOME_INITIAL_NOW_BIAS,
                        toX = nowX + HOME_VISIBLE_WINDOW_UNITS * (1f - HOME_INITIAL_NOW_BIAS),
                    ),
                    thresholdMg,
                )
            )
        )
    }
    val yAxisStep = yAxisMax / 4.0

    val bufferedMinX = (displaySeries.xValues.firstOrNull() ?: 0.0) - TIMELINE_SCROLL_BUFFER_UNITS
    val bufferedMaxX = (displaySeries.xValues.lastOrNull() ?: 0.0) + TIMELINE_SCROLL_BUFFER_UNITS

    val rangeProvider = remember(bufferedMinX, bufferedMaxX, yAxisMax) {
        CartesianLayerRangeProvider.fixed(
            minX = bufferedMinX,
            maxX = bufferedMaxX,
            minY = 0.0,
            maxY = yAxisMax,
        )
    }

    // The model producer is fed here (not in the ViewModel) so the line series, the range
    // provider and the axis formatters always come from the same chart snapshot. Vico only
    // recomputes ranges on a transaction, so a new y scale re-sends the same series too.
    var isModelReady by remember { mutableStateOf(false) }
    LaunchedEffect(displaySeries, yAxisMax) {
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
        domainStartMillis = domainStartMillis,
        userSettings = userSettings,
    )
    val bottomAxisOffset = rememberTimelineAxisOffset(
        domainStartMillis = domainStartMillis,
        spacing = TIMELINE_AXIS_SPACING_UNITS,
        userSettings = userSettings,
    )
    val yAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            if (value.roundToInt() == 0) "​" else "${value.roundToInt()}"
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
    val lineColor = if (maxCaffeine <= 0.0) Color.Transparent else defaultLineColor
    val lineProvider = rememberCurveLineProvider(lineColor)

    val thresholdDecoration = rememberThresholdLineDecoration(
        thresholdLevel = shownChartData.thresholdLevel,
        label = stringResource(R.string.analytics_sleep_threshold),
    )
    val nowLineColor = colorScheme.onSurface.copy(alpha = 0.48f)
    val currentTimeDecoration = remember(nowLineColor) {
        CurrentTimeLineDecoration(
            xValue = { currentTimeXState.value },
            line = LineComponent(fill = Fill(nowLineColor), thickness = 1.dp),
        )
    }
    val bedtimeXValues = rememberBedtimeXValues(
        domainStartMillis = domainStartMillis,
        minX = bufferedMinX,
        maxX = bufferedMaxX,
        userSettings = userSettings,
    )
    val bedtimeLineColor = colorScheme.primary.copy(alpha = 0.8f)
    val bedtimeIconColor = colorScheme.primary
    val bedtimeIcon = remember(context, bedtimeIconColor) {
        // mutate() so tinting this copy never leaks into other users of ic_moon.
        ContextCompat.getDrawable(context, R.drawable.ic_moon)?.mutate()?.apply {
            setTint(bedtimeIconColor.toArgb())
        }
    }
    val bedtimeDecoration = remember(bedtimeXValues, bedtimeLineColor, bedtimeIcon) {
        BedtimeLinesDecoration(
            xValues = bedtimeXValues,
            line = LineComponent(fill = Fill(bedtimeLineColor), thickness = 1.dp),
            icon = bedtimeIcon,
            iconSize = BedtimeIconSize,
            iconTopOffset = BedtimeIconTopOffset,
        )
    }

    var selectedMarker by remember { mutableStateOf<ChartConsumptionMarker?>(null) }
    var popupOffset by remember { mutableStateOf(Offset.Zero) }
    val containerColor = colorScheme.primaryContainer.toArgb()
    val strokeColor = colorScheme.outlineVariant.toArgb()
    val dotColor = colorScheme.primary.toArgb()
    val badgeColor = colorScheme.error.toArgb()
    val badgeTextColor = colorScheme.onError.toArgb()
    val consumptionDecoration = remember(
        shownChartData.consumptionMarkers, containerColor, strokeColor, dotColor, badgeColor, badgeTextColor,
    ) {
        ConsumptionImageDecoration(
            markers = shownChartData.consumptionMarkers,
            appContext = context.applicationContext,
            containerColor = containerColor,
            strokeColor = strokeColor,
            dotColor = dotColor,
            badgeColor = badgeColor,
            textColor = badgeTextColor,
        )
    }

    val zoneId = remember(userSettings.timeZoneId, userSettings.useSystemTimeZone) {
        userSettings.resolvedZoneId()
    }
    val timelineAnchorScrollValue = rememberSaveable { mutableFloatStateOf(Float.NaN) }
    val timelineAnchorXSpacing = rememberSaveable { mutableFloatStateOf(Float.NaN) }
    val timelineAnchorXStep = rememberSaveable { mutableFloatStateOf(Float.NaN) }
    val timelineAnchorScroll = remember {
        Scroll.Absolute { measuringContext, layerDimensions, bounds, maxValue ->
            val anchorScrollValue = Scroll.Absolute.x(
                x = currentTimeXState.value,
                // Keep "now" slightly right of center so both swipe directions have room immediately.
                bias = HOME_INITIAL_NOW_BIAS,
            ).getValue(measuringContext, layerDimensions, bounds, maxValue)
            if (timelineAnchorScrollValue.floatValue != anchorScrollValue) {
                timelineAnchorScrollValue.floatValue = anchorScrollValue
            }
            if (timelineAnchorXSpacing.floatValue != layerDimensions.xSpacing) {
                timelineAnchorXSpacing.floatValue = layerDimensions.xSpacing
            }
            val xStep = measuringContext.ranges.xStep.toFloat()
            if (timelineAnchorXStep.floatValue != xStep) {
                timelineAnchorXStep.floatValue = xStep
            }
            anchorScrollValue
        }
    }
    // Follow "now" as the curve grows (a new point every 15 minutes, or a new drink), but only
    // while "now" is on screen. Before, this yanked people back to today while they were reading
    // an earlier day, and it must never move the chart under an open scrubber.
    val followNowCondition = remember {
        AutoScrollCondition { oldModel, newModel ->
            AutoScrollCondition.OnModelGrowth.shouldScroll(oldModel, newModel) &&
                !scrubState.isActive &&
                scrubState.isXOnScreen(currentTimeXState.value)
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
        autoScrollCondition = followNowCondition,
    )
    val zoomState = rememberVicoZoomState(
        zoomEnabled = false,
        initialZoom = homeViewportZoom,
        minZoom = homeViewportZoom,
        maxZoom = homeViewportZoom,
    )

    val scrubPoint: State<ChartScrubPoint?> = remember(scrollState) {
        derivedStateOf {
            val pointerX = scrubState.pointerX ?: return@derivedStateOf null
            val mapping = scrubState.mapping ?: return@derivedStateOf null
            val x = snapScrubX(mapping.canvasToX(mapping.clampToLayer(pointerX), scrollState.value))
            val series = latestDisplaySeries.value
            ChartScrubPoint(x = x, level = interpolateSeriesLevel(series.xValues, series.yValues, x))
        }
    }
    val scrubLineColor = colorScheme.primary.copy(alpha = 0.55f)
    val scrubDotColor = colorScheme.primary
    val scrubHaloColor = colorScheme.primary.copy(alpha = 0.18f)
    val scrubRingColor = colorScheme.surface
    val scrubDecoration = remember(scrubLineColor, scrubDotColor, scrubHaloColor, scrubRingColor) {
        ChartScrubDecoration(
            state = scrubState,
            scrubPoint = { scrubPoint.value },
            lineColor = scrubLineColor,
            dotColor = scrubDotColor,
            haloColor = scrubHaloColor,
            dotRingColor = scrubRingColor,
        )
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = lineProvider,
            rangeProvider = rangeProvider,
        ),
        startAxis = VerticalAxis.rememberStart(
            itemPlacer = yAxisItemPlacer,
            valueFormatter = yAxisFormatter,
            label = labelComponent,
            guideline = null,
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
            guideline = null,
        ),
        // Scrub decoration last: it draws over the markers and publishes the frame geometry.
        decorations = listOf(
            thresholdDecoration,
            currentTimeDecoration,
            bedtimeDecoration,
            consumptionDecoration,
            scrubDecoration,
        ),
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

    val isScrubbing = scrubState.isActive

    // Edge auto-scroll: while the scrubber is held near a plot edge, scroll that way with a speed
    // that grows with how far into the edge the finger is pushed.
    val edgeZonePx = with(density) { ScrubEdgeZone.toPx() }
    LaunchedEffect(isScrubbing, scrollState) {
        if (!isScrubbing) return@LaunchedEffect
        var previousFrameNanos = withFrameNanos { it }
        var atScrollLimit = false
        while (true) {
            val frameNanos = withFrameNanos { it }
            val deltaSeconds = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrameNanos = frameNanos
            val pointerX = scrubState.pointerX ?: continue
            val mapping = scrubState.mapping ?: continue
            if (!scrubState.autoScrollArmed) continue
            val velocity = edgeAutoScrollVelocity(
                pointerX = pointerX,
                layerLeft = mapping.layerLeft,
                layerRight = mapping.layerRight,
                edgeZonePx = edgeZonePx,
                maxSpeedPxPerSecond = mapping.layerWidth * SCRUB_MAX_AUTO_SCROLL_WIDTHS_PER_SECOND,
            )
            if (velocity == 0f) {
                atScrollLimit = false
                continue
            }
            val before = scrollState.value
            scrollState.scroll(Scroll.Relative.pixels(velocity * deltaSeconds))
            val stuck = scrollState.value == before
            if (stuck && !atScrollLimit) haptics.sliderEdge()
            atScrollLimit = stuck
        }
    }

    // Re-fit the y scale to the visible window once scrolling settles (never mid-scrub).
    LaunchedEffect(scrollState, displaySeries, thresholdMg) {
        snapshotFlow { Triple(scrollState.value, scrubState.mapping, scrubState.isActive) }
            .collectLatest { (scroll, mapping, scrubbing) ->
                if (scrubbing || mapping == null) return@collectLatest
                delay(Y_SCALE_SETTLE_MILLIS)
                val visibleMax = visibleSeriesMax(
                    displaySeries.xValues,
                    displaySeries.yValues,
                    fromX = mapping.canvasToX(mapping.layerLeft, scroll),
                    toX = mapping.canvasToX(mapping.layerRight, scroll),
                )
                val target = chartAxisMaxFor(max(visibleMax, thresholdMg))
                if (target != yAxisMax) yAxisMax = target
            }
    }

    // Scrub haptics: a light tick at every quarter hour, and a firmer click on landmarks —
    // the "now" line, a bedtime line, a logged drink, or the level crossing the sleep threshold.
    val markerXValues = remember(shownChartData.consumptionMarkers) {
        shownChartData.consumptionMarkers.map(ChartConsumptionMarker::xValue)
    }
    LaunchedEffect(isScrubbing) {
        if (!isScrubbing) return@LaunchedEffect
        var previous: ChartScrubPoint? = null
        var lastLightTickNanos = 0L
        snapshotFlow { scrubPoint.value }.collect { point ->
            val before = previous
            previous = point
            if (point == null || before == null || point.x == before.x) return@collect
            val landmark = crossedValue(before.x, point.x, currentTimeXState.value) ||
                bedtimeXValues.any { crossedValue(before.x, point.x, it) } ||
                markerXValues.any { crossedValue(before.x, point.x, it) } ||
                crossedValue(before.level, point.level, thresholdMg)
            val nowNanos = System.nanoTime()
            when {
                landmark -> {
                    haptics.landmark()
                    lastLightTickNanos = nowNanos
                }
                crossedQuarterHour(before.x, point.x) &&
                    nowNanos - lastLightTickNanos >= MIN_LIGHT_TICK_INTERVAL_NANOS -> {
                    // Rate-limited so a fast drag or edge auto-scroll stays a soft texture.
                    haptics.sliderTick()
                    lastLightTickNanos = nowNanos
                }
            }
        }
    }

    val latestOnEntryClick by rememberUpdatedState(onEntryClick)
    val latestInsetPx = rememberUpdatedState(chartInsetPx)
    val latestTouchSlop = rememberUpdatedState(touchSlop)
    val hitRadiusPx = with(density) { (MarkerImageRadius + 8.dp).toPx() }
    val graphDescription = stringResource(R.string.chart_graph_cd)

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = graphDescription }
            // The gesture's pointerInput is keyed on Unit so the 1-second recomposition never
            // restarts it mid-hold; everything it touches is a remembered object or updated state.
            .longPressScrubGesture(
                onStart = { x ->
                    selectedMarker = null
                    frozenChartData = latestChartData
                    scrubState.start(x - latestInsetPx.value)
                    haptics.gestureStart()
                },
                onMove = { x ->
                    scrubState.move(x - latestInsetPx.value, armDistancePx = latestTouchSlop.value)
                },
                onEnd = {
                    if (scrubState.isActive) haptics.gestureEnd()
                    scrubState.end()
                    frozenChartData = null
                },
            )
            .pointerInput(consumptionDecoration, hitRadiusPx, chartInsetPx) {
                detectTapGestures { tap ->
                    val hit = consumptionDecoration.markerAt(
                        point = Offset(tap.x - chartInsetPx, tap.y - chartInsetPx),
                        radiusPx = hitRadiusPx,
                    )
                    val marker = hit?.first
                    when {
                        marker != null && marker.entries.size > 1 -> {
                            selectedMarker = marker
                            popupOffset = hit.second + Offset(chartInsetPx, chartInsetPx)
                        }
                        marker != null -> {
                            selectedMarker = null
                            latestOnEntryClick?.invoke(marker.entries.first().entryId)
                        }
                        else -> selectedMarker = null
                    }
                }
            }
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
                    .padding(ChartInset),
            )
        }

        CaffeineChartStatus(
            currentCaffeineLevel = currentCaffeineLevel,
            predictedBedtimeCaffeineLevel = predictedBedtimeCaffeineLevel,
            sleepThresholdMg = userSettings.sleepThresholdMg.toDouble(),
            scrubPoint = scrubPoint,
            domainStartMillis = domainStartMillis,
            liveNowMillis = liveNowMillis,
            zoneId = zoneId,
            userSettings = userSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )

        AnimatedVisibility(
            visible = dayOffsetFromToday > 0 && !isScrubbing,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = (-24).dp)
                .padding(start = 2.dp),
        ) {
            TimelineJumpButton(
                onClick = { coroutineScope.launch { scrollState.animateScroll(timelineAnchorScroll) } },
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.chart_return_to_today_cd),
            )
        }

        AnimatedVisibility(
            visible = dayOffsetFromToday < 0 && !isScrubbing,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-24).dp)
                .padding(end = 2.dp),
        ) {
            TimelineJumpButton(
                onClick = { coroutineScope.launch { scrollState.animateScroll(timelineAnchorScroll) } },
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.chart_return_to_today_cd),
            )
        }

        selectedMarker?.let { marker ->
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .absoluteOffset {
                        IntOffset(popupOffset.x.roundToInt(), popupOffset.y.roundToInt())
                    }
            ) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { selectedMarker = null },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
                ) {
                    marker.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DrinkIcon(
                                        imageName = entry.imageName,
                                        emoji = entry.emoji,
                                        contentDescription = entry.drinkName,
                                        modifier = Modifier.size(28.dp),
                                        emojiSize = MaterialTheme.typography.bodyMedium.fontSize,
                                    )
                                    Text(
                                        text = entry.drinkName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(R.string.caffeine_mg_compact, entry.caffeineMg),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            },
                            onClick = {
                                selectedMarker = null
                                latestOnEntryClick?.invoke(entry.entryId)
                            },
                        )
                    }
                }
            }
        }
    }
}

internal enum class ScrubDayLabel { NOW, TODAY, TOMORROW, YESTERDAY, OTHER }

/** Half of the 1-minute snap step: exactly one snapped position reads as "Right now". */
private const val SCRUB_NOW_TOLERANCE_MILLIS = 30_000L

internal fun scrubDayLabel(timestampMillis: Long, nowMillis: Long, zoneId: ZoneId): ScrubDayLabel {
    if (abs(timestampMillis - nowMillis) <= SCRUB_NOW_TOLERANCE_MILLIS) return ScrubDayLabel.NOW
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val day = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
    return when (ChronoUnit.DAYS.between(today, day)) {
        0L -> ScrubDayLabel.TODAY
        1L -> ScrubDayLabel.TOMORROW
        -1L -> ScrubDayLabel.YESTERDAY
        else -> ScrubDayLabel.OTHER
    }
}

private fun scrubDayFormatter(dateFormat: AppDateFormat, locale: Locale): DateTimeFormatter {
    val pattern = when (dateFormat) {
        AppDateFormat.DAY_MONTH_YEAR -> "EEE d MMM"
        AppDateFormat.MONTH_DAY_YEAR, AppDateFormat.YEAR_MONTH_DAY -> "EEE MMM d"
    }
    return DateTimeFormatter.ofPattern(pattern, locale)
}

/**
 * The counter in the top corner. Normally it shows the live level and the sleep outlook; while
 * the scrubber is held the same rolling counter shows the level under the finger and the sleep
 * line is replaced by when that is ("Today at 3:08 PM"). It reads [scrubPoint] itself, so only
 * this small composable recomposes as the finger moves.
 */
@Composable
private fun CaffeineChartStatus(
    currentCaffeineLevel: Double,
    predictedBedtimeCaffeineLevel: Double,
    sleepThresholdMg: Double,
    scrubPoint: State<ChartScrubPoint?>,
    domainStartMillis: Long,
    liveNowMillis: Long,
    zoneId: ZoneId,
    userSettings: UserSettings,
    modifier: Modifier = Modifier,
) {
    val point = scrubPoint.value
    val sleepShouldBeUnaffected = predictedBedtimeCaffeineLevel <= sleepThresholdMg
    val sleepText = stringResource(
        if (sleepShouldBeUnaffected) R.string.chart_sleep_unaffected else R.string.chart_sleep_affected
    )
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(userSettings.use24HourClock, locale) { userSettings.timeFormatter(locale) }
    val dayFormatter = remember(userSettings.dateFormat, locale) {
        scrubDayFormatter(userSettings.dateFormat, locale)
    }
    val scrubText = point?.let {
        val timestampMillis = ChartDataGenerator.domainXToTimestamp(domainStartMillis, it.x)
        val zoned = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val time = zoned.format(timeFormatter)
        when (scrubDayLabel(timestampMillis, liveNowMillis, zoneId)) {
            ScrubDayLabel.NOW -> stringResource(R.string.chart_scrub_now)
            ScrubDayLabel.TODAY -> stringResource(R.string.chart_scrub_today, time)
            ScrubDayLabel.TOMORROW -> stringResource(R.string.chart_scrub_tomorrow, time)
            ScrubDayLabel.YESTERDAY -> stringResource(R.string.chart_scrub_yesterday, time)
            ScrubDayLabel.OTHER -> stringResource(R.string.chart_scrub_other_day, zoned.format(dayFormatter), time)
        }
    }
    val shownLevel = point?.level ?: currentCaffeineLevel
    val statusDescription = stringResource(
        R.string.chart_status_cd,
        String.format(locale, "%.1f", currentCaffeineLevel),
        sleepText,
    )

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = statusDescription },
        horizontalAlignment = Alignment.End,
    ) {
        RollingCaffeineCounter(
            value = shownLevel,
            modifier = Modifier.align(Alignment.End),
        )
        Text(
            text = scrubText ?: sleepText,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = MontserratFamily,
                fontWeight = if (scrubText != null) FontWeight.SemiBold else null,
            ),
            color = when {
                scrubText != null -> MaterialTheme.colorScheme.onSurface
                sleepShouldBeUnaffected -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            },
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun TimelineJumpButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp),
        shapes = IconButtonDefaults.shapes(),
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
    // Locale.US keeps ASCII digits and a "." separator so each digit can roll independently.
    val formattedString = String.format(Locale.US, "%.1f", value)
    val counterStyle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MontserratFamily,
        color = MaterialTheme.colorScheme.primary,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
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
                    Text(text = digit.toString(), style = counterStyle)
                }
            } else {
                Text(text = char.toString(), style = counterStyle)
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(text = stringResource(R.string.unit_mg), style = counterStyle)
    }
}

@Composable
private fun rememberCurveLineProvider(lineColor: Color): LineCartesianLayer.LineProvider {
    return remember(lineColor) {
        val areaBrush = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.45f), Color.Transparent),
        )
        LineCartesianLayer.LineProvider.series(
            LineCartesianLayer.Line(
                // The tail fades into the axis instead of running along it as a flat line.
                fill = FadeToBaselineLineFill(lineColor),
                areaFill = LineCartesianLayer.AreaFill.single(Fill(areaBrush)),
                interpolator = LineCartesianLayer.Interpolator.cubic(curvature = CURVE_SMOOTHING),
            )
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
    val yAxisMax = yAxisStep * 4.0

    val contributionMinX = xValues.firstOrNull() ?: 0.0
    val contributionMaxX = xValues.lastOrNull() ?: 0.0
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
    val lineProvider = rememberCurveLineProvider(lineColor)
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
                    addExtremeLabelPadding = true,
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
    chart: CartesianChart,
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
private fun rememberClockValueFormatter(
    domainStartMillis: Long,
    userSettings: UserSettings
): CartesianValueFormatter {
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = remember(userSettings.timeZoneId, userSettings.useSystemTimeZone) {
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
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = remember(userSettings.timeZoneId, userSettings.useSystemTimeZone) {
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
    val zoneId = remember(userSettings.timeZoneId, userSettings.useSystemTimeZone) {
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

/** Chart x values of every bedtime inside [minX]..[maxX], following the weekly rota. */
@Composable
private fun rememberBedtimeXValues(
    domainStartMillis: Long,
    minX: Double,
    maxX: Double,
    userSettings: UserSettings,
): List<Double> {
    return remember(domainStartMillis, minX, maxX, userSettings) {
        val zoneId = userSettings.resolvedZoneId()
        val startDate = Instant.ofEpochMilli(ChartDataGenerator.domainXToTimestamp(domainStartMillis, minX))
            .atZone(zoneId).toLocalDate().minusDays(1)
        val endDate = Instant.ofEpochMilli(ChartDataGenerator.domainXToTimestamp(domainStartMillis, maxX))
            .atZone(zoneId).toLocalDate().plusDays(1)

        buildList {
            var date = startDate
            while (!date.isAfter(endDate)) {
                // Same night rule as calculateNextBedtimeMillis (after-midnight times belong
                // to the previous evening), so the moon line matches the bedtime prediction.
                val bedtimeMillis = userSettings.bedtimeForNightOf(date, zoneId).toInstant().toEpochMilli()
                val bedtimeX = ChartDataGenerator.timestampToDomainX(domainStartMillis, bedtimeMillis)
                if (bedtimeX in minX..maxX) add(bedtimeX)
                date = date.plusDays(1)
            }
        }
    }
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

private val MarkerImageRadius = 12.dp
private val MarkerDotRadius = 4.dp
private val MarkerImageGap = 6.dp
private val MarkerBadgeRadius = 7.dp

private object DrinkBitmapCache {
    private val cache = mutableMapOf<String, Bitmap?>()

    fun get(context: android.content.Context, imageName: String): Bitmap? {
        return cache.getOrPut(imageName) {
            try {
                context.assets.open("items/$imageName.png").use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }
    }
}

/** The live "now" line. Reads its x at draw time so the 1-second tick only redraws. */
private class CurrentTimeLineDecoration(
    private val xValue: () -> Double,
    private val line: LineComponent,
) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            if (layerDimensions.xSpacing == 0f || ranges.xStep == 0.0) return
            val canvasX = xToCanvas(xValue())
            if (canvasX < layerBounds.left || canvasX > layerBounds.right) return
            line.drawVertical(this, canvasX, layerBounds.top, layerBounds.bottom)
        }
    }
}

/** All bedtime lines in one decoration (previously one composable + decoration per day). */
private class BedtimeLinesDecoration(
    private val xValues: List<Double>,
    private val line: LineComponent,
    private val icon: Drawable?,
    private val iconSize: Dp,
    private val iconTopOffset: Dp,
    private val iconHorizontalPadding: Dp = 8.dp,
    private val dashLength: Dp = 6.dp,
    private val gapLength: Dp = 6.dp,
) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            if (layerDimensions.xSpacing == 0f || ranges.xStep == 0.0) return
            val dashPx = dashLength.pixels
            val gapPx = gapLength.pixels
            val iconSizePx = iconSize.pixels
            for (xValue in xValues) {
                val canvasX = xToCanvas(xValue)
                if (canvasX < layerBounds.left || canvasX > layerBounds.right) continue

                var segmentTop = layerBounds.top
                while (segmentTop < layerBounds.bottom) {
                    val segmentBottom = min(segmentTop + dashPx, layerBounds.bottom)
                    line.drawVertical(this, canvasX, segmentTop, segmentBottom)
                    segmentTop = segmentBottom + gapPx
                }

                if (icon != null) {
                    val iconLeft = (canvasX + iconHorizontalPadding.pixels).toInt()
                    val iconTop = (layerBounds.top + iconTopOffset.pixels).toInt()
                    icon.setBounds(iconLeft, iconTop, iconLeft + iconSizePx.toInt(), iconTop + iconSizePx.toInt())
                    icon.draw(canvas.nativeCanvas)
                }
            }
        }
    }
}

private class ConsumptionImageDecoration(
    private val markers: List<ChartConsumptionMarker>,
    private val appContext: android.content.Context,
    private val containerColor: Int,
    private val strokeColor: Int,
    private val dotColor: Int,
    private val badgeColor: Int,
    private val textColor: Int,
) : Decoration {
    // Icon centres drawn in the last frame (chart-host coordinates). Cleared every frame so
    // markers that scrolled away can no longer be hit by a tap at their old position.
    private val drawnCenters = LinkedHashMap<ChartConsumptionMarker, Offset>()
    private val clipPath = android.graphics.Path()
    private val imageBounds = android.graphics.RectF()

    fun markerAt(point: Offset, radiusPx: Float): Pair<ChartConsumptionMarker, Offset>? {
        var best: Pair<ChartConsumptionMarker, Offset>? = null
        var bestDistanceSq = radiusPx * radiusPx
        for ((marker, center) in drawnCenters) {
            val distanceSq = (point - center).getDistanceSquared()
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq
                best = marker to center
            }
        }
        return best
    }

    private fun CartesianDrawingContext.yToCanvas(yValue: Double): Float {
        // The y range Vico is drawing with, so icons stay on the line when the scale changes.
        val yMax = ranges.getYRange(null).maxY
        if (yMax <= 0.0) return layerBounds.bottom
        val fraction = (yValue / yMax).coerceIn(0.0, 1.0).toFloat()
        return layerBounds.bottom - fraction * (layerBounds.bottom - layerBounds.top)
    }

    private val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    private val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
    }
    private val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val emojiPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
    }

    override fun drawOverLayers(context: CartesianDrawingContext) {
        drawnCenters.clear()
        with(context) {
            if (layerDimensions.xSpacing == 0f || ranges.xStep == 0.0) return

            val imageRadius = MarkerImageRadius.pixels
            val dotRadius = MarkerDotRadius.pixels
            val gap = MarkerImageGap.pixels
            val badgeRadius = MarkerBadgeRadius.pixels

            strokePaint.strokeWidth = 1.5.dp.pixels
            textPaint.textSize = 8.dp.pixels
            emojiPaint.textSize = 13.dp.pixels

            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.clipRect(
                layerBounds.left,
                layerBounds.top - imageRadius * 2 - gap,
                layerBounds.right,
                layerBounds.bottom + dotRadius * 2,
            )

            for (marker in markers) {
                val canvasX = xToCanvas(marker.xValue)
                if (canvasX < layerBounds.left - imageRadius * 2 || canvasX > layerBounds.right + imageRadius * 2) continue

                val canvasY = yToCanvas(marker.yValue)
                val imageCenterY = canvasY - gap - imageRadius

                val primaryEntry = marker.entries.firstOrNull() ?: continue
                val bitmap = if (primaryEntry.imageName.isNotBlank()) {
                    DrinkBitmapCache.get(appContext, primaryEntry.imageName)
                } else null

                fillPaint.color = containerColor
                nativeCanvas.drawCircle(canvasX, imageCenterY, imageRadius, fillPaint)

                if (bitmap != null) {
                    clipPath.rewind()
                    clipPath.addCircle(canvasX, imageCenterY, imageRadius, android.graphics.Path.Direction.CW)
                    nativeCanvas.save()
                    nativeCanvas.clipPath(clipPath)
                    imageBounds.set(
                        canvasX - imageRadius,
                        imageCenterY - imageRadius,
                        canvasX + imageRadius,
                        imageCenterY + imageRadius,
                    )
                    nativeCanvas.drawBitmap(bitmap, null, imageBounds, null)
                    nativeCanvas.restore()
                } else {
                    val emoji = primaryEntry.emoji.ifBlank { "☕" }
                    val yOff = (emojiPaint.descent() + emojiPaint.ascent()) / 2f
                    nativeCanvas.drawText(emoji, canvasX, imageCenterY - yOff, emojiPaint)
                }

                strokePaint.color = strokeColor
                nativeCanvas.drawCircle(canvasX, imageCenterY, imageRadius, strokePaint)

                fillPaint.color = dotColor
                nativeCanvas.drawCircle(canvasX, canvasY, dotRadius, fillPaint)
                strokePaint.color = strokeColor
                nativeCanvas.drawCircle(canvasX, canvasY, dotRadius, strokePaint)

                if (marker.entries.size > 1) {
                    val badgeCenterX = canvasX + imageRadius * 0.68f
                    val badgeCenterY = imageCenterY - imageRadius * 0.68f
                    fillPaint.color = badgeColor
                    nativeCanvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, fillPaint)
                    textPaint.color = textColor
                    val yOff = (textPaint.descent() + textPaint.ascent()) / 2f
                    nativeCanvas.drawText("+${marker.entries.size - 1}", badgeCenterX, badgeCenterY - yOff, textPaint)
                }

                // Only icons whose centre is inside the plot are tappable (not ones under the axis).
                if (canvasX in layerBounds.left..layerBounds.right) {
                    drawnCenters[marker] = Offset(canvasX, imageCenterY)
                }
            }

            nativeCanvas.restore()
        }
    }
}

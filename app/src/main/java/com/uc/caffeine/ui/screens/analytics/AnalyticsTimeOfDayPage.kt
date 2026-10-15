package com.uc.caffeine.ui.screens.analytics

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState
import com.uc.caffeine.util.chartTimeFormatter
import com.uc.caffeine.util.timeFormatter
import com.uc.caffeine.util.timeOfDayBucketIndexForHour
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** How one part of the day is drawn: its bars and tile share a color role, plus an icon. */
private data class DayPartStyle(
    @param:StringRes val nameRes: Int,
    @param:StringRes val rangeRes: Int,
    @param:StringRes val mostlyRes: Int,
    val icon: ImageVector,
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
)

/**
 * Indexed like [AnalyticsUiState.timeOfDayValues]: night, morning, afternoon, evening. Night
 * takes the error roles because that's the caffeine most likely to still be around at bedtime.
 */
@Composable
@ReadOnlyComposable
private fun dayPartStyles(): List<DayPartStyle> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        DayPartStyle(
            nameRes = R.string.analytics_bucket_night,
            rangeRes = R.string.analytics_bucket_night_range,
            mostlyRes = R.string.analytics_mostly_night,
            icon = Icons.Rounded.Bedtime,
            accent = scheme.error,
            onAccent = scheme.onError,
            container = scheme.errorContainer,
            onContainer = scheme.onErrorContainer,
        ),
        DayPartStyle(
            nameRes = R.string.analytics_bucket_morning,
            rangeRes = R.string.analytics_bucket_morning_range,
            mostlyRes = R.string.analytics_mostly_morning,
            icon = Icons.Rounded.WbTwilight,
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            container = scheme.primaryContainer,
            onContainer = scheme.onPrimaryContainer,
        ),
        DayPartStyle(
            nameRes = R.string.analytics_bucket_afternoon,
            rangeRes = R.string.analytics_bucket_afternoon_range,
            mostlyRes = R.string.analytics_mostly_afternoon,
            icon = Icons.Rounded.WbSunny,
            accent = scheme.secondary,
            onAccent = scheme.onSecondary,
            container = scheme.secondaryContainer,
            onContainer = scheme.onSecondaryContainer,
        ),
        DayPartStyle(
            nameRes = R.string.analytics_bucket_evening,
            rangeRes = R.string.analytics_bucket_evening_range,
            mostlyRes = R.string.analytics_mostly_evening,
            icon = Icons.Rounded.NightsStay,
            accent = scheme.tertiary,
            onAccent = scheme.onTertiary,
            container = scheme.tertiaryContainer,
            onContainer = scheme.onTertiaryContainer,
        ),
    )
}

/** The tiles read through the day: morning, afternoon, evening, then night. */
private val DayPartDisplayOrder = listOf(1, 2, 3, 0)

private val HourColumnWidth = 8.dp

@Composable
internal fun AnalyticsTimeOfDayPage(
    uiState: AnalyticsUiState,
    userSettings: UserSettings,
    modelProducer: CartesianChartModelProducer,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var showRangeSheet by remember { mutableStateOf(false) }

    SettingsPageScaffold(
        title = stringResource(R.string.analytics_when_you_drink),
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "range") {
                AnalyticsRangeButton(
                    selectedRange = uiState.selectedRange,
                    customStartDate = uiState.customStartDate,
                    customEndDate = uiState.customEndDate,
                    onClick = { showRangeSheet = true },
                )
            }

            if (uiState.hasData) {
                item(key = "peak") {
                    PeakHourCard(
                        uiState = uiState,
                        userSettings = userSettings,
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "hourly") {
                    HourlyChartCard(
                        modelProducer = modelProducer,
                        userSettings = userSettings,
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "parts") {
                    DayPartTiles(
                        values = uiState.timeOfDayValues,
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                item(key = "empty") {
                    AnalyticsEmptyState(
                        selectedRange = uiState.selectedRange,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showRangeSheet) {
        AnalyticsRangeBottomSheet(
            selectedRange = uiState.selectedRange,
            onRangeSelected = { range ->
                haptics.toggle()
                onRangeSelected(range)
            },
            onCustomRange = { start, end ->
                haptics.toggle()
                onCustomRange(start, end)
            },
            onDismiss = { showRangeSheet = false },
        )
    }
}

/**
 * The headline: the single busiest hour, and which part of the day most of the caffeine falls in.
 * The card takes that part of the day's colors.
 */
@Composable
private fun PeakHourCard(
    uiState: AnalyticsUiState,
    userSettings: UserSettings,
    modifier: Modifier = Modifier,
) {
    val hourly = uiState.timeOfDayHourlyValues
    val peakHour = hourly.indices.maxByOrNull { hourly[it] } ?: 0
    val parts = uiState.timeOfDayValues
    val style = dayPartStyles()[parts.indices.maxByOrNull { parts[it] } ?: 0]
    val peakLabel = remember(peakHour, userSettings.use24HourClock) {
        LocalTime.of(peakHour, 0).format(userSettings.timeFormatter())
    }

    val colorSpec = MaterialTheme.motionScheme.slowEffectsSpec<Color>()
    val containerColor by animateColorAsState(style.container, colorSpec, label = "peakContainer")
    val contentColor by animateColorAsState(style.onContainer, colorSpec, label = "peakContent")
    val accentColor by animateColorAsState(style.accent, colorSpec, label = "peakAccent")
    val onAccentColor by animateColorAsState(style.onAccent, colorSpec, label = "peakOnAccent")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.analytics_peak_hour),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.8f),
                )
                RollingNumberText(
                    text = peakLabel,
                    style = MaterialTheme.typography.displaySmallEmphasized.copy(color = contentColor),
                    labelPrefix = "peak_hour",
                )
                AnimatedContent(
                    targetState = style.mostlyRes,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height / 2 } + fadeOut()) using
                            SizeTransform(clip = false)
                    },
                    label = "peakMostly",
                ) { mostlyRes ->
                    Text(
                        text = stringResource(mostlyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.82f),
                    )
                }
            }

            SpinningDayPartBadge(
                icon = style.icon,
                containerColor = accentColor,
                iconColor = onAccentColor,
                size = 88.dp,
            )
        }
    }
}

@Composable
private fun SpinningDayPartBadge(
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    size: Dp,
) {
    val rotation by rememberInfiniteTransition(label = "dayPartBadge").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 20_000, easing = LinearEasing), RepeatMode.Restart),
        label = "dayPartBadgeRotation",
    )
    val shape = MaterialShapes.Cookie9Sided.toShape()

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .clip(shape)
                .background(containerColor),
        )
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
            label = "dayPartBadgeIcon",
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}

/**
 * Caffeine in each of the 24 hours as a Vico column chart. Bars take their part of the day's
 * accent; tapping one pins a label with the hour and amount.
 */
@Composable
private fun HourlyChartCard(
    modelProducer: CartesianChartModelProducer,
    userSettings: UserSettings,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val columns = dayPartStyles().map { style ->
        rememberColumnComponent(color = style.accent, width = HourColumnWidth)
    }
    val columnProvider = remember(columns) {
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(
                entry: ColumnCartesianLayerModel.Entry,
                seriesIndex: Int,
                extraStore: ExtraStore,
            ): LineComponent = columns[timeOfDayBucketIndexForHour(entry.x.toHourOfDay())]

            override fun getWidestSeriesColumn(
                seriesIndex: Int,
                extraStore: ExtraStore,
            ): LineComponent = columns.first()
        }
    }

    val hourFormatter = remember(userSettings.use24HourClock) { userSettings.chartTimeFormatter() }
    val axisLabel = rememberTextComponent(
        style = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant),
    )
    val hourAxisFormatter = remember(hourFormatter) {
        CartesianValueFormatter { _, value, _ ->
            LocalTime.of(value.toHourOfDay(), 0).format(hourFormatter)
        }
    }
    val amountAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            // A zero-width space keeps the baseline unlabelled without collapsing the axis.
            if (value.roundToInt() == 0) "​" else value.roundToInt().toString()
        }
    }

    val resources = LocalResources.current
    val markerLabel = rememberTextComponent(
        style = MaterialTheme.typography.labelLarge.copy(color = scheme.inverseOnSurface),
        padding = Insets(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
        margins = Insets(bottom = 6.dp),
        background = remember(scheme.inverseSurface) {
            ShapeComponent(fill = Fill(scheme.inverseSurface), shape = CircleShape)
        },
    )
    val markerFormatter = remember(hourFormatter, resources) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget
                ?: return@ValueFormatter ""
            resources.getString(
                R.string.analytics_hour_marker,
                LocalTime.of(target.x.toHourOfDay(), 0).format(hourFormatter),
                target.columns.sumOf { it.entry.y }.roundToInt(),
            )
        }
    }

    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = columnProvider,
            columnCollectionSpacing = 4.dp,
        ),
        startAxis = VerticalAxis.rememberStart(
            line = null,
            label = axisLabel,
            valueFormatter = amountAxisFormatter,
            tick = null,
            guideline = rememberAxisGuidelineComponent(fill = Fill(scheme.outlineVariant)),
            itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { 4 }) },
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            line = rememberAxisLineComponent(fill = Fill(scheme.outlineVariant)),
            label = axisLabel,
            valueFormatter = hourAxisFormatter,
            tick = null,
            guideline = null,
            itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 6 }) },
        ),
        marker = rememberDefaultCartesianMarker(
            label = markerLabel,
            valueFormatter = markerFormatter,
            guideline = rememberAxisGuidelineComponent(fill = Fill(scheme.outline)),
        ),
        markerController = CartesianMarkerController.rememberToggleOnTap(),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = CaffeineSurfaceDefaults.chartContainerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.analytics_hour_by_hour),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Text(
                text = stringResource(R.string.analytics_hour_by_hour_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Scrolling off: all 24 hours are zoomed to fit the card.
            CartesianChartHost(
                chart = chart,
                modelProducer = modelProducer,
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(200.dp),
            )
        }
    }
}

private fun Double.toHourOfDay(): Int = roundToInt().coerceIn(0, 23)

/** One tile per part of the day, two to a row, each with its amount and share of the total. */
@Composable
private fun DayPartTiles(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    val styles = dayPartStyles()
    val total = values.sum().takeIf { it > 0.0 } ?: 1.0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DayPartDisplayOrder.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { index ->
                    val value = values.getOrElse(index) { 0.0 }
                    DayPartTile(
                        style = styles[index],
                        amountMg = value.roundToInt(),
                        sharePercent = (value / total * 100).roundToInt(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DayPartTile(
    style: DayPartStyle,
    amountMg: Int,
    sharePercent: Int,
    modifier: Modifier = Modifier,
) {
    // A part of the day with nothing logged steps back to a neutral surface.
    val empty = amountMg == 0
    val colorSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val containerColor by animateColorAsState(
        if (empty) MaterialTheme.colorScheme.surfaceContainer else style.container,
        colorSpec,
        label = "dayPartContainer",
    )
    val contentColor by animateColorAsState(
        if (empty) MaterialTheme.colorScheme.onSurfaceVariant else style.onContainer,
        colorSpec,
        label = "dayPartContent",
    )
    val badgeColor by animateColorAsState(
        if (empty) MaterialTheme.colorScheme.surfaceContainerHighest else style.accent,
        colorSpec,
        label = "dayPartBadge",
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExpressiveIconBadge(index = 0, size = 40.dp, containerColor = badgeColor) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = if (empty) contentColor else style.onAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                RollingNumberText(
                    text = stringResource(R.string.analytics_share_percent, sharePercent),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    labelPrefix = "day_part_share",
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(style.nameRes),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                    text = stringResource(style.rangeRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.72f),
                )
            }
            RollingNumberText(
                text = stringResource(R.string.analytics_value_mg, amountMg),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                labelPrefix = "day_part_amount",
            )
        }
    }
}

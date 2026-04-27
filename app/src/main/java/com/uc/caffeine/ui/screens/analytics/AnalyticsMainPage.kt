package com.uc.caffeine.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState
import kotlin.math.roundToInt

private data class AnalyticsNavItem(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AnalyticsMainPage(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onSourcesClick: () -> Unit,
    onBedtimeClick: () -> Unit,
    onTimeOfDayClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()

    CaffeineScreenScaffold(
        title = "Analytics",
    ) { bottomPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AnalyticsRangePicker(
                    selectedRange = uiState.selectedRange,
                    onRangeSelected = { range ->
                        haptics.toggle()
                        onRangeSelected(range)
                    },
                )
            }

            if (uiState.hasData) {
                item {
                    AnalyticsSummaryCard(uiState = uiState)
                }
                item {
                    AnalyticsNavCard(
                        onSourcesClick = {
                            haptics.navigation()
                            onSourcesClick()
                        },
                        onBedtimeClick = {
                            haptics.navigation()
                            onBedtimeClick()
                        },
                        onTimeOfDayClick = {
                            haptics.navigation()
                            onTimeOfDayClick()
                        },
                    )
                }
            } else {
                item {
                    AnalyticsEmptyState(selectedRange = uiState.selectedRange)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnalyticsNavCard(
    onSourcesClick: () -> Unit,
    onBedtimeClick: () -> Unit,
    onTimeOfDayClick: () -> Unit,
) {
    val items = listOf(
        AnalyticsNavItem(
            title = "Caffeine by Source",
            summary = "Where your caffeine came from over this period.",
            icon = Icons.Filled.PieChart,
            onClick = onSourcesClick,
        ),
        AnalyticsNavItem(
            title = "Bedtime Impact",
            summary = "How much caffeine is still active by the time you sleep.",
            icon = Icons.Filled.Bedtime,
            onClick = onBedtimeClick,
        ),
        AnalyticsNavItem(
            title = "When You Drink Caffeine",
            summary = "How your intake is spread across the day.",
            icon = Icons.Filled.Schedule,
            onClick = onTimeOfDayClick,
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            SegmentedListItem(
                onClick = item.onClick,
                leadingContent = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                    )
                },
                content = {
                    Text(text = item.title)
                },
                supportingContent = {
                    Text(text = item.summary)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                shapes = ListItemDefaults.segmentedShapes(
                    index = index,
                    count = items.size,
                ),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(uiState: AnalyticsUiState) {
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            DecorativeShapesBackground(
                color = contentColor,
                modifier = Modifier
                    .matchParentSize()
                    .clip(AnalyticsCardShape),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = analyticsHeadlineForRange(uiState.selectedRange),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "${uiState.totalCaffeineMg} mg",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                    Text(
                        text = "Total caffeine logged in this range",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.82f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryMetric(
                        label = "Average/day",
                        value = "${uiState.averageCaffeinePerDayMg} mg",
                        modifier = Modifier.weight(1f),
                        contentColor = contentColor,
                    )
                    SummaryMetric(
                        label = "Safe nights",
                        value = "${uiState.safeNights} / ${uiState.totalNights}",
                        modifier = Modifier.weight(1f),
                        contentColor = contentColor,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryMetric(
                        label = "Top source",
                        value = uiState.topSourceLabel,
                        modifier = Modifier.weight(1f),
                        contentColor = contentColor,
                    )
                    SummaryMetric(
                        label = "Sleep threshold",
                        value = "${uiState.sleepThresholdMg.roundToInt()} mg",
                        modifier = Modifier.weight(1f),
                        contentColor = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    contentColor: Color,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DecorativeShapesBackground(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shapeAlpha = 0.07f
    val transition = rememberInfiniteTransition(label = "morphShapes")

    // ─── Shape definitions ───────────────────────────────────────────────
    // Each decorative element morphs between two polygon forms instead of
    // drifting its offset position (which caused pixel-by-pixel jank).

    // Shape 1 — top-right, 12-sided polygon ↔ 12-pointed star
    val morph1 = remember {
        Morph(
            start = RoundedPolygon(numVertices = 12, rounding = CornerRounding(0.15f)),
            end = RoundedPolygon.star(numVerticesPerRadius = 12, rounding = CornerRounding(0.1f)),
        )
    }
    val progress1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "morph1",
    )
    val rotation1 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot1",
    )

    // Shape 2 — bottom-left, 6-sided polygon ↔ 6-pointed star
    val morph2 = remember {
        Morph(
            start = RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.2f)),
            end = RoundedPolygon.star(numVerticesPerRadius = 6, rounding = CornerRounding(0.12f)),
        )
    }
    val progress2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "morph2",
    )
    val rotation2 by transition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot2",
    )

    // Shape 3 — top-left, 5-sided polygon ↔ 8-sided polygon
    val morph3 = remember {
        Morph(
            start = RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.25f)),
            end = RoundedPolygon(numVertices = 8, rounding = CornerRounding(0.15f)),
        )
    }
    val progress3 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "morph3",
    )
    val rotation3 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot3",
    )

    // Shape 4 — bottom-right, 9-sided polygon ↔ 4-sided polygon
    val morph4 = remember {
        Morph(
            start = RoundedPolygon(numVertices = 9, rounding = CornerRounding(0.18f)),
            end = RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)),
        )
    }
    val progress4 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "morph4",
    )
    val rotation4 by transition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(32000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot4",
    )

    // ─── Drawing ─────────────────────────────────────────────────────────
    // All shapes are drawn in a single drawWithCache pass — no per-frame
    // layout recomposition, just GPU path rendering.
    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .drawWithCache {
                val morphColor = color.copy(alpha = shapeAlpha)

                // Pre-compute morph paths for each shape
                val path1 = buildMorphPath(morph1, progress1, rotation1, 110.dp.toPx())
                val path2 = buildMorphPath(morph2, progress2, rotation2, 90.dp.toPx())
                val path3 = buildMorphPath(morph3, progress3, rotation3, 70.dp.toPx())
                val path4 = buildMorphPath(morph4, progress4, rotation4, 75.dp.toPx())

                onDrawBehind {
                    // Top-right — pulled left & down so it doesn't clip into the rounded corner
                    translate(left = size.width - 110.dp.toPx() + 10.dp.toPx(), top = -10.dp.toPx()) {
                        drawPath(path1, morphColor)
                    }
                    // Bottom-left — pulled right & up
                    translate(left = 5.dp.toPx(), top = size.height - 90.dp.toPx() + 5.dp.toPx()) {
                        drawPath(path2, morphColor)
                    }
                    // Top-left — pulled right & down
                    translate(left = 10.dp.toPx(), top = 5.dp.toPx()) {
                        drawPath(path3, morphColor)
                    }
                    // Bottom-right — pulled well away from the corner
                    translate(left = size.width - 75.dp.toPx() + 10.dp.toPx(), top = size.height - 75.dp.toPx() + 5.dp.toPx()) {
                        drawPath(path4, morphColor)
                    }
                }
            },
    )
}

/**
 * Build a Compose [Path] from a [Morph] at the given [progress] and [rotation],
 * scaled to fit within [sizePx] × [sizePx].
 */
private fun buildMorphPath(
    morph: Morph,
    progress: Float,
    rotation: Float,
    sizePx: Float,
): androidx.compose.ui.graphics.Path {
    val matrix = Matrix()
    matrix.scale(sizePx / 2f, sizePx / 2f)
    matrix.translate(1f, 1f)
    matrix.rotateZ(rotation)
    val path = morph.toPath(progress = progress).asComposePath()
    path.transform(matrix)
    return path
}

private fun analyticsHeadlineForRange(range: AnalyticsRange): String = when (range) {
    AnalyticsRange.TODAY -> "Today"
    AnalyticsRange.YESTERDAY -> "Yesterday"
    AnalyticsRange.LAST_30_DAYS -> "Last 30 days"
    AnalyticsRange.LAST_90_DAYS -> "Last 90 days"
    AnalyticsRange.CUSTOM -> "Custom range"
}


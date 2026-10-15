package com.uc.caffeine.ui.screens.analytics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.DrinkIcon
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState
import com.uc.caffeine.util.CategoryIcons
import com.uc.caffeine.util.CategoryUtils
import com.uc.caffeine.util.OtherCategoryKey
import com.uc.caffeine.util.SourceItemEntry
import java.text.NumberFormat
import kotlin.math.roundToInt

private enum class SourceView {
    CATEGORY,
    ITEM,
}

internal const val AnalyticsSourceBarTag = "analytics_source_bar"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AnalyticsBySourcePage(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRange: (java.time.LocalDate, java.time.LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var selectedView by remember { mutableStateOf(SourceView.CATEGORY) }
    var showRangeSheet by remember { mutableStateOf(false) }
    val slices = remember(uiState.sourceCategoryKeys, uiState.sourceAxisLabels, uiState.sourceValues) {
        uiState.sourceSlices()
    }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    // A range without the picked category simply shows the total again.
    val selectedSlice = slices.firstOrNull { it.key == selectedSource }
    val onSelectSource: (String) -> Unit = { key ->
        haptics.toggle()
        selectedSource = key.takeIf { it != selectedSlice?.key }
    }

    SettingsPageScaffold(title = stringResource(R.string.analytics_caffeine_by_source), showBackButton = true, onBack = onBack) { bottomPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                val rangeLabel = if (uiState.selectedRange == AnalyticsRange.CUSTOM &&
                    uiState.customStartDate != null && uiState.customEndDate != null) {
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    stringResource(R.string.analytics_custom_range_format, uiState.customStartDate.format(fmt), uiState.customEndDate.format(fmt))
                } else {
                    stringResource(uiState.selectedRange.labelRes)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AnalyticsCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Range selector — less-rounded chip (medium vs card's extraLarge)
                        Surface(
                            onClick = { showRangeSheet = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 0.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = rangeLabel,
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.analytics_change_range_cd),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        if (uiState.hasData) {
                            VerticalDivider(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                ToggleButton(
                                    checked = selectedView == SourceView.CATEGORY,
                                    onCheckedChange = { if (it && selectedView != SourceView.CATEGORY) { haptics.toggle(); selectedView = SourceView.CATEGORY } },
                                    modifier = Modifier.weight(1f),
                                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.DonutLarge, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(stringResource(R.string.analytics_view_category), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                ToggleButton(
                                    checked = selectedView == SourceView.ITEM,
                                    onCheckedChange = { if (it && selectedView != SourceView.ITEM) { haptics.toggle(); selectedView = SourceView.ITEM } },
                                    modifier = Modifier.weight(1f),
                                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.LocalCafe, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(stringResource(R.string.analytics_view_item), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.hasData) {

                when (selectedView) {
                    SourceView.CATEGORY -> {
                        item {
                            SourceBreakdownCard(
                                slices = slices,
                                selected = selectedSlice,
                                onSelect = onSelectSource,
                            )
                        }
                        item {
                            SourceCategoryList(
                                slices = slices,
                                selectedKey = selectedSlice?.key,
                                onSelect = onSelectSource,
                            )
                        }
                    }

                    SourceView.ITEM -> {
                        item {
                            DrinkCollage(items = uiState.sourceItemEntries)
                        }
                        item {
                            SourceItemList(items = uiState.sourceItemEntries)
                        }
                    }
                }
            } else {
                item {
                    AnalyticsEmptyState(selectedRange = uiState.selectedRange)
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

/** One category's part of the period's caffeine. */
private data class SourceSlice(
    val key: String,
    val label: String,
    val mg: Double,
    val share: Float,
)

/** Slices in the app's category order with "Other" last, which is the order the bar's colors were checked in. */
private fun AnalyticsUiState.sourceSlices(): List<SourceSlice> {
    val total = sourceValues.sum()
    if (total <= 0.0) return emptyList()
    return sourceCategoryKeys.mapIndexedNotNull { index, key ->
        val mg = sourceValues.getOrNull(index) ?: return@mapIndexedNotNull null
        SourceSlice(key, sourceAxisLabels.getOrElse(index) { key }, mg, (mg / total).toFloat())
    }
}

/** Whole percents, with a decimal under 10% so a small source doesn't read as 0%. */
private fun formatShare(share: Float): String =
    NumberFormat.getPercentInstance()
        .apply { maximumFractionDigits = if (share < 0.1f) 1 else 0 }
        .format(share)

/**
 * Each category keeps its own color in every range rather than cycling theme colors, whose
 * primary, secondary and tertiary can sit only a few degrees apart under dynamic color. The hues
 * are a colorblind-checked set, assigned so neighbours in the bar stay distinguishable even with a
 * category missing between them; the "Other" neutral clears every hue. Light to dark pairs.
 */
private val SourceColors = mapOf(
    "coffee" to (Color(0xFFE87BA4) to Color(0xFFD55181)),
    "energy_drink" to (Color(0xFFEDA100) to Color(0xFFC98500)),
    "tea" to (Color(0xFF6250D6) to Color(0xFF9085E9)),
    "soft_drink" to (Color(0xFF1BAF7A) to Color(0xFF199E70)),
    "chocolate" to (Color(0xFFEB6834) to Color(0xFFD95926)),
    "pill" to (Color(0xFF2A78D6) to Color(0xFF3987E5)),
    OtherCategoryKey to (Color(0xFF5D5F66) to Color(0xFFBFC1C7)),
)

@Composable
private fun sourceColor(key: String): Color {
    val (light, dark) = SourceColors[key] ?: SourceColors.getValue(OtherCategoryKey)
    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) dark else light
}

/** A shape per category as well as a color, so the badges tell categories apart without hue. */
@Composable
private fun sourceShape(key: String): Shape = when (key) {
    "coffee" -> MaterialShapes.Cookie9Sided
    "energy_drink" -> MaterialShapes.SoftBurst
    "tea" -> MaterialShapes.Flower
    "soft_drink" -> MaterialShapes.Arch
    "chocolate" -> MaterialShapes.Cookie6Sided
    "pill" -> MaterialShapes.Pill
    else -> MaterialShapes.Circle
}.toShape()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceBreakdownCard(
    slices: List<SourceSlice>,
    selected: SourceSlice?,
    onSelect: (String) -> Unit,
) {
    val totalMg = slices.sumOf { it.mg }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = CaffeineSurfaceDefaults.chartContainerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.analytics_source_breakdown),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            // The total, or the tapped category's part of it.
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) using SizeTransform(clip = false)
                },
                contentKey = { it?.key },
                label = "source-hero",
            ) { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row {
                        Text(
                            text = NumberFormat.getIntegerInstance().format((shown?.mg ?: totalMg).roundToInt()),
                            style = MaterialTheme.typography.displayMediumEmphasized,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.unit_mg),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Text(
                        text = if (shown == null) {
                            stringResource(R.string.analytics_total_caffeine_logged)
                        } else {
                            "${shown.label} · ${formatShare(shown.share)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            SourceShareBar(
                slices = slices,
                selectedKey = selected?.key,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val ShareBarHeight = 24.dp
private val ShareBarGap = 3.dp
private val ShareBarInnerCorner = 4.dp
private val ShareBarMinSegment = 6.dp

private class ShareBarSegment(val share: State<Float>, val color: Color, val alpha: State<Float>)

/**
 * The period as one pill split by category, in the fixed category order: rounded outer ends, small
 * inner corners and gaps, like a connected button group. It fills in on arrival, reflows when the
 * range changes, and tapping a segment picks that category.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceShareBar(
    slices: List<SourceSlice>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shareSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val alphaSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val segments = slices.map { slice ->
        key(slice.key) {
            // Starts empty, so the bar fills in and a category new to this range grows in.
            val share = remember { Animatable(0f) }
            LaunchedEffect(slice.share) { share.animateTo(slice.share, shareSpec) }
            val alpha = animateFloatAsState(
                targetValue = if (selectedKey == null || selectedKey == slice.key) 1f else 0.3f,
                animationSpec = alphaSpec,
                label = "source-segment-alpha",
            )
            ShareBarSegment(share.asState(), sourceColor(slice.key), alpha)
        }
    }
    val description = slices.joinToString { "${it.label} ${formatShare(it.share)}" }

    Canvas(
        modifier = modifier
            .height(ShareBarHeight)
            .testTag(AnalyticsSourceBarTag)
            .semantics { contentDescription = description }
            .pointerInput(slices) {
                detectTapGestures { tap ->
                    val gap = ShareBarGap.toPx()
                    val widths = segmentWidths(slices.map { it.share }, size.width.toFloat(), gap, ShareBarMinSegment.toPx())
                    var end = 0f
                    for (index in widths.indices) {
                        if (widths[index] == 0f) continue
                        end += widths[index] + gap
                        if (tap.x < end - gap / 2) {
                            onSelect(slices[index].key)
                            break
                        }
                    }
                }
            },
    ) {
        val gap = ShareBarGap.toPx()
        val widths = segmentWidths(segments.map { it.share.value }, size.width, gap, ShareBarMinSegment.toPx())
        val outer = CornerRadius(size.height / 2)
        val inner = CornerRadius(ShareBarInnerCorner.toPx())
        val showing = widths.indices.filter { widths[it] > 0f }
        var x = 0f
        fun drawPiece(width: Float, roundStart: Boolean, roundEnd: Boolean, color: Color) {
            val start = if (roundStart) outer else inner
            val end = if (roundEnd) outer else inner
            val path = Path().apply {
                addRoundRect(RoundRect(Rect(x, 0f, x + width, size.height), start, end, end, start))
            }
            drawPath(path, color)
        }
        showing.forEachIndexed { order, index ->
            val segment = segments[index]
            drawPiece(
                width = widths[index],
                roundStart = order == 0,
                roundEnd = order == showing.lastIndex,
                color = segment.color.copy(alpha = segment.color.alpha * segment.alpha.value),
            )
            x += widths[index] + gap
        }
        // While the bar fills, what's left of it shows as track.
        if (size.width - x > ShareBarMinSegment.toPx()) {
            drawPiece(size.width - x, roundStart = showing.isEmpty(), roundEnd = true, color = trackColor)
        }
    }
}

/**
 * Widths of the bar's segments for [shares] of the whole, with [gap] between the ones showing.
 * Once the bar has room, every non-zero share is at least [minWidth] wide, taken from the larger
 * ones, so a sliver like "Other" can still be seen and tapped.
 */
private fun segmentWidths(shares: List<Float>, width: Float, gap: Float, minWidth: Float): List<Float> {
    val showing = shares.count { it > 0.0005f }
    val available = (width - gap * (showing - 1).coerceAtLeast(0)).coerceAtLeast(0f)
    val raw = shares.map { if (it > 0.0005f) it * available else 0f }
    val shortfall = raw.filter { it > 0f && it < minWidth }.sumOf { (minWidth - it).toDouble() }.toFloat()
    val roomy = raw.filter { it >= minWidth }.sum()
    if (shortfall == 0f || roomy <= shortfall) return raw
    return raw.map { w ->
        when {
            w == 0f -> 0f
            w < minWidth -> minWidth
            else -> w - shortfall * w / roomy
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceCategoryList(
    slices: List<SourceSlice>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
) {
    // Biggest first; "Other" stays at the bottom whatever its size.
    val ranked = remember(slices) {
        slices.sortedWith(compareBy<SourceSlice> { it.key == OtherCategoryKey }.thenByDescending { it.mg })
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.analytics_categories),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            ranked.forEachIndexed { index, slice ->
                SegmentedListItem(
                    onClick = { onSelect(slice.key) },
                    leadingContent = { SourceBadge(slice.key) },
                    content = {
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = formatShare(slice.share),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    trailingContent = {
                        Text(
                            text = stringResource(R.string.analytics_value_mg, slice.mg.roundToInt()),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = ranked.size,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = if (slice.key == selectedKey) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            CaffeineSurfaceDefaults.groupedListContainerColor
                        },
                    ),
                    modifier = Modifier.heightIn(min = 65.dp),
                )

                if (index < ranked.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(key: String) {
    val color = sourceColor(key)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(sourceShape(key))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CategoryIcons.getIcon(CategoryUtils.getCategoryDisplayName(key)),
            contentDescription = null,
            // Whichever of near-black or white reads better on the category color.
            tint = if (color.luminance() > 0.18f) Color(0xFF1B1B1F) else Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DrinkCollage(items: List<SourceItemEntry>) {
    if (items.isEmpty()) return

    val collageShapes = listOf(
        MaterialShapes.Circle.toShape(),
        MaterialShapes.Arch.toShape(),
        MaterialShapes.Pill.toShape(),
        MaterialShapes.Pentagon.toShape(),
        MaterialShapes.Sunny.toShape(),
        MaterialShapes.Cookie7Sided.toShape(),
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Flower.toShape(),
        MaterialShapes.PixelCircle.toShape(),
        MaterialShapes.SoftBurst.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
    )

    val collageItems = buildList {
        while (size < 14) addAll(items)
    }.take(14)

    val cols = 4
    val rows = (collageItems.size + cols - 1) / cols
    val placements = remember(collageItems.map { it.imageName }) {
        collageItems.mapIndexed { index, item ->
            val rng = kotlin.random.Random(item.imageName.hashCode() + index)
            val gridCol = index % cols
            val gridRow = index / cols
            val cellX = (gridCol.toFloat() + rng.nextFloat() * 0.6f + 0.2f) / cols
            val cellY = (gridRow.toFloat() + rng.nextFloat() * 0.6f + 0.2f) / rows
            CollagePlacement(
                xFrac = cellX.coerceIn(0.05f, 0.95f),
                yFrac = cellY.coerceIn(0.05f, 0.95f),
                rotation = rng.nextFloat() * 40f - 20f,
                sizeFrac = 0.4f + rng.nextFloat() * 0.6f,
                shapeIndex = rng.nextInt(collageShapes.size),
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(312.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                .clearAndSetSemantics {},
        ) {
            val containerW = maxWidth
            val containerH = maxHeight
            val minSize = 44.dp
            val maxSize = 72.dp
            val shapeColor = MaterialTheme.colorScheme.tertiary
            val itemColors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
            )

            // Decorative background shapes
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = containerW - 60.dp, y = (-20).dp)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(shapeColor.copy(alpha = 0.10f)),
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-15).dp, y = containerH - 80.dp)
                    .clip(MaterialShapes.Cookie6Sided.toShape())
                    .background(shapeColor.copy(alpha = 0.10f)),
            )
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .offset(x = containerW - 90.dp, y = containerH - 50.dp)
                    .clip(MaterialShapes.Arch.toShape())
                    .background(shapeColor.copy(alpha = 0.10f)),
            )

            collageItems.forEachIndexed { index, item ->
                val p = placements[index]
                val itemShape = collageShapes[p.shapeIndex]
                val itemSize = minSize + (maxSize - minSize) * p.sizeFrac
                val xOffset = (containerW - itemSize) * p.xFrac
                val yOffset = (containerH - itemSize) * p.yFrac

                val itemBgColor = itemColors[index % itemColors.size]

                Box(
                    modifier = Modifier
                        .size(itemSize + 6.dp)
                        .offset(x = xOffset, y = yOffset)
                        .graphicsLayer { rotationZ = p.rotation }
                        .clip(itemShape)
                        .background(itemBgColor)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DrinkIcon(
                        imageName = item.imageName,
                        emoji = item.emoji,
                        contentDescription = item.name,
                        modifier = Modifier.size(itemSize).clip(itemShape),
                        emojiSize = MaterialTheme.typography.headlineSmall.fontSize,
                    )
                }
            }
        }
    }
}

private data class CollagePlacement(
    val xFrac: Float,
    val yFrac: Float,
    val rotation: Float,
    val sizeFrac: Float,
    val shapeIndex: Int = 0,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceItemList(items: List<SourceItemEntry>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.analytics_recent_consumptions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                SegmentedListItem(
                    onClick = {},
                    leadingContent = {
                        ExpressiveIconBadge(
                            index = item.name.hashCode(),
                            size = 44.dp,
                        ) {
                            DrinkIcon(
                                imageName = item.imageName,
                                emoji = item.emoji,
                                contentDescription = item.name,
                                modifier = Modifier.size(28.dp),
                                emojiSize = MaterialTheme.typography.titleLarge.fontSize,
                            )
                        }
                    },
                    content = {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(
                                if (item.count == 1) R.string.analytics_servings_one else R.string.analytics_servings_other,
                                item.count,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    trailingContent = {
                        Text(
                            text = stringResource(R.string.analytics_value_mg, item.totalCaffeineMg),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = items.size,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                    ),
                    modifier = Modifier.heightIn(min = 65.dp),
                )

                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

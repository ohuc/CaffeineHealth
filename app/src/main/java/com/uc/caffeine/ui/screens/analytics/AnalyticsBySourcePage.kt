package com.uc.caffeine.ui.screens.analytics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.PieValueFormatter
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.uc.caffeine.ui.components.DrinkIcon
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState
import com.uc.caffeine.util.SourceItemEntry
import kotlin.math.roundToInt

private enum class SourceView(val label: String) {
    CATEGORY("Category"),
    ITEM("Item"),
}

internal const val AnalyticsSingleSliceChartTag = "analytics_single_slice_chart"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AnalyticsBySourcePage(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRange: (java.time.LocalDate, java.time.LocalDate) -> Unit,
    onBack: () -> Unit,
    modelProducer: PieChartModelProducer,
) {
    val haptics = rememberAppHaptics()
    var selectedView by remember { mutableStateOf(SourceView.CATEGORY) }
    var showRangeSheet by remember { mutableStateOf(false) }
    val isSingleCategory = uiState.sourceValues.size == 1 && uiState.sourceAxisLabels.size == 1

    val sliceColors = with(MaterialTheme.colorScheme) {
        listOf(primary, secondary, tertiary, error, outline)
    }
    val sliceLabelTextComponent = rememberTextComponent(
        style = MaterialTheme.typography.labelSmall.copy(
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold,
        ),
    )
    val percentFormatter = remember {
        PieValueFormatter { context, value, _ ->
            val sum = context.model.sum
            if (sum > 0f) "${(value / sum * 100f).roundToInt()}%" else "\u200B"
        }
    }
    val pieChart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(
            sliceColors.map { color ->
                PieChart.Slice(
                    fill = Fill(color),
                    label = PieChart.SliceLabel.Inside(textComponent = sliceLabelTextComponent),
                )
            },
        ),
        spacing = 0.dp,
        valueFormatter = percentFormatter,
    )

    SettingsPageScaffold(title = "Caffeine by Source", showBackButton = true, onBack = onBack) { bottomPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                val rangeLabel = if (uiState.selectedRange == AnalyticsRange.CUSTOM &&
                    uiState.customStartDate != null && uiState.customEndDate != null) {
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    "${uiState.customStartDate.format(fmt)} – ${uiState.customEndDate.format(fmt)}"
                } else {
                    uiState.selectedRange.label
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AnalyticsCardShape,
                    colors = CardDefaults.elevatedCardColors(
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
                        // Range selector — less-rounded chip (12dp vs card's 28dp)
                        Surface(
                            onClick = { showRangeSheet = true },
                            shape = RoundedCornerShape(12.dp),
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
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Change range",
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
                                        Icon(Icons.Outlined.DonutLarge, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Category", style = MaterialTheme.typography.labelMedium)
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
                                        Icon(Icons.Outlined.LocalCafe, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Item", style = MaterialTheme.typography.labelMedium)
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
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AnalyticsCardShape,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = CaffeineSurfaceDefaults.chartContainerColor,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Source breakdown",
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                        Text(
                                            text = "Total caffeine by category for the selected period.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isSingleCategory) {
                                        SingleCategoryPieFallback(
                                            color = sliceColors.first(),
                                            modifier = Modifier.height(240.dp),
                                        )
                                    } else {
                                        PieChartHost(
                                            chart = pieChart,
                                            modelProducer = modelProducer,
                                            animationSpec = null,
                                            animateIn = false,
                                            modifier = Modifier.height(240.dp),
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AnalyticsCardShape,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "Categories",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    uiState.sourceAxisLabels.forEachIndexed { index, label ->
                                        val value = uiState.sourceValues.getOrNull(index) ?: 0.0
                                        val color = sliceColors[index % sliceColors.size]
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(color),
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Text(
                                                text = "${value.roundToInt()} mg",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
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

@Composable
private fun SingleCategoryPieFallback(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AnalyticsSingleSliceChartTag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "100%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AnalyticsCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(312.dp)
                .clip(RoundedCornerShape(28.dp))
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
            text = "Recent Consumptions",
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
                            text = "${item.count} serving${if (item.count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    trailingContent = {
                        Text(
                            text = "${item.totalCaffeineMg} mg",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
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

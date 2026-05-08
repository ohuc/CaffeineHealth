package com.uc.caffeine.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.uc.caffeine.R
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.AnalyticsRange
import kotlin.math.min
import kotlin.math.roundToInt

internal val AnalyticsCardShape = RoundedCornerShape(28.dp)
internal val AnalyticsColumnShape = RoundedCornerShape(14.dp)
internal val SleepThresholdColor = Color(0xFF6FC06B)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AnalyticsRangePicker(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit,
) {
    val mainPageRanges = remember { AnalyticsRange.entries.filter { it != AnalyticsRange.CUSTOM } }
    val row1 = mainPageRanges.subList(0, 2)  // Today, Yesterday
    val row2 = mainPageRanges.subList(2, 4)  // 30 Days, 90 Days

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        // Row 1: Today, Yesterday
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            row1.forEachIndexed { index, range ->
                ToggleButton(
                    checked = selectedRange == range,
                    onCheckedChange = { checked ->
                        if (checked && selectedRange != range) onRangeSelected(range)
                    },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(range.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        // Row 2: 30 Days, 90 Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            row2.forEachIndexed { index, range ->
                ToggleButton(
                    checked = selectedRange == range,
                    onCheckedChange = { checked ->
                        if (checked && selectedRange != range) onRangeSelected(range)
                    },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(range.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsRangeButton(
    selectedRange: AnalyticsRange,
    customStartDate: java.time.LocalDate? = null,
    customEndDate: java.time.LocalDate? = null,
    onClick: () -> Unit,
) {
    val label = if (selectedRange == AnalyticsRange.CUSTOM && customStartDate != null && customEndDate != null) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
        stringResource(R.string.analytics_custom_range_format, customStartDate.format(formatter), customEndDate.format(formatter))
    } else {
        stringResource(selectedRange.labelRes)
    }

    ElevatedCard(
        onClick = onClick,
        shape = AnalyticsCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.analytics_change_range_cd),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AnalyticsRangeBottomSheet(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRange: (java.time.LocalDate, java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { java.time.LocalDate.now() }
    var customStart by remember { mutableStateOf(today.minusDays(29)) }
    var customEnd by remember { mutableStateOf(today) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = customStart.toEpochDay() * 86400000L,
    )
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = customEnd.toEpochDay() * 86400000L,
    )
    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy") }

    val quickSelectColors = ToggleButtonDefaults.toggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.analytics_quick_select),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )

            // Row 1: Today, Yesterday, 30 Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                listOf(AnalyticsRange.TODAY, AnalyticsRange.YESTERDAY, AnalyticsRange.LAST_30_DAYS)
                    .forEachIndexed { index, range ->
                        ToggleButton(
                            checked = selectedRange == range,
                            onCheckedChange = { checked ->
                                if (checked) { onRangeSelected(range); onDismiss() }
                            },
                            modifier = Modifier.weight(1f),
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            colors = quickSelectColors,
                        ) {
                            Text(text = stringResource(range.labelRes), style = MaterialTheme.typography.labelLarge)
                        }
                    }
            }

            // Row 2: 90 Days, Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                listOf(AnalyticsRange.LAST_90_DAYS, AnalyticsRange.CUSTOM)
                    .forEachIndexed { index, range ->
                        ToggleButton(
                            checked = selectedRange == range,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (range != AnalyticsRange.CUSTOM) {
                                        onRangeSelected(range)
                                        onDismiss()
                                    } else {
                                        onRangeSelected(range)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            },
                            colors = quickSelectColors,
                        ) {
                            Text(text = stringResource(range.labelRes), style = MaterialTheme.typography.labelLarge)
                        }
                    }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.analytics_custom_range_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            RangePickerDateField(
                label = stringResource(R.string.analytics_from),
                date = customStart,
                formatter = dateFormatter,
                onClick = { showStartPicker = true },
            )

            RangePickerDateField(
                label = stringResource(R.string.analytics_to),
                date = customEnd,
                formatter = dateFormatter,
                onClick = { showEndPicker = true },
            )

            Button(
                onClick = {
                    onCustomRange(customStart, customEnd)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.analytics_apply_custom_range))
            }
        }
    }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { millis ->
                        customStart = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showStartPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { millis ->
                        customEnd = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showEndPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

@Composable
private fun RangePickerDateField(
    label: String,
    date: java.time.LocalDate,
    formatter: java.time.format.DateTimeFormatter,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = date.format(formatter),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AnalyticsEmptyState(
    selectedRange: AnalyticsRange,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_empty_state"),
        shape = AnalyticsCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.analytics_nothing_to_chart),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.analytics_empty_body, stringResource(selectedRange.labelRes).lowercase()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun AnalyticsChartCard(
    title: String,
    supportingText: String,
    content: @Composable () -> Unit,
) {
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
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .clip(RoundedCornerShape(22.dp)),
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun AnalyticsColumnChart(
    axisLabels: List<String>,
    modelProducer: CartesianChartModelProducer,
    columnProvider: ColumnCartesianLayer.ColumnProvider,
    modifier: Modifier = Modifier,
    decorations: List<Decoration> = emptyList(),
) {
    val colorScheme = MaterialTheme.colorScheme
    val labelComponent = rememberTextComponent(
        style = MaterialTheme.typography.labelSmall.copy(
            color = colorScheme.onSurfaceVariant,
        ),
    )
    val yAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            if (value.roundToInt() == 0) "\u200B" else value.roundToInt().toString()
        }
    }
    val xAxisFormatter = remember(axisLabels) {
        CartesianValueFormatter { _, value, _ ->
            axisLabels.getOrNull(value.roundToInt()) ?: "\u200B"
        }
    }
    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = columnProvider,
            columnCollectionSpacing = 18.dp,
        ),
        startAxis = VerticalAxis.rememberStart(
            valueFormatter = yAxisFormatter,
            label = labelComponent,
            guideline = null,
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = xAxisFormatter,
            label = labelComponent,
            guideline = null,
            itemPlacer = remember {
                HorizontalAxis.ItemPlacer.aligned(
                    spacing = { 1 },
                    addExtremeLabelPadding = true,
                )
            },
        ),
        decorations = decorations,
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        animationSpec = null,
        animateIn = false,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
internal fun SleepImpactChart(
    axisLabels: List<String>,
    modelProducer: CartesianChartModelProducer,
    sleepThresholdMg: Double,
) {
    val safeColumn = rememberColumnComponent(
        color = MaterialTheme.colorScheme.tertiary,
        width = if (axisLabels.size > 8) 10.dp else 14.dp,
    )
    val unsafeColumn = rememberColumnComponent(
        color = MaterialTheme.colorScheme.error,
        width = if (axisLabels.size > 8) 10.dp else 14.dp,
    )
    val columnProvider = remember(safeColumn, unsafeColumn, sleepThresholdMg) {
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(
                entry: com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel.Entry,
                seriesIndex: Int,
                extraStore: ExtraStore,
            ): LineComponent {
                return if (entry.y <= sleepThresholdMg) safeColumn else unsafeColumn
            }

            override fun getWidestSeriesColumn(
                seriesIndex: Int,
                extraStore: ExtraStore,
            ): LineComponent = unsafeColumn
        }
    }

    AnalyticsColumnChart(
        axisLabels = axisLabels,
        modelProducer = modelProducer,
        columnProvider = columnProvider,
        decorations = listOf(
            rememberSleepThresholdDecoration(thresholdLevel = sleepThresholdMg),
        ),
    )
}

@Composable
internal fun rememberSingleColumnProvider(
    color: Color,
    width: Dp,
): ColumnCartesianLayer.ColumnProvider {
    val column = rememberColumnComponent(
        color = color,
        width = width,
    )
    return remember(column) {
        ColumnCartesianLayer.ColumnProvider.series(column)
    }
}

@Composable
internal fun rememberColumnComponent(
    color: Color,
    width: Dp,
): LineComponent {
    return remember(color, width) {
        LineComponent(
            fill = Fill(color),
            thickness = width,
            shape = AnalyticsColumnShape,
        )
    }
}

@Composable
internal fun rememberSleepThresholdDecoration(
    thresholdLevel: Double,
): Decoration {
    val line = remember {
        LineComponent(
            fill = Fill(SleepThresholdColor.copy(alpha = 0.72f)),
            thickness = 1.dp,
            shape = RectangleShape,
        )
    }
    val labelComponent = rememberTextComponent(
        style = MaterialTheme.typography.labelSmall.copy(
            color = SleepThresholdColor,
        ),
    )

    val sleepThresholdLabel = stringResource(R.string.analytics_sleep_threshold)
    return remember(thresholdLevel, line, labelComponent, sleepThresholdLabel) {
        DashedHorizontalThresholdDecoration(
            y = { thresholdLevel },
            line = line,
            label = sleepThresholdLabel,
            labelComponent = labelComponent,
        )
    }
}

internal class DashedHorizontalThresholdDecoration(
    private val y: (ExtraStore) -> Double,
    private val line: LineComponent,
    private val label: CharSequence,
    private val labelComponent: TextComponent,
    private val dashLength: Dp = 6.dp,
    private val gapLength: Dp = 6.dp,
) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            val yRange = ranges.getYRange(null)
            val canvasY = layerBounds.bottom -
                ((y(model.extraStore) - yRange.minY) / yRange.length).toFloat() * layerBounds.height

            var segmentLeft = layerBounds.left
            val dashLengthPx = dashLength.pixels
            val gapLengthPx = gapLength.pixels

            while (segmentLeft < layerBounds.right) {
                val segmentRight = min(segmentLeft + dashLengthPx, layerBounds.right)
                line.drawHorizontal(
                    context = this,
                    left = segmentLeft,
                    right = segmentRight,
                    y = canvasY,
                )
                segmentLeft = segmentRight + gapLengthPx
            }

            labelComponent.draw(
                context = this,
                text = label,
                x = layerBounds.left + 6.dp.pixels,
                y = canvasY - (line.thickness.pixels / 2f),
                horizontalPosition = Position.Horizontal.Start,
                verticalPosition = Position.Vertical.Bottom,
                maxWidth = layerBounds.width.toInt(),
            )
        }
    }
}

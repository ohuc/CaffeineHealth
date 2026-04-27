package com.uc.caffeine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uc.caffeine.LocalSnackbarHostState
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.ui.components.CaffeineChart
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.components.ConsumptionContributionChart
import com.uc.caffeine.ui.components.ConsumptionTimingSection
import com.uc.caffeine.ui.components.DrinkIcon
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.ServingQuantityStepper
import com.uc.caffeine.ui.components.ServingUnitSelector
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.shimmerEffect
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.ui.viewmodel.HomeScreenUiEvent
import com.uc.caffeine.util.ConsumptionContributionDetail
import com.uc.caffeine.util.calculateServingTotalCaffeine
import com.uc.caffeine.util.findMatchingUnit
import com.uc.caffeine.util.formatConsumptionDateHeader
import com.uc.caffeine.util.formatDurationMinutes
import com.uc.caffeine.util.formatServingSummary
import com.uc.caffeine.util.formatTimeOfDay
import com.uc.caffeine.util.formatTimestampToTime
import com.uc.caffeine.util.resolvedZoneId
import java.time.LocalDate
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

private const val DetailContentFadeInDurationMillis = 180
private const val DetailContentFadeInDelayMillis = 40
private const val DetailContentFadeOutDurationMillis = 80


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen(
    viewModel: CaffeineViewModel = viewModel()
) {
    // Grab the global snackbar instance!
    val snackbarHostState = LocalSnackbarHostState.current

    val currentLevel by viewModel.currentCaffeineLevel.collectAsStateWithLifecycle()
    val liveNowMillis by viewModel.liveCurrentTimeMillis.collectAsStateWithLifecycle()
    val bedtimeForecast by viewModel.caffeineAtBedtime.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val isConsumptionEntriesLoading by viewModel.isConsumptionEntriesLoading.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val groupedConsumptionEntries by viewModel.groupedConsumptionEntries.collectAsStateWithLifecycle()

    var selectedEntry by remember { mutableStateOf<ConsumptionEntry?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = rememberAppHaptics()

    LaunchedEffect(viewModel, snackbarHostState, sheetState) {
        viewModel.homeScreenEvents.collectLatest { event ->
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            showEditDialog = false
            selectedEntry = null
            snackbarHostState.currentSnackbarData?.dismiss()

            when (event) {
                is HomeScreenUiEvent.LogActionCompleted -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    CaffeineScreenScaffold(
        title = "Home"
    ) { bottomPadding ->
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = CaffeineSurfaceDefaults.chartContainerColor,
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isConsumptionEntriesLoading) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    CaffeineChart(
                        chartData = chartData,
                        modelProducer = viewModel.chartModelProducer,
                        userSettings = userSettings,
                        liveNowMillis = liveNowMillis,
                        currentCaffeineLevel = currentLevel,
                        predictedBedtimeCaffeineLevel = bedtimeForecast.first,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "My Consumptions",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
        ) {
            if (isConsumptionEntriesLoading) {
                item(key = "history-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ElevatedCard {
                            Box(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }
                }
            } else if (groupedConsumptionEntries.isEmpty()) {
                item(key = "history-empty") {
                    Text(
                        text = "No consumptions logged yet",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                groupedConsumptionEntries.entries.forEachIndexed { index, (date, entriesForDay) ->
                    if (index > 0) {
                        item(
                            key = "history-gap-$date",
                            contentType = "history-gap",
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    stickyHeader(key = "history-header-$date") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = formatTimelineHeaderText(
                                    date = date,
                                    settings = userSettings,
                                    referenceTimeMillis = chartData.currentTimeMillis,
                                ),
                                modifier = Modifier.padding(bottom = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    entriesForDay.forEachIndexed { entryIndex, entry ->
                        item(
                            key = "history-entry-${entry.id}",
                            contentType = "history-entry",
                        ) {
                            ConsumptionHistoryListItem(
                                entry = entry,
                                index = entryIndex,
                                count = entriesForDay.size,
                                userSettings = userSettings,
                                onClick = {
                                    haptics.navigation()
                                    selectedEntry = entry
                                },
                                modifier = Modifier.heightIn(min = 65.dp),
                            )
                        }

                        if (entryIndex < entriesForDay.lastIndex) {
                            item(
                                key = "history-entry-gap-${entry.id}",
                                contentType = "history-entry-gap",
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        val detailSnapshotTimeMillis = remember(entry.id) {
            chartData.currentTimeMillis
        }
        val canRevealDetailContent = true

        val detail by produceState<ConsumptionContributionDetail?>(
            initialValue = null,
            key1 = entry.id,
            key2 = userSettings,
        ) {
            value = withContext(Dispatchers.Default) {
                viewModel.getContributionDetail(
                    entry = entry,
                    currentTimeMillis = detailSnapshotTimeMillis
                )
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                showEditDialog = false
                selectedEntry = null
            },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ConsumptionLogDetailSheet(
                entry = entry,
                detail = detail,
                canRevealDetailContent = canRevealDetailContent,
                userSettings = userSettings,
                onEdit = { showEditDialog = true },
                onDuplicate = { viewModel.duplicateLoggedEntry(entry) },
                onDelete = { viewModel.deleteLoggedEntry(entry) }
            )
        }

        if (showEditDialog) {
            EditConsumptionEntryDialog(
                entry = entry,
                viewModel = viewModel,
                userSettings = userSettings,
                onDismiss = { showEditDialog = false },
                onSave = { quantity, unit, startedAtMillis, durationMinutes ->
                    viewModel.updateLoggedEntry(
                        entry = entry,
                        quantity = quantity,
                        unit = unit,
                        startedAtMillis = startedAtMillis,
                        durationMinutes = durationMinutes,
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConsumptionHistoryListItem(
    entry: ConsumptionEntry,
    index: Int,
    count: Int,
    userSettings: UserSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        leadingContent = {
            ExpressiveIconBadge(
                index = index,
                size = 44.dp,
            ) {
                DrinkIcon(
                    imageName = entry.imageName,
                    emoji = entry.emoji,
                    contentDescription = entry.drinkName,
                    modifier = Modifier.size(28.dp),
                    emojiSize = MaterialTheme.typography.titleLarge.fontSize,
                )
            }
        },
        content = {
            Text(
                text = entry.drinkName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = buildLoggedEntryMetaText(entry, userSettings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Text(
                text = "${entry.caffeineMg}mg",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        },
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = count,
        ),
        colors = ListItemDefaults.colors(
            containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
        ),
    )
}

@Composable
private fun SleepForecastCard(
    caffeineAtBedtimeMg: Double,
    userSettings: UserSettings,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                caffeineAtBedtimeMg < userSettings.sleepThresholdMg ->
                    MaterialTheme.colorScheme.primaryContainer
                caffeineAtBedtimeMg < userSettings.sleepThresholdMg * 1.5 ->
                    MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    caffeineAtBedtimeMg < userSettings.sleepThresholdMg -> Icons.Default.CheckCircle
                    caffeineAtBedtimeMg < userSettings.sleepThresholdMg * 1.5 -> Icons.Default.Warning
                    else -> Icons.Default.Cancel
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sleep Forecast",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = when {
                        caffeineAtBedtimeMg < userSettings.sleepThresholdMg ->
                            "Safe to sleep at ${formatBedtime(userSettings)}"
                        caffeineAtBedtimeMg < userSettings.sleepThresholdMg * 1.5 ->
                            "May affect sleep (${caffeineAtBedtimeMg.toInt()}mg at bedtime)"
                        else ->
                            "Sleep disruption likely (${caffeineAtBedtimeMg.toInt()}mg)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ConsumptionLogDetailSheet(
    entry: ConsumptionEntry,
    detail: ConsumptionContributionDetail?,
    canRevealDetailContent: Boolean,
    userSettings: UserSettings,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = rememberAppHaptics()
    val presentedDetail = detail.takeIf { canRevealDetailContent }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveIconBadge(
                    index = entry.id,
                    size = 76.dp,
                ) {
                    DrinkIcon(
                        imageName = entry.imageName,
                        emoji = entry.emoji,
                        contentDescription = entry.drinkName,
                        modifier = Modifier.size(48.dp),
                        emojiSize = MaterialTheme.typography.headlineLarge.fontSize
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.drinkName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Started ${buildLoggedEntryMetaText(entry, userSettings)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedContent(
                        targetState = presentedDetail,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = DetailContentFadeInDurationMillis,
                                    delayMillis = DetailContentFadeInDelayMillis,
                                )
                            ) togetherWith fadeOut(
                                animationSpec = tween(durationMillis = DetailContentFadeOutDurationMillis)
                            )
                        },
                        label = "detail-sheet-summary"
                    ) { targetDetail ->
                        if (targetDetail == null) {
                            SkeletonSummaryLine()
                        } else {
                            Text(
                                text = "Adds ${formatPreciseMg(targetDetail.currentContributionMg)} now",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = presentedDetail,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = DetailContentFadeInDurationMillis,
                        delayMillis = DetailContentFadeInDelayMillis,
                    )
                ) togetherWith fadeOut(
                    animationSpec = tween(durationMillis = DetailContentFadeOutDurationMillis)
                )
            },
            label = "detail-sheet-body"
        ) { targetDetail ->
            if (targetDetail == null) {
                SkeletonDetailSheetBody()
            } else {
                ConsumptionLogDetailSheetBody(
                    entry = entry,
                    detail = targetDetail,
                    userSettings = userSettings,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            SheetActionButton(
                modifier = Modifier.weight(1f),
                index = 0,
                count = 3,
                icon = Icons.Default.Edit,
                label = "Edit",
                enabled = presentedDetail != null,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    haptics.navigation()
                    onEdit()
                }
            )
            SheetActionButton(
                modifier = Modifier.weight(1f),
                index = 1,
                count = 3,
                icon = Icons.Default.ContentCopy,
                label = "Duplicate",
                onClick = {
                    haptics.navigation()
                    onDuplicate()
                }
            )
            SheetActionButton(
                modifier = Modifier.weight(1f),
                index = 2,
                count = 3,
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                onClick = {
                    haptics.navigation()
                    onDelete()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ConsumptionLogDetailSheetBody(
    entry: ConsumptionEntry,
    detail: ConsumptionContributionDetail,
    userSettings: UserSettings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            color = CaffeineSurfaceDefaults.detailPanelContainerColor,
            shape = MaterialTheme.shapes.large
        ) {
            ConsumptionContributionChart(
                detail = detail,
                userSettings = userSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Drink contribution to caffeine levels",
                style = MaterialTheme.typography.titleMedium
            )
            ContributionStatRow(
                label = "Serving",
                value = formatLoggedServing(entry)
            )
            ContributionStatRow(
                label = "Started at",
                value = formatLoggedTime(entry.startedAtMillis, userSettings)
            )
            ContributionStatRow(
                label = "Time to finish",
                value = formatDurationMinutes(entry.normalizedDurationMinutes)
            )
            ContributionStatRow(
                label = "At peak (${formatLoggedTime(detail.peakTimestampMillis, userSettings)})",
                value = formatPreciseMg(detail.peakContributionMg)
            )
            ContributionStatRow(
                label = "Now",
                value = formatPreciseMg(detail.currentContributionMg)
            )
            ContributionStatRow(
                label = "In total (over time)",
                value = formatPreciseMg(detail.totalContributionMg)
            )
        }
    }
}

@Composable
private fun SkeletonSummaryLine() {
    SkeletonBlock(
        modifier = Modifier
            .fillMaxWidth(0.58f)
            .height(22.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SkeletonDetailSheetBody() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            color = CaffeineSurfaceDefaults.detailPanelContainerColor,
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp),
                    shape = MaterialTheme.shapes.medium
                )
                SkeletonBlock(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 4.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .height(2.dp),
                    shape = RoundedCornerShape(999.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 16.dp)
                        .width(2.dp)
                        .height(132.dp),
                    shape = RoundedCornerShape(999.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .height(24.dp),
                shape = RoundedCornerShape(12.dp)
            )
            SkeletonStatRow()
            SkeletonStatRow()
            SkeletonStatRow()
        }
    }
}

@Composable
private fun SkeletonStatRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBlock(
            modifier = Modifier
                .weight(1f)
                .padding(end = 56.dp)
                .height(18.dp),
            shape = RoundedCornerShape(10.dp)
        )
        SkeletonBlock(
            modifier = Modifier
                .width(72.dp)
                .height(18.dp),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    shape: Shape,
) {
    Box(
        modifier = modifier.shimmerEffect(shape)
    )
}

@Composable
private fun ContributionStatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SheetActionButton(
    modifier: Modifier = Modifier,
    index: Int,
    count: Int,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color? = null,
    containerColor: Color? = null,
    onClick: () -> Unit
) {
    val contentTint = tint ?: MaterialTheme.colorScheme.onSurface
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.surfaceVariant
    val buttonShape = when (index) {
        0 -> RoundedCornerShape(
            topStart = 36.dp,
            bottomStart = 36.dp,
            topEnd = 4.dp,
            bottomEnd = 4.dp,
        )
        count - 1 -> RoundedCornerShape(
            topStart = 4.dp,
            bottomStart = 4.dp,
            topEnd = 36.dp,
            bottomEnd = 36.dp,
        )
        else -> RoundedCornerShape(4.dp)
    }

    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = buttonShape,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = contentTint,
            disabledContainerColor = resolvedContainerColor.copy(alpha = 0.5f),
            disabledContentColor = contentTint.copy(alpha = 0.38f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentTint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditConsumptionEntryDialog(
    entry: ConsumptionEntry,
    viewModel: CaffeineViewModel,
    userSettings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (Int, DrinkUnit, Long, Int) -> Unit
) {
    val availableUnits by produceState<List<DrinkUnit>?>(initialValue = null, key1 = entry.id, key2 = entry.presetItemId) {
        value = viewModel.getUnitsForPresetItemId(entry.presetItemId)
    }
    var quantity by remember(entry.id) {
        mutableStateOf(entry.quantity.coerceAtLeast(1))
    }
    var startedAtMillis by remember(entry.id) {
        mutableStateOf(entry.startedAtMillis)
    }
    var durationMinutes by remember(entry.id) {
        mutableIntStateOf(entry.normalizedDurationMinutes)
    }
    val fallbackUnit = remember(entry) {
        if (entry.unitKey.isBlank()) {
            null
        } else {
            DrinkUnit(
                drinkId = 0,
                unitKey = entry.unitKey,
                caffeineMg = entry.unitCaffeineMg,
                milliliters = null,
                grams = null,
                isDefault = true,
            )
        }
    }
    val initialUnit = remember(availableUnits, entry.unitKey, entry.unitCaffeineMg, fallbackUnit) {
        val resolvedUnits = availableUnits.orEmpty()
        if (resolvedUnits.isEmpty()) {
            fallbackUnit
        } else {
            findMatchingUnit(resolvedUnits, entry.unitKey, entry.unitCaffeineMg)
        }
    }
    val displayedUnits = remember(availableUnits, fallbackUnit) {
        val resolvedUnits = availableUnits.orEmpty()
        if (resolvedUnits.isEmpty()) {
            listOfNotNull(fallbackUnit)
        } else {
            resolvedUnits
        }
    }
    var selectedUnitKey by remember(entry.id, availableUnits) {
        mutableStateOf(initialUnit?.unitKey)
    }
    val selectedUnit = remember(displayedUnits, selectedUnitKey, initialUnit) {
        displayedUnits.firstOrNull { it.unitKey == selectedUnitKey } ?: initialUnit
    }
    val totalCaffeineMg = remember(quantity, selectedUnit) {
        selectedUnit?.let { calculateServingTotalCaffeine(quantity, it.caffeineMg) } ?: entry.caffeineMg
    }
    val isSaveEnabled = selectedUnit != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit ${entry.drinkName}")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (availableUnits == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ContainedLoadingIndicator()
                    }
                } else {
                    Text(
                        text = "Serving",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ServingQuantityStepper(
                        quantity = quantity,
                        onDecrement = { quantity = (quantity - 1).coerceAtLeast(1) },
                        onIncrement = { quantity += 1 },
                    )
                    RollingNumberText(
                        text = "$totalCaffeineMg mg",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        labelPrefix = "edit_entry_total",
                    )
                    if (displayedUnits.isEmpty()) {
                        Text(
                            text = "Serving options are unavailable for this entry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ServingUnitSelector(
                            units = displayedUnits,
                            selectedUnit = selectedUnit,
                            onUnitSelected = { selectedUnitKey = it.unitKey },
                        )
                    }
                }
                ConsumptionTimingSection(
                    startedAtMillis = startedAtMillis,
                    durationMinutes = durationMinutes,
                    settings = userSettings,
                    onStartedAtChange = { startedAtMillis = it },
                    onDurationChange = { durationMinutes = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isSaveEnabled,
                onClick = {
                    selectedUnit?.let { unit ->
                        onSave(
                            quantity,
                            unit,
                            startedAtMillis,
                            durationMinutes,
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBedtime(settings: UserSettings): String {
    return formatTimeOfDay(settings.sleepTimeHour, settings.sleepTimeMinute, settings)
}

private fun formatTimelineHeaderText(
    date: LocalDate,
    settings: UserSettings,
    referenceTimeMillis: Long,
): String {
    return formatConsumptionDateHeader(
        date = date,
        settings = settings,
        referenceTimeMillis = referenceTimeMillis
    )
}

private fun formatLoggedTime(
    timestampMillis: Long,
    settings: UserSettings
): String {
    return formatTimestampToTime(timestampMillis, settings)
}

private fun buildLoggedEntryMetaText(
    entry: ConsumptionEntry,
    settings: UserSettings,
): String {
    return buildString {
        append(formatLoggedTime(entry.startedAtMillis, settings))
        append(" • ")
        append(formatLoggedServing(entry))
        append(" • ")
        append(formatDurationMinutes(entry.normalizedDurationMinutes))
    }
}

private fun formatLoggedServing(entry: ConsumptionEntry): String {
    return if (entry.unitKey.isBlank()) {
        "${entry.caffeineMg}mg"
    } else {
        formatServingSummary(entry.quantity, entry.unitKey)
    }
}

private fun formatPreciseMg(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f mg", value)
}

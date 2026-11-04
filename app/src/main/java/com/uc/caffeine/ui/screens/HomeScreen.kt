package com.uc.caffeine.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import com.uc.caffeine.ui.components.DrinkListItem
import com.uc.caffeine.ui.components.SwipeRevealAction
import com.uc.caffeine.ui.components.SwipeRevealActions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import com.uc.caffeine.ui.components.DrinkListSupportingText
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
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
import androidx.compose.runtime.saveable.rememberSaveable
import com.uc.caffeine.ui.components.AdaptiveConnectedButtonGroup
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uc.caffeine.LocalSnackbarHostState
import com.uc.caffeine.R
import com.uc.caffeine.data.HomeViewMode
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.ui.components.CaffeineChart
import com.uc.caffeine.ui.components.CaffeineCircularView
import com.uc.caffeine.ui.components.CaffeineCoachChip
import com.uc.caffeine.ui.components.CaffeineCoachSheet
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.components.AdaptiveMainSupportingLayout
import com.uc.caffeine.ui.components.LocalPanePlacement
import com.uc.caffeine.ui.components.PanePlacement
import com.uc.caffeine.ui.components.ScreenHeaderMinHeight
import com.uc.caffeine.LocalAppScaffoldPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import com.uc.caffeine.ui.components.ConsumptionContributionChart
import com.uc.caffeine.ui.components.ConsumptionTimingSection
import com.uc.caffeine.ui.components.DrinkIcon
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.ServingQuantityStepper
import com.uc.caffeine.ui.components.ServingUnitSelector
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.WhatsNewCard
import com.uc.caffeine.ui.components.WhatsNewSheet
import com.uc.caffeine.ui.components.shimmerEffect
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.ui.viewmodel.CaffeineTrend
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.ui.viewmodel.HomeScreenUiEvent
import com.uc.caffeine.util.ConsumptionContributionDetail
import com.uc.caffeine.util.MAX_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.util.calculateNextBedtimeMillis
import com.uc.caffeine.util.calculateServingTotalCaffeine
import com.uc.caffeine.util.findMatchingUnit
import com.uc.caffeine.util.MIN_SERVING_QUANTITY
import com.uc.caffeine.util.formatConsumptionDateHeader
import com.uc.caffeine.util.formatDurationMinutes
import com.uc.caffeine.util.formatServingSummary
import com.uc.caffeine.util.formatTimestampToTime
import com.uc.caffeine.util.resolvedZoneId
import java.time.LocalDate
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ToggleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlinx.coroutines.withContext

/** Sticky graph pins only when at least this much list height stays visible below the chart. */
private val StickyChartMinListHeight = 240.dp

/**
 * In a split Home the chart fills its pane only when the pane also has this much room for the Plus
 * status and What's new cards beneath a chart of at least the phone height; shorter panes scroll.
 */
private val MainPaneCardsAllowance = 200.dp

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
    viewModel: CaffeineViewModel = viewModel(),
    onOpenPlus: () -> Unit = {},
    onOpenHealthConnect: () -> Unit = {},
) {
    val access by viewModel.accessSnapshot.collectAsStateWithLifecycle()
    // Keyed on the snapshot so Home re-checks access whenever it (or the lock policy) changes;
    // canUse() alone reads no Compose state and would leave Home showing a stale answer.
    val coachAllowed = remember(access) {
        viewModel.distributionServices.featureAccess.canUse(com.uc.caffeine.monetization.ProFeature.Coach)
    }
    // Grab the global snackbar instance!
    val snackbarHostState = LocalSnackbarHostState.current

    val currentLevel by viewModel.currentCaffeineLevel.collectAsStateWithLifecycle()
    val liveNowMillis by viewModel.liveCurrentTimeMillis.collectAsStateWithLifecycle()
    val bedtimeForecast by viewModel.caffeineAtBedtime.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val isConsumptionEntriesLoading by viewModel.isConsumptionEntriesLoading.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val groupedConsumptionEntries by viewModel.groupedConsumptionEntries.collectAsStateWithLifecycle()
    val showWhatsNew by viewModel.showWhatsNew.collectAsStateWithLifecycle()
    val caffeineTrend by viewModel.caffeineTrend.collectAsStateWithLifecycle()
    val coachRecommendation by viewModel.coachRecommendation.collectAsStateWithLifecycle()

    var selectedEntryId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedEntry = selectedEntryId?.let { id ->
        groupedConsumptionEntries.values.asSequence().flatten().firstOrNull { it.id == id }
    }
    var showCoachSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(coachAllowed) { if (!coachAllowed) showCoachSheet = false }
    var showWhatsNewSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = rememberAppHaptics()
    val listState = rememberLazyListState()
    // The history pane's own list when Home is split into two panes.
    val historyListState = rememberLazyListState()
    // Swipe actions: at most one row is open; a vertical scroll closes it.
    var swipedEntryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingDeleteEntryId by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(listState, historyListState) {
        snapshotFlow { listState.isScrollInProgress || historyListState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) swipedEntryId = null
        }
    }

    LaunchedEffect(viewModel, snackbarHostState, sheetState) {
        viewModel.homeScreenEvents.collectLatest { event ->
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            selectedEntryId = null
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

    val headerActions: @Composable RowScope.() -> Unit = {
        val modes = HomeViewMode.entries
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            modes.forEachIndexed { index, mode ->
                ToggleButton(
                    checked = userSettings.homeViewMode == mode,
                    onCheckedChange = { if (it) { haptics.toggle(); viewModel.updateHomeViewMode(mode) } },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Icon(
                        imageVector = when (mode) {
                            HomeViewMode.GRAPH -> Icons.AutoMirrored.Filled.ShowChart
                            HomeViewMode.CIRCULAR -> Icons.Filled.DonutLarge
                        },
                        contentDescription = when (mode) {
                            HomeViewMode.GRAPH -> stringResource(R.string.home_view_toggle_to_graph_cd)
                            HomeViewMode.CIRCULAR -> stringResource(R.string.home_view_toggle_to_circular_cd)
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    val chartHeight = 300.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    // The chart card's height comes from the caller: fixed in the phone list, filling the pane
    // in a side-by-side or tabletop layout.
    val chartCard: @Composable (Modifier) -> Unit = { cardModifier ->
        Card(
            modifier = cardModifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CaffeineSurfaceDefaults.chartContainerColor,
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isConsumptionEntriesLoading) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    AnimatedContent(
                        targetState = userSettings.homeViewMode,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90))) togetherWith
                                fadeOut(animationSpec = tween(90))
                        },
                        label = "home_view_mode",
                    ) { viewMode ->
                        when (viewMode) {
                            HomeViewMode.GRAPH -> CaffeineChart(
                                chartData = chartData,
                                modelProducer = viewModel.chartModelProducer,
                                userSettings = userSettings,
                                liveNowMillis = liveNowMillis,
                                currentCaffeineLevel = currentLevel,
                                predictedBedtimeCaffeineLevel = bedtimeForecast.first,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp),
                                onEntryClick = { entryId ->
                                    val entry = groupedConsumptionEntries.values
                                        .flatten()
                                        .find { it.id == entryId }
                                    if (entry != null) {
                                        haptics.navigation()
                                        selectedEntryId = entry.id
                                    }
                                }
                            )
                            HomeViewMode.CIRCULAR -> CaffeineCircularView(
                                currentMg = currentLevel,
                                maxMg = userSettings.sleepThresholdMg.toDouble(),
                                trend = caffeineTrend,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    // The Coach's slot beside the "My Consumptions" title.
    val coachSlot: @Composable (Modifier) -> Unit = { slotModifier ->
        val coachPill = coachRecommendation?.takeIf { coachAllowed }
        if (!coachAllowed) {
            // Free users see the Coach's place, locked; tapping explains Plus. Where Plus is not on
            // sale the slot stays empty: the Coach is simply off, with nothing nudging a purchase.
            if (access.plusOnSale) com.uc.caffeine.monetization.LockedCoachChip(
                expired = access.expired,
                expiryKey = access.paidExpiresAtMillis ?: access.previewEndsAtMillis,
                onClick = {
                    haptics.navigation()
                    viewModel.requestFeature(com.uc.caffeine.monetization.ProFeature.Coach)
                },
                modifier = slotModifier,
            )
        } else AnimatedVisibility(
            visible = coachPill != null,
            modifier = slotModifier,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            coachPill?.let { recommendation ->
                CaffeineCoachChip(
                    recommendation = recommendation,
                    userSettings = userSettings,
                    onClick = {
                        haptics.navigation()
                        showCoachSheet = true
                    },
                )
            }
        }
    }

    val plusStatusItem: LazyListScope.() -> Unit = {
        item(key = "home-plus-status") {
            viewModel.distributionServices.distributionUi.StatusCard(onOpen = onOpenPlus)
        }
    }

    val whatsNewItem: LazyListScope.() -> Unit = {
        if (showWhatsNew) {
            item(key = "home-whats-new", contentType = "home-whats-new") {
                Column(modifier = Modifier.animateItem()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    WhatsNewCard(
                        onOpen = {
                            haptics.navigation()
                            showWhatsNewSheet = true
                        },
                        onDismiss = {
                            haptics.toggle()
                            viewModel.markWhatsNewSeen()
                        },
                    )
                }
            }
        }
    }

    val historyTitleItem: LazyListScope.(topSpacing: Dp) -> Unit = { topSpacing ->
        item(key = "home-history-title", contentType = "home-history-title") {
            Column {
                Spacer(modifier = Modifier.height(topSpacing))
                // The Coach rides beside the title as a compact pill; FlowRow wraps it under
                // the title when a long translation or large font leaves no room.
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Pane header: as tall as the page header in the other pane, so they align.
                        .then(if (topSpacing == 0.dp) Modifier.heightIn(min = ScreenHeaderMinHeight) else Modifier),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_my_consumptions),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = 12.dp),
                    )
                    coachSlot(Modifier.align(Alignment.CenterVertically))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    val historyItems: LazyListScope.() -> Unit = {
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
                    text = stringResource(R.string.home_no_consumptions),
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
                                todayLabel = stringResource(R.string.home_history_today),
                                yesterdayLabel = stringResource(R.string.home_history_yesterday),
                            ),
                            modifier = Modifier.padding(bottom = 12.dp),
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
                        val colorScheme = MaterialTheme.colorScheme
                        val duplicateLabel = stringResource(R.string.home_action_duplicate)
                        val deleteLabel = stringResource(R.string.home_action_delete)
                        // Swipe right-to-left to reveal Duplicate and Delete (icons only).
                        SwipeRevealActions(
                            actions = listOf(
                                SwipeRevealAction(
                                    icon = Icons.Filled.ContentCopy,
                                    label = duplicateLabel,
                                    containerColor = colorScheme.secondaryContainer,
                                    contentColor = colorScheme.onSecondaryContainer,
                                ) {
                                    haptics.confirm()
                                    swipedEntryId = null
                                    viewModel.duplicateLoggedEntry(entry)
                                },
                                SwipeRevealAction(
                                    icon = Icons.Filled.Delete,
                                    label = deleteLabel,
                                    containerColor = colorScheme.errorContainer,
                                    contentColor = colorScheme.onErrorContainer,
                                ) {
                                    haptics.toggle()
                                    pendingDeleteEntryId = entry.id
                                },
                            ),
                            open = swipedEntryId == entry.id,
                            onOpenChange = { isOpen ->
                                if (isOpen) swipedEntryId = entry.id
                                else if (swipedEntryId == entry.id) swipedEntryId = null
                            },
                        ) { closeRow ->
                            // Same row as the Add Consumption catalogue (DrinkListItem).
                            DrinkListItem(
                                name = entry.drinkName,
                                imageName = entry.imageName,
                                emoji = entry.emoji,
                                caffeineMg = entry.caffeineMg,
                                index = entryIndex,
                                count = entriesForDay.size,
                                onClick = {
                                    if (closeRow != null) {
                                        haptics.toggle()
                                        closeRow()
                                    } else {
                                        haptics.navigation()
                                        selectedEntryId = entry.id
                                    }
                                },
                                supportingContent = {
                                    DrinkListSupportingText(buildLoggedEntryMetaText(entry, userSettings))
                                },
                            )
                        }
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

    val title = stringResource(R.string.home_title)
    AdaptiveMainSupportingLayout(
        splitTabletop = true,
        // Phone and medium windows: one scrolling list, exactly as before.
        single = {
            CaffeineScreenScaffold(title = title, actions = headerActions) { bottomPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                ) {
                    // Sticky graph pins the chart above the list. It only pins when the window still
                    // leaves room for the list below it: a pinned chart over a weighted list left
                    // almost no visible list on short screens or with large font/display scaling
                    // (#45), so those keep everything scrolling as one list.
                    val pinChart = userSettings.stickyHomeChart && maxHeight >= chartHeight + StickyChartMinListHeight
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (pinChart) {
                            chartCard(Modifier.height(chartHeight))
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true),
                            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
                        ) {
                            if (!pinChart) {
                                item(key = "home-chart", contentType = "home-chart") {
                                    chartCard(Modifier.height(chartHeight))
                                }
                            }
                            plusStatusItem()
                            whatsNewItem()
                            historyTitleItem(16.dp)
                            historyItems()
                        }
                    }
                }
            }
        },
        // Side by side (or above a tabletop hinge): the chart fills its pane, so the curve is as
        // tall as the window allows. The Plus status and What's new cards sit under it when the
        // pane is tall enough, and otherwise scroll below a chart of the phone height.
        main = {
            val placement = LocalPanePlacement.current
            CaffeineScreenScaffold(
                title = title,
                actions = headerActions,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = if (placement == PanePlacement.Start) 8.dp else 16.dp,
                    top = 12.dp,
                ),
            ) { bottomPadding ->
                val paneBottom = if (placement.touchesBottom) bottomPadding + 16.dp else 12.dp
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                ) {
                    if (maxHeight - paneBottom >= chartHeight + MainPaneCardsAllowance) {
                        Column(Modifier.fillMaxSize().padding(bottom = paneBottom)) {
                            chartCard(Modifier.weight(1f, fill = true))
                            LazyColumn(userScrollEnabled = false) {
                                plusStatusItem()
                                whatsNewItem()
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = paneBottom),
                        ) {
                            item(key = "home-chart", contentType = "home-chart") {
                                chartCard(Modifier.height(minOf(chartHeight, maxHeight - paneBottom).coerceAtLeast(160.dp)))
                            }
                            plusStatusItem()
                            whatsNewItem()
                        }
                    }
                }
            }
        },
        supporting = {
            val placement = LocalPanePlacement.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(placement.headerInsetSides))
                    .padding(
                        start = if (placement == PanePlacement.End) 8.dp else 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                    ),
            ) {
                LazyColumn(
                    state = historyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = LocalAppScaffoldPadding.current.calculateBottomPadding() + 16.dp),
                ) {
                    // No top spacing: the title lines up with the Home title in the other pane.
                    historyTitleItem(0.dp)
                    historyItems()
                }
            }
        },
    )

    pendingDeleteEntryId?.let { id ->
        val entry = groupedConsumptionEntries.values.asSequence().flatten().firstOrNull { it.id == id }
        if (entry == null) {
            LaunchedEffect(id) { pendingDeleteEntryId = null }
        } else {
            DeleteLoggedEntryDialog(
                drinkName = entry.drinkName,
                onConfirm = {
                    haptics.confirm()
                    pendingDeleteEntryId = null
                    swipedEntryId = null
                    viewModel.deleteLoggedEntry(entry)
                },
                // Cancel keeps the row open so Duplicate is still one tap away.
                onDismiss = { pendingDeleteEntryId = null },
            )
        }
    }

    if (showCoachSheet && coachAllowed) {
        coachRecommendation?.let { recommendation ->
            CaffeineCoachSheet(
                recommendation = recommendation,
                userSettings = userSettings,
                onLogDose = { doseMg, entryName ->
                    haptics.confirm()
                    viewModel.logCoachDose(doseMg, entryName)
                    showCoachSheet = false
                },
                onDismiss = { showCoachSheet = false },
                onConnectSleep = {
                    haptics.navigation()
                    showCoachSheet = false
                    onOpenHealthConnect()
                },
            )
        }
    }

    selectedEntry?.let { entry ->
        val detailSnapshotTimeMillis = rememberSaveable(entry.id) {
            chartData.currentTimeMillis
        }
        // Composing the Vico chart while the sheet's entrance spring is running drops
        // frames, so the skeleton stays up until the sheet settles. Latched (never reset
        // for the same entry) so the chart doesn't flicker back to skeleton on dismiss.
        var canRevealDetailContent by remember(entry.id) { mutableStateOf(false) }
        LaunchedEffect(entry.id, sheetState) {
            snapshotFlow {
                sheetState.currentValue == sheetState.targetValue &&
                    sheetState.currentValue != SheetValue.Hidden
            }.first { it }
            canRevealDetailContent = true
        }

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

        var isEditing by rememberSaveable(entry.id) { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { selectedEntryId = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "edit-mode-transition",
            ) { editing ->
                if (editing) {
                    EditConsumptionEntrySheet(
                        entry = entry,
                        viewModel = viewModel,
                        userSettings = userSettings,
                        onBack = { isEditing = false },
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
                } else {
                    ConsumptionLogDetailSheet(
                        entry = entry,
                        detail = detail,
                        canRevealDetailContent = canRevealDetailContent,
                        userSettings = userSettings,
                        onEdit = { isEditing = true },
                        onDuplicate = { viewModel.duplicateLoggedEntry(entry) },
                        onDelete = { viewModel.deleteLoggedEntry(entry) }
                    )
                }
            }
        }
    }

    if (showWhatsNewSheet) {
        WhatsNewSheet(
            onDismiss = {
                showWhatsNewSheet = false
                viewModel.markWhatsNewSeen()
            },
        )
    }
}

/** "Are you sure?" before a swipe-delete; Duplicate needs no confirmation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteLoggedEntryDialog(
    drinkName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ExpressiveIconBadge(
                    index = 0,
                    size = 56.dp,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Text(
                    text = stringResource(R.string.home_delete_entry_title, drinkName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = stringResource(R.string.home_delete_entry_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_cancel), textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = onConfirm,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.home_action_delete), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepForecastCard(
    caffeineAtBedtimeMg: Double,
    userSettings: UserSettings,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when {
        caffeineAtBedtimeMg < userSettings.sleepThresholdMg ->
            colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        caffeineAtBedtimeMg < userSettings.sleepThresholdMg * 1.5 ->
            colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        else -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
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
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sleep_forecast_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
                Text(
                    text = when {
                        caffeineAtBedtimeMg < userSettings.sleepThresholdMg ->
                            stringResource(R.string.sleep_forecast_safe, formatBedtime(userSettings))
                        caffeineAtBedtimeMg < userSettings.sleepThresholdMg * 1.5 ->
                            stringResource(R.string.sleep_forecast_may_affect, caffeineAtBedtimeMg.toInt())
                        else ->
                            stringResource(R.string.sleep_forecast_disruption, caffeineAtBedtimeMg.toInt())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
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
                        text = stringResource(R.string.home_started_meta, buildLoggedEntryMetaText(entry, userSettings)),
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
                                text = stringResource(R.string.home_adds_now, formatPreciseMg(targetDetail.currentContributionMg)),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = MaterialTheme.colorScheme.primary,
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

        AdaptiveConnectedButtonGroup(itemCount = 3) { index, shapes ->
            when (index) {
            0 ->
            SheetActionButton(
                shapes = shapes,
                icon = Icons.Default.Edit,
                label = stringResource(R.string.home_action_edit),
                enabled = presentedDetail != null,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    haptics.navigation()
                    onEdit()
                }
            )
            1 -> SheetActionButton(
                shapes = shapes,
                icon = Icons.Default.ContentCopy,
                label = stringResource(R.string.home_action_duplicate),
                onClick = {
                    haptics.navigation()
                    onDuplicate()
                }
            )
            else -> SheetActionButton(
                shapes = shapes,
                icon = Icons.Default.Delete,
                label = stringResource(R.string.home_action_delete),
                tint = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                onClick = {
                    haptics.navigation()
                    onDelete()
                }
            )
            }
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
                text = stringResource(R.string.home_drink_contribution_title),
                style = MaterialTheme.typography.titleMedium
            )
            ContributionStatRow(
                label = stringResource(R.string.home_at_peak, formatLoggedTime(detail.peakTimestampMillis, userSettings)),
                value = formatPreciseMg(detail.peakContributionMg)
            )
            ContributionStatRow(
                label = stringResource(R.string.home_now),
                value = formatPreciseMg(detail.currentContributionMg)
            )
            HorizontalDivider()
            ContributionStatRow(
                label = stringResource(R.string.home_in_total_over_time),
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
                    shape = CircleShape
                )
                SkeletonBlock(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 16.dp)
                        .width(2.dp)
                        .height(132.dp),
                    shape = CircleShape
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
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
    }
}

@Composable
private fun SheetActionButton(
    modifier: Modifier = Modifier,
    shapes: ToggleButtonShapes,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color? = null,
    containerColor: Color? = null,
    onClick: () -> Unit
) {
    val contentTint = tint ?: MaterialTheme.colorScheme.onSurface
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.surfaceVariant

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
        shapes = ButtonShapes(
            shape = shapes.shape,
            pressedShape = shapes.pressedShape,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = contentTint,
            disabledContainerColor = resolvedContainerColor.copy(alpha = 0.5f),
            disabledContentColor = contentTint.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
            tint = contentTint,
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditConsumptionEntrySheet(
    entry: ConsumptionEntry,
    viewModel: CaffeineViewModel,
    userSettings: UserSettings,
    onBack: () -> Unit,
    onSave: (Double, DrinkUnit, Long, Int) -> Unit
) {
    val availableUnits by produceState<List<DrinkUnit>?>(initialValue = null, key1 = entry.id, key2 = entry.presetItemId) {
        value = viewModel.getUnitsForPresetItemId(entry.presetItemId)
    }
    var quantity by rememberSaveable(entry.id) {
        mutableStateOf(entry.quantity.coerceAtLeast(MIN_SERVING_QUANTITY))
    }
    var startedAtMillis by rememberSaveable(entry.id) {
        mutableStateOf(entry.startedAtMillis)
    }
    var durationMinutes by rememberSaveable(entry.id) {
        mutableIntStateOf(entry.normalizedDurationMinutes)
    }
    val presetIsExtendedRelease by produceState(initialValue = false, key1 = entry.presetItemId) {
        value = viewModel.getPresetForItemId(entry.presetItemId)?.releaseMinutes != null
    }
    // A window longer than any time to finish can only be a release time (or an import); the
    // finish-time picker would silently clamp it to 3 hours, so it stays read-only.
    val isExtendedRelease = presetIsExtendedRelease ||
        entry.normalizedDurationMinutes > MAX_CONSUMPTION_DURATION_MINUTES
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
    // An entry can be logged weaker than its catalog serving (a later tea steep). Scaling every unit
    // by the strength it was logged at keeps an edit from quietly restoring full strength.
    val displayedUnits = remember(availableUnits, fallbackUnit, entry.unitKey, entry.unitCaffeineMg) {
        val resolvedUnits = availableUnits.orEmpty()
        if (resolvedUnits.isEmpty()) {
            listOfNotNull(fallbackUnit)
        } else {
            val catalogMg = resolvedUnits.firstOrNull { it.unitKey == entry.unitKey }?.caffeineMg
            val strength = catalogMg?.takeIf { it > 0.0 }?.let { entry.unitCaffeineMg / it } ?: 1.0
            if (abs(strength - 1.0) < 0.001) {
                resolvedUnits
            } else {
                resolvedUnits.map { it.copy(caffeineMg = it.caffeineMg * strength) }
            }
        }
    }
    val initialUnit = remember(availableUnits, displayedUnits, entry.unitKey, entry.unitCaffeineMg, fallbackUnit) {
        if (availableUnits.isNullOrEmpty()) {
            fallbackUnit
        } else {
            findMatchingUnit(displayedUnits, entry.unitKey, entry.unitCaffeineMg)
        }
    }
    var selectedUnitKey by rememberSaveable(entry.id) {
        mutableStateOf(initialUnit?.unitKey)
    }
    val selectedUnit = remember(displayedUnits, selectedUnitKey, initialUnit) {
        displayedUnits.firstOrNull { it.unitKey == selectedUnitKey } ?: initialUnit
    }
    val totalCaffeineMg = remember(quantity, selectedUnit) {
        selectedUnit?.let { calculateServingTotalCaffeine(quantity, it.caffeineMg) } ?: entry.caffeineMg
    }
    val isSaveEnabled = selectedUnit != null
    val haptics = rememberAppHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = {
                haptics.navigation()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.home_edit_drink, entry.drinkName),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                enabled = isSaveEnabled,
                onClick = {
                    selectedUnit?.let { unit ->
                        haptics.navigation()
                        onSave(quantity, unit, startedAtMillis, durationMinutes)
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        }

        if (availableUnits == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_serving),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ServingQuantityStepper(
                    quantity = quantity,
                    unitKey = selectedUnit?.unitKey ?: entry.unitKey,
                    onQuantityChange = { quantity = it },
                )
                RollingNumberText(
                    text = stringResource(R.string.caffeine_mg, totalCaffeineMg),
                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    labelPrefix = "edit_entry_total",
                )
                if (displayedUnits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.home_serving_unavailable),
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
        }

        ConsumptionTimingSection(
            startedAtMillis = startedAtMillis,
            durationMinutes = durationMinutes,
            settings = userSettings,
            onStartedAtChange = { startedAtMillis = it },
            onDurationChange = { durationMinutes = it },
            isExtendedRelease = isExtendedRelease,
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun formatBedtime(settings: UserSettings): String {
    val nextBedtimeMillis = calculateNextBedtimeMillis(System.currentTimeMillis(), settings)
    return formatTimestampToTime(nextBedtimeMillis, settings)
}

private fun formatTimelineHeaderText(
    date: LocalDate,
    settings: UserSettings,
    referenceTimeMillis: Long,
    todayLabel: String,
    yesterdayLabel: String,
): String {
    return formatConsumptionDateHeader(
        date = date,
        settings = settings,
        referenceTimeMillis = referenceTimeMillis,
        todayLabel = todayLabel,
        yesterdayLabel = yesterdayLabel,
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

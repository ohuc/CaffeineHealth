package com.uc.caffeine.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.uc.caffeine.LocalSnackbarHostState
import com.uc.caffeine.R
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.util.CaffeineCalculator
import com.uc.caffeine.util.calculateNextBedtimeMillis
import kotlin.math.roundToInt
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.components.ConsumptionTimingSection
import com.uc.caffeine.ui.components.ReleaseTimeSetting
import com.uc.caffeine.ui.components.DrinkDurationDefault
import com.uc.caffeine.util.defaultDurationFor
import com.uc.caffeine.ui.components.DrinkIcon
import com.uc.caffeine.ui.components.DrinkListBadgeSize
import com.uc.caffeine.ui.components.DrinkListImageSize
import com.uc.caffeine.ui.components.DrinkListItem
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.SegmentedListGroup
import com.uc.caffeine.ui.components.ServingQuantityStepper
import com.uc.caffeine.ui.components.ServingUnitSelector
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.ui.viewmodel.AddScreenUiEvent
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.util.calculateServingTotalCaffeine
import com.uc.caffeine.util.CategoryIcons
import com.uc.caffeine.util.CategoryUtils
import com.uc.caffeine.util.formatServingSummary
import com.uc.caffeine.util.formatUnitLabel
import kotlinx.coroutines.Job
import com.uc.caffeine.util.formatCaffeineAmount
import com.uc.caffeine.data.toCustomDrinkDraft
import com.uc.caffeine.data.ProductLookup
import com.uc.caffeine.data.CustomDrinkDraft
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.CloudOff
import com.uc.caffeine.util.canBeResteeped
import com.uc.caffeine.util.TeaSteep
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import com.uc.caffeine.ui.components.AdaptiveMainSupportingLayout
import com.uc.caffeine.ui.components.AdaptivePanePlaceholder
import com.uc.caffeine.ui.components.LocalPanePlacement
import com.uc.caffeine.ui.components.PanePlacement
import com.uc.caffeine.ui.components.rememberWindowLayoutPolicy
import com.uc.caffeine.LocalAppScaffoldPadding
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.uc.caffeine.ui.components.AdaptiveConnectedButtonGroup
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.uc.caffeine.ui.components.rememberImagePickerRequest
import com.uc.caffeine.ui.scanner.BarcodeScanner

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onDrinkLogged: () -> Unit,
    onFullScreenChange: (Boolean) -> Unit = {},
    viewModel: CaffeineViewModel = viewModel()
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val haptics = rememberAppHaptics()
    val resources = androidx.compose.ui.platform.LocalResources.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }

    val groupedDrinks by viewModel.groupedDrinkPresets.collectAsStateWithLifecycle()
    val drinkPresets by viewModel.drinkPresets.collectAsStateWithLifecycle()
    val recentDrinks by viewModel.recentDrinks.collectAsStateWithLifecycle()
    val isCatalogLoading by viewModel.isDrinkCatalogLoading.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val todayEntries by viewModel.todayEntries.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val categorizedDrinks = remember(groupedDrinks) {
        // Results come back re-ranked as you type. Without this the list stays anchored to
        // whichever drink led before and lands part-way down.
        listState.requestScrollToItem(0)
        groupedDrinks.entries.toList()
    }
    val showRecentServings = selectedFilter == null && searchQuery.isBlank() && recentDrinks.isNotEmpty()
    val categories = viewModel.getAvailableCategories()
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDrinkId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedDrink = drinkPresets.firstOrNull { it.id == selectedDrinkId }
    // Which custom drink's editor is open over its serving view (null: the serving view).
    var editingDrinkId by rememberSaveable { mutableStateOf<Int?>(null) }
    // Keeps an unfinished serving per drink by key, not by position, so it survives the move
    // between the bottom sheet and the side pane when the window is folded or resized.
    val servingStateHolder = rememberSaveableStateHolder()
    // Two-pane windows show the serving beside the catalog instead of in a bottom sheet.
    val servingInPane = rememberWindowLayoutPolicy().twoPane
    // Leaving a drink discards its unfinished serving and editor, so picking it again starts
    // fresh, as a newly opened sheet always did. A fold or rotation keeps the same drink and so
    // keeps the draft.
    var lastServingDrinkId by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(selectedDrinkId) {
        val previous = lastServingDrinkId
        if (previous != null && previous != selectedDrinkId) servingStateHolder.removeState("serving-$previous")
        if (editingDrinkId != selectedDrinkId) editingDrinkId = null
        lastServingDrinkId = selectedDrinkId
    }
    var showCustomDrinkSheet by rememberSaveable { mutableStateOf(false) }
    var customDraft by rememberSaveable(stateSaver = CustomDrinkDraftSaver) { mutableStateOf(CustomDrinkDraft()) }
    var customNotice by rememberSaveable { mutableStateOf<CustomDrinkNotice?>(null) }
    val customSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var onlineLookup by rememberSaveable(stateSaver = OnlineLookupSaver) { mutableStateOf<OnlineLookup?>(null) }
    var lookupJob by remember { mutableStateOf<Job?>(null) }
    // A scanned barcode that turned out to be a drink already here, kept on it once it's logged.
    var pendingBarcodeLink by rememberSaveable(stateSaver = BarcodeLinkSaver) { mutableStateOf<BarcodeLink?>(null) }
    val scope = rememberCoroutineScope()

    fun openCustomDrink(draft: CustomDrinkDraft, barcode: String? = null, notice: CustomDrinkNotice? = null) {
        customDraft = draft
        scannedBarcode = barcode
        customNotice = notice
        showCustomDrinkSheet = true
    }

    fun closeCustomDrink() {
        showCustomDrinkSheet = false
        customDraft = CustomDrinkDraft()
        scannedBarcode = null
        customNotice = null
    }

    // Only runs once the user has agreed to this lookup in the dialog.
    fun lookUpOnline(barcode: String) {
        onlineLookup = OnlineLookup(barcode, LookupPhase.Searching)
        lookupJob = scope.launch {
            when (val result = viewModel.lookUpProductOnline(barcode)) {
                is ProductLookup.Found -> {
                    val known = viewModel.findDrinkForProduct(result.product)
                    onlineLookup = null
                    if (known != null) {
                        pendingBarcodeLink = BarcodeLink(known.id, barcode)
                        selectedDrinkId = known.id
                        return@launch
                    }
                    val draft = result.product.toCustomDrinkDraft()
                    val notice = if (draft.caffeineMg != null) {
                        CustomDrinkNotice.FromOpenFoodFacts
                    } else {
                        CustomDrinkNotice.FromOpenFoodFactsWithoutCaffeine
                    }
                    openCustomDrink(draft, barcode, notice)
                }
                ProductLookup.NotFound -> {
                    onlineLookup = null
                    openCustomDrink(CustomDrinkDraft(), barcode, CustomDrinkNotice.NotOnOpenFoodFacts)
                }
                ProductLookup.Unreachable -> onlineLookup = OnlineLookup(barcode, LookupPhase.Failed)
            }
        }
    }

    BackHandler(enabled = scannerOpen) { scannerOpen = false }
    // The scanner covers the whole screen, floating toolbar included.
    DisposableEffect(scannerOpen) {
        onFullScreenChange(scannerOpen)
        onDispose { onFullScreenChange(false) }
    }

    LaunchedEffect(viewModel, snackbarHostState, sheetState, configuration) {
        viewModel.addScreenEvents.collectLatest { event ->
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            selectedDrinkId = null

            when (event) {
                is AddScreenUiEvent.CustomDrinkDeleted -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val message = resources.getString(R.string.custom_drink_deleted_toast, event.drinkName)
                    val undoLabel = resources.getString(R.string.action_undo)
                    val showUndo: suspend () -> Unit = {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = undoLabel,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            haptics.confirm()
                            viewModel.undoDeleteCustomDrink()
                        }
                    }
                    // Outlives this screen, so Undo still works if the user navigates away.
                    activity?.lifecycleScope?.launch { showUndo() } ?: showUndo()
                }
                is AddScreenUiEvent.DrinkLogged -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val message = resources.getString(R.string.add_logged_toast, event.drinkName)
                    activity?.lifecycleScope?.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    } ?: snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                    onDrinkLogged()
                }
            }
        }
    }

    // The serving view (or a custom drink's editor) for one drink. Shared by the bottom sheet on
    // phones and the supporting pane on wide windows; its state is kept per drink.
    val servingContent: @Composable (DrinkPreset) -> Unit = { drink ->
        servingStateHolder.SaveableStateProvider("serving-${drink.id}") {
            AnimatedContent(
                targetState = editingDrinkId == drink.id,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "edit-custom-drink-transition",
            ) { editing ->
                if (editing) {
                    EditCustomDrinkSheet(
                        preset = drink,
                        viewModel = viewModel,
                        onBack = { editingDrinkId = null },
                        onDone = { selectedDrinkId = null },
                    )
                } else {
                    AddDrinkServingSheet(
                        preset = drink,
                        viewModel = viewModel,
                        userSettings = userSettings,
                        todayEntries = todayEntries,
                        onAdd = { quantity, unit, startedAtMillis, durationMinutes, drinkName ->
                            haptics.confirm()
                            pendingBarcodeLink?.takeIf { it.presetId == drink.id }?.let {
                                viewModel.linkBarcode(it.presetId, it.barcode)
                            }
                            pendingBarcodeLink = null
                            viewModel.logDrinkFromAddScreen(
                                preset = drink,
                                quantity = quantity,
                                unit = unit,
                                startedAtMillis = startedAtMillis,
                                durationMinutes = durationMinutes,
                                drinkName = drinkName,
                            )
                        },
                        onEditCustomDrink = if (drink.isCustom) {
                            { editingDrinkId = drink.id }
                        } else null,
                    )
                }
            }
        }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
      val catalog: @Composable (PaddingValues) -> Unit = { catalogPadding ->
        CaffeineScreenScaffold(
            title = stringResource(R.string.add_title),
            headerBottomSpacing = 0.dp,
            contentPadding = catalogPadding,
            actions = {
                FilledIconButton(onClick = { showCustomDrinkSheet = true }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.add_create_custom_cd))
                }
            }
        ) { bottomPadding ->
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearch = { focusManager.clearFocus() },
                        expanded = false,
                        onExpandedChange = { },
                        placeholder = { Text(stringResource(R.string.add_search_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.add_search_cd)
                            )
                        },
                        trailingIcon = {
                            Row(
                                modifier = Modifier.padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        listState.requestScrollToItem(0)
                                        viewModel.updateSearchQuery("")
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = stringResource(R.string.add_clear_search_cd)
                                        )
                                    }
                                }
                                AnimatedVisibility(
                                    visible = !scannerOpen,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None,
                                ) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            haptics.navigation()
                                            focusManager.clearFocus()
                                            scannerOpen = true
                                        },
                                        modifier = Modifier.scannerMorph(this@SharedTransitionLayout, this, isScanner = false),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_barcode_scanner),
                                            contentDescription = stringResource(R.string.scanner_open_cd),
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
                expanded = false,
                onExpandedChange = { },
                modifier = Modifier.fillMaxWidth(),
                windowInsets = WindowInsets(0.dp),
                shape = RoundedCornerShape(16.dp),
                content = { }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val allLabel = stringResource(R.string.add_filter_all)
                val allOptions = listOf(allLabel) + categories

                allOptions.forEachIndexed { index, _ ->
                    val isAllButton = index == 0
                    val category = if (isAllButton) null else categories[index - 1]
                    val isChecked = if (isAllButton) {
                        selectedFilter == null
                    } else {
                        selectedFilter == category
                    }

                    ToggleButton(
                        checked = isChecked,
                        onCheckedChange = {
                            if (it) {
                                haptics.toggle()
                                listState.requestScrollToItem(0)
                                viewModel.selectCategoryFilter(category)
                            }
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            allOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Icon(
                            imageVector = if (isAllButton) {
                                CategoryIcons.getAllIcon()
                            } else {
                                CategoryIcons.getIcon(category!!)
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = if (isAllButton) allLabel else CategoryUtils.getCategoryButtonLabel(category!!),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showRecentServings) {
                Text(
                    text = stringResource(R.string.add_recent_servings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                SegmentedListGroup(
                    items = recentDrinks,
                    onItemClick = { recent ->
                        haptics.confirm()
                        viewModel.logRecentDrink(recent)
                    },
                    itemModifier = Modifier.heightIn(min = 72.dp),
                    leadingContent = { recent ->
                        ExpressiveIconBadge(
                            index = recent.quantity.toInt(),
                            size = DrinkBadgeSize,
                        ) {
                            DrinkIcon(
                                imageName = recent.imageName,
                                emoji = recent.emoji,
                                contentDescription = recent.drinkName,
                                modifier = Modifier.size(DrinkImageSize),
                                emojiSize = MaterialTheme.typography.headlineMedium.fontSize,
                            )
                        }
                    },
                    content = { recent ->
                        Text(
                            text = recent.drinkName,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = { recent ->
                        Text(
                            text = formatServingSummary(recent.quantity, recent.unitKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = { recent ->
                        Text(
                            text = stringResource(R.string.caffeine_mg_compact, recent.caffeineMg),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                if (isCatalogLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ElevatedCard {
                            Box(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }
                } else if (groupedDrinks.isEmpty()) {
                    EmptyDrinkResultsState(
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onClearFilters = {
                            listState.requestScrollToItem(0)
                            viewModel.selectCategoryFilter(null)
                            viewModel.updateSearchQuery("")
                            focusManager.clearFocus()
                        },
                        onCreateCustomDrink = { name ->
                            openCustomDrink(CustomDrinkDraft(name = name))
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
                    ) {
                        categorizedDrinks.forEachIndexed { index, entry ->
                            val (category, drinks) = entry

                            if (index > 0) {
                                item(
                                    key = "section-gap-$category",
                                    contentType = "section-gap",
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            stickyHeader(
                                key = "header-$category",
                                contentType = "header"
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            itemsIndexed(
                                items = drinks,
                                key = { _, drink -> drink.id },
                                contentType = { _, _ -> "drink" },
                            ) { drinkIndex, drink ->
                                SegmentedDrinkListItem(
                                    drink = drink,
                                    index = drinkIndex,
                                    count = drinks.size,
                                    onClick = {
                                        haptics.navigation()
                                        selectedDrinkId = drink.id
                                    },
                                    modifier = Modifier
                                        .animateItem()
                                        .heightIn(min = 65.dp),
                                )

                                if (drinkIndex < drinks.lastIndex) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
      }

        AdaptiveMainSupportingLayout(
            single = { catalog(PaddingValues(horizontal = 16.dp, vertical = 12.dp)) },
            main = { catalog(PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp)) },
            supporting = {
                AddServingPane(drink = selectedDrink, servingContent = servingContent)
            },
        )

        AnimatedVisibility(
            visible = scannerOpen,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            BarcodeScanner(
                onBarcode = { code ->
                    scannerOpen = false
                    scope.launch {
                        val match = viewModel.findDrinkByBarcode(code)
                        if (match != null) {
                            selectedDrinkId = match.id
                        } else {
                            onlineLookup = OnlineLookup(code, LookupPhase.Asking)
                        }
                    }
                },
                onClose = { scannerOpen = false },
                modifier = Modifier.scannerMorph(this@SharedTransitionLayout, this, isScanner = true),
            )
        }
    }

    if (!servingInPane) selectedDrink?.let { drink ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedDrinkId = null
                pendingBarcodeLink = null
            },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            servingContent(drink)
        }
    }

    onlineLookup?.let { lookup ->
        OnlineLookupDialog(
            lookup = lookup,
            onSearch = { lookUpOnline(lookup.barcode) },
            onAddManually = {
                onlineLookup = null
                openCustomDrink(CustomDrinkDraft(), lookup.barcode, CustomDrinkNotice.NewBarcode)
            },
            onDismiss = {
                lookupJob?.cancel()
                onlineLookup = null
            },
        )
    }

    if (showCustomDrinkSheet) {
        ModalBottomSheet(
            onDismissRequest = { closeCustomDrink() },
            sheetState = customSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CreateCustomDrinkSheet(
                viewModel = viewModel,
                draft = customDraft,
                barcode = scannedBarcode,
                notice = customNotice,
                onDismiss = { closeCustomDrink() },
            )
        }
    }
}

/**
 * The serving view as a persistent pane beside the catalog on two-pane windows. It shows a short
 * prompt until a drink is picked, and cross-fades when the drink changes.
 */
@Composable
private fun AddServingPane(
    drink: DrinkPreset?,
    servingContent: @Composable (DrinkPreset) -> Unit,
) {
    val placement = LocalPanePlacement.current
    val motion = MaterialTheme.motionScheme
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(placement.headerInsetSides))
            .padding(
                start = if (placement == PanePlacement.End) 8.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = LocalAppScaffoldPadding.current.calculateBottomPadding() + 16.dp,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        AnimatedContent(
            targetState = drink,
            contentKey = { it?.id },
            transitionSpec = {
                (fadeIn(motion.defaultEffectsSpec()) +
                    slideInVertically(motion.defaultSpatialSpec()) { it / 16 }) togetherWith
                    fadeOut(motion.fastEffectsSpec())
            },
            label = "add_serving_pane",
        ) { shown ->
            if (shown == null) {
                AdaptivePanePlaceholder(
                    title = stringResource(R.string.adaptive_add_placeholder_title),
                    message = stringResource(R.string.adaptive_add_placeholder),
                )
            } else {
                Box(Modifier.fillMaxSize().padding(top = 12.dp)) { servingContent(shown) }
            }
        }
    }
}

private enum class LookupPhase { Asking, Searching, Failed }

private data class OnlineLookup(val barcode: String, val phase: LookupPhase)

private data class BarcodeLink(val presetId: Int, val barcode: String)

private val CustomDrinkDraftSaver = listSaver<CustomDrinkDraft, String>(
    save = { listOf(it.name, it.category, it.unitKey, it.caffeineMg?.toString().orEmpty()) },
    restore = { CustomDrinkDraft(it[0], it[1], it[2], it[3].toDoubleOrNull()) },
)

private val OnlineLookupSaver = listSaver<OnlineLookup?, String>(
    save = { lookup -> lookup?.let { listOf(it.barcode, it.phase.name) }.orEmpty() },
    restore = { saved ->
        saved.takeIf { it.size == 2 }?.let {
            val phase = LookupPhase.valueOf(it[1])
            // The composition's lookup job cannot survive recreation. Keep the barcode and
            // offer Search again instead of restoring an unfinishable loading indicator.
            OnlineLookup(it[0], if (phase == LookupPhase.Searching) LookupPhase.Asking else phase)
        }
    },
)

private val BarcodeLinkSaver = listSaver<BarcodeLink?, String>(
    save = { link -> link?.let { listOf(it.presetId.toString(), it.barcode) }.orEmpty() },
    restore = { saved -> saved.takeIf { it.size == 2 }?.let { BarcodeLink(it[0].toInt(), it[1]) } },
)

/** Why the custom drink sheet opened pre-filled (or not) after a scan. */
private enum class CustomDrinkNotice {
    NewBarcode,
    FromOpenFoodFacts,
    FromOpenFoodFactsWithoutCaffeine,
    NotOnOpenFoodFacts,
}

/**
 * Asks before anything leaves the device: a scanned barcode that isn't one of the user's drinks is
 * only looked up on Open Food Facts once they agree, and the dialog carries the search through.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlineLookupDialog(
    lookup: OnlineLookup,
    onSearch: () -> Unit,
    onAddManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = lookup.phase,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "online-lookup-icon",
                ) { phase ->
                    when (phase) {
                        LookupPhase.Searching -> ContainedLoadingIndicator(
                            modifier = Modifier.size(LookupIconSize),
                        )
                        LookupPhase.Asking -> ExpressiveIconBadge(index = 0, size = LookupIconSize) {
                            Icon(
                                imageVector = Icons.Filled.TravelExplore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        LookupPhase.Failed -> ExpressiveIconBadge(
                            index = 0,
                            size = LookupIconSize,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(
                        when (lookup.phase) {
                            LookupPhase.Asking -> R.string.lookup_title
                            LookupPhase.Searching -> R.string.lookup_searching_title
                            LookupPhase.Failed -> R.string.lookup_failed_title
                        }
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )

                BarcodeChip(
                    barcode = lookup.barcode,
                    modifier = Modifier.padding(top = 12.dp),
                )

                val body = when (lookup.phase) {
                    LookupPhase.Asking -> R.string.lookup_body
                    LookupPhase.Failed -> R.string.lookup_failed_body
                    LookupPhase.Searching -> null
                }
                if (body != null) {
                    Text(
                        text = stringResource(body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                // Equal halves of one row, so the two choices never wrap onto separate lines.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (lookup.phase == LookupPhase.Searching) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onAddManually,
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = LookupActionPadding,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.lookup_add_manually), textAlign = TextAlign.Center)
                        }
                        Button(
                            onClick = onSearch,
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = LookupActionPadding,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(
                                    if (lookup.phase == LookupPhase.Failed) R.string.lookup_retry else R.string.lookup_search
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeChip(barcode: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.lookup_barcode_cd, barcode)
    Surface(
        shape = CircleShape,
        color = CaffeineSurfaceDefaults.iconContainerColor,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_barcode_scanner),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = barcode,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val LookupIconSize = 56.dp

// Trimmed from the 24dp default so both labels fit side by side on narrow phones.
private val LookupActionPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

@Composable
private fun AddDrinkServingSheet(
    preset: DrinkPreset,
    viewModel: CaffeineViewModel,
    userSettings: com.uc.caffeine.data.UserSettings,
    todayEntries: List<ConsumptionEntry>,
    onAdd: (quantity: Double, unit: DrinkUnit, startedAtMillis: Long, durationMinutes: Int, drinkName: String) -> Unit,
    onEditCustomDrink: (() -> Unit)? = null,
) {
    val units by produceState<List<DrinkUnit>?>(initialValue = null, key1 = preset.id) {
        value = viewModel.getUnitsForDrink(preset.id)
    }
    val frozenTodayEntries = remember { todayEntries }
    var quantity by rememberSaveable(preset.id) { mutableStateOf(1.0) }
    var startedAtMillis by rememberSaveable(preset.id) { mutableStateOf(System.currentTimeMillis()) }
    var durationMinutes by rememberSaveable(preset.id, preset.releaseMinutes) {
        mutableIntStateOf(userSettings.defaultDurationFor(preset))
    }
    val isExtendedRelease = preset.releaseMinutes != null
    // An extended-release drink's window is its release time, so there is no default to save.
    val drinkDurationDefault = preset.itemId.takeIf { it.isNotBlank() && !isExtendedRelease }?.let { itemId ->
        DrinkDurationDefault(
            drinkName = preset.name,
            isSet = itemId in userSettings.drinkDurationMinutes,
            onChange = { minutes -> viewModel.setDrinkDurationMinutes(itemId, minutes) },
        )
    }
    val defaultUnit = remember(units) {
        units?.firstOrNull { it.isDefault } ?: units?.firstOrNull()
    }
    var selectedUnitKey by rememberSaveable(preset.id) {
        mutableStateOf(defaultUnit?.unitKey)
    }
    val selectedUnit = remember(units, selectedUnitKey, defaultUnit) {
        units?.firstOrNull { it.unitKey == selectedUnitKey } ?: defaultUnit
    }
    val canResteep = remember(preset.category, units, selectedUnit) {
        canBeResteeped(preset.category, units.orEmpty().map { it.unitKey }, selectedUnit?.unitKey)
    }
    var steep by rememberSaveable(preset.id) { mutableStateOf(TeaSteep.First) }
    val effectiveSteep = if (canResteep) steep else TeaSteep.First
    // What actually gets logged: the serving scaled down for a later steep.
    val loggedUnit = remember(selectedUnit, effectiveSteep) {
        selectedUnit?.let { it.copy(caffeineMg = it.caffeineMg * effectiveSteep.caffeineFactor) }
    }
    val steepEntryName = when (effectiveSteep) {
        TeaSteep.First -> preset.name
        TeaSteep.Second -> stringResource(R.string.add_tea_steep_entry_name, preset.name, stringResource(R.string.add_tea_steep_second))
        TeaSteep.ThirdOrLater -> stringResource(R.string.add_tea_steep_entry_name, preset.name, stringResource(R.string.add_tea_steep_third))
    }
    val servingSummary = remember(quantity, selectedUnit, preset.defaultUnit) {
        formatServingSummary(
            quantity = quantity,
            unitKey = selectedUnit?.unitKey ?: preset.defaultUnit,
        )
    }
    val totalCaffeineMg = remember(quantity, loggedUnit) {
        loggedUnit?.let { calculateServingTotalCaffeine(quantity, it.caffeineMg) } ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveIconBadge(
                index = preset.id,
                size = 76.dp,
            ) {
                DrinkIcon(
                    imageName = preset.imageName,
                    emoji = preset.emoji,
                    contentDescription = preset.name,
                    modifier = Modifier.size(48.dp),
                    emojiSize = MaterialTheme.typography.headlineLarge.fontSize
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.add_log_serving_of, servingSummary),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        if (units == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator()
            }
        } else {
            ServingQuantityStepper(
                quantity = quantity,
                unitKey = selectedUnit?.unitKey ?: preset.defaultUnit,
                onQuantityChange = { quantity = it },
            )

            if (units.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.add_no_serving_options),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.add_choose_unit),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ServingUnitSelector(
                        units = units.orEmpty(),
                        selectedUnit = selectedUnit,
                        onUnitSelected = { selectedUnitKey = it.unitKey },
                    )
                }
            }

            AnimatedVisibility(visible = canResteep) {
                TeaSteepSelector(steep = steep, onSteepChange = { steep = it })
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.add_total_caffeine),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    RollingNumberText(
                        text = stringResource(R.string.caffeine_mg, totalCaffeineMg),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        labelPrefix = "add_sheet_total",
                    )
                }
            }

            val caffeineAtBedtimeMg = remember(
                totalCaffeineMg, startedAtMillis, durationMinutes, userSettings,
            ) {
                if (totalCaffeineMg == 0) return@remember 0.0
                val virtualEntry = ConsumptionEntry(
                    id = -1,
                    drinkName = preset.name,
                    caffeineMg = totalCaffeineMg,
                    emoji = preset.emoji,
                    absorptionRate = preset.absorptionRate,
                    startedAtMillis = startedAtMillis,
                    durationMinutes = durationMinutes,
                )
                val bedtimeMillis = calculateNextBedtimeMillis(System.currentTimeMillis(), userSettings)
                CaffeineCalculator.calculateCurrentLevel(
                    entries = frozenTodayEntries + virtualEntry,
                    currentTimeMillis = bedtimeMillis,
                    halfLifeMinutes = userSettings.effectiveHalfLifeMinutes,
                )
            }
            AnimatedVisibility(visible = caffeineAtBedtimeMg > userSettings.sleepThresholdMg) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.add_bedtime_warning,
                                caffeineAtBedtimeMg.roundToInt(),
                                userSettings.sleepThresholdMg,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
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
                drinkDurationDefault = drinkDurationDefault,
                isExtendedRelease = isExtendedRelease,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        loggedUnit?.let {
                            onAdd(quantity, it, startedAtMillis, durationMinutes, steepEntryName)
                        }
                    },
                    enabled = loggedUnit != null,
                    shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(
                        text = stringResource(R.string.add_entry),
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                    )
                }

                if (onEditCustomDrink != null) {
                    TextButton(
                        onClick = onEditCustomDrink,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.custom_drink_edit_button))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TeaSteepSelector(
    steep: TeaSteep,
    onSteepChange: (TeaSteep) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val steeps = listOf(
        TeaSteep.First to stringResource(R.string.add_tea_steep_first),
        TeaSteep.Second to stringResource(R.string.add_tea_steep_second),
        TeaSteep.ThirdOrLater to stringResource(R.string.add_tea_steep_third),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.add_tea_steep_label),
            style = MaterialTheme.typography.titleMedium,
        )
        AdaptiveConnectedButtonGroup(itemCount = steeps.size) { index, shapes ->
            val (value, label) = steeps[index]
                ToggleButton(
                    checked = steep == value,
                    onCheckedChange = {
                        if (it) {
                            haptics.toggle()
                            onSteepChange(value)
                        }
                    },
                    shapes = shapes,
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                }
        }
        Text(
            text = stringResource(R.string.add_tea_steep_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyDrinkResultsState(
    selectedFilter: String?,
    searchQuery: String,
    onClearFilters: () -> Unit,
    onCreateCustomDrink: (String) -> Unit,
) {
    val hasSearchQuery = searchQuery.isNotBlank()
    val hasActiveFilters = selectedFilter != null || hasSearchQuery
    val title = stringResource(
        if (hasActiveFilters) R.string.add_empty_no_drinks_found else R.string.add_empty_no_drinks_available
    )
    val message = when {
        selectedFilter != null && hasSearchQuery ->
            stringResource(R.string.add_empty_message_filter_search, searchQuery, selectedFilter)
        selectedFilter != null ->
            stringResource(R.string.add_empty_message_filter, selectedFilter)
        hasSearchQuery ->
            stringResource(R.string.add_empty_message_search, searchQuery)
        else ->
            stringResource(R.string.add_empty_message_default)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasSearchQuery) {
                FilledTonalButton(onClick = { onCreateCustomDrink(searchQuery) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.add_create_custom_drink_button))
                }
            } else if (hasActiveFilters) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.add_clear_filters))
                }
            }
        }
    }
}

// Drink photo sizes are shared with Home through DrinkListItem.kt.
private val DrinkBadgeSize = DrinkListBadgeSize
private val DrinkImageSize = DrinkListImageSize

@Composable
private fun DrinkSupportingContent(
    drink: DrinkPreset,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (drink.brand.isNotBlank()) {
            Text(
                text = drink.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatUnitLabel(drink.defaultUnit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun SegmentedDrinkListItem(
    drink: DrinkPreset,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DrinkListItem(
        name = drink.name,
        imageName = drink.imageName,
        emoji = drink.emoji,
        caffeineMg = drink.defaultCaffeineMg,
        index = index,
        count = count,
        onClick = onClick,
        supportingContent = { DrinkSupportingContent(drink = drink) },
        modifier = modifier,
    )
}

@Composable
private fun CreateCustomDrinkSheet(
    viewModel: CaffeineViewModel,
    draft: CustomDrinkDraft,
    barcode: String?,
    notice: CustomDrinkNotice?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    var name by rememberSaveable { mutableStateOf(draft.name) }
    var emoji by rememberSaveable { mutableStateOf("☕") }
    var imageUri by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(draft.category) }
    var selectedUnitKey by rememberSaveable { mutableStateOf(draft.unitKey) }
    var caffeineText by rememberSaveable { mutableStateOf(draft.caffeineMg?.let(::formatCaffeineAmount).orEmpty()) }
    var releaseMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    val imageRequest = rememberImagePickerRequest()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = viewModel.copyCustomDrinkImage(uri)
                if (path != null) imageUri = path
            }
        }
    }

    val caffeineValue = caffeineText.toDoubleOrNull()
    val isValid = name.isNotBlank() && caffeineValue != null && caffeineValue > 0

    val categoryKeys = CategoryUtils.getCategoryOrder()
    val unitKeys = listOf(
        "cup", "shot", "can", "bottle", "mug", "ml", "fl oz", "liter",
        "g", "pod", "teabag", "pill", "gummy", "piece", "bar", "scoop",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.clickable { pickImage.launch(imageRequest) }) {
                ExpressiveIconBadge(index = 0, size = 76.dp) {
                    DrinkIcon(
                        imageName = imageUri,
                        emoji = emoji.ifBlank { "☕" },
                        contentDescription = stringResource(R.string.custom_drink_image_picker_cd),
                        modifier = Modifier.size(48.dp),
                        emojiSize = MaterialTheme.typography.headlineLarge.fontSize,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.custom_drink_new),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = name.ifBlank { "—" },
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        if (notice != null && barcode != null) {
            Text(
                text = when (notice) {
                    CustomDrinkNotice.NewBarcode -> stringResource(R.string.custom_drink_new_barcode, barcode)
                    CustomDrinkNotice.FromOpenFoodFacts -> stringResource(R.string.custom_drink_from_open_food_facts)
                    CustomDrinkNotice.FromOpenFoodFactsWithoutCaffeine ->
                        stringResource(R.string.custom_drink_from_open_food_facts_no_caffeine)
                    CustomDrinkNotice.NotOnOpenFoodFacts -> stringResource(R.string.custom_drink_not_on_open_food_facts)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.custom_drink_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text(stringResource(R.string.custom_drink_emoji_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickImage.launch(imageRequest) }) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(if (imageUri.isBlank()) R.string.custom_drink_pick_image else R.string.custom_drink_change_image))
            }
            if (imageUri.isNotBlank()) {
                TextButton(onClick = { imageUri = "" }) {
                    Text(stringResource(R.string.custom_drink_remove))
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_drink_category), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                categoryKeys.forEachIndexed { index, key ->
                    ToggleButton(
                        checked = selectedCategory == key,
                        onCheckedChange = { if (it) { haptics.toggle(); selectedCategory = key } },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            categoryKeys.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(
                            text = CategoryUtils.getCategoryButtonLabel(
                                CategoryUtils.getCategoryDisplayName(key)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_drink_serving_unit), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                unitKeys.forEachIndexed { index, key ->
                    ToggleButton(
                        checked = selectedUnitKey == key,
                        onCheckedChange = { if (it) { haptics.toggle(); selectedUnitKey = key } },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            unitKeys.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(
                            text = formatUnitLabel(key),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_drink_caffeine_per_serving), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = caffeineText,
                onValueChange = { caffeineText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.custom_drink_amount_label)) },
                suffix = { Text(stringResource(R.string.unit_mg)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider()

        ReleaseTimeSetting(
            releaseMinutes = releaseMinutes,
            onReleaseMinutesChange = { releaseMinutes = it },
        )

        Button(
            onClick = {
                val caffeine = caffeineValue ?: return@Button
                haptics.confirm()
                viewModel.saveCustomDrink(
                    name = name.trim(),
                    emoji = emoji.ifBlank { "☕" },
                    imageUri = imageUri,
                    category = selectedCategory,
                    unitKey = selectedUnitKey,
                    caffeineMg = caffeine,
                    barcode = barcode,
                    releaseMinutes = releaseMinutes,
                )
                onDismiss()
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.custom_drink_save))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EditCustomDrinkSheet(
    preset: DrinkPreset,
    viewModel: CaffeineViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    var showDeleteDialog by rememberSaveable(preset.id) { mutableStateOf(false) }

    val existingUnit by produceState<DrinkUnit?>(initialValue = null, key1 = preset.id) {
        value = viewModel.getUnitsForDrink(preset.id).firstOrNull()
    }

    var name by rememberSaveable(preset.id) { mutableStateOf(preset.name) }
    var emoji by rememberSaveable(preset.id) { mutableStateOf(preset.emoji) }
    var imageUri by rememberSaveable(preset.id) { mutableStateOf(preset.imageName) }
    var selectedCategory by rememberSaveable(preset.id) { mutableStateOf(preset.category) }
    var selectedUnitKey by rememberSaveable(preset.id) { mutableStateOf(preset.defaultUnit) }
    var releaseMinutes by rememberSaveable(preset.id) { mutableStateOf(preset.releaseMinutes) }
    var caffeineText by rememberSaveable(preset.id) { mutableStateOf(preset.defaultCaffeineMg.toString()) }
    var caffeineEdited by rememberSaveable(preset.id) { mutableStateOf(false) }
    LaunchedEffect(preset.id, existingUnit) {
        if (!caffeineEdited) {
            existingUnit?.caffeineMg?.let { caffeineText = formatCaffeineAmount(it) }
        }
    }

    val imageRequest = rememberImagePickerRequest()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = viewModel.copyCustomDrinkImage(uri)
                if (path != null) imageUri = path
            }
        }
    }

    val caffeineValue = caffeineText.toDoubleOrNull()
    val isValid = name.isNotBlank() && caffeineValue != null && caffeineValue > 0

    val categoryKeys = CategoryUtils.getCategoryOrder()
    val unitKeys = listOf(
        "cup", "shot", "can", "bottle", "mug", "ml", "fl oz", "liter",
        "g", "pod", "teabag", "pill", "gummy", "piece", "bar", "scoop",
    )

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
            IconButton(onClick = { haptics.navigation(); onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.home_edit_drink, preset.name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                enabled = isValid,
                onClick = {
                    val caffeine = caffeineValue ?: return@TextButton
                    haptics.confirm()
                    viewModel.updateCustomDrink(
                        preset = preset,
                        name = name.trim(),
                        emoji = emoji.ifBlank { "☕" },
                        imageUri = imageUri,
                        category = selectedCategory,
                        unitKey = selectedUnitKey,
                        caffeineMg = caffeine,
                        releaseMinutes = releaseMinutes,
                    )
                    onDone()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.clickable { pickImage.launch(imageRequest) }) {
                ExpressiveIconBadge(index = preset.id, size = 76.dp) {
                    DrinkIcon(
                        imageName = imageUri,
                        emoji = emoji.ifBlank { "☕" },
                        contentDescription = stringResource(R.string.custom_drink_image_picker_cd),
                        modifier = Modifier.size(48.dp),
                        emojiSize = MaterialTheme.typography.headlineLarge.fontSize,
                    )
                }
            }
            Text(
                text = name.ifBlank { "—" },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider()

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.custom_drink_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text(stringResource(R.string.custom_drink_emoji_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickImage.launch(imageRequest) }) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(if (imageUri.isBlank()) R.string.custom_drink_pick_image else R.string.custom_drink_change_image))
            }
            if (imageUri.isNotBlank()) {
                TextButton(onClick = { imageUri = "" }) {
                    Text(stringResource(R.string.custom_drink_remove))
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_drink_category), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                categoryKeys.forEachIndexed { index, key ->
                    ToggleButton(
                        checked = selectedCategory == key,
                        onCheckedChange = { if (it) { haptics.toggle(); selectedCategory = key } },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            categoryKeys.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(
                            text = CategoryUtils.getCategoryButtonLabel(
                                CategoryUtils.getCategoryDisplayName(key)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_drink_serving_unit), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                unitKeys.forEachIndexed { index, key ->
                    ToggleButton(
                        checked = selectedUnitKey == key,
                        onCheckedChange = { if (it) { haptics.toggle(); selectedUnitKey = key } },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            unitKeys.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(
                            text = formatUnitLabel(key),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_drink_caffeine_per_serving), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = caffeineText,
                onValueChange = {
                    caffeineEdited = true
                    caffeineText = it.filter { c -> c.isDigit() || c == '.' }
                },
                label = { Text(stringResource(R.string.custom_drink_amount_label)) },
                suffix = { Text(stringResource(R.string.unit_mg)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider()

        ReleaseTimeSetting(
            releaseMinutes = releaseMinutes,
            onReleaseMinutesChange = { releaseMinutes = it },
        )

        HorizontalDivider()

        // Kept apart at the very bottom, in the error colour, so it is never hit on the way to Save.
        FilledTonalButton(
            onClick = {
                haptics.navigation()
                showDeleteDialog = true
            },
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                text = stringResource(R.string.custom_drink_delete_button),
                style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showDeleteDialog) {
        DeleteCustomDrinkDialog(
            drinkName = preset.name,
            onConfirm = {
                haptics.confirm()
                showDeleteDialog = false
                viewModel.deleteCustomDrink(preset)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/**
 * Confirms removing a custom drink. Only the drink list changes: entries already logged keep their
 * own copy of the drink, so history and charts stay exactly as they were.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeleteCustomDrinkDialog(
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
                    size = LookupIconSize,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                Text(
                    text = stringResource(R.string.custom_drink_delete_title, drinkName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )

                Text(
                    text = stringResource(R.string.custom_drink_delete_body),
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
                        contentPadding = LookupActionPadding,
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
                        contentPadding = LookupActionPadding,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.custom_drink_delete_confirm), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private const val ScannerTransitionKey = "barcode-scanner"
private val ScanButtonCorner = 20.dp

/**
 * The scan button and the scanner share bounds, so opening grows the scanner out of the button and
 * closing shrinks it back in, rounding off as it goes. Whichever side is leaving fades out quickly
 * and the arriving side fades in late, which keeps the morphing shape solid rather than letting the
 * list show through a crossfade.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Modifier.scannerMorph(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isScanner: Boolean,
): Modifier = with(sharedTransitionScope) {
    val boundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val corner by animatedVisibilityScope.transition.animateDp(label = "scanner-corner") { state ->
        val shown = state == EnterExitState.Visible
        if (shown == isScanner) 0.dp else ScanButtonCorner
    }
    val quickly = tween<Float>(durationMillis = 90)
    val late = tween<Float>(durationMillis = 150, delayMillis = 200)
    this@scannerMorph.sharedBounds(
        sharedContentState = rememberSharedContentState(ScannerTransitionKey),
        animatedVisibilityScope = animatedVisibilityScope,
        enter = fadeIn(if (isScanner) quickly else late),
        exit = fadeOut(if (isScanner) late else quickly),
        boundsTransform = { _, _ -> boundsSpec },
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.Center),
        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(corner)),
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}

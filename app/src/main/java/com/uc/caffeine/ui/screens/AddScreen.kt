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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.uc.caffeine.LocalSnackbarHostState
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DEFAULT_CONSUMPTION_DURATION_MINUTES
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.util.CaffeineCalculator
import com.uc.caffeine.util.calculateNextBedtimeMillis
import kotlin.math.roundToInt
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.components.ConsumptionTimingSection
import com.uc.caffeine.ui.components.DrinkIcon
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onDrinkLogged: () -> Unit,
    viewModel: CaffeineViewModel = viewModel()
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }

    val groupedDrinks by viewModel.groupedDrinkPresets.collectAsStateWithLifecycle()
    val recentDrinks by viewModel.recentDrinks.collectAsStateWithLifecycle()
    val isCatalogLoading by viewModel.isDrinkCatalogLoading.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val todayEntries by viewModel.todayEntries.collectAsStateWithLifecycle()
    val categorizedDrinks = remember(groupedDrinks) {
        groupedDrinks.entries.toList()
    }
    val showRecentServings = selectedFilter == null && searchQuery.isBlank() && recentDrinks.isNotEmpty()
    val categories = viewModel.getAvailableCategories()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDrink by remember { mutableStateOf<DrinkPreset?>(null) }
    var showCustomDrinkSheet by remember { mutableStateOf(false) }
    var customDrinkInitialName by remember { mutableStateOf("") }
    val customSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel, snackbarHostState, sheetState) {
        viewModel.addScreenEvents.collectLatest { event ->
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            selectedDrink = null

            when (event) {
                is AddScreenUiEvent.DrinkLogged -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val message = "Logged ${event.drinkName}"
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

    CaffeineScreenScaffold(
        title = "Add a drink",
        headerBottomSpacing = 0.dp,
        actions = {
            FilledIconButton(onClick = { showCustomDrinkSheet = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create custom drink")
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
                    placeholder = { Text("Search drinks...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                listState.requestScrollToItem(0)
                                viewModel.updateSearchQuery("")
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
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
            val allOptions = listOf("All") + categories

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
                        text = if (isAllButton) "All" else CategoryUtils.getCategoryButtonLabel(category!!),
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
                text = "Recent servings",
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
                itemModifier = Modifier.heightIn(min = 65.dp),
                leadingContent = { recent ->
                    ExpressiveIconBadge(
                        index = recent.quantity,
                        size = 44.dp,
                    ) {
                        DrinkIcon(
                            imageName = recent.imageName,
                            emoji = recent.emoji,
                            contentDescription = recent.drinkName,
                            modifier = Modifier.size(28.dp),
                            emojiSize = MaterialTheme.typography.titleLarge.fontSize,
                        )
                    }
                },
                content = { recent ->
                    Text(
                        text = recent.drinkName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = { recent ->
                    Text(
                        text = formatServingSummary(recent.quantity, recent.unitKey),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = { recent ->
                    Text(
                        text = "${recent.caffeineMg}mg",
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
                        customDrinkInitialName = name
                        showCustomDrinkSheet = true
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
                                    selectedDrink = drink
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

    selectedDrink?.let { drink ->
        ModalBottomSheet(
            onDismissRequest = { selectedDrink = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AddDrinkServingSheet(
                preset = drink,
                viewModel = viewModel,
                userSettings = userSettings,
                todayEntries = todayEntries,
                onAdd = { quantity, unit, startedAtMillis, durationMinutes ->
                    haptics.confirm()
                    viewModel.logDrinkFromAddScreen(
                        preset = drink,
                        quantity = quantity,
                        unit = unit,
                        startedAtMillis = startedAtMillis,
                        durationMinutes = durationMinutes,
                    )
                }
            )
        }
    }

    if (showCustomDrinkSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showCustomDrinkSheet = false
                customDrinkInitialName = ""
            },
            sheetState = customSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CreateCustomDrinkSheet(
                viewModel = viewModel,
                initialName = customDrinkInitialName,
                onDismiss = {
                    showCustomDrinkSheet = false
                    customDrinkInitialName = ""
                },
            )
        }
    }
}

@Composable
private fun AddDrinkServingSheet(
    preset: DrinkPreset,
    viewModel: CaffeineViewModel,
    userSettings: com.uc.caffeine.data.UserSettings,
    todayEntries: List<ConsumptionEntry>,
    onAdd: (Int, DrinkUnit, Long, Int) -> Unit,
) {
    val units by produceState<List<DrinkUnit>?>(initialValue = null, key1 = preset.id) {
        value = viewModel.getUnitsForDrink(preset.id)
    }
    var quantity by remember(preset.id) { mutableIntStateOf(1) }
    var startedAtMillis by remember(preset.id) { mutableStateOf(System.currentTimeMillis()) }
    var durationMinutes by remember(preset.id) {
        mutableIntStateOf(DEFAULT_CONSUMPTION_DURATION_MINUTES)
    }
    val defaultUnit = remember(units) {
        units?.firstOrNull { it.isDefault } ?: units?.firstOrNull()
    }
    var selectedUnitKey by remember(preset.id, units) {
        mutableStateOf(defaultUnit?.unitKey)
    }
    val selectedUnit = remember(units, selectedUnitKey, defaultUnit) {
        units?.firstOrNull { it.unitKey == selectedUnitKey } ?: defaultUnit
    }
    val servingSummary = remember(quantity, selectedUnit, preset.defaultUnit) {
        formatServingSummary(
            quantity = quantity,
            unitKey = selectedUnit?.unitKey ?: preset.defaultUnit,
        )
    }
    val totalCaffeineMg = remember(quantity, selectedUnit) {
        selectedUnit?.let { calculateServingTotalCaffeine(quantity, it.caffeineMg) } ?: 0
    }

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
                    text = "Log $servingSummary of",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        HorizontalDivider()

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
                onDecrement = { quantity = (quantity - 1).coerceAtLeast(1) },
                onIncrement = { quantity += 1 },
            )

            HorizontalDivider()

            if (units.isNullOrEmpty()) {
                Text(
                    text = "No serving options are available for this drink yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose a unit",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ServingUnitSelector(
                        units = units.orEmpty(),
                        selectedUnit = selectedUnit,
                        onUnitSelected = { selectedUnitKey = it.unitKey },
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total caffeine",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RollingNumberText(
                    text = "$totalCaffeineMg mg",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    labelPrefix = "add_sheet_total",
                )
            }

            val caffeineAtBedtimeMg = remember(
                todayEntries, totalCaffeineMg, startedAtMillis, durationMinutes, userSettings,
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
                    entries = todayEntries + virtualEntry,
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
                            text = "By bedtime, ~${caffeineAtBedtimeMg.roundToInt()} mg will still be active — above your ${userSettings.sleepThresholdMg} mg sleep threshold.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            HorizontalDivider()

            ConsumptionTimingSection(
                startedAtMillis = startedAtMillis,
                durationMinutes = durationMinutes,
                settings = userSettings,
                onStartedAtChange = { startedAtMillis = it },
                onDurationChange = { durationMinutes = it },
            )

            Button(
                onClick = {
                    selectedUnit?.let {
                        onAdd(quantity, it, startedAtMillis, durationMinutes)
                    }
                },
                enabled = selectedUnit != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Add entry")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
    val title = if (hasActiveFilters) "No drinks found" else "No drinks available"
    val message = when {
        selectedFilter != null && hasSearchQuery ->
            "No drinks match \"$searchQuery\" in $selectedFilter."
        selectedFilter != null ->
            "No drinks are available in $selectedFilter right now."
        hasSearchQuery ->
            "No drinks match \"$searchQuery\"."
        else ->
            "Add some presets to see drinks here."
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
                    Text("Create Custom Drink")
                }
            } else if (hasActiveFilters) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear filters")
                }
            }
        }
    }
}

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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatUnitLabel(drink.defaultUnit),
            style = MaterialTheme.typography.bodySmall,
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
    SegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        leadingContent = {
            ExpressiveIconBadge(
                index = index,
                size = 44.dp,
            ) {
                DrinkIcon(
                    imageName = drink.imageName,
                    emoji = drink.emoji,
                    contentDescription = drink.name,
                    modifier = Modifier.size(28.dp),
                    emojiSize = MaterialTheme.typography.titleLarge.fontSize,
                )
            }
        },
        content = {
            Text(
                text = drink.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            DrinkSupportingContent(drink = drink)
        },
        trailingContent = {
            Text(
                text = "${drink.defaultCaffeineMg}mg",
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
private fun CreateCustomDrinkSheet(
    viewModel: CaffeineViewModel,
    initialName: String = "",
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf("☕") }
    var imageUri by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("coffee") }
    var selectedUnitKey by remember { mutableStateOf("cup") }
    var caffeineText by remember { mutableStateOf("") }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
        "g", "pod", "teabag", "pill", "piece", "bar", "scoop",
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
            Box(modifier = Modifier.clickable { pickImage.launch("image/*") }) {
                ExpressiveIconBadge(index = 0, size = 76.dp) {
                    DrinkIcon(
                        imageName = imageUri,
                        emoji = emoji.ifBlank { "☕" },
                        contentDescription = "Tap to pick image",
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
                    text = "New custom drink",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = name.ifBlank { "—" },
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Drink name") },
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
                label = { Text("Emoji") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickImage.launch("image/*") }) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(if (imageUri.isBlank()) "Pick image" else "Change")
            }
            if (imageUri.isNotBlank()) {
                TextButton(onClick = { imageUri = "" }) {
                    Text("Remove")
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Category", style = MaterialTheme.typography.titleMedium)
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
            Text("Serving unit", style = MaterialTheme.typography.titleMedium)
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
            Text("Caffeine per serving", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = caffeineText,
                onValueChange = { caffeineText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                suffix = { Text("mg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

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
                )
                onDismiss()
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Save custom drink")
        }

        Spacer(Modifier.height(8.dp))
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}

package com.uc.caffeine.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uc.caffeine.data.model.DrinkPreset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uc.caffeine.ui.components.CaffeineScreenScaffold
import com.uc.caffeine.ui.viewmodel.AddScreenUiEvent
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.util.CategoryIcons
import com.uc.caffeine.util.CategoryUtils
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(viewModel: CaffeineViewModel = viewModel()) {

    val allDrinks by viewModel.drinkPresets.collectAsStateWithLifecycle()
    val groupedDrinks by viewModel.groupedDrinkPresets.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categorizedDrinks = remember(groupedDrinks) {
        groupedDrinks.entries.toList()
    }
    val categories = viewModel.getAvailableCategories()
    val focusManager = LocalFocusManager.current
    val isCatalogLoading = allDrinks.isEmpty() && selectedFilter == null && searchQuery.isBlank()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.addScreenEvents.collectLatest { event ->
            when (event) {
                is AddScreenUiEvent.DrinkLogged -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Logged ${event.drinkName}",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    CaffeineScreenScaffold(
        title = "Add a drink",
        subtitle = "Tap to log it instantly",
        snackbarHostState = snackbarHostState,
        headerBottomSpacing = 0.dp
    ) {
        // ── Material 3 SearchBar ────────────────────────────────────────────
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
            content = { /* No search suggestions needed */ }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category filter - Material3 ButtonGroup with ToggleButton
        FlowRow(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            val allOptions = listOf("All") + categories
            
            allOptions.forEachIndexed { index, option ->
                val isAllButton = (index == 0)
                val category = if (isAllButton) null else categories[index - 1]
                val isChecked = if (isAllButton) (selectedFilter == null) else (selectedFilter == category)
                
                ToggleButton(
                    checked = isChecked,
                    onCheckedChange = { 
                        if (it) { // Only act when checking (single-select pattern)
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
                        imageVector = if (isAllButton) CategoryIcons.getAllIcon() else CategoryIcons.getIcon(category!!),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            if (isCatalogLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categorizedDrinks.forEach { entry ->
                        val (category, drinks) = entry

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
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(
                            items = drinks,
                            key = { drink -> drink.id },
                            contentType = { "drink" }
                        ) { drink ->
                            val currentDrink by rememberUpdatedState(drink)

                            ElevatedDrinkCard(
                                drink = currentDrink,
                                onClick = { viewModel.logDrinkFromAddScreen(currentDrink) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDrinkResultsState(
    selectedFilter: String?,
    searchQuery: String,
    onClearFilters: () -> Unit
) {
    val hasActiveFilters = selectedFilter != null || searchQuery.isNotBlank()
    val title = if (hasActiveFilters) "No drinks found" else "No drinks available"
    val message = when {
        selectedFilter != null && searchQuery.isNotBlank() ->
            "No drinks match \"$searchQuery\" in $selectedFilter."
        selectedFilter != null ->
            "No drinks are available in $selectedFilter right now."
        searchQuery.isNotBlank() ->
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasActiveFilters) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear filters")
                }
            }
        }
    }
}

// ── Compact Elevated Card for Single-Column List ──────────────────────────
@Composable
fun ElevatedDrinkCard(
    drink: DrinkPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow, // Gives a card-like look without shadows
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji or icon (32dp)
            Text(
                text = drink.emoji.ifBlank { "☕" },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(32.dp)
            )
            
            // Drink info - name and unit
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drink.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
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
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = drink.defaultUnit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            // Caffeine amount - bold, highlighted
            Text(
                text = "${drink.defaultCaffeineMg.toInt()}mg",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

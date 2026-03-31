package com.uc.caffeine.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uc.caffeine.data.CaffeineDatabase
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.util.CategoryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class CaffeineViewModel(application: Application) : AndroidViewModel(application) {

    private val db        = CaffeineDatabase.getDatabase(application)
    private val presetDao = db.drinkPresetDao()
    private val logDao    = db.consumptionLogDao()

    // Selected category filter (null = "All", shows all categories)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Search query filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Full drink catalog — used by the Add screen
    val drinkPresets: StateFlow<List<DrinkPreset>> = presetDao
        .getAllPresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Grouped drink catalog by category — used by the Add screen for categorized display
    val groupedDrinkPresets: StateFlow<Map<String, List<DrinkPreset>>> = 
        combine(
            presetDao.getAllPresets(), 
            selectedCategoryFilter,
            searchQuery  // Add search query
        ) { drinks, filter, query ->
            // First apply category filter
            val categoryFiltered = if (filter != null) {
                val lowercaseKey = CategoryUtils.getAllCategories()
                    .entries.find { it.value == filter }?.key ?: filter.lowercase()
                drinks.filter { it.category.lowercase() == lowercaseKey }
            } else {
                drinks
            }
            
            // Then apply search filter
            val searchFiltered = if (query.isNotBlank()) {
                categoryFiltered.filter { drink ->
                    drink.name.contains(query, ignoreCase = true) ||
                    drink.brand.contains(query, ignoreCase = true) ||
                    drink.description?.contains(query, ignoreCase = true) == true
                }
            } else {
                categoryFiltered
            }
            
            // Group and sort
            searchFiltered
                .groupBy { it.category.lowercase() }
                .mapKeys { (category, _) -> CategoryUtils.getCategoryDisplayName(category) }
                .toSortedMap(compareBy { displayName ->
                    val lowercaseKey = CategoryUtils.getAllCategories()
                        .entries.find { it.value == displayName }?.key ?: ""
                    CategoryUtils.getCategoryOrder().indexOf(lowercaseKey)
                })
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    // The 2 most recently tapped drinks — used by Quick Add on HomeScreen
    // Automatically updates every time the user logs a drink
    val recentDrinks: StateFlow<List<RecentDrink>> = logDao
        .getRecentlyUsedDrinks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Today's caffeine total — the big number
    val todayTotalMg: StateFlow<Int> = logDao
        .getTodayTotal(startOfToday())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    // All of today's individual entries — for the log list
    val todayEntries: StateFlow<List<ConsumptionEntry>> = logDao
        .getTodayEntries(startOfToday())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun logDrink(preset: DrinkPreset) {
        viewModelScope.launch {
            logDao.logDrink(
                ConsumptionEntry(
                    drinkName  = preset.name,
                    caffeineMg = preset.defaultCaffeineMg,
                    emoji      = preset.emoji
                )
            )
        }
    }

    // Log from a RecentDrink (tapped from Quick Add)
    fun logRecentDrink(recent: RecentDrink) {
        viewModelScope.launch {
            logDao.logDrink(
                ConsumptionEntry(
                    drinkName  = recent.drinkName,
                    caffeineMg = recent.caffeineMg,
                    emoji      = recent.emoji
                )
            )
        }
    }

    fun resetToday() {
        viewModelScope.launch {
            logDao.clearToday(startOfToday())
        }
    }

    // Function to update filter
    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Helper function to get all available categories
    fun getAvailableCategories(): List<String> {
        return CategoryUtils.getCategoryDisplayNamesOrdered()
    }
}

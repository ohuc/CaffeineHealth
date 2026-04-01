package com.uc.caffeine.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uc.caffeine.data.CaffeineDatabase
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.util.CaffeineCalculator
import com.uc.caffeine.util.CategoryUtils
import com.uc.caffeine.util.ChartData
import com.uc.caffeine.util.ChartDataGenerator
import com.uc.caffeine.util.ConsumptionContributionDetail
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes

sealed interface AddScreenUiEvent {
    data class DrinkLogged(val drinkName: String) : AddScreenUiEvent
}

sealed interface HomeScreenUiEvent {
    data class LogActionCompleted(val message: String) : HomeScreenUiEvent
}

class CaffeineViewModel(application: Application) : AndroidViewModel(application) {

    private val db        = CaffeineDatabase.getDatabase(application)
    private val presetDao = db.drinkPresetDao()
    private val logDao    = db.consumptionLogDao()
    private val settingsRepo = SettingsRepository(application)

    // User settings flow
    val userSettings = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = com.uc.caffeine.data.UserSettings()
    )

    // Ticker flow - emits every 5 minutes to trigger caffeine level recalculation
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(5.minutes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = System.currentTimeMillis()
    )

    // Selected category filter (null = "All", shows all categories)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Search query filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val addScreenEventsChannel = Channel<AddScreenUiEvent>(capacity = Channel.BUFFERED)
    val addScreenEvents: Flow<AddScreenUiEvent> = addScreenEventsChannel.receiveAsFlow()
    private val homeScreenEventsChannel = Channel<HomeScreenUiEvent>(capacity = Channel.BUFFERED)
    val homeScreenEvents: Flow<HomeScreenUiEvent> = homeScreenEventsChannel.receiveAsFlow()

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private val allDrinkPresets = presetDao.getAllPresets()

    // Full drink catalog — used by the Add screen
    val drinkPresets: StateFlow<List<DrinkPreset>> = allDrinkPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Grouped drink catalog by category — used by the Add screen for categorized display
    val groupedDrinkPresets: StateFlow<Map<String, List<DrinkPreset>>> = 
        combine(
            drinkPresets,
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

    // Current active caffeine level with real-time decay
    val currentCaffeineLevel: StateFlow<Double> = combine(
        logDao.getAllEntries(),
        tickerFlow,
        userSettings
    ) { allEntries, currentTime, settings ->
        CaffeineCalculator.calculateCurrentLevel(
            entries = allEntries,
            currentTimeMillis = currentTime,
            halfLifeMinutes = settings.halfLifeMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )

    /**
     * Predicts caffeine level at user's next bedtime.
     * Returns: Pair<caffeineLevelAtBedtime, timeUntilBedtime>
     */
    val caffeineAtBedtime: StateFlow<Pair<Double, Long>> = combine(
        logDao.getAllEntries(),
        userSettings,
        tickerFlow
    ) { allEntries, settings, _ ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Calculate next bedtime
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, settings.sleepTimeHour)
        calendar.set(Calendar.MINUTE, settings.sleepTimeMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If bedtime already passed today, move to tomorrow
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val bedtime = calendar.timeInMillis
        val caffeineLevel = CaffeineCalculator.calculateCurrentLevel(
            entries = allEntries,
            currentTimeMillis = bedtime,  // Calculate AT bedtime, not now
            halfLifeMinutes = settings.halfLifeMinutes
        )
        
        Pair(caffeineLevel, bedtime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Pair(0.0, System.currentTimeMillis())
    )

    // Time until peak absorption - shows when caffeine is still being absorbed
    val timeUntilPeak: StateFlow<Long?> = combine(
        logDao.getAllEntries(),
        tickerFlow
    ) { allEntries, _ ->
        if (allEntries.isEmpty()) return@combine null
        val mostRecent = allEntries.maxByOrNull { it.timestamp } ?: return@combine null
        val peakTime = mostRecent.timestamp + (mostRecent.absorptionRate * 60 * 1000L)
        val now = System.currentTimeMillis()
        if (peakTime > now) peakTime else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    // Reactive 24-hour caffeine curve data for charting
    val chartData: StateFlow<ChartData> = combine(
        logDao.getAllEntries(),
        tickerFlow,
        userSettings
    ) { entries, currentTime, settings ->
        ChartDataGenerator.generateChartData(
            entries = entries,
            settings = settings,
            currentTime = currentTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChartData(
            dataPoints = emptyList(),
            consumptionMarkers = emptyList(),
            thresholdLevel = 100.0,
            bedtimeMillis = 0L,
            currentTimeMillis = System.currentTimeMillis()
        )
    )

    fun logDrink(preset: DrinkPreset) {
        viewModelScope.launch {
            logDao.logDrink(
                ConsumptionEntry(
                    drinkName  = preset.name,
                    caffeineMg = preset.defaultCaffeineMg,
                    emoji      = preset.emoji,
                    absorptionRate = preset.absorptionRate
                )
            )
        }
    }

    fun logDrinkFromAddScreen(preset: DrinkPreset) {
        viewModelScope.launch {
            logDao.logDrink(
                ConsumptionEntry(
                    drinkName = preset.name,
                    caffeineMg = preset.defaultCaffeineMg,
                    emoji = preset.emoji,
                    absorptionRate = preset.absorptionRate
                )
            )
            addScreenEventsChannel.send(AddScreenUiEvent.DrinkLogged(preset.name))
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

    fun getContributionDetail(
        entry: ConsumptionEntry,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ConsumptionContributionDetail {
        return ChartDataGenerator.generateContributionDetail(
            entry = entry,
            settings = userSettings.value,
            currentTime = currentTimeMillis
        )
    }

    fun updateLoggedEntry(
        entry: ConsumptionEntry,
        caffeineMg: Int,
        timestamp: Long
    ) {
        viewModelScope.launch {
            logDao.updateEntryById(
                entryId = entry.id,
                caffeineMg = caffeineMg,
                timestamp = timestamp
            )
            homeScreenEventsChannel.send(
                HomeScreenUiEvent.LogActionCompleted("Updated ${entry.drinkName}")
            )
        }
    }

    fun duplicateLoggedEntry(entry: ConsumptionEntry) {
        viewModelScope.launch {
            logDao.logDrink(
                entry.copy(
                    id = 0,
                    timestamp = System.currentTimeMillis()
                )
            )
            homeScreenEventsChannel.send(
                HomeScreenUiEvent.LogActionCompleted("Logged ${entry.drinkName} again")
            )
        }
    }

    fun deleteLoggedEntry(entry: ConsumptionEntry) {
        viewModelScope.launch {
            logDao.deleteEntryById(entry.id)
            homeScreenEventsChannel.send(
                HomeScreenUiEvent.LogActionCompleted("Deleted ${entry.drinkName}")
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

    // Settings update functions
    fun updateHalfLife(hours: Int) {
        viewModelScope.launch {
            settingsRepo.updateHalfLife(hours * 60) // Convert hours to minutes
        }
    }

    fun updateSleepTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepo.updateSleepTime(hour, minute)
        }
    }

    fun updateSleepThreshold(milligrams: Int) {
        viewModelScope.launch {
            settingsRepo.updateSleepThreshold(milligrams)
        }
    }
}

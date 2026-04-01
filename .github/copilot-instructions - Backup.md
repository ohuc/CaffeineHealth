# Copilot Instructions - Caffeine Tracker

Android app for tracking caffeine consumption with real-time blood caffeine level calculations and sleep predictions using scientifically accurate pharmacokinetic modeling.

## Build & Test Commands

```bash
# Build the app
./gradlew build

# Run on device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests com.uc.caffeine.YourTestClass

# Clean build
./gradlew clean build

# Generate APK
./gradlew assembleDebug
./gradlew assembleRelease
```

## Architecture

### MVVM + Repository Pattern

```
UI Layer (Compose)
    ↓
ViewModel (CaffeineViewModel)
    ↓
Repository (SettingsRepository) + DAOs
    ↓
Data Layer (Room Database + DataStore)
```

**Single ViewModel Design**: `CaffeineViewModel` manages all app state using StateFlow. All screens observe the same ViewModel instance.

**Reactive Data Flows**: Use `StateFlow` and `Flow` for all state management:
- DAOs return `Flow<List<T>>` for reactive queries
- ViewModel exposes `StateFlow<UiState>` to UI
- Combine flows with `combine()` for computed state
- Use `SharingStarted.WhileSubscribed(5_000)` for lifecycle-aware subscriptions

**Screen Pattern**: Screens are split into two composables:
1. `Screen()` - Collects state from ViewModel, handles lifecycle
2. `ScreenContent()` - Stateless presentation layer with callbacks

Example:
```kotlin
@Composable
fun HomeScreen(viewModel: CaffeineViewModel) {
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onAction = viewModel::handleAction
    )
}

@Composable
fun HomeContent(uiState: HomeUiState, onAction: (Action) -> Unit) {
    // Stateless UI
}
```

## Data Layer Conventions

### Room Database

**Three entities with specific purposes**:
- `DrinkPreset` - Catalog of drinks (coffee, tea, soda, etc.)
- `DrinkUnit` - Serving sizes (oz, mL, cup, shot)
- `ConsumptionEntry` - Historical log of consumed drinks

**Denormalization Strategy**: `ConsumptionEntry` stores drink metadata (name, caffeine amount) instead of foreign keys. This preserves accurate history even if presets change.

```kotlin
// CORRECT: Denormalized for historical accuracy
@Entity
data class ConsumptionEntry(
    val drinkName: String,        // Snapshot of name
    val caffeineMg: Int,           // Snapshot of caffeine
    val timestamp: Long
)

// INCORRECT: Don't use foreign keys for consumption history
// @Entity
// data class ConsumptionEntry(
//     val drinkPresetId: Long  // ❌ If preset changes, history is wrong
// )
```

**DAO Query Patterns**:
- Return `Flow<T>` for reactive queries that update UI
- Return `List<T>` for one-time reads
- Use `@Query` with `ORDER BY timestamp DESC` for recent-first sorting

### DataStore for Settings

Use Preferences DataStore (not Proto DataStore) for user settings:
```kotlin
// Good: Type-safe keys with defaults
val HALF_LIFE = intPreferencesKey("half_life")
val DEFAULT_HALF_LIFE = 300

// Collect as Flow for reactive updates
settingsRepository.settings.collectAsStateWithLifecycle()
```

## Caffeine Calculation Logic

### Two-Phase Pharmacokinetic Model

**Critical**: Calculations must be scientifically accurate. The app models real caffeine metabolism:

1. **Absorption Phase** (0-45 min): Linear ramp to peak blood concentration
2. **Elimination Phase** (45+ min): Exponential decay with 5-hour half-life

```kotlin
// Absorption: Linear increase
if (elapsedMinutes < absorptionMinutes) {
    caffeineMg * (elapsedMinutes / absorptionMinutes)
}

// Elimination: Exponential decay using half-life
else {
    val timeInElimination = elapsedMinutes - absorptionMinutes
    val halfLives = timeInElimination / halfLifeMinutes
    caffeineMg * (0.5.pow(halfLives))
}
```

**Key Parameters**:
- `absorptionRate`: Minutes to peak (typically 45)
- `halfLifeMinutes`: Decay rate (default 300 = 5 hours, user configurable)
- `sleepThreshold`: Safe level for sleep (default 50mg)

**Real-Time Updates**: ViewModel uses `tickerFlow(5.minutes)` to recalculate caffeine levels every 5 minutes as time passes.

### Time-Based Edge Cases

Handle these correctly:
- Future timestamps (shouldn't happen, return 0.0)
- Very old entries (exponential decay approaches 0)
- Timezone changes (use `System.currentTimeMillis()` consistently)
- Multiple drinks in absorption phase simultaneously

## UI/Compose Conventions

### Material Design 3

**Theme System**:
- Use `MaterialTheme.colorScheme` for colors
- Dynamic color support on Android 12+ (`dynamicDarkColorScheme()`)
- Typography from Google Fonts via `androidx.compose.ui.text.googlefonts`

**Opt-In APIs**:
```kotlin
// Already configured in build.gradle.kts
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

### Navigation

**Adaptive Navigation Suite**: Use `NavigationSuiteScaffold` for responsive layouts:
- Mobile: Bottom navigation bar
- Tablet: Navigation rail or drawer

```kotlin
NavigationSuiteScaffold(
    navigationSuiteItems = {
        NavigationSuiteScope.item(
            selected = currentScreen == Screen.Home,
            onClick = { /* navigate */ },
            icon = { Icon(...) },
            label = { Text("Home") }
        )
    }
) { /* content */ }
```

### State Hoisting

**Always hoist state to ViewModel**:
```kotlin
// CORRECT
@Composable
fun AddScreen(viewModel: CaffeineViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    TextField(
        value = searchQuery,
        onValueChange = viewModel::updateSearchQuery
    )
}

// INCORRECT - don't use remember for business state
@Composable
fun AddScreen() {
    var searchQuery by remember { mutableStateOf("") }  // ❌
}
```

**Use `remember` only for**:
- UI-only state (scroll position, focus, animation)
- Transient state that doesn't affect business logic

## Code Style

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Entity | PascalCase noun | `ConsumptionEntry`, `DrinkPreset` |
| DAO | `*Dao` | `ConsumptionLogDao`, `DrinkPresetDao` |
| Repository | `*Repository` | `SettingsRepository` |
| ViewModel | `*ViewModel` | `CaffeineViewModel` |
| Screen | `*Screen` | `HomeScreen`, `AddScreen` |
| Util | PascalCase + `object` | `CaffeineCalculator`, `CategoryUtils` |
| Private StateFlow | `_camelCase` | `_selectedCategory` |
| Public StateFlow | `camelCase` | `selectedCategory` |

### Flow Patterns

**Backing property pattern for mutable state**:
```kotlin
class CaffeineViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
```

**Combine flows for computed state**:
```kotlin
val groupedDrinks = combine(
    drinkPresetsFlow,
    categoryFilter,
    searchQuery
) { drinks, category, query ->
    drinks
        .filter { it.category == category || category == "All" }
        .filter { it.name.contains(query, ignoreCase = true) }
        .groupBy { it.category }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
```

## Database Seeding

**Automatic Initialization**: `CaffeineDatabase.onCreate()` seeds the database with JSON data on first launch.

**Drink Catalog** (`app/src/main/res/raw/drinks.json`):
- Structured as category → drinks → variants
- Each variant has: name, size, caffeineMg, absorptionRate
- Update JSON to add new drinks (changes only apply to new installs)

**Migration Strategy**: Currently using `fallbackToDestructiveMigration()` (dev mode). Before production:
- Write proper `Migration` objects for schema changes
- Test migrations with `MigrationTestHelper`

## Testing

### Unit Tests
- Test `CaffeineCalculator` extensively (pharmacokinetic model is critical)
- Test edge cases: zero caffeine, negative time, future timestamps
- Use parameterized tests for multiple scenarios

### UI Tests
- Use Compose Testing API (`composeTestRule`)
- Test navigation flows between screens
- Test state updates (search filtering, category selection)
- Verify real-time updates with `advanceTimeBy()` if using TestCoroutineDispatcher

### Integration Tests
- Test DAO queries with in-memory database (`Room.inMemoryDatabaseBuilder`)
- Test ViewModel with fake repositories
- Test DataStore with `TestDataStore`

## KSP (Kotlin Symbol Processing)

**Room Compiler**: KSP generates DAO implementations automatically.

**Incremental Builds**: KSP caches are in `.gradle/` and `app/build/generated/ksp/`

**If build fails after schema changes**:
```bash
./gradlew clean
./gradlew kspDebugKotlin  # Regenerate KSP code
```

## Platform Constraints

- **Min SDK 36** (Android 15): Latest-only app, use modern APIs freely
- **Edge-to-Edge**: UI renders behind system bars, use `WindowInsets` for padding
- **Java 11**: Target compatibility, use modern Kotlin features but Java-compatible APIs

## Common Patterns

### Adding a New Screen

1. Create `*Screen.kt` in `ui/screens/`
2. Add `@Composable fun NewScreen(viewModel: CaffeineViewModel)`
3. Add `@Composable fun NewScreenContent(uiState, onAction)`
4. Update navigation in `MainActivity` or navigation controller
5. Add screen to `NavigationSuiteScaffold` items

### Adding a Setting

1. Add key to `SettingsRepository` (`*PreferencesKey`)
2. Add field to `UserSettings` data class
3. Update `SettingsRepository.settings` flow mapping
4. Add UI in `SettingsScreen`
5. Use in ViewModel: `settingsRepository.updateSetting()`

### Adding a Database Field

1. Update entity class with `@ColumnInfo`
2. Increment `CaffeineDatabase.VERSION`
3. Run clean build to trigger KSP regeneration
4. Add migration or use destructive migration for dev

## AI Assistant Skills

See `.github/skills/` for specialized guidance:
- `android-jetpack-compose-expert/` - Compose architecture patterns
- `android-ui-verification/` - Testing with ADB and emulators

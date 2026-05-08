<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="100" alt="Caffeine Health logo" />
</p>

<h1 align="center">Caffeine Health</h1>

<p align="center">
  <b>Track your caffeine. Protect your sleep.</b>
</p>

<p align="center">
  <a href="https://github.com/ohuc/CaffeineHealth/releases/latest">
    <img src="https://img.shields.io/github/v/release/ohuc/CaffeineHealth?logo=github&labelColor=1a1a1a" alt="GitHub release" />
  </a>
  <a href="https://f-droid.org/packages/com.uc.caffeine">
    <img src="https://img.shields.io/f-droid/v/com.uc.caffeine?logo=f-droid&labelColor=1a1a1a" alt="F-Droid" />
  </a>
  <a href="https://github.com/ohuc/CaffeineHealth/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/ohuc/CaffeineHealth?logo=gnu&color=blue&labelColor=1a1a1a" alt="License" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white&labelColor=1a1a1a" alt="Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-31-brightgreen&labelColor=1a1a1a" alt="Min SDK" />
</p>

<p align="center">
  <a href="https://github.com/ohuc/CaffeineHealth/releases/latest">
    <img src="https://raw.githubusercontent.com/rubenpgrady/get-it-on-github/refs/heads/main/get-it-on-github.png" alt="Get it on GitHub" width="200" />
  </a>
  <a href="https://f-droid.org/packages/com.uc.caffeine">
    <img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" width="200" />
  </a>
</p>

<p align="center">
  <a href="https://hosted.weblate.org/engage/caffeine-health/">
    <img src="https://hosted.weblate.org/widget/caffeine-health/strings/287x66-grey.png" alt="Translation status" />
  </a>
</p>

---

Caffeine Health is a beautifully designed Android app that helps you understand how your caffeine intake affects your body throughout the day, and whether that late-afternoon espresso might cost you sleep tonight.

Log drinks from a curated catalog, watch your active caffeine level in real time on a 24-hour curve, and get a personalized sleep forecast based on your unique metabolism.

## 📱 Screenshots

<table align="center">
  <tr>
    <td align="center"><img src="screenshots/Welcome.png" width="220" alt="Set your baseline" /><br><sub>Welcome</sub></td>
    <td align="center"><img src="screenshots/Onboarding.png" width="220" alt="Profile Ready" /><br><sub>Onboarding</sub></td>
    <td align="center"><img src="screenshots/home.png" width="220" alt="Home Dashboard" /><br><sub>Dashboard</sub></td>
    <td align="center"><img src="screenshots/caffeine_sources.png" width="220" alt="Caffeine by Source" /><br><sub>Source Analytics</sub></td>
  </tr>
</table>

---

## ✨ Features

### 🏠 Dashboard
- **Live caffeine curve:** a smooth 24-hour chart showing your active caffeine level, updated in real time
- **Emoji markers:** each logged drink appears on the chart with its emoji for quick visual reference
- **Sleep forecast:** see how much caffeine will still be active at your bedtime and whether it crosses your safe threshold
- **Today's total:** at-a-glance milligram count for everything consumed today
- **Drink detail sheet:** tap any logged drink to see its contribution curve, peak level, current level, and total impact over time
- **Quick actions:** edit, duplicate, or delete entries directly from the detail sheet

### ☕ Drink Catalog
- **Curated drink database:** browse drinks across categories like coffee, tea, energy drinks, soda, and more
- **Search & filter:** Material 3 search bar with expressive category filter chips
- **One-tap logging:** log a drink with a single tap and get snackbar confirmation
- **Custom serving sizes:** adjust caffeine amount and timing before logging

### 🧬 Personalized Onboarding
- **Guided profiling flow:** set your age range, weight, bedtime, sleep sensitivity, lifestyle factors, and relevant medications
- **Pharmacokinetic modeling:** the app calculates a personalized caffeine half-life based on real-world factors (smoking, alcohol, CYP1A2 inhibitors)
- **Science-backed sources:** every lifestyle adjustment links to its pharmacological source
- **Skip-friendly:** sensible defaults if you prefer to get started immediately

### ⚙️ Settings
- **Half-life tuning:** fine-tune your caffeine metabolism rate
- **Bedtime configuration:** set your typical sleep time for accurate forecasts
- **Sleep threshold:** define the milligram level you consider safe before bed
- **Appearance:** theme and display preferences
- **Date & time format:** 12-hour / 24-hour clock, date format customization

---

## 🏗️ Architecture

```
com.uc.caffeine
├── MainActivity.kt          # Root scaffold, bottom nav, Navigation 3
├── MainNavigation.kt        # Navigation destinations
├── data/
│   ├── model/               # Room entities (DrinkPreset, ConsumptionEntry, DrinkUnit)
│   ├── dao/                 # Room DAOs
│   ├── CaffeineDatabase.kt  # Room database with JSON preset seeding
│   ├── SettingsRepository.kt # DataStore-backed user preferences
│   └── UserSettings.kt      # Settings data class
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt     # Dashboard with chart, sleep forecast, log
│   │   ├── AddScreen.kt      # Drink catalog with search & filter
│   │   └── settings/         # Settings sub-screens (appearance, profile, etc.)
│   ├── components/           # Reusable UI: chart, scaffold, haptics, shimmer
│   ├── onboarding/           # Multi-step profiling flow
│   ├── theme/                # Material 3 color scheme, Montserrat typography
│   └── viewmodel/            # Shared CaffeineViewModel (MVVM)
└── util/
    ├── CaffeineCalculator.kt # One-compartment pharmacokinetic model
    ├── ChartDataGenerator.kt # 24-hour curve + contribution chart data
    └── CategoryUtils.kt     # Drink category helpers & icons
```

The app follows a **single-activity MVVM** pattern:

| Layer | Responsibility |
|---|---|
| **View** | Jetpack Compose screens + reusable components |
| **ViewModel** | `CaffeineViewModel` - shared state, actions, caffeine math orchestration |
| **Data** | Room database for drinks & entries, DataStore for user settings |
| **Util** | Pure calculation logic (pharmacokinetics, chart data generation) |

---

## 🧪 Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material 3 with Expressive motion |
| Navigation | Navigation 3 |
| Local Database | Room |
| Preferences | DataStore |
| Charts | Vico (Compose + M3 theming) |
| Image Loading | Coil 3 |
| Typography | Montserrat (Google Fonts) |
| Min SDK | 31 (Android 12) |
| Target SDK | 36 |

---

## 🔬 Caffeine Science

The app uses a **one-compartment oral pharmacokinetic model**:

- **Absorption phase:** linear ramp from ingestion to peak based on drink-specific absorption rate
- **Elimination phase:** exponential decay governed by your personal half-life setting
- **Half-life personalization:** computed from age, weight, smoking, alcohol, liver health, and CYP1A2-inhibiting medications during onboarding

This keeps the curve and sleep forecast grounded in real pharmacology while remaining practical for daily use.

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.3) or newer
- **JDK 11+**
- **Android SDK 36** installed via SDK Manager

### Build & Run

```bash
# Clone the repository
git clone https://github.com/ohuc/CaffeineHealth.git
cd CaffeineHealth

# Open in Android Studio and sync Gradle, or build from CLI:
./gradlew assembleDebug

# Install on a connected device / emulator:
./gradlew installDebug
```

> **Note:** The app requires Android 12 (API 31) or higher. `compileSdk` targets API 36 - use an emulator with API 31+ to run it.

### Project Structure

```
CaffeineHealth/
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── assets/         # Drink preset JSON database
│   │   ├── java/           # Kotlin source code
│   │   └── res/            # Resources (drawables, fonts, values)
│   └── build.gradle.kts    # App-level dependencies
├── build.gradle.kts        # Project-level config
├── settings.gradle.kts     # Module includes
└── gradle/                 # Gradle wrapper & version catalog
```

---

## 🎨 Design

Caffeine Health is built with **Material 3 Expressive** principles:

- **Dynamic Color:** adapts to your device wallpaper on Android 12+ with a curated espresso-toned fallback palette
- **Expressive Motion:** `MotionScheme.expressive()` for fluid, personality-rich transitions
- **Montserrat typography:** clean, modern font family throughout the entire app
- **Haptic feedback:** tactile responses on key interactions across dashboard, catalog, and settings
- **Dark mode:** full dark theme support with carefully tuned surface/container colors

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** your changes: `git commit -m 'Add amazing feature'`
4. **Push** to the branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

### Guidelines

- Follow existing code style and architecture patterns
- Keep the shared ViewModel pattern - avoid per-screen ViewModels unless there's a strong reason
- Use Material 3 components and the app's existing design tokens
- Test on API 31+ emulator before submitting

---

## 🌍 Translation

This project is available on [Hosted Weblate](https://hosted.weblate.org/engage/caffeine-health/) for translation.

You can contribute to this project even if you are not a developer by helping in translating this project into languages you know.

<a href="https://hosted.weblate.org/engage/caffeine-health/"><img src="https://hosted.weblate.org/widget/caffeine-health/strings/287x66-grey.png" alt="Translation status"></a>

---

## ⭐ Star History

<a href="https://star-history.com/#ohuc/CaffeineHealth&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=ohuc/CaffeineHealth&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=ohuc/CaffeineHealth&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=ohuc/CaffeineHealth&type=Date" />
 </picture>
</a>

---

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Vico](https://github.com/patrykandpatrick/vico) - beautiful charting library for Compose
- [Coil](https://coil-kt.github.io/coil/) - fast image loading for Kotlin
- [Material 3](https://m3.material.io/) - Google's latest design system
- [Montserrat](https://fonts.google.com/specimen/Montserrat) - typeface by Julieta Ulanovsky

---

<p align="center">
  Made with ☕ and Kotlin
</p>

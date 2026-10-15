package com.uc.caffeine.data

/**
 * What this release introduces, in the order What's New shows it; the first entry is the headline.
 * Replace the entries for the next release.
 *
 * Seen state is stored by entry name rather than version code, so new entries reach people who
 * already dismissed the previous ones without depending on a version bump.
 */
enum class NewFeature {
    BarcodeScanner,
    Analytics,
    DrinkSearch,
    TeaSteeps,
    Widgets,
    ExpressiveBlur,
}

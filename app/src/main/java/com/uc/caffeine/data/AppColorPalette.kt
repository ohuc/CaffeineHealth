package com.uc.caffeine.data

enum class AppColorPalette {
    DYNAMIC,
    ESPRESSO,
    MATCHA,
    OCEAN,
    SAKURA;

    companion object {
        fun fromStorage(value: String?): AppColorPalette =
            entries.find { it.name == value } ?: DYNAMIC
    }
}

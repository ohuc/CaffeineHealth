package com.uc.caffeine.data

enum class HomeViewMode {
    GRAPH,
    CIRCULAR;

    companion object {
        fun fromStorage(value: String?): HomeViewMode =
            entries.firstOrNull { it.name == value } ?: GRAPH
    }
}

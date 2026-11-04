package com.uc.caffeine.util

/** Selecting a category in a visible list replaces the previous category's detail history. */
internal fun <T> MutableList<T>.openPane(destination: T) {
    if (size == 2 && last() == destination) return
    while (size > 1) removeAt(lastIndex)
    add(destination)
}

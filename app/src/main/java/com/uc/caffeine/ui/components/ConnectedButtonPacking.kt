package com.uc.caffeine.ui.components

/** For each original index: (position inside its row, number of options in that row). */
internal fun connectedButtonPositions(rows: List<IntRange>, itemCount: Int): List<Pair<Int, Int>> {
    val positions = MutableList(itemCount) { 0 to 1 }
    rows.forEach { row ->
        row.forEachIndexed { position, item -> positions[item] = position to row.count() }
    }
    return positions
}

/** Consecutive options that can share equal-width cells without wrapping their preferred content. */
internal fun packConnectedButtonRows(
    preferredWidths: List<Int>,
    availableWidth: Int,
    spacing: Int,
): List<IntRange> {
    require(availableWidth >= 0)
    require(spacing >= 0)
    require(preferredWidths.all { it >= 0 })
    val rows = mutableListOf<IntRange>()
    var start = 0
    while (start < preferredWidths.size) {
        var end = start
        var widest = preferredWidths[start]
        while (end + 1 < preferredWidths.size) {
            val nextWidest = maxOf(widest, preferredWidths[end + 1])
            val count = end - start + 2
            val requiredWidth = nextWidest.toLong() * count + spacing.toLong() * (count - 1)
            if (requiredWidth > availableWidth) break
            widest = nextWidest
            end++
        }
        rows += start..end
        start = end + 1
    }
    return rows
}

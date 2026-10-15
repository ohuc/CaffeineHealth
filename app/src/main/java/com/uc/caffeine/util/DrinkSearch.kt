package com.uc.caffeine.util

import com.uc.caffeine.data.model.DrinkPreset
import java.text.Normalizer
import java.util.Locale

/**
 * Typo-tolerant drink search for the Add screen.
 *
 * Every word of the query has to match a word of the drink's name or brand: whole, as a prefix (so
 * results narrow while typing), inside a longer word ("espresso" finds "Nespresso"), or against two
 * adjacent words joined ("redbull" finds "Red Bull"). Case, accents and punctuation are ignored, so
 * "nescafe" finds "Nescafé" and "coca cola" finds "Coca-Cola". Descriptions only match on word
 * prefixes.
 *
 * A single slip in a longer word ("expresso", "capuccino") always matches. Looser typos only count
 * when nothing matches better; otherwise "cola" would also bring up "Cold Brew". Results come back
 * best match first, keeping the incoming order (catalog relevance) among equal matches.
 */
fun searchDrinks(drinks: List<DrinkPreset>, query: String): List<DrinkPreset> {
    val queryWords = searchWords(query)
    if (queryWords.isEmpty()) return drinks

    val matches = drinks.mapNotNull { drink -> matchDrink(drink, queryWords)?.let { drink to it } }
    val confident = matches.filter { (_, match) -> match.fit != Fit.LooseTypo }
    return confident.ifEmpty { matches }
        .sortedByDescending { (_, match) -> match.score }
        .map { (drink, _) -> drink }
}

/** How a query word matched, from best to worst; a drink's fit is that of its worst word. */
private enum class Fit { Clean, CloseTypo, LooseTypo }

private class Match(val score: Double, val fit: Fit)

private fun matchDrink(drink: DrinkPreset, queryWords: List<String>): Match? {
    val nameWords = searchWords(drink.name)
    val titleWords = nameWords + searchWords(drink.brand) + nameWords.zipWithNext { a, b -> a + b }
    val descriptionWords by lazy { searchWords(drink.description.orEmpty()) }

    var total = 0.0
    var fit = Fit.Clean
    for (queryWord in queryWords) {
        val titleMatch = titleWords.mapNotNull { matchWord(queryWord, it) }.maxByOrNull { it.score }
        // A clean match anywhere beats a typo match in the title.
        val match = titleMatch?.takeIf { it.fit == Fit.Clean }
            ?: Match(DESCRIPTION_SCORE, Fit.Clean).takeIf {
                queryWord.length >= 3 && descriptionWords.any { word -> word.startsWith(queryWord) }
            }
            ?: titleMatch
            ?: return null
        total += match.score
        fit = maxOf(fit, match.fit)
    }
    // Ties go to drinks whose name leads with what was typed: "tea" ranks "Tea (Black)" above
    // "Arizona Green Tea".
    val leadBonus = if (nameWords.firstOrNull()?.startsWith(queryWords.first()) == true) 0.05 else 0.0
    return Match(score = total / queryWords.size + leadBonus, fit = fit)
}

private fun matchWord(query: String, word: String): Match? {
    if (word == query) return Match(1.0, Fit.Clean)
    if (word.startsWith(query)) return Match(0.9, Fit.Clean)
    // Shorter fragments hit too many unrelated words: "tea" in "steamer", "cola" in "chocolate".
    if (query.length >= 5 && query in word) return Match(0.7, Fit.Clean)

    val allowedEdits = allowedEdits(query.length)
    if (allowedEdits == 0) return null
    // Budgeted on the shorter of the two: two edits turn "chocla" into "cola", but that's half
    // of "cola".
    val fullDistance = editDistance(query, word)
    if (fullDistance <= allowedEdits(minOf(query.length, word.length))) {
        val fit = if (fullDistance == 1 && query.length >= 5) Fit.CloseTypo else Fit.LooseTypo
        return Match(0.6 - 0.1 * (fullDistance - 1), fit)
    }
    // The word cut to about the query's length, so a typo still matches mid-word ("expr" -> "espresso").
    if (query.length >= 4 && word.length > query.length) {
        val prefixDistance = (query.length - 1..query.length + 1).minOf { editDistance(query, word.take(it)) }
        if (prefixDistance <= allowedEdits) return Match(0.5 - 0.1 * (prefixDistance - 1), Fit.LooseTypo)
    }
    return null
}

private fun allowedEdits(length: Int): Int = when {
    length < 3 -> 0
    length < 6 -> 1
    else -> 2
}

/** Optimal string alignment distance: Levenshtein, plus swapped neighbours count as one edit. */
private fun editDistance(a: String, b: String): Int {
    val rows = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) rows[i][0] = i
    for (j in 0..b.length) rows[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var best = minOf(rows[i - 1][j] + 1, rows[i][j - 1] + 1, rows[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                best = minOf(best, rows[i - 2][j - 2] + 1)
            }
            rows[i][j] = best
        }
    }
    return rows[a.length][b.length]
}

private val combiningMarks = Regex("\\p{Mn}+")
private val separators = Regex("[^\\p{L}\\p{N}]+")

private fun searchWords(text: String): List<String> =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)
        .split(separators)
        .filter { it.isNotEmpty() }

private const val DESCRIPTION_SCORE = 0.3

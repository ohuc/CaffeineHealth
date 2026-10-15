package com.uc.caffeine.util

import com.uc.caffeine.data.model.DrinkPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkSearchTest {

    private val espresso = drink("Espresso")
    private val dripCoffee = drink("Drip coffee")
    private val cappuccino = drink("Cappuccino")
    // Misspelt in the catalog itself.
    private val dolceGustoCappuccino = drink("Nescafé Dolce Gusto - Capuccino")
    private val nespresso = drink("Nespresso Vertuo Pod - 230 ml")
    private val caffeLatte = drink("Caffè Latte")
    private val coldBrew = drink("Cold Brew")
    private val redBull = drink("Red Bull")
    private val monster = drink("Monster Energy")
    private val cocaCola = drink("Coca-Cola")
    private val drPepper = drink("Dr. Pepper")
    private val blackTea = drink("Tea (Black)")
    private val arizona = drink("Arizona Arnold Palmer", description = "Half iced tea, half lemonade.")
    private val yerbaMate = drink("Guayakí Yerba Mate")
    private val steamer = drink("Vanilla Steamer", description = "Steamed milk with vanilla syrup.")
    private val hotChocolate = drink("Hot Chocolate")

    private val catalog = listOf(
        espresso, dripCoffee, cappuccino, dolceGustoCappuccino, nespresso, caffeLatte, coldBrew,
        redBull, monster, cocaCola, drPepper, blackTea, arizona, yerbaMate, steamer, hotChocolate,
    )

    @Test
    fun blankQueryKeepsCatalogOrder() {
        assertEquals(catalog, searchDrinks(catalog, "  "))
    }

    @Test
    fun ignoresCaseAccentsAndPunctuation() {
        assertEquals(listOf(dolceGustoCappuccino), searchDrinks(catalog, "NESCAFE"))
        assertEquals(listOf(cocaCola), searchDrinks(catalog, "coca cola"))
        assertEquals(listOf(drPepper), searchDrinks(catalog, "dr pepper"))
        assertEquals(listOf(caffeLatte), searchDrinks(catalog, "caffe latte"))
        assertEquals(listOf(yerbaMate), searchDrinks(catalog, "guayaki"))
    }

    @Test
    fun matchesWordsJoinedOrInAnyOrder() {
        assertEquals(listOf(redBull), searchDrinks(catalog, "redbull"))
        assertEquals(listOf(cocaCola), searchDrinks(catalog, "cocacola"))
        assertEquals(listOf(dripCoffee), searchDrinks(catalog, "coffee drip"))
    }

    @Test
    fun narrowsWhileTyping() {
        assertEquals(listOf(monster), searchDrinks(catalog, "monst"))
        assertEquals(listOf(redBull), searchDrinks(catalog, "red bu"))
    }

    @Test
    fun toleratesTypos() {
        assertEquals(espresso, searchDrinks(catalog, "expresso").first())
        assertEquals(espresso, searchDrinks(catalog, "esspresso").first())
        assertEquals(listOf(monster), searchDrinks(catalog, "monstr"))
        assertEquals(listOf(redBull), searchDrinks(catalog, "redbul"))
        assertEquals(listOf(dripCoffee), searchDrinks(catalog, "cofee"))
        assertEquals(listOf(caffeLatte), searchDrinks(catalog, "caffe latet"))
        assertEquals(listOf(hotChocolate), searchDrinks(catalog, "chocla"))
    }

    @Test
    fun closeTyposRankBelowCleanMatches() {
        assertEquals(listOf(cappuccino, dolceGustoCappuccino), searchDrinks(catalog, "cappuccino"))
        assertEquals(listOf(dolceGustoCappuccino, cappuccino), searchDrinks(catalog, "capuccino"))
    }

    @Test
    fun looseTyposOnlyApplyWhenNothingMatchesCleanly() {
        assertEquals(listOf(cocaCola), searchDrinks(catalog, "cola"))
        assertTrue(blackTea in searchDrinks(catalog, "tae"))
    }

    @Test
    fun shortFragmentsDoNotMatchInsideUnrelatedWords() {
        assertEquals(listOf(blackTea, arizona), searchDrinks(catalog, "tea"))
        assertEquals(listOf(cocaCola), searchDrinks(catalog, "cola"))
    }

    @Test
    fun longerFragmentsMatchInsideWords() {
        assertEquals(listOf(espresso, nespresso), searchDrinks(catalog, "espresso"))
    }

    @Test
    fun unrelatedQueryFindsNothing() {
        assertEquals(emptyList<DrinkPreset>(), searchDrinks(catalog, "zzzz"))
    }

    private fun drink(name: String, description: String? = null) =
        DrinkPreset(itemId = name, name = name, description = description)
}

package com.uc.caffeine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenFoodFactsDraftTest {

    @Test
    fun energyDrinkCanCarriesTheWholeCan() {
        val draft = ScannedProduct(
            name = "Red Bull",
            categoryTags = listOf("en:beverages", "en:energy-drinks"),
            caffeineMgPer100 = 32.0,
            packageAmount = 250.0,
            packageUnit = "ml",
        ).toCustomDrinkDraft()
        assertEquals(CustomDrinkDraft("Red Bull", "energy_drink", "can", 80.0), draft)
    }

    @Test
    fun largeSodaIsABottle() {
        val draft = ScannedProduct("Cola", listOf("en:sodas", "en:colas"), 10.0, 1500.0, "ml").toCustomDrinkDraft()
        assertEquals("soft_drink", draft.category)
        assertEquals("bottle", draft.unitKey)
        assertEquals(150.0, draft.caffeineMg!!, 0.001)
    }

    @Test
    fun solidsAreLoggedByTheGram() {
        val draft = ScannedProduct("Instant coffee", listOf("en:instant-coffees"), 3200.0, 40.0, "g").toCustomDrinkDraft()
        assertEquals(CustomDrinkDraft("Instant coffee", "coffee", "g", 32.0), draft)
    }

    @Test
    fun missingCaffeineLeavesItBlank() {
        val draft = ScannedProduct("Iced tea", listOf("en:iced-teas"), null, 330.0, "ml").toCustomDrinkDraft()
        assertEquals("tea", draft.category)
        assertNull(draft.caffeineMg)
    }

    @Test
    fun unknownSizeKeepsDefaults() {
        val draft = ScannedProduct("Mystery", emptyList(), null, null, null).toCustomDrinkDraft()
        assertEquals(CustomDrinkDraft(name = "Mystery"), draft)
    }

    @Test
    fun unknownSizeStillCarriesTheCaffeine() {
        val drink = ScannedProduct("Energy", listOf("en:beverages", "en:energy-drinks"), 32.0, null, null)
            .toCustomDrinkDraft()
        assertEquals(CustomDrinkDraft("Energy", "energy_drink", "ml", 0.32), drink)
        val powder = ScannedProduct("Powder", emptyList(), 3200.0, null, null).toCustomDrinkDraft()
        assertEquals(CustomDrinkDraft("Powder", "coffee", "g", 32.0), powder)
    }

    @Test
    fun packageSizeComesFromTheLabel() {
        assertEquals(250.0 to "ml", parsePackageSize("250 ml"))
        assertEquals(250.0 to "ml", parsePackageSize("4 x 250 ml"))
        assertEquals(330.0 to "ml", parsePackageSize("6x33cl"))
        assertEquals(1500.0 to "ml", parsePackageSize("1,5 L"))
        assertEquals(250.0 to "ml", parsePackageSize("8.4 FL OZ (250 mL)"))
        assertEquals(500.0 to "g", parsePackageSize("500g"))
        assertNull(parsePackageSize("250"))
        assertNull(parsePackageSize(""))
    }

    @Test
    fun fluidOuncesAloneBecomeMillilitres() {
        val (amount, unit) = parsePackageSize("12 fl oz")!!
        assertEquals("ml", unit)
        assertEquals(354.9, amount, 0.1)
    }

    @Test
    fun caffeineIsReadFromTheIngredients() {
        val redBull = "Water, Sucrose, Taurine (0.4%), Colours (Caramel), Caffeine (0.03%), Vitamins (B6, B12)."
        assertEquals(30.0, caffeineFromIngredients(redBull)!!, 0.001)
        assertEquals(32.0, caffeineFromIngredients("Wasser, KOFFEIN (0,032 %)")!!, 0.001)
        assertNull(caffeineFromIngredients("Water, Caffeine, Guarana extract (22% caffeine)"))
        assertNull(caffeineFromIngredients(""))
    }

    @Test
    fun productNamesMatchTheDrinkTheyAre() {
        assertEquals(drinkMatchKey("Red Bull"), drinkMatchKey("Red Bull Energy Drink"))
        assertEquals(drinkMatchKey("Red Bull"), drinkMatchKey("Red Bull® Energy Drink 250 ml"))
        assertEquals(drinkMatchKey("Red Bull Sugarfree"), drinkMatchKey("Red Bull Sugar Free"))
        assertNotEquals(drinkMatchKey("Red Bull"), drinkMatchKey("Red Bull Sugar Free"))
        assertNotEquals(drinkMatchKey("Red Bull"), drinkMatchKey("Red Bull Zero"))
        assertEquals(drinkMatchKey("Café Latte"), drinkMatchKey("cafe latte"))
        assertEquals("", drinkMatchKey("Energy Drink"))
    }
}

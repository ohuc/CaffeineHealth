package com.uc.caffeine.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeaSteepsTest {

    @Test
    fun brewedTeasCanBeResteeped() {
        assertTrue(canBeResteeped("tea", listOf("cup", "teabag"), "teabag"))
        assertTrue(canBeResteeped("tea", listOf("cup", "teabag"), "cup"))
        assertTrue(canBeResteeped("tea", listOf("cup", "liter", "ml"), "ml"))
        assertTrue(canBeResteeped("tea", listOf("cup", "teabag", "g"), "g"))
    }

    @Test
    fun bottledTeasCannot() {
        assertFalse(canBeResteeped("tea", listOf("ml", "can", "liter", "fl oz"), "ml"))
        assertFalse(canBeResteeped("tea", listOf("can", "bottle", "teabag"), "teabag"))
    }

    @Test
    fun matchaAndServedDrinksCannot() {
        assertFalse(canBeResteeped("tea", listOf("teaspoon", "cup"), "cup"))
        assertFalse(canBeResteeped("tea", listOf("cup(small)", "cup(medium)"), "cup(medium)"))
    }

    @Test
    fun otherCategoriesCannot() {
        assertFalse(canBeResteeped("coffee", listOf("cup", "ml"), "cup"))
    }

    @Test
    fun laterSteepsCarryLessCaffeine() {
        assertTrue(TeaSteep.First.caffeineFactor > TeaSteep.Second.caffeineFactor)
        assertTrue(TeaSteep.Second.caffeineFactor > TeaSteep.ThirdOrLater.caffeineFactor)
    }
}

package com.uc.caffeine.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationTest {
    @Test fun changingCategoryReplacesItsNestedHistory() {
        val stack = mutableListOf("settings", "profile", "sleep rota")
        stack.openPane("appearance")
        assertEquals(listOf("settings", "appearance"), stack)
    }

    @Test fun reselectingCategoryReturnsToItsRoot() {
        val stack = mutableListOf("settings", "profile", "sleep rota")
        stack.openPane("profile")
        assertEquals(listOf("settings", "profile"), stack)
        stack.openPane("profile")
        assertEquals(listOf("settings", "profile"), stack)
    }
}

package com.uc.caffeine.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class YesNoOrderTest {

    @Test
    fun yesNoOptions_putsNoBeforeYes() {
        assertEquals(listOf(false, true), yesNoOptions())
    }

    @Test
    fun noneFirstEnums_matchYesNoDirection() {
        assertEquals(SmokingHabit.None, SmokingHabit.entries.first())
        assertEquals(LiverDisease.None, LiverDisease.entries.first())
    }
}

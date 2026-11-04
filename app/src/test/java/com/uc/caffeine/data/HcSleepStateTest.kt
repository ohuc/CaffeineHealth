package com.uc.caffeine.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HcSleepStateTest {

    @Test
    fun notInstalled_winsOverEverything() {
        assertEquals(HcSleepState.NOT_INSTALLED, hcSleepState(installed = false, sleepGranted = true, sleepEnabled = true, bedtimeKnown = true))
        assertEquals(HcCaffeineState.NOT_INSTALLED, hcCaffeineState(installed = false, granted = true, enabled = true))
    }

    @Test
    fun sleepWithoutPermission_isNotGranted_evenWhenSwitchedOn() {
        assertEquals(HcSleepState.NOT_GRANTED, hcSleepState(installed = true, sleepGranted = false, sleepEnabled = true, bedtimeKnown = true))
        assertEquals(HcSleepState.NOT_GRANTED, hcSleepState(installed = true, sleepGranted = false, sleepEnabled = false, bedtimeKnown = false))
    }

    @Test
    fun grantedButSwitchedOff_isOff() {
        assertEquals(HcSleepState.OFF, hcSleepState(installed = true, sleepGranted = true, sleepEnabled = false, bedtimeKnown = true))
    }

    @Test
    fun onWithoutABedtimeYet_waitsForData() {
        assertEquals(HcSleepState.WAITING_FOR_DATA, hcSleepState(installed = true, sleepGranted = true, sleepEnabled = true, bedtimeKnown = false))
    }

    @Test
    fun onWithABedtime_isActive() {
        assertEquals(HcSleepState.ACTIVE, hcSleepState(installed = true, sleepGranted = true, sleepEnabled = true, bedtimeKnown = true))
    }

    @Test
    fun caffeine_enabledButRevoked_isNotGranted() {
        assertEquals(HcCaffeineState.NOT_GRANTED, hcCaffeineState(installed = true, granted = false, enabled = true))
    }

    @Test
    fun caffeine_grantedAndOff_isOff_andOn_isActive() {
        assertEquals(HcCaffeineState.OFF, hcCaffeineState(installed = true, granted = true, enabled = false))
        assertEquals(HcCaffeineState.ACTIVE, hcCaffeineState(installed = true, granted = true, enabled = true))
    }
}

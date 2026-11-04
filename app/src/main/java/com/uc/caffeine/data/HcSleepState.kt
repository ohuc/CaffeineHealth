package com.uc.caffeine.data

/** What the Health Connect screen has to say about sleep, derived from facts only. */
enum class HcSleepState { NOT_INSTALLED, NOT_GRANTED, OFF, WAITING_FOR_DATA, ACTIVE }

/** The same idea for caffeine sync (nutrition and hydration). */
enum class HcCaffeineState { NOT_INSTALLED, NOT_GRANTED, OFF, ACTIVE }

/**
 * A missing permission outranks the saved switch: a user whose sleep access was revoked in Health
 * Connect must see "not provided" even though the setting still says on.
 */
fun hcSleepState(installed: Boolean, sleepGranted: Boolean, sleepEnabled: Boolean, bedtimeKnown: Boolean): HcSleepState = when {
    !installed -> HcSleepState.NOT_INSTALLED
    !sleepGranted -> HcSleepState.NOT_GRANTED
    !sleepEnabled -> HcSleepState.OFF
    !bedtimeKnown -> HcSleepState.WAITING_FOR_DATA
    else -> HcSleepState.ACTIVE
}

fun hcCaffeineState(installed: Boolean, granted: Boolean, enabled: Boolean): HcCaffeineState = when {
    !installed -> HcCaffeineState.NOT_INSTALLED
    !granted -> HcCaffeineState.NOT_GRANTED
    !enabled -> HcCaffeineState.OFF
    else -> HcCaffeineState.ACTIVE
}

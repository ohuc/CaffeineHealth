package com.uc.caffeine.monetization

data class PlusSettingState(val locked: Boolean, val savedEnabled: Boolean) {
    val canOpenSettings: Boolean get() = !locked || savedEnabled
    val canTurnOff: Boolean get() = savedEnabled
}

fun FeatureAccess.plusSettingState(feature: ProFeature, savedEnabled: Boolean = false) =
    PlusSettingState(locked = !canUse(feature), savedEnabled = savedEnabled)

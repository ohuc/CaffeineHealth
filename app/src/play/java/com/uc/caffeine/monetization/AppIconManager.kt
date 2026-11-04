package com.uc.caffeine.monetization

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Switches the launcher icon between the two Play activity aliases. The enabled alias is the
 * stored choice, so nothing else needs persisting. Enable the new alias before disabling the
 * old one so the app is never without a launcher entry.
 */
internal object AppIconManager {
    private const val CLASSIC = "com.uc.caffeine.LauncherClassic"
    private const val PLUS = "com.uc.caffeine.LauncherPlus"

    fun aliasFor(plus: Boolean): String = if (plus) PLUS else CLASSIC

    fun isPlusIcon(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(ComponentName(context, PLUS)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setPlusIcon(context: Context, plus: Boolean) {
        if (isPlusIcon(context) == plus) return
        val (enable, disable) = if (plus) PLUS to CLASSIC else CLASSIC to PLUS
        val pm = context.packageManager
        pm.setComponentEnabledSetting(ComponentName(context, enable),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(ComponentName(context, disable),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}

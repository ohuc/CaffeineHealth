package com.uc.caffeine.monetization

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process

/**
 * Changing the enabled launcher alias can make Android kill the running app, so the icon is
 * never switched under the user's feet. Either:
 *  - [restartNow]: a helper activity in its own process switches the alias and relaunches the
 *    app through the new icon (the same approach as ProcessPhoenix), or
 *  - [applyWhenAppCloses]: the change is stored and applied once no activity is started.
 */
internal object IconSwitcher {
    private const val PREFS = "plus_app_icon"
    private const val KEY_PENDING = "pending_plus_icon"
    internal const val EXTRA_PLUS = "plus"
    internal const val EXTRA_PID = "pid"

    fun restartNow(activity: Activity, plus: Boolean) {
        clearPending(activity)
        activity.startActivity(
            Intent(activity, IconRestartActivity::class.java)
                .putExtra(EXTRA_PLUS, plus)
                .putExtra(EXTRA_PID, Process.myPid())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        activity.finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    fun applyWhenAppCloses(context: Context, plus: Boolean) {
        prefs(context).edit().putBoolean(KEY_PENDING, plus).apply()
    }

    /** The icon waiting for the app to close, or null when nothing is pending. */
    fun pending(context: Context): Boolean? =
        prefs(context).takeIf { it.contains(KEY_PENDING) }?.getBoolean(KEY_PENDING, false)

    fun clearPending(context: Context) {
        prefs(context).edit().remove(KEY_PENDING).apply()
    }

    /** Applies a pending change when the last started activity stops (not on rotation). */
    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: Activity) { started++ }
            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
                if (started == 0 && !activity.isChangingConfigurations) {
                    val plus = pending(application) ?: return
                    clearPending(application)
                    AppIconManager.setPlusIcon(application, plus)
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Runs in the ":iconswitch" process: stop the app, switch the alias, open the app again. */
class IconRestartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val plus = intent.getBooleanExtra(IconSwitcher.EXTRA_PLUS, false)
        intent.getIntExtra(IconSwitcher.EXTRA_PID, -1).takeIf { it > 0 }?.let(Process::killProcess)
        AppIconManager.setPlusIcon(this, plus)
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(this, AppIconManager.aliasFor(plus)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
        Runtime.getRuntime().exit(0)
    }
}

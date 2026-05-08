package com.uc.caffeine.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.setWidgetPreviews
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CaffeineWidgetUpdater {

    private const val WORK_NAME = "caffeine_widget_refresh"
    private const val TAG = "CaffeineWidgetUpdater"

    suspend fun update(context: Context) {
        CaffeineNowWidget().updateAll(context)
        QuickAddWidget().updateAll(context)
        CombinedWidget().updateAll(context)
    }

    fun schedulePeriodicRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CaffeineDecayWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    /**
     * Publishes Glance-rendered previews to the system widget picker on Android 15+.
     * No-op on earlier versions — the appwidget-provider's `previewImage` drawable is used instead.
     * Rate limited by the platform (~2 calls/hour); failures are logged but not surfaced.
     */
    suspend fun publishWidgetPreviews(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val mgr = GlanceAppWidgetManager(context)
        runCatching { mgr.setWidgetPreviews<CaffeineNowWidgetReceiver>() }
            .onFailure { Log.w(TAG, "setWidgetPreviews(CaffeineNow) failed", it) }
        runCatching { mgr.setWidgetPreviews<QuickAddWidgetReceiver>() }
            .onFailure { Log.w(TAG, "setWidgetPreviews(QuickAdd) failed", it) }
        runCatching { mgr.setWidgetPreviews<CombinedWidgetReceiver>() }
            .onFailure { Log.w(TAG, "setWidgetPreviews(Combined) failed", it) }
    }
}

package com.uc.caffeine.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        // Glance's ActionCallback runs in a suspend coroutine via goAsync(); doing the
        // update inline avoids the ~10s WorkManager scheduling delay that made the
        // refresh button feel unresponsive.
        CaffeineWidgetUpdater.update(context.applicationContext)
    }
}

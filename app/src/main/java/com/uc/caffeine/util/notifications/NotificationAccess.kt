package com.uc.caffeine.util.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class NotificationAccessAction { REQUEST, SETTINGS }

fun notificationAccessAction(
    sdk: Int,
    runtimeGranted: Boolean,
    previouslyRequested: Boolean,
    showRationale: Boolean,
): NotificationAccessAction = if (sdk >= 33 && !runtimeGranted && (!previouslyRequested || showRationale)) {
    NotificationAccessAction.REQUEST
} else {
    NotificationAccessAction.SETTINGS
}

data class NotificationAccess(
    val runtimeGranted: Boolean,
    val appEnabled: Boolean,
    val dailyEnabled: Boolean,
    val inactivityEnabled: Boolean,
) {
    companion object {
        fun read(context: Context): NotificationAccess {
            val manager = context.getSystemService(NotificationManager::class.java)
            val granted = Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val enabled = granted && manager.areNotificationsEnabled()
            fun channelEnabled(id: String) = enabled &&
                manager.getNotificationChannel(id)?.importance != NotificationManager.IMPORTANCE_NONE
            return NotificationAccess(granted, enabled,
                channelEnabled(NotificationChannels.CHANNEL_DAILY_REMINDER),
                channelEnabled(NotificationChannels.CHANNEL_INACTIVITY))
        }
    }
}

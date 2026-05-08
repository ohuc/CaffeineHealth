package com.uc.caffeine.util.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.uc.caffeine.R

object NotificationChannels {
    const val CHANNEL_DAILY_REMINDER = "daily_reminder"
    const val CHANNEL_INACTIVITY = "inactivity_reminder"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                context.getString(R.string.notification_channel_daily),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_daily_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INACTIVITY,
                context.getString(R.string.notification_channel_inactivity),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_inactivity_description)
            },
        )
    }
}

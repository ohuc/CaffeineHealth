package com.uc.caffeine.util.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.uc.caffeine.MainActivity
import com.uc.caffeine.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationScheduler.INTENT_ACTION) return
        if (!hasNotificationPermission(context)) return

        when (intent.getStringExtra(NotificationScheduler.EXTRA_NOTIFICATION_TYPE)) {
            NotificationScheduler.TYPE_DAILY_REMINDER -> {
                val hour = intent.getIntExtra(NotificationScheduler.EXTRA_HOUR, -1)
                val minute = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTE, -1)
                if (hour == -1 || minute == -1) return
                showDailyReminderNotification(context, hour, minute)
                NotificationScheduler.scheduleDailyReminder(context, hour, minute)
            }
            NotificationScheduler.TYPE_INACTIVITY -> {
                showInactivityNotification(context)
            }
        }
    }

    private fun showDailyReminderNotification(context: Context, hour: Int, minute: Int) {
        val bodyRes = when {
            hour < 12 -> R.string.notification_daily_morning
            hour < 17 -> R.string.notification_daily_afternoon
            else -> R.string.notification_daily_evening
        }
        val notificationId = NotificationScheduler.dailyReminderRequestCode(hour, minute)
        showNotification(
            context = context,
            notificationId = notificationId,
            channelId = NotificationChannels.CHANNEL_DAILY_REMINDER,
            title = context.getString(R.string.notification_daily_title),
            body = context.getString(bodyRes),
        )
    }

    private fun showInactivityNotification(context: Context) {
        showNotification(
            context = context,
            notificationId = 9999,
            channelId = NotificationChannels.CHANNEL_INACTIVITY,
            title = context.getString(R.string.notification_inactivity_title),
            body = context.getString(R.string.notification_inactivity_body),
        )
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}

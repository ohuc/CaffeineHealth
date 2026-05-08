package com.uc.caffeine.util.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.uc.caffeine.data.UserSettings
import java.util.Calendar

object NotificationScheduler {

    const val INTENT_ACTION = "com.uc.caffeine.NOTIFICATION_ALARM"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val TYPE_DAILY_REMINDER = "daily_reminder"
    const val TYPE_INACTIVITY = "inactivity"
    const val EXTRA_HOUR = "hour"
    const val EXTRA_MINUTE = "minute"

    private const val INACTIVITY_REQUEST_CODE = 9999
    private const val INACTIVITY_DAYS_MS = 5L * 24 * 60 * 60 * 1000

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = dailyReminderPendingIntent(context, hour, minute, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.setDozeCompat(nextAlarmMillis(hour, minute), pendingIntent)
    }

    fun cancelDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = dailyReminderPendingIntent(context, hour, minute, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleInactivityReminder(context: Context) {
        scheduleInactivityAt(context, System.currentTimeMillis() + INACTIVITY_DAYS_MS)
    }

    fun scheduleInactivityAfterLastOpen(context: Context, lastOpenedAt: Long) {
        val fireAt = if (lastOpenedAt == 0L) {
            System.currentTimeMillis() + INACTIVITY_DAYS_MS
        } else {
            maxOf(lastOpenedAt + INACTIVITY_DAYS_MS, System.currentTimeMillis() + 60_000L)
        }
        scheduleInactivityAt(context, fireAt)
    }

    fun cancelInactivityReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = inactivityPendingIntent(context, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun syncDailyReminders(context: Context, oldTimes: Set<String>, newTimes: Set<String>) {
        for (timeStr in oldTimes - newTimes) {
            val (h, m) = parseTime(timeStr) ?: continue
            cancelDailyReminder(context, h, m)
        }
        for (timeStr in newTimes - oldTimes) {
            val (h, m) = parseTime(timeStr) ?: continue
            scheduleDailyReminder(context, h, m)
        }
    }

    fun scheduleAllFromSettings(context: Context, settings: UserSettings, lastOpenedAt: Long = 0L) {
        for (timeStr in settings.dailyReminderTimes) {
            val (h, m) = parseTime(timeStr) ?: continue
            scheduleDailyReminder(context, h, m)
        }
        if (settings.inactivityReminderEnabled) {
            scheduleInactivityAfterLastOpen(context, lastOpenedAt)
        }
    }

    fun dailyReminderRequestCode(hour: Int, minute: Int): Int = 1000 + hour * 60 + minute

    fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    fun formatTimeKey(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    private fun scheduleInactivityAt(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = inactivityPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        alarmManager.setDozeCompat(triggerAtMillis, pendingIntent)
    }

    internal fun nextAlarmMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun dailyReminderPendingIntent(context: Context, hour: Int, minute: Int, flags: Int): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = INTENT_ACTION
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_DAILY_REMINDER)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(context, dailyReminderRequestCode(hour, minute), intent, flags)
    }

    private fun inactivityPendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = INTENT_ACTION
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_INACTIVITY)
        }
        return PendingIntent.getBroadcast(context, INACTIVITY_REQUEST_CODE, intent, flags)
    }
}

private fun AlarmManager.setDozeCompat(triggerAtMillis: Long, pendingIntent: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canScheduleExactAlarms()) {
        setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
        setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}

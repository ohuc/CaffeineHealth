package com.uc.caffeine.util.notifications

import android.content.Context

/**
 * The one record of "the app has already asked for notification access". Onboarding and
 * Settings > Notifications both read and write it, so a denial in one place is never forgotten
 * by the other and the app never asks twice on its own.
 */
object NotificationConsent {
    private const val FILE = "notification_consent"
    private const val KEY_REQUESTED = "requested"

    fun requested(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_REQUESTED, false)

    fun markRequested(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_REQUESTED, true).apply()
    }

    /** The reminders question in setup was answered (either way). Separate from [requested]: "Not now" leaves Settings free to ask once, in-app. */
    fun setupAnswered(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_SETUP_ANSWERED, false)

    fun markSetupAnswered(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_SETUP_ANSWERED, true).apply()
    }

    private const val KEY_SETUP_ANSWERED = "setup_answered"
}

data class ReminderPrefs(val dailyTimes: Set<String>, val inactivity: Boolean)

/** "Not now" really turns reminders off (there is no permission to hide behind below API 33). */
fun reminderPrefsAfterChoice(turnOn: Boolean, current: ReminderPrefs): ReminderPrefs =
    if (turnOn) current else ReminderPrefs(dailyTimes = emptySet(), inactivity = false)

/** The system dialog is only ever shown once on the app's own initiative. */
fun notificationPromptAllowed(sdk: Int, previouslyRequested: Boolean): Boolean =
    sdk >= 33 && !previouslyRequested

package com.uc.caffeine.util.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uc.caffeine.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val settings = repo.settingsFlow.first()
                val lastOpenedAt = repo.getLastAppOpenedAt()
                NotificationScheduler.scheduleAllFromSettings(context, settings, lastOpenedAt)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

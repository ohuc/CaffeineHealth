package com.uc.caffeine.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.uc.caffeine.R
import com.uc.caffeine.data.CaffeineDatabase
import com.uc.caffeine.data.HealthConnectManager
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.util.resolvedZoneId
import com.uc.caffeine.widget.CaffeineWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receives [AutomationContract.ACTION_LOG_DRINK] broadcasts from automation apps and logs
 * the drink. Exported so other apps can reach it; gated by the user's automation opt-in.
 */
class LogDrinkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AutomationContract.ACTION_LOG_DRINK) return
        val appContext = context.applicationContext
        val extras = intent.extras?.let { bundle ->
            @Suppress("DEPRECATION")
            bundle.keySet().associateWith { key -> bundle.get(key) }
        }.orEmpty()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(appContext, extras)
            } catch (e: Exception) {
                Log.e(TAG, "Automation log failed", e)
                toast(appContext, appContext.getString(R.string.automation_error_toast, e.message ?: ""))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, extras: Map<String, Any?>) {
        val settings = SettingsRepository(context).settingsFlow.first()
        if (!settings.automationEnabled) {
            Log.w(TAG, "Ignoring $extras: automation is disabled in settings")
            toast(context, context.getString(R.string.automation_disabled_toast))
            return
        }

        val request = when (val parsed = LogDrinkRequest.parse(extras, System.currentTimeMillis())) {
            is LogDrinkRequest.ParseResult.Invalid -> return fail(context, parsed.reason)
            is LogDrinkRequest.ParseResult.Valid -> parsed.request
        }

        val db = CaffeineDatabase.getDatabase(context)
        val preset = request.drinkId?.let { db.drinkPresetDao().getPresetByItemId(it) }
            ?: request.drinkName?.let { db.drinkPresetDao().getPresetByName(it) }
        val units = preset?.let { db.drinkUnitDao().getUnitsForDrink(it.id) }.orEmpty()

        val resolved = when (val resolution = resolveAutomationEntry(request, preset, units)) {
            is AutomationEntryResolution.Failed -> return fail(context, resolution.reason)
            is AutomationEntryResolution.Resolved -> resolution
        }

        val entry = resolved.entry
        val newId = db.consumptionLogDao().logDrink(entry)
        if (settings.healthConnectEnabled) {
            runCatching {
                HealthConnectManager(context).writeEntry(
                    entry.copy(id = newId.toInt()),
                    settings.resolvedZoneId(),
                    resolved.volumeMl,
                )
            }
        }
        CaffeineWidgetUpdater.update(context)
        toast(context, context.getString(R.string.automation_logged_toast, entry.drinkName, entry.caffeineMg))
    }

    private suspend fun fail(context: Context, reason: String) {
        Log.w(TAG, "Rejected automation request: $reason")
        toast(context, context.getString(R.string.automation_error_toast, reason))
    }

    private suspend fun toast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val TAG = "LogDrinkReceiver"
    }
}

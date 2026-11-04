package com.uc.caffeine.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** LocaleManager owns Android 13+; Android 12 stores the same explicit choice locally. */
object AppLocale {
    private fun preferences(context: Context) = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context
        val tag = preferences(context).getString("tag", null) ?: return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    fun select(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            preferences(activity).edit().putString("tag", tag).apply()
            Locale.setDefault(Locale.forLanguageTag(tag))
            // The Application survives Activity.recreate(); update its background notification strings too.
            val resources = activity.application.resources
            val configuration = Configuration(resources.configuration).apply { setLocales(LocaleList.forLanguageTags(tag)) }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            activity.recreate()
        }
    }

    fun migrateToPlatform(context: Context) {
        if (Build.VERSION.SDK_INT < 33) return
        val preferences = preferences(context)
        val tag = preferences.getString("tag", null) ?: return
        val manager = context.getSystemService(LocaleManager::class.java)
        if (manager.applicationLocales.isEmpty) manager.applicationLocales = LocaleList.forLanguageTags(tag)
        preferences.edit().remove("tag").apply()
    }
}

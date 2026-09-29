package com.uc.caffeine.ui.screens.settings

import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.text.Collator
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics

private data class AppLanguage(
    val tag: String,
    val flag: String,
    val nativeName: String,
)

private data class LanguageDisplay(val flag: String, val nativeName: String)

// Which locales the app ships is derived at runtime from the translated resources actually
// packaged in the APK (see buildSupportedLanguages) — the same values-<locale> directories
// AGP's generateLocaleConfig (app/build.gradle.kts) reads to build the OS-level locale list.
// This map only overrides display: a flag emoji (a language isn't a country, so it can't be
// derived) and a native name where Java's Locale display name isn't the one we want to show.
private val languageDisplayOverrides = mapOf(
    "en" to LanguageDisplay("🇬🇧", "English"),
    "nl" to LanguageDisplay("🇳🇱", "Nederlands"),
    "de" to LanguageDisplay("🇩🇪", "Deutsch"),
    "da" to LanguageDisplay("🇩🇰", "Dansk"),
    "is" to LanguageDisplay("🇮🇸", "Íslenska"),
    "sv" to LanguageDisplay("🇸🇪", "Svenska"),
    "nb" to LanguageDisplay("🇳🇴", "Norsk"),
    "fi" to LanguageDisplay("🇫🇮", "Suomi"),
    "es" to LanguageDisplay("🇪🇸", "Español"),
    "pt" to LanguageDisplay("🇵🇹", "Português"),
    "fr" to LanguageDisplay("🇫🇷", "Français"),
    "it" to LanguageDisplay("🇮🇹", "Italiano"),
    "pl" to LanguageDisplay("🇵🇱", "Polski"),
    "cs" to LanguageDisplay("🇨🇿", "Čeština"),
    "ro" to LanguageDisplay("🇷🇴", "Română"),
    "tr" to LanguageDisplay("🇹🇷", "Türkçe"),
    "ru" to LanguageDisplay("🇷🇺", "Русский"),
    "uk" to LanguageDisplay("🇺🇦", "Українська"),
    "ar" to LanguageDisplay("🇸🇦", "العربية"),
    "bn" to LanguageDisplay("🇧🇩", "বাংলা"),
    "hi" to LanguageDisplay("🇮🇳", "हिंदी"),
    "ml" to LanguageDisplay("🇮🇳", "മലയാളം"),
    "kn" to LanguageDisplay("🇮🇳", "ಕನ್ನಡ"),
    "te" to LanguageDisplay("🇮🇳", "తెలుగు"),
    "ta" to LanguageDisplay("🇮🇳", "தமிழ்"),
    "zh-CN" to LanguageDisplay("🇨🇳", "简体中文"),
)

private const val FALLBACK_FLAG = "🌐"

/**
 * Reads the locale-config AGP generates from this module's own values-<locale> directories
 * (generateLocaleConfig in app/build.gradle.kts) — the same list the OS shows in its
 * per-app language settings. Don't use AssetManager.getLocales() here: it also reports every
 * locale shipped by AndroidX/Material resources and the en-XA / ar-XB pseudo-locales.
 */
private fun readLocaleConfigTags(context: Context): List<String> {
    val tags = mutableListOf<String>()
    context.resources.getXml(R.xml._generated_res_locale_config).use { parser ->
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                parser.getAttributeValue(ANDROID_XML_NAMESPACE, "name")?.let(tags::add)
            }
        }
    }
    return tags
}

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"

private fun buildSupportedLanguages(context: Context): List<AppLanguage> {
    // The base values/ (English) directory is declared via resources.properties and is
    // normally listed already; add it defensively so English is always selectable.
    val tags = (readLocaleConfigTags(context) + "en").distinct()
    val all = tags.map { tag ->
        val override = languageDisplayOverrides[tag]
        AppLanguage(
            tag = tag,
            flag = override?.flag ?: FALLBACK_FLAG,
            nativeName = override?.nativeName ?: run {
                val locale = Locale.forLanguageTag(tag)
                locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
            },
        )
    }
    val pinned = listOf("en", "hi")
    return all.filter { it.tag in pinned }.sortedBy { pinned.indexOf(it.tag) } +
        all.filter { it.tag !in pinned }.sortedWith(compareBy(Collator.getInstance(Locale.ENGLISH)) { it.nativeName })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LanguageSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()

    val supportedLanguages = remember { buildSupportedLanguages(context) }

    val currentTag = remember {
        val appLocaleTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appLocales = context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales
            if (appLocales.isEmpty) null else appLocales[0]?.toLanguageTag()
        } else {
            null
        } ?: java.util.Locale.getDefault().language
        // App locale tags are region-qualified only where the entry needs it (e.g. zh-CN);
        // fall back to a base-language match so a plain system locale (e.g. "zh") still highlights it.
        supportedLanguages.firstOrNull { it.tag == appLocaleTag }?.tag
            ?: supportedLanguages.firstOrNull {
                it.tag.substringBefore('-') == appLocaleTag.substringBefore('-')
            }?.tag
            ?: appLocaleTag
    }
    var selectedTag by remember { mutableStateOf(currentTag) }

    SettingsPageScaffold(
        title = stringResource(R.string.settings_language_title),
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
        ) {
            itemsIndexed(supportedLanguages) { index, language ->
                val isSelected = selectedTag == language.tag
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        selectedTag = language.tag
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.getSystemService(android.app.LocaleManager::class.java)
                                .applicationLocales = LocaleList.forLanguageTags(language.tag)
                        }
                    },
                    leadingContent = {
                        Text(
                            text = language.flag,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    content = {
                        Text(text = language.nativeName)
                    },
                    trailingContent = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.language_selected_cd),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = supportedLanguages.size,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

package com.uc.caffeine.ui.screens.settings

import android.os.Build
import android.os.LocaleList
import java.text.Collator
import java.util.Locale
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

private val supportedLanguages = listOf(
    AppLanguage("en", "🇬🇧", "English"),
    AppLanguage("nl", "🇳🇱", "Nederlands"),
    AppLanguage("de", "🇩🇪", "Deutsch"),
    AppLanguage("da", "🇩🇰", "Dansk"),
    AppLanguage("is", "🇮🇸", "Íslenska"),
    AppLanguage("sv", "🇸🇪", "Svenska"),
    AppLanguage("nb", "🇳🇴", "Norsk"),
    AppLanguage("fi", "🇫🇮", "Suomi"),
    AppLanguage("es", "🇪🇸", "Español"),
    AppLanguage("pt", "🇵🇹", "Português"),
    AppLanguage("fr", "🇫🇷", "Français"),
    AppLanguage("it", "🇮🇹", "Italiano"),
    AppLanguage("pl", "🇵🇱", "Polski"),
    AppLanguage("cs", "🇨🇿", "Čeština"),
    AppLanguage("ro", "🇷🇴", "Română"),
    AppLanguage("tr", "🇹🇷", "Türkçe"),
    AppLanguage("ru", "🇷🇺", "Русский"),
    AppLanguage("uk", "🇺🇦", "Українська"),
    AppLanguage("ar", "🇸🇦", "العربية"),
    AppLanguage("bn", "🇧🇩", "বাংলা"),
    AppLanguage("hi", "🇮🇳", "हिंदी"),
    AppLanguage("ml", "🇮🇳", "മലയാളം"),
    AppLanguage("kn", "🇮🇳", "ಕನ್ನಡ"),
    AppLanguage("te", "🇮🇳", "తెలుగు"),
    AppLanguage("ta", "🇮🇳", "தமிழ்"),
    AppLanguage("zh-CN", "🇨🇳", "简体中文"),
).let { all ->
    val pinned = listOf("en", "hi")
    all.filter { it.tag in pinned }.sortedBy { pinned.indexOf(it.tag) } +
    all.filter { it.tag !in pinned }.sortedWith(compareBy(Collator.getInstance(Locale.ENGLISH)) { it.nativeName })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LanguageSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()

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

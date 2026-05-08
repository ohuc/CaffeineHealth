package com.uc.caffeine.ui.screens.settings

import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
    AppLanguage("hi", "🇮🇳", "हिंदी"),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LanguageSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()

    val currentTag = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appLocales = context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales
            if (appLocales.isEmpty) null else appLocales[0]?.language
        } else {
            null
        } ?: java.util.Locale.getDefault().language
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
            item {
                Text(
                    text = stringResource(R.string.language_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }

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
                                imageVector = Icons.Filled.Check,
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

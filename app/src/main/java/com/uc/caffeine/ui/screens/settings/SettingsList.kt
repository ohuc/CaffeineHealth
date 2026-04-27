package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.SettingsPageScaffold

private data class SettingsCategoryItem(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsListScreen(
    onCaffeineProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onDateTimeClick: () -> Unit,
    onHealthConnectClick: () -> Unit,
    onMyDataClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val categories = listOf(
        SettingsCategoryItem(
            title = "Caffeine Profile",
            summary = "Half-life, bedtime, safe threshold",
            icon = Icons.Filled.Tune,
            onClick = onCaffeineProfileClick,
        ),
        SettingsCategoryItem(
            title = "Appearance",
            summary = "Theme mode and dynamic color",
            icon = Icons.Filled.Palette,
            onClick = onAppearanceClick,
        ),
        SettingsCategoryItem(
            title = "Language",
            summary = "Coming later",
            icon = Icons.Filled.Language,
            onClick = onLanguageClick,
        ),
        SettingsCategoryItem(
            title = "Date and Time",
            summary = "Clock, date format, timezone",
            icon = Icons.Filled.Schedule,
            onClick = onDateTimeClick,
        ),
        SettingsCategoryItem(
            title = "Health Connect",
            summary = "Sync caffeine data to Health Connect",
            icon = Icons.Filled.Favorite,
            onClick = onHealthConnectClick,
        ),
        SettingsCategoryItem(
            title = "My Data",
            summary = "Export and import your data",
            icon = Icons.Filled.Storage,
            onClick = onMyDataClick,
        ),
    )

    SettingsPageScaffold(title = "Settings") { bottomPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
        ) {
            itemsIndexed(categories) { index, category ->
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        category.onClick()
                    },
                    leadingContent = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                        )
                    },
                    content = {
                        Text(text = category.title)
                    },
                    supportingContent = {
                        Text(text = category.summary)
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = categories.size,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

package com.uc.caffeine.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uc.caffeine.ui.components.SettingsPageScaffold

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LanguageSettingsScreen(
    onBack: () -> Unit,
) {
    SettingsPageScaffold(
        title = "Language",
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
                SegmentedListItem(
                    onClick = { },
                    content = {
                        Text(text = "Localization is deferred")
                    },
                    supportingContent = {
                        Text(text = "English remains the active app language in this build.")
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        index = 0,
                        count = 1,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

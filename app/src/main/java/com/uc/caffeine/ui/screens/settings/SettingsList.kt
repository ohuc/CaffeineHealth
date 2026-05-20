package com.uc.caffeine.ui.screens.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import com.uc.caffeine.ui.components.segmentedListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.uc.caffeine.BuildConfig
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.WhatsNewSheet
import com.uc.caffeine.ui.components.rememberAppHaptics

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
    onNotificationsClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    var showWhatsNewSheet by remember { mutableStateOf(false) }
    val categories = listOf(
        SettingsCategoryItem(
            title = stringResource(R.string.settings_caffeine_profile_title),
            summary = stringResource(R.string.settings_caffeine_profile_summary),
            icon = Icons.Filled.Tune,
            onClick = onCaffeineProfileClick,
        ),
        SettingsCategoryItem(
            title = stringResource(R.string.settings_appearance_title),
            summary = stringResource(R.string.settings_appearance_summary),
            icon = Icons.Filled.Palette,
            onClick = onAppearanceClick,
        ),
        SettingsCategoryItem(
            title = stringResource(R.string.settings_language_title),
            summary = stringResource(R.string.settings_language_summary),
            icon = Icons.Filled.Language,
            onClick = onLanguageClick,
        ),
        SettingsCategoryItem(
            title = stringResource(R.string.settings_date_time_title),
            summary = stringResource(R.string.settings_date_time_summary),
            icon = Icons.Filled.Schedule,
            onClick = onDateTimeClick,
        ),
        SettingsCategoryItem(
            title = stringResource(R.string.settings_health_connect_title),
            summary = stringResource(R.string.settings_health_connect_summary),
            icon = Icons.Filled.Favorite,
            onClick = onHealthConnectClick,
        ),
        SettingsCategoryItem(
            title = stringResource(R.string.settings_my_data_title),
            summary = stringResource(R.string.settings_my_data_summary),
            icon = Icons.Filled.Storage,
            onClick = onMyDataClick,
        ),
        SettingsCategoryItem(
            title = stringResource(R.string.settings_notifications_title),
            summary = stringResource(R.string.settings_notifications_summary),
            icon = Icons.Filled.NotificationsActive,
            onClick = onNotificationsClick,
        ),
    )

    val context = LocalContext.current
    val appIconPainter = remember(context) {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        drawable.setBounds(0, 0, 192, 192)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }

    SettingsPageScaffold(title = stringResource(R.string.settings_title)) { bottomPadding ->
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
                    shapes = segmentedListItemShapes(index, categories.size),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
                )
                SegmentedListItem(
                    onClick = {},
                    leadingContent = {
                        Image(
                            painter = appIconPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    },
                    content = {
                        Text(text = stringResource(R.string.app_name))
                    },
                    supportingContent = {
                        Text(text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                haptics.toggle()
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ohuc/CaffeineHealth"))
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = stringResource(R.string.settings_about_github_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    shapes = segmentedListItemShapes(0, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }

            item {
                SegmentedListItem(
                    onClick = {
                        haptics.toggle()
                        showWhatsNewSheet = true
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.NewReleases,
                            contentDescription = null,
                        )
                    },
                    content = {
                        Text(text = stringResource(R.string.settings_whats_new_title))
                    },
                    supportingContent = {
                        Text(text = stringResource(R.string.settings_whats_new_summary, BuildConfig.VERSION_NAME))
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    shapes = segmentedListItemShapes(1, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }

    if (showWhatsNewSheet) {
        WhatsNewSheet(onDismiss = { showWhatsNewSheet = false })
    }
}

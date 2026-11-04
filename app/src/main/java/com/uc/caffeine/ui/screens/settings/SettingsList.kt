package com.uc.caffeine.ui.screens.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.uc.caffeine.BuildConfig
import com.uc.caffeine.R
import com.uc.caffeine.monetization.DistributionUi
import com.uc.caffeine.ui.components.SettingsNavGroup
import com.uc.caffeine.ui.components.SettingsNavItem
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.WhatsNewSheet
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.components.settingsRowColors

/** Renders an adaptive launcher icon (or any drawable) into a painter for in-app previews. */
@Composable
internal fun rememberDrawablePainter(@DrawableRes resId: Int): Painter {
    val context = LocalContext.current
    return remember(context, resId) {
        val drawable = ContextCompat.getDrawable(context, resId)!!
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 192, 192)
        drawable.draw(AndroidCanvas(bitmap))
        BitmapPainter(bitmap.asImageBitmap())
    }
}

@Composable
internal fun SettingsListScreen(
    onCaffeineProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLookAndFeelClick: () -> Unit,
    onConnectionsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onWhatsNewSeen: () -> Unit,
    distributionUi: DistributionUi,
    onDistributionClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    selectedCategory: String? = null,
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    var showWhatsNewSheet by rememberSaveable { mutableStateOf(false) }

    val personal = listOf(
        SettingsNavItem(
            title = stringResource(R.string.settings_caffeine_profile_title),
            summary = stringResource(R.string.settings_caffeine_profile_summary),
            icon = Icons.Rounded.Tune,
            onClick = onCaffeineProfileClick,
            selected = selectedCategory == "profile",
        ),
        SettingsNavItem(
            title = stringResource(R.string.settings_notifications_title),
            summary = stringResource(R.string.settings_notifications_summary),
            icon = Icons.Rounded.NotificationsActive,
            onClick = onNotificationsClick,
            selected = selectedCategory == "notifications",
        ),
    )
    val app = listOf(
        SettingsNavItem(
            title = stringResource(R.string.settings_look_feel_title),
            summary = stringResource(R.string.settings_look_feel_summary),
            icon = Icons.Rounded.Palette,
            onClick = onLookAndFeelClick,
            selected = selectedCategory == "appearance",
        ),
        SettingsNavItem(
            title = stringResource(R.string.settings_connections_title),
            summary = stringResource(R.string.settings_connections_summary),
            icon = Icons.Rounded.Cable,
            onClick = onConnectionsClick,
            selected = selectedCategory == "connections",
        ),
    )
    val language = listOf(
        SettingsNavItem(
            title = stringResource(R.string.settings_language_title),
            summary = stringResource(R.string.settings_language_summary),
            icon = Icons.Rounded.Language,
            onClick = onLanguageClick,
            selected = selectedCategory == "language",
        ),
    )
    val brand = distributionUi.aboutBrand()
    val appIconPainter = rememberDrawablePainter(brand.icon)

    SettingsPageScaffold(title = stringResource(R.string.settings_title)) { bottomPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
        ) {
            item { distributionUi.SettingsEntry(onDistributionClick) }
            item { SettingsNavGroup(personal) }
            item { SettingsNavGroup(app) }
            item { SettingsNavGroup(language) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SettingsSectionLabel(stringResource(R.string.settings_about_title))
                    SegmentedListItem(
                        onClick = {},
                        leadingContent = {
                            Image(
                                painter = appIconPainter,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .pointerInput(distributionUi.hasDeveloperSettings) {
                                        // Hidden developer page: double-tap the app icon.
                                        if (distributionUi.hasDeveloperSettings) {
                                            detectTapGestures(onDoubleTap = { haptics.toggle(); onDeveloperClick() })
                                        }
                                    },
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME))
                                Text(stringResource(R.string.distribution_edition_label))
                            }
                        },
                        trailingContent = {
                            FilledTonalIconButton(
                                onClick = {
                                    haptics.toggle()
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ohuc/CaffeineHealth")),
                                    )
                                },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_github),
                                    contentDescription = stringResource(R.string.settings_about_github_cd),
                                )
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        shapes = segmentedListItemShapes(0, 3),
                        colors = settingsRowColors(),
                    ) {
                        Text(brand.name, style = MaterialTheme.typography.titleMediumEmphasized)
                    }
                    SegmentedListItem(
                        onClick = {
                            haptics.toggle()
                            showWhatsNewSheet = true
                        },
                        leadingContent = { Icon(Icons.Rounded.NewReleases, contentDescription = null) },
                        supportingContent = {
                            Text(stringResource(R.string.settings_whats_new_summary, BuildConfig.VERSION_NAME))
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                        verticalAlignment = Alignment.CenterVertically,
                        shapes = segmentedListItemShapes(1, 3),
                        colors = settingsRowColors(),
                    ) {
                        Text(stringResource(R.string.settings_whats_new_title), style = MaterialTheme.typography.titleMediumEmphasized)
                    }
                    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
                    SegmentedListItem(
                        onClick = {
                            haptics.toggle()
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl)))
                        },
                        leadingContent = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                        supportingContent = { Text(stringResource(R.string.privacy_policy_summary)) },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                        verticalAlignment = Alignment.CenterVertically,
                        shapes = segmentedListItemShapes(2, 3),
                        colors = settingsRowColors(),
                    ) {
                        Text(stringResource(R.string.privacy_policy_title), style = MaterialTheme.typography.titleMediumEmphasized)
                    }
                }
            }
        }
    }

    if (showWhatsNewSheet) {
        WhatsNewSheet(
            onDismiss = {
                showWhatsNewSheet = false
                onWhatsNewSeen()
            },
        )
    }
}

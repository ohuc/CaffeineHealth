package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uc.caffeine.LocalAppScaffoldPadding
import com.uc.caffeine.R

private val SettingsPageHorizontalPadding = 16.dp
private val SettingsPageTopPadding = 12.dp
private val SettingsHeaderBottomSpacing = 16.dp

@Composable
fun SettingsPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.(bottomPadding: Dp) -> Unit,
) {
    val appPadding = LocalAppScaffoldPadding.current
    val haptics = rememberAppHaptics()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(
                horizontal = SettingsPageHorizontalPadding,
                vertical = SettingsPageTopPadding,
            ),
    ) {
        if (showBackButton && onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = {
                    haptics.navigation()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Spacer(modifier = Modifier.height(SettingsHeaderBottomSpacing))
        content(appPadding.calculateBottomPadding())
    }
}

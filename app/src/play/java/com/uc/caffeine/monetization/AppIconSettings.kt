package com.uc.caffeine.monetization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.screens.settings.rememberDrawablePainter
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults

/**
 * Look & feel → App icon. Choosing never switches immediately: it opens the full-screen
 * icon screen, which explains the restart and offers "now" or "when I close the app".
 */
@Composable
internal fun AppIconPicker(
    owned: Boolean,
    refreshKey: Any?,
    onRequest: (plus: Boolean) -> Unit,
    onOpenPlus: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    // Re-read after the icon screen closes (it may have queued a change for later).
    val current = remember(refreshKey) { AppIconManager.isPlusIcon(context) }
    val pending = remember(refreshKey) { IconSwitcher.pending(context) }
    val shown = pending ?: current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionLabel(stringResource(R.string.plus_app_icon_title))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIconOption(
                label = stringResource(R.string.plus_app_icon_classic),
                iconRes = R.mipmap.ic_launcher,
                selected = !shown,
                locked = false,
                modifier = Modifier.weight(1f),
                onClick = { haptics.toggle(); if (shown) onRequest(false) },
            )
            AppIconOption(
                label = stringResource(R.string.plus_app_icon_plus),
                iconRes = R.mipmap.ic_launcher_plus,
                selected = shown,
                locked = !owned,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.toggle()
                    when {
                        !owned -> onOpenPlus()
                        !shown -> onRequest(true)
                    }
                },
            )
        }
        if (pending != null && pending != current) {
            Row(
                Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.icon_pending), style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            stringResource(R.string.plus_app_icon_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun AppIconOption(
    label: String,
    iconRes: Int,
    selected: Boolean,
    locked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else CaffeineSurfaceDefaults.groupedListContainerColor,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box {
                Image(
                    painter = rememberDrawablePainter(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)),
                )
                when {
                    selected -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp))
                    locked -> Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(20.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.titleSmallEmphasized, textAlign = TextAlign.Center)
            if (locked) {
                Text(stringResource(R.string.plus_app_icon_locked), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** On the Plus page for owners still on the classic icon. */
@Composable
internal fun AppIconOffer(onUsePlusIcon: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    if (AppIconManager.isPlusIcon(context) || IconSwitcher.pending(context) == true) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(28.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(
                    painter = rememberDrawablePainter(R.mipmap.ic_launcher_plus),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.plus_app_icon_offer_title), style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(stringResource(R.string.plus_app_icon_offer_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            FilledTonalButton(onClick = { haptics.toggle(); onUsePlusIcon() }, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.plus_app_icon_use))
            }
        }
    }
}

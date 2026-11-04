package com.uc.caffeine.monetization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.rememberAppHaptics

private val LocalPlusAccess = staticCompositionLocalOf<FeatureAccess?> { null }
val LocalOpenPlus = staticCompositionLocalOf<() -> Unit> { {} }
val LocalRequestPlusFeature = staticCompositionLocalOf<(ProFeature) -> Unit> { {} }

@Composable
fun ProvidePlusSettings(
    access: FeatureAccess,
    onOpenPlus: () -> Unit,
    onRequestFeature: (ProFeature) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPlusAccess provides access, LocalOpenPlus provides onOpenPlus,
        LocalRequestPlusFeature provides onRequestFeature, content = content)
}

@Composable
fun isPlusFeatureLocked(feature: ProFeature): Boolean {
    val access = LocalPlusAccess.current ?: return false
    val snapshot by access.snapshots.collectAsStateWithLifecycle()
    // Observe ownership changes, then ask the same feature gate used by operations.
    return snapshot.let { !access.canUse(feature) }
}

const val PlusLockedContentAlpha = 0.72f

@Composable
fun PlusDivider(label: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val haptics = rememberAppHaptics()
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        val fullWidth = maxWidth < 360.dp || fontScale > 1.3f
        val pill: @Composable () -> Unit = {
            val content: @Composable RowScope.() -> Unit = {
                PlusBadge(size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = if (fontScale > 1.3f) Int.MAX_VALUE else 2,
                    modifier = Modifier.weight(1f, fill = false))
            }
            val pillModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier.widthIn(max = maxWidth - 48.dp)
            if (onClick != null) Button(onClick = { haptics.toggle(); onClick() }, shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = pillModifier.heightIn(min = 48.dp), content = content)
            else Surface(pillModifier.heightIn(min = 48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
            }
        }
        if (fullWidth) pill() else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider(Modifier.weight(1f).widthIn(min = 16.dp).clip(CircleShape), thickness = 4.dp, color = MaterialTheme.colorScheme.outlineVariant)
            pill()
            HorizontalDivider(Modifier.weight(1f).widthIn(min = 16.dp).clip(CircleShape), thickness = 4.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

fun Modifier.plusLockedContent(locked: Boolean): Modifier =
    if (locked) graphicsLayer { alpha = PlusLockedContentAlpha } else this

@Composable
fun Modifier.plusLockedSemantics(
    locked: Boolean,
    savedValue: Boolean? = null,
    clickLabel: String = stringResource(R.string.plus_locked_click_label),
): Modifier {
    if (!locked) return this
    val description = stringResource(R.string.plus_locked_description) + (savedValue?.let {
        " " + stringResource(if (it) R.string.plus_saved_on else R.string.plus_saved_off)
    } ?: "")
    return semantics {
        stateDescription = description
        role = Role.Button
        onClick(label = clickLabel, action = null)
    }
}

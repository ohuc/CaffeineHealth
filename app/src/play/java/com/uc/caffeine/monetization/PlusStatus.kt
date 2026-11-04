package com.uc.caffeine.monetization

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

@Composable
internal fun PlusStatusStrip(strip: PlusStrip?, restoreEnabled: Boolean, refreshEnabled: Boolean,
    topSpacing: androidx.compose.ui.unit.Dp = 0.dp, onAction: (PlusAction) -> Unit) {
    val motion = LocalPlusMotionEnabled.current
    val effects = if (motion) MaterialTheme.motionScheme.defaultEffectsSpec<Float>() else snap()
    val fastEffects = if (motion) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap()
    val spatial = if (motion) MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>() else snap()
    val fastSpatial = if (motion) MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntSize>() else snap()
    var lastStrip by remember { mutableStateOf(strip) }
    if (strip != null) lastStrip = strip
    AnimatedVisibility(strip != null, enter = fadeIn(effects) + expandVertically(spatial),
        exit = fadeOut(fastEffects) + shrinkVertically(fastSpatial)) {
        AnimatedContent(strip ?: lastStrip,
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(fastEffects) using SizeTransform(clip = false) { _, _ -> spatial } }, label = "plus_status") { current ->
            current?.let {
                val colors = MaterialTheme.colorScheme
                val (container, foreground) = when (it.severity) {
                    PlusSeverity.PROBLEM -> colors.errorContainer to colors.onErrorContainer
                    PlusSeverity.POSITIVE -> colors.primaryContainer to colors.onPrimaryContainer
                    else -> colors.secondaryContainer to colors.onSecondaryContainer
                }
                Surface(color = container, contentColor = foreground, shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = topSpacing).semantics { liveRegion = LiveRegionMode.Polite }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (it.severity == PlusSeverity.PROGRESS && motion) LoadingIndicator(Modifier.size(24.dp), color = foreground)
                            else Icon(if (it.severity == PlusSeverity.POSITIVE) Icons.Rounded.CheckCircle else Icons.Rounded.Info, null, Modifier.size(24.dp))
                            Text(stringResource(it.text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                        if (it.actions.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            it.actions.forEach { action ->
                                TextButton(onClick = { onAction(action) }, enabled = when (action) {
                                    PlusAction.REFRESH -> refreshEnabled
                                    PlusAction.RESTORE -> restoreEnabled
                                    PlusAction.MANAGE -> true
                                }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.textButtonColors(contentColor = foreground),
                                    modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(stringResource(when (action) {
                                        PlusAction.REFRESH -> R.string.plus_try_again
                                        PlusAction.RESTORE -> R.string.plus_restore
                                        PlusAction.MANAGE -> R.string.plus_manage
                                    }))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

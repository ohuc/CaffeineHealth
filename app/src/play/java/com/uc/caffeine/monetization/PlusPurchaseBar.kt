package com.uc.caffeine.monetization

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

@Composable
internal fun PlusPurchaseBar(
    cta: PlusCta,
    selectedPlan: StorePlan?,
    previewPurchaseNote: Boolean,
    dismissLabel: Int,
    activityAvailable: Boolean,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val horizontalActions = configuration.screenHeightDp < 480 && configuration.screenWidthDp >= 480 && cta != PlusCta.None
    // A tonal sheet with rounded top corners, the same anchor the onboarding Plus intro uses, so the
    // decision always sits in one place and the content above reads as one calm scroll.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 6.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cta != PlusCta.None) {
                    val hint = when {
                        cta is PlusCta.Subscribe && !activityAvailable -> stringResource(R.string.plus_activity_unavailable)
                        cta == PlusCta.RefreshToContinue -> stringResource(R.string.plus_cta_hint_refresh)
                        else -> null
                    }
                    // Keep both actions reachable when large type makes the legal copy several lines tall.
                    Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp / 4).dp)
                        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedPlan?.let { BarNote(stringResource(it.duration.plusTermsResource(), it.localizedPrice)) }
                        hint?.let { BarNote(it) }
                        if (previewPurchaseNote) BarNote(stringResource(R.string.plus_preview_purchase_note))
                    }
                }
                if (horizontalActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlusPrimaryAction(cta, activityAvailable, onPrimary, Modifier.weight(1f))
                        PlusFreeAction(dismissLabel, false, onDismiss, Modifier.weight(0.65f))
                    }
                } else {
                    if (cta != PlusCta.None) PlusPrimaryAction(cta, activityAvailable, onPrimary, Modifier.fillMaxWidth())
                    PlusFreeAction(dismissLabel, cta == PlusCta.None, onDismiss, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PlusPrimaryAction(cta: PlusCta, activityAvailable: Boolean, onPrimary: () -> Unit, modifier: Modifier) {
    val label = when (cta) {
        is PlusCta.Subscribe -> if (activityAvailable) stringResource(cta.plan.duration.plusActionResource(), cta.plan.localizedPrice)
            else stringResource(R.string.plus_cta_reopen)
        is PlusCta.Disabled -> stringResource(cta.label)
        PlusCta.RefreshToContinue -> stringResource(R.string.plus_cta_refresh)
        PlusCta.None -> error("No purchase action")
    }
    val motion = LocalPlusMotionEnabled.current
    val fadeInSpec = if (motion) MaterialTheme.motionScheme.defaultEffectsSpec<Float>() else snap()
    val fadeOutSpec = if (motion) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap()
    val sizeSpec = if (motion) MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>() else snap()
    val height = ButtonDefaults.MediumContainerHeight
    Button(onClick = onPrimary, enabled = cta == PlusCta.RefreshToContinue || (cta is PlusCta.Subscribe && activityAvailable),
        shapes = ButtonDefaults.shapesFor(height), contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier = modifier.heightIn(min = height).semantics { stateDescription = label }) {
        if (cta is PlusCta.Disabled && cta.progress && motion) {
            LoadingIndicator(Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        AnimatedContent(label, transitionSpec = { fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) using SizeTransform { _, _ -> sizeSpec } },
            modifier = Modifier.weight(1f), label = "plus_purchase_label") {
            Text(it, style = ButtonDefaults.textStyleFor(height), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlusFreeAction(label: Int, tonal: Boolean, onDismiss: () -> Unit, modifier: Modifier) {
    if (tonal) FilledTonalButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(min = 48.dp)) { Text(stringResource(label), textAlign = TextAlign.Center) }
    else TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(min = 48.dp)) { Text(stringResource(label), textAlign = TextAlign.Center) }
}

@Composable
private fun BarNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

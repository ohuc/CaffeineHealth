package com.uc.caffeine.monetization

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.components.settingsRowColors

internal data class PlusPageActions(
    val onDismiss: () -> Unit,
    val onSelect: (String) -> Unit,
    val onPrimary: () -> Unit,
    val onStartTrial: () -> Unit,
    val onAction: (PlusAction) -> Unit,
    val onInfo: (PlusInfoDialog) -> Unit,
)

@Composable
internal fun PlusPageContent(
    model: PlusPageModel,
    selectedId: String?,
    actions: PlusPageActions,
    chipText: String? = null,
    activityAvailable: Boolean = true,
    iconOffer: @Composable () -> Unit = {},
) {
    val motion = !LocalInspectionMode.current && ValueAnimator.areAnimatorsEnabled()
    CompositionLocalProvider(LocalPlusMotionEnabled provides motion) {
        val pitch = model.layout == PlusLayout.PITCH
        Scaffold(containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Box(Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
                    Row(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        FilledTonalIconButton(onClick = actions.onDismiss, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.plus_close))
                        }
                    }
                }
            },
            bottomBar = {
                if (pitch) PlusPurchaseBar(model.cta, model.selectedPlan, model.previewPurchaseNote, model.dismissLabel,
                    activityAvailable, actions.onPrimary, actions.onDismiss)
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column {
                        if (pitch) PlusHero(chipText) else PlusOwnerHeader(stringResource(model.ownerSubtitle), chipText)
                        PlusStatusStrip(model.strip, model.restoreEnabled, model.refreshEnabled, topSpacing = 24.dp, onAction = actions.onAction)
                    }
                    if (pitch) {
                        // Free first, then the one Plus divider, then what Plus adds: the app-wide
                        // Free/Plus barrier, kept as a single tight group.
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PlusFreeTier()
                            PlusDivider(stringResource(R.string.plus_divider_label), onClick = null)
                            PlusFeatureShowcase()
                        }
                        model.plans?.let { PlusPlanCards(it, selectedId, actions.onSelect) }
                        if (model.freeDay) PlusFreeDayRow(actions.onStartTrial)
                    } else {
                        if (model.iconOffer) iconOffer()
                        if (model.included) PlusIncludedList()
                    }
                    PlusManagement(model.managementActions, model.restoreEnabled, actions.onAction)
                    PlusFooter(pitch && model.showRestore, model.restoreEnabled, onRestore = { actions.onAction(PlusAction.RESTORE) }, onInfo = actions.onInfo)
                    if (!pitch) TextButton(onClick = actions.onDismiss, shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(model.dismissLabel)) }
                }
            }
        }
    }
}

@Composable
private fun PlusManagement(actions: List<PlusAction>, restoreEnabled: Boolean, onAction: (PlusAction) -> Unit) {
    if (actions.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlusSectionLabel(stringResource(R.string.plus_pitch_section_subscription))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            actions.forEachIndexed { index, action ->
                SegmentedListItem(onClick = { onAction(action) }, enabled = action != PlusAction.RESTORE || restoreEnabled,
                    leadingContent = { Icon(if (action == PlusAction.MANAGE) Icons.AutoMirrored.Rounded.OpenInNew else Icons.Rounded.Restore, null) },
                    verticalAlignment = Alignment.CenterVertically, shapes = segmentedListItemShapes(index, actions.size), colors = settingsRowColors()) {
                    Text(stringResource(if (action == PlusAction.MANAGE) R.string.plus_manage else R.string.plus_restore), style = MaterialTheme.typography.titleMediumEmphasized)
                }
            }
        }
    }
}

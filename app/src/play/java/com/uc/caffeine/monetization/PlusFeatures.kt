package com.uc.caffeine.monetization

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.settingsCollapseExit
import com.uc.caffeine.ui.components.settingsExpandEnter

private data class PlusBenefit(val feature: ProFeature, @param:StringRes val title: Int, @param:StringRes val summary: Int)
private val plusBenefits = listOf(
    PlusBenefit(ProFeature.Coach, R.string.plus_feature_coach, R.string.plus_benefit_coach_summary),
    PlusBenefit(ProFeature.HealthConnect, R.string.plus_feature_health, R.string.plus_benefit_health_summary),
    PlusBenefit(ProFeature.Automation, R.string.plus_feature_automation, R.string.plus_benefit_automation_summary),
    PlusBenefit(ProFeature.PremiumWidgets, R.string.plus_feature_widgets, R.string.plus_benefit_widgets_summary),
    PlusBenefit(ProFeature.AdvancedAppearance, R.string.plus_feature_appearance, R.string.plus_benefit_appearance_summary),
    PlusBenefit(ProFeature.ExtendedAnalytics, R.string.plus_feature_analytics, R.string.plus_benefit_analytics_summary),
)

@Composable
internal fun PlusFeatureIcon(icon: ImageVector) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp)) }
    }
}

/**
 * What Plus adds, in two calm cards: the Coach with its illustrative sample grouped in one inset
 * panel, then every other benefit as a quiet icon row. No tiles of uneven height, no shape cycling.
 */
@Composable
internal fun PlusFeatureShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PlusBenefitLine(plusBenefits.first(), Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp))
                // Inner radius = card radius (28) − card padding (16).
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.plus_coach_sample_label), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PlusCoachArt(Modifier.height(96.dp))
                        PlusSampleChip()
                        Text(stringResource(R.string.plus_coach_sample_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                plusBenefits.drop(1).forEach { PlusBenefitLine(it) }
            }
        }
    }
}

@Composable
private fun PlusBenefitLine(benefit: PlusBenefit, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        PlusFeatureIcon(benefit.feature.icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(benefit.title), style = MaterialTheme.typography.titleMediumEmphasized)
            Text(stringResource(benefit.summary), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The Free tier as one row that expands to the full list. It sits above the Plus divider, so the
 * Free-then-Plus order stays, but it no longer pushes the pitch a screen down.
 */
@Composable
internal fun PlusFreeTier() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    val motion = LocalPlusMotionEnabled.current
    val chevron by animateFloatAsState(if (expanded) 180f else 0f,
        if (motion) MaterialTheme.motionScheme.defaultSpatialSpec() else snap(), label = "plus_free_chevron")
    val items = listOf(R.string.plus_free_item_log, R.string.plus_free_item_live, R.string.plus_free_item_settings,
        R.string.plus_free_item_records, R.string.plus_free_item_reminders, R.string.plus_free_item_widgets,
        R.string.plus_free_item_analytics, R.string.plus_free_item_dynamic)
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(onClickLabel = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                        role = Role.Button) { haptics.toggle(); expanded = !expanded }
                    .heightIn(min = 72.dp).padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlusFeatureIcon(Icons.Rounded.Check)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.plus_free_card_title), style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.plus_free_card_summary), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(chevron), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded,
                enter = if (motion) settingsExpandEnter() else EnterTransition.None,
                exit = if (motion) settingsCollapseExit() else ExitTransition.None) {
                // The gap below the header lives inside the animated body so nothing snaps on collapse.
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics(mergeDescendants = true) {}) {
                            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(stringResource(item), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlusIncludedList() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlusSectionLabel(stringResource(R.string.plus_pitch_section_included))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            plusBenefits.forEachIndexed { index, benefit ->
                val outer = 24.dp
                val inner = 4.dp
                Surface(shape = RoundedCornerShape(topStart = if (index == 0) outer else inner,
                    topEnd = if (index == 0) outer else inner,
                    bottomStart = if (index == plusBenefits.lastIndex) outer else inner,
                    bottomEnd = if (index == plusBenefits.lastIndex) outer else inner),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlusFeatureIcon(benefit.feature.icon)
                        Text(stringResource(benefit.title), style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.Check, stringResource(R.string.plus_included_check), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

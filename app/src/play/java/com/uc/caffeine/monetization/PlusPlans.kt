package com.uc.caffeine.monetization

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.shimmerEffect

@Composable
internal fun PlusPlanCards(plans: PlusPlans, selectedId: String?, onSelect: (String) -> Unit) {
    val motion = LocalPlusMotionEnabled.current
    val fadeInSpec = if (motion) MaterialTheme.motionScheme.defaultEffectsSpec<Float>() else snap()
    val fadeOutSpec = if (motion) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap()
    val sizeSpec = if (motion) MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>() else snap()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlusSectionLabel(stringResource(R.string.plus_pitch_section_plans))
        AnimatedContent(targetState = plans, contentKey = { it is PlusPlans.Loading },
            transitionSpec = { fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) using SizeTransform { _, _ -> sizeSpec } }, label = "plus_prices") { current ->
            when (current) {
                PlusPlans.Loading -> Column(verticalArrangement = Arrangement.spacedBy(PlanGap), modifier = Modifier.clearAndSetSemantics {}) {
                    repeat(3) { index ->
                        val shape = planShape(index, 3, selected = false)
                        Box(Modifier.fillMaxWidth().height(80.dp).then(
                            if (motion) Modifier.shimmerEffect(shape) else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)))
                    }
                }
                is PlusPlans.Ready -> Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(PlanGap)) {
                    current.plans.forEachIndexed { index, plan ->
                        PlusPlanCard(plan, index, current.plans.size, plan.id == selectedId, current.enabled) { onSelect(plan.id) }
                    }
                }
            }
        }
    }
}

private val PlanGap = 3.dp
private val PlanOuterRadius = 24.dp
private val PlanInnerRadius = 6.dp
private val PlanSelectedRadius = 28.dp

/** Connected-group corners: rounded at the group's ends, tight inside, a full 28dp pill when selected. */
private fun planTopRadius(index: Int, selected: Boolean): Dp =
    if (selected) PlanSelectedRadius else if (index == 0) PlanOuterRadius else PlanInnerRadius
private fun planBottomRadius(index: Int, count: Int, selected: Boolean): Dp =
    if (selected) PlanSelectedRadius else if (index == count - 1) PlanOuterRadius else PlanInnerRadius
private fun planShape(index: Int, count: Int, selected: Boolean): RoundedCornerShape {
    val top = planTopRadius(index, selected)
    val bottom = planBottomRadius(index, count, selected)
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * One plan in the connected group. Selection is the page's second expressive moment: the card
 * leaves the group's tight corners for a full pill (spatial spring) while its colour fades to
 * primaryContainer (effects spring). The radio and heavier price keep it readable without colour.
 */
@Composable
private fun PlusPlanCard(plan: StorePlan, index: Int, count: Int, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    val motion = LocalPlusMotionEnabled.current
    val spatial = if (motion) MaterialTheme.motionScheme.defaultSpatialSpec<Dp>() else snap()
    val density = LocalDensity.current
    val top by animateDpAsState(planTopRadius(index, selected), spatial, label = "plus_plan_top")
    val bottom by animateDpAsState(planBottomRadius(index, count, selected), spatial, label = "plus_plan_bottom")
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        if (motion) MaterialTheme.motionScheme.defaultEffectsSpec() else snap(), label = "plus_plan_color")
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val supporting = if (selected) foreground else MaterialTheme.colorScheme.onSurfaceVariant
    val priceStyle = MaterialTheme.typography.titleLargeEmphasized
    val measurer = rememberTextMeasurer()
    Surface(shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = container, contentColor = foreground,
        modifier = Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton,
            onClick = onSelect)) {
        BoxWithConstraints(Modifier.heightIn(min = 80.dp).padding(horizontal = 20.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
            val priceWidth = with(density) { measurer.measure(AnnotatedString(plan.localizedPrice), priceStyle).size.width.toDp() }
            val stacked = density.fontScale > 1.2f || maxWidth - priceWidth - 48.dp < 120.dp
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null, enabled = enabled,
                    colors = RadioButtonDefaults.colors(selectedColor = foreground, unselectedColor = supporting))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(plan.duration.plusLabelResource()), style = MaterialTheme.typography.titleMediumEmphasized)
                        if (plan.duration == PlanDuration.YEARLY) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                                Text(stringResource(R.string.plus_best_value), style = MaterialTheme.typography.labelMediumEmphasized,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Text(stringResource(plan.duration.plusCaptionResource()), style = MaterialTheme.typography.bodySmall, color = supporting)
                    if (stacked) Text(plan.localizedPrice, style = priceStyle, modifier = Modifier.padding(top = 4.dp))
                }
                if (!stacked) Text(plan.localizedPrice, style = priceStyle, maxLines = 1)
            }
        }
    }
}

/** A quiet second path for undecided users: centred, one tonal button, the no-card promise below. */
@Composable
internal fun PlusFreeDayRow(onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.plus_free_day_card_title), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        FilledTonalButton(onClick = onStart, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.plus_trial_try), textAlign = TextAlign.Center)
        }
        Text(stringResource(R.string.plus_trial_terms), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

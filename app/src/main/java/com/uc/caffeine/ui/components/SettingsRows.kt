package com.uc.caffeine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.uc.caffeine.R
import com.uc.caffeine.monetization.plusLockedContent
import com.uc.caffeine.monetization.plusLockedSemantics
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults

/** One navigation row: icon, bold title, a one-line summary of what is inside, chevron. */
data class SettingsNavItem(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val locked: Boolean = false,
    val savedEnabled: Boolean? = null,
    val selected: Boolean = false,
)

@Composable
fun settingsRowColors(): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
    leadingContentColor = MaterialTheme.colorScheme.primary,
    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** A connected group of navigation rows (shared top/bottom rounding, 2dp gaps). */
@Composable
fun SettingsNavGroup(items: List<SettingsNavItem>, modifier: Modifier = Modifier) {
    val haptics = rememberAppHaptics()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { index, item ->
            SegmentedListItem(
                onClick = { haptics.toggle(); item.onClick() },
                modifier = Modifier.semantics { selected = item.selected }.plusLockedSemantics(item.locked, item.savedEnabled,
                    if (item.savedEnabled == true) stringResource(R.string.plus_manage_saved, item.title)
                    else stringResource(R.string.plus_locked_click_label)),
                leadingContent = { Icon(item.icon, contentDescription = null, modifier = Modifier.plusLockedContent(item.locked)) },
                supportingContent = {
                    Text(item.summary,
                        color = if (item.locked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.plusLockedContent(item.locked))
                },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null,
                    modifier = Modifier.plusLockedContent(item.locked)) },
                verticalAlignment = Alignment.CenterVertically,
                shapes = segmentedListItemShapes(index, items.size),
                colors = if (item.selected) ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) else settingsRowColors(),
            ) {
                Text(item.title, style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.plusLockedContent(item.locked))
            }
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** Title style shared by every settings row, so sub-pages read like the top-level list. */
@Composable
private fun SettingsRowTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMediumEmphasized, modifier = modifier)
}

@Composable
private fun SettingsRowSummary(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/** A row with a trailing switch. The whole row toggles; the switch only shows the state. */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    index: Int = 0,
    count: Int = 1,
    enabled: Boolean = true,
    /** Plus barrier: dims the content like every locked row; the caller supplies the lock semantics via [modifier]. */
    locked: Boolean = false,
) {
    val haptics = rememberAppHaptics()
    SegmentedListItem(
        onClick = { haptics.toggle(); onCheckedChange(!checked) },
        enabled = enabled,
        modifier = if (locked) modifier else modifier.semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.plusLockedContent(locked)) } },
        supportingContent = summary?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (locked) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
                    modifier = Modifier.plusLockedContent(locked),
                )
            }
        },
        trailingContent = {
            Switch(
                modifier = Modifier.plusLockedContent(locked),
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        },
        verticalAlignment = Alignment.CenterVertically,
        shapes = segmentedListItemShapes(index, count),
        colors = settingsRowColors(),
    ) { SettingsRowTitle(title, Modifier.plusLockedContent(locked)) }
}

/**
 * A row that opens a picker or page. The current value is plain trailing text, never a button:
 * a tonal button inside a tappable row is a button inside a button.
 */
@Composable
fun SettingsValueRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    index: Int = 0,
    count: Int = 1,
    enabled: Boolean = true,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()
    SegmentedListItem(
        onClick = { haptics.toggle(); onClick() },
        enabled = enabled,
        modifier = modifier,
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        supportingContent = summary?.let { { SettingsRowSummary(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                trailingAction?.invoke()
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        shapes = segmentedListItemShapes(index, count),
        colors = settingsRowColors(),
    ) { SettingsRowTitle(title) }
}

/** A row that performs an action (for example "Redo onboarding"). Replaces an outlined button in a card. */
@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    index: Int = 0,
    count: Int = 1,
    enabled: Boolean = true,
) {
    val haptics = rememberAppHaptics()
    SegmentedListItem(
        onClick = { haptics.toggle(); onClick() },
        enabled = enabled,
        modifier = modifier,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = summary?.let { { SettingsRowSummary(it) } },
        verticalAlignment = Alignment.CenterVertically,
        shapes = segmentedListItemShapes(index, count),
        colors = settingsRowColors(),
    ) { SettingsRowTitle(title) }
}

/**
 * Height springs for expanding bodies. They are critically damped on purpose: an under-damped
 * expressive spatial spring overshoots in proportion to the distance, so a tall body (the health
 * profile is well over 1000dp) over-expanded by tens of dp and, on collapse, sank past zero and
 * visibly sprang open again. The expressive character comes from the content instead: it settles
 * into place with the scheme's spatial spring over a short, fixed distance.
 */
private val ExpandSizeSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntSize.VisibilityThreshold,
)
private val CollapseSizeSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = IntSize.VisibilityThreshold,
)
private val ExpandContentOffset = 16.dp

/** Enter transition for a settings body that expands below its header. */
@Composable
fun settingsExpandEnter(): EnterTransition {
    val motion = MaterialTheme.motionScheme
    val offsetPx = with(LocalDensity.current) { ExpandContentOffset.roundToPx() }
    return expandVertically(ExpandSizeSpec, expandFrom = Alignment.Top) +
        fadeIn(motion.defaultEffectsSpec()) +
        slideInVertically(motion.defaultSpatialSpec()) { -offsetPx }
}

/** Exit transition paired with [settingsExpandEnter]: quicker, no rebound. */
@Composable
fun settingsCollapseExit(): ExitTransition {
    val motion = MaterialTheme.motionScheme
    return shrinkVertically(CollapseSizeSpec, shrinkTowards = Alignment.Top) +
        fadeOut(motion.fastEffectsSpec())
}

/**
 * A row that expands to reveal more settings. The space between header and body belongs to the
 * animated body, never to a parent's `spacedBy`, so a collapsing group leaves no gap behind and
 * nothing snaps when the animation ends.
 */
@Composable
fun SettingsExpandableGroup(
    icon: ImageVector,
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = rememberAppHaptics()
    val motion = MaterialTheme.motionScheme
    // The expressive bounce lives on the small chevron, where a little overshoot reads as lively.
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "settingsGroupChevron",
    )
    val chevronContainer by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "settingsGroupChevronContainer",
    )
    val toggleLabel = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = CaffeineSurfaceDefaults.groupedListContainerColor,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = toggleLabel, role = Role.Button) {
                        haptics.toggle()
                        onExpandedChange(!expanded)
                    }
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    SettingsRowTitle(title)
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(chevronContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = if (expanded) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevron),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = settingsExpandEnter(),
                exit = settingsCollapseExit(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    content = content,
                )
            }
        }
    }
}

/** A labelled control inside an expanded group: bold title, readable description, then the control. */
@Composable
fun SettingsField(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

package com.uc.caffeine.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** One icon-only action revealed behind a swiped row. */
data class SwipeRevealAction(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit,
)

private enum class SwipeRevealValue { Closed, Open }

private val ActionButtonSize = 48.dp
private val ActionSpacing = 8.dp
private val ActionEdgePadding = 8.dp

/**
 * A row that slides toward the start (right to left in LTR, mirrored in RTL) to reveal
 * icon-only [actions] at its end. It only moves in that one direction and settles open or
 * closed with the theme's spatial spring. Haptics mark the open/close threshold as the finger
 * crosses it. Only one row should be open at a time: the caller passes [open] and is told
 * through [onOpenChange]. While open, a tap on the row closes it instead of opening it.
 * The actions are also exposed as accessibility custom actions, so they never depend on the
 * swipe.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRevealActions(
    actions: List<SwipeRevealAction>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (closeOrNull: (() -> Unit)?) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val motion = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)

    val revealWidthPx = with(density) {
        (ActionButtonSize * actions.size + ActionSpacing * (actions.size - 1) + ActionEdgePadding * 2).toPx()
    }
    val anchors = remember(revealWidthPx) {
        DraggableAnchors {
            SwipeRevealValue.Closed at 0f
            SwipeRevealValue.Open at -revealWidthPx
        }
    }
    val state = remember { AnchoredDraggableState(SwipeRevealValue.Closed, anchors) }
    LaunchedEffect(anchors) { state.updateAnchors(anchors) }

    val settleSpec = motion.defaultSpatialSpec<Float>()
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * 0.4f },
        animationSpec = settleSpec,
    )

    // Light tick each time the drag crosses the open/close threshold.
    LaunchedEffect(state) {
        snapshotFlow { state.targetValue }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.sliderTick() }
    }
    // Report settled state so the caller can keep a single row open.
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }
            .distinctUntilChanged()
            .drop(1)
            .collect { value -> currentOnOpenChange(value == SwipeRevealValue.Open) }
    }
    // The caller closed this row (another row opened, the list scrolled, an action ran).
    LaunchedEffect(open) {
        val target = if (open) SwipeRevealValue.Open else SwipeRevealValue.Closed
        if (state.settledValue != target) state.animateTo(target, settleSpec)
    }

    val close: () -> Unit = {
        scope.launch { state.animateTo(SwipeRevealValue.Closed, settleSpec) }
        currentOnOpenChange(false)
    }

    Box(
        modifier = modifier.semantics {
            customActions = actions.map { action ->
                CustomAccessibilityAction(action.label) { action.onClick(); true }
            }
        },
    ) {
        // Revealed actions, pinned to the end and scaled in with the swipe progress.
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = ActionEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(ActionSpacing, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                FilledIconButton(
                    onClick = action.onClick,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = action.containerColor,
                        contentColor = action.contentColor,
                    ),
                    modifier = Modifier.graphicsLayer {
                        val progress = progressOf(state.offset, revealWidthPx)
                        alpha = progress
                        val scale = 0.6f + 0.4f * progress
                        scaleX = scale
                        scaleY = scale
                    },
                ) {
                    Icon(imageVector = action.icon, contentDescription = action.label)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(state.offset.takeUnless { it.isNaN() }?.roundToInt() ?: 0, 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior,
                ),
        ) {
            content(if (state.settledValue == SwipeRevealValue.Open || state.targetValue == SwipeRevealValue.Open) close else null)
        }
    }
}

private fun progressOf(offset: Float, revealWidthPx: Float): Float =
    if (offset.isNaN() || revealWidthPx <= 0f) 0f else (abs(offset) / revealWidthPx).coerceIn(0f, 1f)

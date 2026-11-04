package com.uc.caffeine.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

/** Window width breakpoints (Material window size classes), in dp. */
internal const val MediumWidthDp = 600
internal const val ExpandedWidthDp = 840
internal const val LargeWidthDp = 1200

/** A single workflow never stretches wider than this; wider windows centre it or add a pane. */
internal val SinglePaneMaxWidth = 720.dp

/**
 * The window-level layout decisions, derived only from the window width class. The shell and every
 * pane layout (Settings, Analytics, Home, Add) read this one policy, so the navigation arrangement
 * and the number of panes always change at the same breakpoints. Navigation is always the floating
 * toolbar; there is no navigation rail.
 *
 * - compact (< 600dp): Add stacked above the toolbar, selected label only, one pane
 * - medium (600-839dp): Add beside the toolbar, all labels when they fit, one pane
 * - expanded (840-1199dp): as medium, two panes
 * - large (1200dp+): as medium, two panes with a wider supporting pane
 *
 * Thresholds that depend on a component's own container (time picker, Plus pill, scanner
 * controls) stay local to that component; they are not window decisions.
 */
@Immutable
internal data class WindowLayoutPolicy(
    /** Add sits beside the toolbar (not above it) and every destination may show its label. */
    val wideToolbar: Boolean,
    val paneCount: Int,
    val supportingPaneWidth: Dp,
) {
    val twoPane: Boolean get() = paneCount >= 2
}

internal fun windowLayoutPolicy(windowWidthDp: Int): WindowLayoutPolicy = when {
    windowWidthDp >= LargeWidthDp -> WindowLayoutPolicy(
        wideToolbar = true, paneCount = 2, supportingPaneWidth = 412.dp,
    )
    windowWidthDp >= ExpandedWidthDp -> WindowLayoutPolicy(
        wideToolbar = true, paneCount = 2, supportingPaneWidth = 360.dp,
    )
    windowWidthDp >= MediumWidthDp -> WindowLayoutPolicy(
        wideToolbar = true, paneCount = 1, supportingPaneWidth = 360.dp,
    )
    else -> WindowLayoutPolicy(
        wideToolbar = false, paneCount = 1, supportingPaneWidth = 360.dp,
    )
}

@Composable
internal fun rememberWindowLayoutPolicy(): WindowLayoutPolicy {
    val widthDp = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp
    return remember(widthDp) { windowLayoutPolicy(widthDp) }
}

/** Where a pane of [AdaptiveMainSupportingLayout] sits, so it can pick its insets and padding. */
enum class PanePlacement {
    /** The only pane: it touches every window edge. */
    Single,
    /** The left pane of a side-by-side pair (right in RTL). */
    Start,
    /** The right pane of a side-by-side pair (left in RTL). */
    End,
    /** Above a tabletop hinge: touches the top and side edges, not the bottom. */
    Top,
    /** Below a tabletop hinge: touches the bottom and side edges, not the top. */
    Bottom;

    /** The safe-drawing sides a page header in this pane must keep clear of. */
    val headerInsetSides: WindowInsetsSides
        get() = when (this) {
            Single, Top -> WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            Start -> WindowInsetsSides.Top + WindowInsetsSides.Start
            End -> WindowInsetsSides.Top + WindowInsetsSides.End
            Bottom -> WindowInsetsSides.Horizontal
        }

    /** Whether the floating navigation and system bars sit along this pane's bottom edge. */
    val touchesBottom: Boolean get() = this != Top
}

val LocalPanePlacement = staticCompositionLocalOf { PanePlacement.Single }

/**
 * The two halves of a window folded in tabletop posture: everything above a horizontal hinge that
 * crosses the whole layout, and everything below it. Null when there is no such hinge inside
 * [bounds] (flat device, book posture, or a hinge outside this layout).
 */
internal data class TabletopPanes(val top: Rect, val bottom: Rect)

internal fun tabletopPanes(bounds: Rect, horizontalHinges: List<Rect>): TabletopPanes? {
    if (bounds.width <= 0f || bounds.height <= 0f) return null
    val hinge = horizontalHinges.firstOrNull {
        it.width >= it.height && it.left <= bounds.left && it.right >= bounds.right &&
            it.top > bounds.top && it.bottom < bounds.bottom
    } ?: return null
    return TabletopPanes(
        top = Rect(bounds.left, bounds.top, bounds.right, hinge.top),
        bottom = Rect(bounds.left, hinge.bottom, bounds.right, bounds.bottom),
    )
}

/**
 * A screen with one primary pane and one supporting pane, laid out for the current window:
 *
 * - two-pane windows: [main] and [supporting] side by side in a [SupportingPaneScaffold], kept
 *   clear of a separating vertical hinge, with a drag handle to resize them;
 * - tabletop posture (when [splitTabletop]): [main] above the hinge and [supporting] below it;
 * - otherwise: [single], width-capped and kept on one side of any separating or occluding hinge.
 *
 * State that must survive a fold or resize belongs above this call (it is hoisted by the screens),
 * because the arrangement switch recreates the panes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AdaptiveMainSupportingLayout(
    modifier: Modifier = Modifier,
    splitTabletop: Boolean = false,
    single: @Composable () -> Unit,
    main: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
) {
    val posture = currentWindowAdaptiveInfoV2().windowPosture
    val directive = caffeinePaneDirective()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val tabletop = if (splitTabletop && posture.isTabletop) {
        tabletopPanes(bounds, posture.hingeList.filter { !it.isVertical }.map { it.bounds })
    } else null
    val density = LocalDensity.current

    Box(modifier.fillMaxSize().onGloballyPositioned { bounds = it.boundsInWindow() }) {
        when {
            tabletop != null -> {
                Box(Modifier.fillMaxSize().absolutePadding(
                    bottom = with(density) { (bounds.bottom - tabletop.top.bottom).toDp() },
                )) { CompositionLocalProvider(LocalPanePlacement provides PanePlacement.Top) { main() } }
                Box(Modifier.fillMaxSize().absolutePadding(
                    top = with(density) { (tabletop.bottom.top - bounds.top).toDp() },
                )) { CompositionLocalProvider(LocalPanePlacement provides PanePlacement.Bottom) { supporting() } }
            }

            directive.maxHorizontalPartitions >= 2 -> {
                val navigator = rememberSupportingPaneScaffoldNavigator(
                    scaffoldDirective = directive,
                    adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
                        supportingPaneAdaptStrategy = AdaptStrategy.Hide,
                    ),
                )
                val expansionState = rememberPaneExpansionState()
                val resizeLabel = stringResource(R.string.adaptive_pane_resize_cd)
                SupportingPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    scaffoldState = navigator.scaffoldState,
                    mainPane = {
                        AnimatedPane {
                            CompositionLocalProvider(LocalPanePlacement provides PanePlacement.Start) { main() }
                        }
                    },
                    supportingPane = {
                        AnimatedPane {
                            CompositionLocalProvider(LocalPanePlacement provides PanePlacement.End) { supporting() }
                        }
                    },
                    paneExpansionDragHandle = { state ->
                        val interactionSource = remember { MutableInteractionSource() }
                        VerticalDragHandle(
                            modifier = Modifier.paneExpansionDraggable(
                                state,
                                LocalMinimumInteractiveComponentSize.current,
                                interactionSource,
                            ) { contentDescription = resizeLabel },
                            interactionSource = interactionSource,
                        )
                    },
                    paneExpansionState = expansionState,
                )
            }

            else -> SinglePaneWindow {
                Box(
                    Modifier.widthIn(max = SinglePaneMaxWidth).fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) { single() }
            }
        }
    }
}

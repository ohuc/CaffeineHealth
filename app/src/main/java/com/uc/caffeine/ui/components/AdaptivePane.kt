package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The pane directive every adaptive scaffold uses (Settings and Analytics list/detail, Home and Add
 * main/supporting). The pane count and supporting width come from [windowLayoutPolicy], the same
 * policy that picks the rail, so navigation and panes switch together. Separating vertical hinges
 * are excluded so a book-posture fold never splits a pane across the crease.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun caffeinePaneDirective(): PaneScaffoldDirective {
    val info = currentWindowAdaptiveInfoV2()
    val policy = windowLayoutPolicy(info.windowSizeClass.minWidthDp)
    val directive = calculatePaneScaffoldDirective(info, verticalHingePolicy = HingePolicy.AvoidSeparating)
    return directive.copy(
        maxHorizontalPartitions = policy.paneCount,
        horizontalPartitionSpacerSize = 16.dp,
        defaultPanePreferredWidth = policy.supportingPaneWidth,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun isListPaneVisible(): Boolean {
    val scope = LocalListDetailSceneScope.current ?: return false
    return scope.scaffoldTransitionScope.scaffoldStateTransition.targetState[ListDetailPaneScaffoldRole.List] ==
        PaneAdaptedValue.Expanded
}

internal val LocalNestedDetailBack = compositionLocalOf { false }

@Composable
internal fun AdaptivePaneContent(content: @Composable () -> Unit) {
    SinglePaneWindow {
        Box(Modifier.widthIn(max = SinglePaneMaxWidth).fillMaxSize()) { content() }
    }
}

@Composable
internal fun AdaptivePanePlaceholder(title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

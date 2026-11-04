package com.uc.caffeine.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

/** Keeps a single workflow on one usable display area without remounting its content on resize. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SinglePaneWindow(enabled: Boolean = true, content: @Composable () -> Unit) {
    val posture = currentWindowAdaptiveInfoV2().windowPosture
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val hinges = if (enabled) posture.hingeList.filter { it.isSeparating || it.isOccluding }.map { it.bounds } else emptyList()
    val pane = largestUnobstructedPane(bounds, hinges)
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().onGloballyPositioned { bounds = it.boundsInWindow() }) {
        Box(Modifier.fillMaxSize().absolutePadding(
            left = with(density) { (pane.left - bounds.left).toDp() },
            top = with(density) { (pane.top - bounds.top).toDp() },
            right = with(density) { (bounds.right - pane.right).toDp() },
            bottom = with(density) { (bounds.bottom - pane.bottom).toDp() },
        ), contentAlignment = Alignment.TopCenter) { content() }
    }
}

internal fun largestUnobstructedPane(bounds: Rect, hinges: List<Rect>): Rect {
    var panes = listOf(bounds)
    for (hinge in hinges) {
        panes = panes.flatMap { pane ->
            when {
                hinge.height >= hinge.width && hinge.top <= pane.top && hinge.bottom >= pane.bottom &&
                    hinge.left > pane.left && hinge.right < pane.right -> listOf(
                    Rect(pane.left, pane.top, hinge.left, pane.bottom),
                    Rect(hinge.right, pane.top, pane.right, pane.bottom),
                )
                hinge.width > hinge.height && hinge.left <= pane.left && hinge.right >= pane.right &&
                    hinge.top > pane.top && hinge.bottom < pane.bottom -> listOf(
                    Rect(pane.left, pane.top, pane.right, hinge.top),
                    Rect(pane.left, hinge.bottom, pane.right, pane.bottom),
                )
                else -> listOf(pane)
            }
        }
    }
    return panes.maxByOrNull { it.width * it.height } ?: bounds
}

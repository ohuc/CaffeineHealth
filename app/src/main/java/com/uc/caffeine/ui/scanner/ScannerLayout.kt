package com.uc.caffeine.ui.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.uc.caffeine.ui.components.largestUnobstructedPane

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ScannerLayout(
    viewfinder: @Composable BoxScope.() -> Unit,
    controls: @Composable BoxScope.() -> Unit,
) {
    val posture = currentWindowAdaptiveInfoV2().windowPosture
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val tabletopHinge = posture.hingeList.firstOrNull {
        posture.isTabletop && !it.isVertical && it.bounds.top > bounds.top && it.bounds.bottom < bounds.bottom
    }
    val pane = largestUnobstructedPane(bounds, posture.hingeList
        .filter { (it.isVertical || tabletopHinge == null) && (it.isSeparating || it.isOccluding) }.map { it.bounds })
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().onGloballyPositioned { bounds = it.boundsInWindow() }) {
        Box(Modifier.fillMaxSize().absolutePadding(
            left = with(density) { (pane.left - bounds.left).toDp() },
            right = with(density) { (bounds.right - pane.right).toDp() },
            top = with(density) { (pane.top - bounds.top).toDp() },
            bottom = with(density) { (bounds.bottom - pane.bottom).toDp() },
        )) {
            Box(Modifier.fillMaxSize().absolutePadding(
                bottom = with(density) { (tabletopHinge?.let { bounds.bottom - it.bounds.top } ?: 0f).toDp() },
            ), content = viewfinder)
            Box(Modifier.fillMaxSize().absolutePadding(
                top = with(density) { (tabletopHinge?.let { it.bounds.bottom - bounds.top } ?: 0f).toDp() },
            ), content = controls)
        }
    }
}

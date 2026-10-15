package com.uc.caffeine.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * The app's frosted glass: whatever the matching [dev.chrisbanes.haze.hazeSource] draws behind
 * this node is blurred, lightly grained and washed with [tint]. Shared by the floating toolbar,
 * the Add Consumption button and the analytics dashboard so they all read as the same material.
 */
fun Modifier.frostedGlass(hazeState: HazeState, tint: Color): Modifier =
    hazeEffect(state = hazeState) {
        blurRadius = 24.dp
        noiseFactor = 0.08f
        tints = listOf(HazeTint(tint.copy(alpha = 0.72f)))
    }

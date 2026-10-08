package com.uc.caffeine.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Material 3 Expressive "medium" slider: thick rounded track, tall bar handle, stop indicator.
private val ExpressiveTrackHeight = 40.dp
private val ExpressiveTrackCornerSize = 12.dp
private val ExpressiveThumbSize = DpSize(width = 4.dp, height = 52.dp)

/**
 * Whole-number Material 3 Expressive slider with a haptic tick on every step and a stronger
 * bump at either end of the range.
 *
 * @param showStepMarks draw a mark per step and snap to it — only sensible for short ranges
 *   (e.g. 0–5); long ranges stay continuous so the track isn't covered in dots.
 */
@Composable
fun ExpressiveIntSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    showStepMarks: Boolean = false,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors()

    Slider(
        value = value.toFloat(),
        onValueChange = { raw ->
            val stepped = raw.roundToInt().coerceIn(valueRange.first, valueRange.last)
            // Raw drag values arrive continuously; only a change of whole step counts.
            if (stepped != value) {
                if (stepped == valueRange.first || stepped == valueRange.last) {
                    haptics.sliderEdge()
                } else {
                    haptics.sliderTick()
                }
                onValueChange(stepped)
            }
        },
        modifier = modifier,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = if (showStepMarks) (valueRange.last - valueRange.first - 1).coerceAtLeast(0) else 0,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                colors = colors,
                thumbSize = ExpressiveThumbSize,
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                trackCornerSize = ExpressiveTrackCornerSize,
                modifier = Modifier.height(ExpressiveTrackHeight),
                colors = colors,
            )
        },
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
    )
}

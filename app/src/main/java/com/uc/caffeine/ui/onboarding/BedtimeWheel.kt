package com.uc.caffeine.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uc.caffeine.ui.components.rememberAppHaptics
import java.time.LocalTime

enum class Bedtime(val label: String, val time: LocalTime) {
    NinePM  ("9 PM",  LocalTime.of(21, 0)),
    TenPM   ("10 PM", LocalTime.of(22, 0)),
    ElevenPM("11 PM", LocalTime.of(23, 0)),
    Midnight("12 AM", LocalTime.of(0,  0)),
    OneAM   ("1 AM",  LocalTime.of(1,  0));

    companion object {
        fun from(time: LocalTime): Bedtime =
            entries.firstOrNull { it.time.hour == time.hour } ?: ElevenPM
    }
}

private val IndicatorEasing = CubicBezierEasing(0.32f, 0.72f, 0.24f, 1.06f)
private const val SLOT_COUNT = 5
private const val ARC_VP_W   = 300f
private const val ARC_VP_H   = 40f

@Composable
fun BedtimeWheel(
    selected: Bedtime,
    onSelect: (Bedtime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val primary          = cs.primary
    val primaryContainer = cs.primaryContainer
    val onSurfaceVariant = cs.onSurfaceVariant
    val tertiary         = cs.tertiary
    val outline          = cs.outline

    val haptics = rememberAppHaptics()
    val selectedIndex = selected.ordinal
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp, bottom = 10.dp),
        ) {

            // ── Track + indicator area ─────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val x = change.position.x.coerceIn(0f, size.width.toFloat())
                            val newIndex = ((x / size.width.toFloat()) * SLOT_COUNT)
                                .toInt().coerceIn(0, SLOT_COUNT - 1)
                            val newBedtime = Bedtime.entries[newIndex]
                            if (newBedtime != currentSelected) {
                                haptics.toggle()
                                currentOnSelect(newBedtime)
                            }
                        }
                    },
            ) {
                val rowWidth = maxWidth

                val centerX by animateDpAsState(
                    targetValue = rowWidth * ((selectedIndex + 0.5f) / SLOT_COUNT.toFloat()),
                    animationSpec = tween(durationMillis = 420, easing = IndicatorEasing),
                    label = "indicatorCenterX",
                )

                // Dusk-to-night arc (300×40 viewport, 50% alpha)
                val arcH = 40.dp
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(arcH)
                        .alpha(0.5f),
                ) {
                    val xS = size.width / ARC_VP_W
                    val yS = size.height / ARC_VP_H
                    val path = Path().apply {
                        moveTo(0f, 35f * yS)
                        quadraticBezierTo(150f * xS, 5f * yS, 300f * xS, 35f * yS)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.25f),
                                tertiary.copy(alpha = 0.25f),
                            ),
                        ),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }

                // Static-dot track (22 dp, vertically centred in the 40 dp arc canvas)
                val trackH  = 22.dp
                val trackY  = (arcH - trackH) / 2   // 9 dp
                val trackCY = arcH / 2               // 20 dp — indicator vertical centre

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackH)
                        .offset(y = trackY),
                ) {
                    Bedtime.entries.forEachIndexed { index, bedtime ->
                        val isSelected  = index == selectedIndex
                        val staticAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 0f else 1f,
                            animationSpec = tween(durationMillis = 250),
                            label = "staticDotAlpha_$index",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(trackH)
                                .selectable(
                                    selected = isSelected,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.RadioButton,
                                    onClick = {
                                        if (!isSelected) {
                                            haptics.toggle()
                                            onSelect(bedtime)
                                        }
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        outline.copy(alpha = 0.45f * staticAlpha),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }

                // Indicator z-stack: halo → glow ring → dot (back-to-front)

                // Halo  28 dp
                Box(
                    modifier = Modifier
                        .offset(x = centerX - 14.dp, y = trackCY - 14.dp)
                        .size(28.dp)
                        .background(primaryContainer, CircleShape),
                )

                // Outer glow ring  20 dp (dot 12 dp + 4 dp spread each side)
                Box(
                    modifier = Modifier
                        .offset(x = centerX - 10.dp, y = trackCY - 10.dp)
                        .size(20.dp)
                        .background(primary.copy(alpha = 0.13f), CircleShape),
                )

                // Dot  12 dp
                Box(
                    modifier = Modifier
                        .offset(x = centerX - 6.dp, y = trackCY - 6.dp)
                        .size(12.dp)
                        .background(primary, CircleShape),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Labels ──────────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                Bedtime.entries.forEach { bedtime ->
                    val isSelected = bedtime == selected
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) primary else onSurfaceVariant,
                        animationSpec = tween(durationMillis = 250),
                        label = "labelColor_${bedtime.name}",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .padding(vertical = 6.dp)
                            .selectable(
                                selected = isSelected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.RadioButton,
                                onClick = {
                                    if (!isSelected) {
                                        haptics.toggle()
                                        onSelect(bedtime)
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = bedtime.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight(600) else FontWeight(500),
                                color = labelColor,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ── Usage example ──────────────────────────────────────────────────────────────
//
// @Composable
// fun SleepStep() {
//     var selected by rememberSaveable { mutableStateOf(Bedtime.ElevenPM) }
//     BedtimeWheel(selected = selected, onSelect = { selected = it })
// }

@Preview(showBackground = true, backgroundColor = 0xFF1C1410)
@Composable
private fun BedtimeWheelPreview() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        BedtimeWheel(
            selected = Bedtime.ElevenPM,
            onSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

package com.uc.caffeine.monetization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import kotlin.math.exp

@Composable
internal fun PlusCoachArt(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(120.dp).clearAndSetSemantics {}) {
        Canvas(Modifier.fillMaxSize().padding(top = 10.dp, bottom = 8.dp)) {
            val baseline = size.height - 4.dp.toPx()
            val curve = Path()
            val fill = Path()
            for (step in 0..80) {
                val fraction = step / 80f
                val x = fraction * size.width
                val y = baseline - (size.height * 0.86f * exp(-3.1f * fraction))
                if (step == 0) { curve.moveTo(x, y); fill.moveTo(x, baseline); fill.lineTo(x, y) }
                else { curve.lineTo(x, y); fill.lineTo(x, y) }
            }
            fill.lineTo(size.width, baseline)
            fill.close()
            drawPath(fill, Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.16f), colors.primary.copy(alpha = 0f))))
            drawLine(colors.outline, Offset(0f, size.height * 0.77f), Offset(size.width, size.height * 0.77f),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())))
            drawLine(colors.outlineVariant, Offset(size.width * 0.78f, 24.dp.toPx()), Offset(size.width * 0.78f, baseline),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
            drawPath(curve, colors.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Icon(Icons.Rounded.Bedtime, null, Modifier.align(Alignment.TopEnd).padding(end = 12.dp).size(18.dp),
            tint = colors.onSurfaceVariant)
    }
}

@Composable
internal fun PlusSampleChip() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.clearAndSetSemantics {}) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, Modifier.size(20.dp))
            Text(stringResource(R.string.plus_coach_sample), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
        }
    }
}

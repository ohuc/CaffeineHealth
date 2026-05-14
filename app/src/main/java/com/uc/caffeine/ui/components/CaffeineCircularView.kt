package com.uc.caffeine.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.uc.caffeine.R
import com.uc.caffeine.ui.viewmodel.CaffeineTrend

@Composable
fun CaffeineCircularView(
    currentMg: Double,
    maxMg: Double,
    trend: CaffeineTrend,
    modifier: Modifier = Modifier,
) {
    val progress = if (maxMg > 0.0) (currentMg / maxMg).toFloat().coerceIn(0f, 1f) else 0f
    val strokeWidthPx = with(LocalDensity.current) { 16.dp.toPx() }
    val indicatorStroke = Stroke(width = strokeWidthPx)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val indicatorSize = min(maxWidth * 0.88f, maxHeight * 0.78f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(indicatorSize),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    stroke = indicatorStroke,
                    trackStroke = indicatorStroke,
                    gapSize = 8.dp,
                    wavelength = 60.dp,
                    waveSpeed = 60.dp,
                )

                // Center: chip + number + unit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    RollingNumberText(
                        text = "%.0f".format(currentMg),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        labelPrefix = "circular_view",
                    )
                    Text(
                        text = stringResource(R.string.home_circular_mg_unit),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    AnimatedContent(
                        targetState = trend,
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                        label = "trend_chip",
                    ) { currentTrend ->
                        TrendChip(trend = currentTrend)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_circular_threshold_format, maxMg.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun TrendChip(trend: CaffeineTrend) {
    val isRising = trend == CaffeineTrend.RISING
    val chipColor = if (isRising) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (isRising) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = chipColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (trend) {
                    CaffeineTrend.RISING -> Icons.Rounded.ArrowUpward
                    CaffeineTrend.FALLING -> Icons.Rounded.ArrowDownward
                    CaffeineTrend.STEADY -> Icons.Rounded.Remove
                },
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = contentColor,
            )
            Text(
                text = stringResource(
                    when (trend) {
                        CaffeineTrend.RISING -> R.string.home_trend_rising
                        CaffeineTrend.FALLING -> R.string.home_trend_falling
                        CaffeineTrend.STEADY -> R.string.home_trend_steady
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

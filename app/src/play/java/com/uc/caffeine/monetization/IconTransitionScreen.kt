package com.uc.caffeine.monetization

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.screens.settings.rememberDrawablePainter
import kotlinx.coroutines.delay

private const val PLUS_ENDED_COUNTDOWN_SECONDS = 5

/**
 * Full-screen explanation before the launcher icon changes (the app has to restart).
 * After Plus ends there is no choice: a short countdown, then the restart.
 */
@Composable
internal fun IconTransitionScreen(
    kind: IconTransition,
    onRestartNow: () -> Unit,
    onLater: () -> Unit,
    onClose: () -> Unit,
) {
    val toPlus = kind == IconTransition.WELCOME || kind == IconTransition.TO_PLUS
    val forced = kind == IconTransition.PLUS_ENDED
    val haptics = rememberAppHaptics()
    BackHandler { if (!forced) onClose() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (!forced) {
                    FilledTonalIconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.plus_close))
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                IconSwap(
                    fromRes = if (toPlus) R.mipmap.ic_launcher else R.mipmap.ic_launcher_plus,
                    toRes = if (toPlus) R.mipmap.ic_launcher_plus else R.mipmap.ic_launcher,
                )
                Text(
                    stringResource(when (kind) {
                        IconTransition.WELCOME -> R.string.icon_welcome_title
                        IconTransition.TO_PLUS -> R.string.icon_to_plus_title
                        IconTransition.TO_CLASSIC -> R.string.icon_to_classic_title
                        IconTransition.PLUS_ENDED -> R.string.icon_plus_ended_title
                    }),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(when (kind) {
                        IconTransition.WELCOME -> R.string.icon_welcome_body
                        IconTransition.PLUS_ENDED -> R.string.icon_plus_ended_body
                        else -> R.string.icon_restart_body
                    }),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (kind == IconTransition.WELCOME) {
                    Text(
                        stringResource(R.string.icon_restart_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (forced) Countdown(onFinished = onRestartNow)
            }
            if (!forced) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val height = ButtonDefaults.MediumContainerHeight
                    Button(
                        onClick = { haptics.confirm(); onRestartNow() },
                        shapes = ButtonDefaults.shapesFor(height),
                        contentPadding = ButtonDefaults.contentPaddingFor(height),
                        modifier = Modifier.fillMaxWidth().heightIn(min = height),
                    ) { Text(stringResource(R.string.icon_restart_now), style = ButtonDefaults.textStyleFor(height)) }
                    OutlinedButton(
                        onClick = { haptics.toggle(); onLater() },
                        shapes = ButtonDefaults.shapesFor(height),
                        contentPadding = ButtonDefaults.contentPaddingFor(height),
                        modifier = Modifier.fillMaxWidth().heightIn(min = height),
                    ) { Text(stringResource(R.string.icon_restart_later), style = ButtonDefaults.textStyleFor(height)) }
                    TextButton(
                        onClick = onClose,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(if (toPlus) R.string.icon_keep_classic else R.string.icon_cancel)) }
                }
            }
        }
    }
}

/** Current icon → pulsing arrows → new icon, which springs in. */
@Composable
private fun IconSwap(fromRes: Int, toRes: Int) {
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    val arrows = rememberInfiniteTransition(label = "icon_swap_arrows")
    val phase by arrows.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 3f else 0f,
        animationSpec = infiniteRepeatable(tween(1_200), RepeatMode.Restart),
        label = "icon_swap_phase",
    )
    val arrive = remember { Animatable(if (animate) 0.6f else 1f) }
    val spring = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(toRes) { if (animate) arrive.animateTo(1f, spring) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LauncherIcon(fromRes, 72.dp, Modifier.graphicsLayer { alpha = 0.6f })
        Row {
            repeat(3) { index ->
                // Each chevron brightens in turn, so the arrow reads left to right.
                val lit = if (animate) (1f - kotlin.math.abs(phase - index - 0.5f)).coerceIn(0.25f, 1f) else 1f
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp).graphicsLayer { alpha = lit },
                )
            }
        }
        LauncherIcon(toRes, 96.dp, Modifier.graphicsLayer { scaleX = arrive.value; scaleY = arrive.value })
    }
}

@Composable
private fun LauncherIcon(res: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = rememberDrawablePainter(res),
        contentDescription = null,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)),
    )
}

@Composable
private fun Countdown(onFinished: () -> Unit) {
    var seconds by remember { mutableIntStateOf(PLUS_ENDED_COUNTDOWN_SECONDS) }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1_000L)
            seconds--
        }
        onFinished()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = seconds,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                label = "icon_countdown",
            ) { value ->
                Text(value.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(
            stringResource(R.string.icon_countdown, seconds),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

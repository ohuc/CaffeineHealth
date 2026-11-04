package com.uc.caffeine.monetization

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

/**
 * The Coach pill for users without Caffeine Health+. It takes the live Coach pill's place beside
 * the Home "My Consumptions" title: Plus mark, "Coach" and a lock, on a neutral tonal container so
 * it never reads as a live recommendation. Tapping asks for the feature, which opens the Plus
 * sheet explaining that the Coach is part of Caffeine Health+. It briefly glows when access ends.
 */
@Composable
fun LockedCoachChip(
    expired: Boolean,
    expiryKey: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var highlightedExpiry by rememberSaveable { mutableStateOf<Long?>(null) }
    val highlight = remember { Animatable(0f) }
    val motion = MaterialTheme.motionScheme
    val reducedMotion = !ValueAnimator.areAnimatorsEnabled()
    LaunchedEffect(expired, expiryKey, reducedMotion) {
        val event = expiryKey ?: 0L
        val animate = shouldHighlightCoachExpiry(highlightedExpiry == event, expired, reducedMotion)
        if (expired) highlightedExpiry = event
        if (animate) {
            highlight.animateTo(1f, motion.fastEffectsSpec())
            highlight.animateTo(0f, motion.slowEffectsSpec())
        } else highlight.snapTo(0f)
    }
    val colorScheme = MaterialTheme.colorScheme
    val lockedLabel = stringResource(R.string.plus_locked_state)
    val spokenLabel = stringResource(R.string.plus_feature_coach) + ". " + stringResource(R.string.plus_unlock_coach)

    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = lerp(colorScheme.surfaceContainerHigh, colorScheme.primaryContainer, highlight.value),
            contentColor = colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(start = 6.dp, end = 12.dp),
        modifier = modifier.semantics {
            contentDescription = spokenLabel
            stateDescription = lockedLabel
        },
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlusBadge(size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.coach_pill_locked),
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

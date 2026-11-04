package com.uc.caffeine.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.rememberAppHaptics
import kotlinx.coroutines.launch

/**
 * Shared Plus bottom sheet: a centred hero, title and body, then a medium primary button
 * and "Not now". Every way out closes with the sheet's hide animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlusSheet(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    hero: @Composable () -> Unit = { PlusAppIcon(size = 88.dp) },
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            hero()
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            extra()
            Spacer(Modifier.height(4.dp))
            val height = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = { haptics.toggle(); hideThen(onPrimary) },
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.fillMaxWidth().heightIn(min = height),
            ) { Text(primaryLabel, style = ButtonDefaults.textStyleFor(height)) }
            TextButton(
                onClick = { hideThen(onDismiss) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.plus_not_now)) }
        }
    }
}

/** The Caffeine Health+ launcher icon, as shown on the home screen. */
@Composable
internal fun PlusAppIcon(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = com.uc.caffeine.ui.screens.settings.rememberDrawablePainter(R.mipmap.ic_launcher_plus),
        contentDescription = null,
        modifier = modifier.size(size).clip(androidx.compose.foundation.shape.RoundedCornerShape(size * 0.28f)),
    )
}

/** A tonal pill for short facts ("No card · no automatic charge"). */
@Composable
internal fun PlusInfoPill(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
        Text(
            text,
            style = MaterialTheme.typography.labelLargeEmphasized,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Offered once, on the third separate day the app is opened. */
@Composable
internal fun TrialOfferSheet(openDays: Int, onStart: () -> Unit, onDismiss: () -> Unit) {
    PlusSheet(
        title = stringResource(R.string.plus_trial_offer_title),
        body = stringResource(R.string.plus_trial_offer_body, openDays),
        primaryLabel = stringResource(R.string.plus_trial_start),
        onPrimary = onStart,
        onDismiss = onDismiss,
        hero = { ThreeDaysHero() },
        extra = { PlusInfoPill(stringResource(R.string.plus_trial_terms)) },
    )
}

/** Three ticked days leading to the Plus badge: the reason the offer appeared. */
@Composable
private fun ThreeDaysHero() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        repeat(PlusJourneyPolicy.TRIAL_UNLOCK_DAYS) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(
                    stringResource(R.string.plus_trial_day, index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        PlusBadge(size = 56.dp, modifier = Modifier.padding(bottom = 20.dp))
    }
}

/** Shown once, on the first app open after the free day ended. */
@Composable
internal fun TrialEndedSheet(onSeePlans: () -> Unit, onDismiss: () -> Unit) {
    PlusSheet(
        title = stringResource(R.string.plus_trial_ended_title),
        body = stringResource(R.string.plus_trial_ended_body),
        primaryLabel = stringResource(R.string.plus_see_plans),
        onPrimary = onSeePlans,
        onDismiss = onDismiss,
    )
}

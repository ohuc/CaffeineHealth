package com.uc.caffeine.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uc.caffeine.BuildConfig
import com.uc.caffeine.R
import com.uc.caffeine.data.NewFeature
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Highlight(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
)

@Composable
private fun NewFeature.highlight(): Highlight = when (this) {
    NewFeature.BarcodeScanner -> Highlight(
        ImageVector.vectorResource(R.drawable.ic_barcode_scanner),
        R.string.whats_new_scanner_title,
        R.string.whats_new_scanner_body,
    )
    NewFeature.Analytics -> Highlight(Icons.Filled.Insights, R.string.whats_new_analytics_title, R.string.whats_new_analytics_body)
    NewFeature.DrinkSearch -> Highlight(Icons.Filled.Search, R.string.whats_new_search_title, R.string.whats_new_search_body)
    NewFeature.TeaSteeps -> Highlight(Icons.Filled.EmojiFoodBeverage, R.string.whats_new_steeps_title, R.string.whats_new_steeps_body)
    NewFeature.Widgets -> Highlight(Icons.Filled.Widgets, R.string.whats_new_widgets_title, R.string.whats_new_widgets_body)
    NewFeature.ExpressiveBlur -> Highlight(Icons.Filled.BlurOn, R.string.whats_new_blur_title, R.string.whats_new_blur_body)
}

/**
 * Home's note that the app changed after an update, shaped like a split button: the wide part
 * opens What's New, the small end dismisses it. Nothing opens until the user asks.
 */
@Composable
fun WhatsNewCard(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val round = CornerSize(percent = 50)
    val seam = CornerSize(6.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Surface(
            onClick = onOpen,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = round, bottomStart = round, topEnd = seam, bottomEnd = seam),
            color = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeatureIconStack(NewFeature.entries.take(3))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.whats_new_card_cta),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Surface(
            onClick = onDismiss,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = seam, bottomStart = seam, topEnd = round, bottomEnd = round),
            color = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.whats_new_card_dismiss_cd),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** The first few highlights' icons, overlapping like avatars and popping in one after another. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeatureIconStack(features: List<NewFeature>) {
    val colorScheme = MaterialTheme.colorScheme
    val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    Box {
        features.forEachIndexed { index, feature ->
            val scale = remember { Animatable(0.4f) }
            LaunchedEffect(Unit) {
                delay(index * 90L)
                scale.animateTo(1f, popSpec)
            }
            Surface(
                modifier = Modifier
                    .padding(start = 16.dp * index)
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
                shape = CircleShape,
                color = colorScheme.tertiary,
                contentColor = colorScheme.onTertiary,
                border = BorderStroke(2.dp, colorScheme.tertiaryContainer),
            ) {
                Icon(
                    imageVector = feature.highlight().icon,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val features = NewFeature.entries
    val headline = features.firstOrNull()
    val others = features.drop(1)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        // Only the highlights scroll; "Got it" stays reachable however long the list gets.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            EnterInSequence(order = 0) { WhatsNewHeader() }
            Spacer(modifier = Modifier.height(24.dp))
            headline?.let {
                EnterInSequence(order = 1) { HeadlineHighlight(it.highlight()) }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                others.forEachIndexed { index, feature ->
                    EnterInSequence(order = index + 2) {
                        HighlightRow(highlight = feature.highlight(), index = index, count = others.size)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                haptics.confirm()
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.whats_new_got_it),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WhatsNewHeader() {
    val colorScheme = MaterialTheme.colorScheme
    // The same slowly turning burst as the Add Consumption button.
    val burstRotation by rememberInfiniteTransition(label = "whats_new_burst").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "whats_new_burst",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { rotationZ = burstRotation }
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(colorScheme.primaryContainer),
            )
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = CircleShape,
            color = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = stringResource(R.string.whats_new_version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.whats_new_title),
            style = MaterialTheme.typography.headlineMediumEmphasized,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.whats_new_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The release's lead feature, given a card of its own above the rest. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeadlineHighlight(highlight: Highlight) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colorScheme.tertiaryContainer,
        contentColor = colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    tint = colorScheme.onTertiary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(highlight.titleRes),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
                Text(
                    text = stringResource(highlight.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HighlightRow(highlight: Highlight, index: Int, count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = segmentedListItemShapes(index, count).shape,
        color = CaffeineSurfaceDefaults.groupedListContainerColor,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExpressiveIconBadge(index = index, size = 44.dp) {
                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(highlight.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(highlight.bodyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Fades and lifts [content] into place, [order] steps after the sheet opens. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EnterInSequence(order: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        delay(order * 60L)
        progress.animateTo(1f, spec)
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1f - progress.value) * 32.dp.toPx()
        },
    ) {
        content()
    }
}

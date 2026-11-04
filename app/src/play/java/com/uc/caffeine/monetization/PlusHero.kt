package com.uc.caffeine.monetization

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uc.caffeine.R
import com.uc.caffeine.ui.screens.settings.rememberDrawablePainter

internal val LocalPlusMotionEnabled = staticCompositionLocalOf { true }

@Composable
internal fun PlusHeroMark(size: Dp) {
    val motion = LocalPlusMotionEnabled.current
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(if (entered || !motion) 1f else 0.85f,
        if (motion) MaterialTheme.motionScheme.defaultSpatialSpec() else snap(), label = "plus_mark_scale")
    val alpha by animateFloatAsState(if (entered || !motion) 1f else 0f,
        if (motion) MaterialTheme.motionScheme.defaultEffectsSpec() else snap(), label = "plus_mark_alpha")
    Box(
        Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie12Sided.toShape())
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_plus_glyph), null,
            Modifier.size(size * 0.48f), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun PlusHero(chipText: String?, @StringRes subtitleRes: Int = R.string.plus_pitch_subtitle_short) {
    val compact = LocalConfiguration.current.screenHeightDp < 480 || LocalDensity.current.fontScale >= 1.5f
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!compact) {
            PlusHeroMark(if (LocalConfiguration.current.screenHeightDp < 700) 80.dp else 96.dp)
            Spacer(Modifier.height(24.dp))
        }
        Text(stringResource(R.string.plus_pitch_eyebrow), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = if (compact) 0.sp else 3.sp),
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(buildAnnotatedString {
            append(stringResource(R.string.plus_pitch_headline_1))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontStyle = FontStyle.Italic)) {
                append(stringResource(R.string.plus_pitch_headline_2))
            }
            append(stringResource(R.string.plus_pitch_headline_3))
        }, style = if (compact) MaterialTheme.typography.headlineSmallEmphasized else MaterialTheme.typography.displaySmallEmphasized, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() })
        Spacer(Modifier.height(12.dp))
        Text(stringResource(subtitleRes), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp))
        chipText?.let {
            Spacer(Modifier.height(16.dp))
            PlusStatusChip(it)
        }
    }
}

@Composable
internal fun PlusOwnerHeader(subtitle: String, chipText: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(rememberDrawablePainter(R.mipmap.ic_launcher_plus), null,
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.plus_tier_name), style = MaterialTheme.typography.headlineSmallEmphasized,
                    modifier = Modifier.semantics { heading() })
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        chipText?.let { PlusStatusChip(it) }
    }
}

@Composable
internal fun PlusStatusChip(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Text(text, style = MaterialTheme.typography.labelLargeEmphasized,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
internal fun PlusSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() })
}

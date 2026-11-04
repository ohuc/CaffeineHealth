package com.uc.caffeine.monetization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.rememberAppHaptics

/**
 * The Caffeine Health+ introduction inside first-run setup (Play only, and only while the Plus
 * journey is active). Calm and typographic like the Plus page: hero, one sample, three benefits,
 * and two honest exits. "Continue with Free" is always on screen; no prices, countdowns or
 * preselected plan appear here, since plans live on the Plus page that "See Plus plans" opens.
 */
@Composable
internal fun PlusOnboardingIntro(
    onContinue: () -> Unit,
    onSeePlans: () -> Unit,
    onShown: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    LaunchedEffect(Unit) { onShown() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    PlusHero(chipText = null, subtitleRes = R.string.plus_onboarding_subtitle)
                    PlusOnboardingBenefits()
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { haptics.confirm(); onSeePlans() },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) { Text(stringResource(R.string.plus_see_plans), style = MaterialTheme.typography.titleMedium) }
                        TextButton(
                            onClick = { haptics.toggle(); onContinue() },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.plus_continue)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlusOnboardingBenefits() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlusBenefitRow(ProFeature.Coach, R.string.plus_feature_coach, R.string.plus_benefit_coach_summary)
                PlusCoachArt()
                Text(
                    stringResource(R.string.plus_coach_sample_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PlusBenefitRow(ProFeature.HealthConnect, R.string.plus_feature_health, R.string.plus_benefit_health_summary)
                PlusBenefitRow(ProFeature.ExtendedAnalytics, R.string.plus_feature_analytics, R.string.plus_benefit_analytics_summary)
            }
        }
    }
}

@Composable
private fun PlusBenefitRow(feature: ProFeature, title: Int, summary: Int) {
    Row(
        Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusFeatureIcon(feature.icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMediumEmphasized)
            Text(
                stringResource(summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

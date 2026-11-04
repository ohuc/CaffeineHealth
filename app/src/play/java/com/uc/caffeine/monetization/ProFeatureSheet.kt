package com.uc.caffeine.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R

internal val ProFeature.icon: ImageVector
    get() = when (this) {
        ProFeature.Coach -> Icons.Rounded.Lightbulb
        ProFeature.HealthConnect -> Icons.Rounded.Favorite
        ProFeature.Automation -> Icons.Rounded.Bolt
        ProFeature.PremiumWidgets -> Icons.Rounded.Widgets
        ProFeature.AdvancedAppearance -> Icons.Rounded.Palette
        ProFeature.ExtendedAnalytics -> Icons.Rounded.Insights
    }

@Composable
internal fun ProFeatureSheet(feature: ProFeature, onSeePlans: () -> Unit, onDismiss: () -> Unit) {
    val (title, body) = when (feature) {
        ProFeature.Coach -> R.string.plus_feature_coach to R.string.plus_feature_coach_body
        ProFeature.HealthConnect -> R.string.plus_feature_health to R.string.plus_feature_health_body
        ProFeature.Automation -> R.string.plus_feature_automation to R.string.plus_feature_automation_body
        ProFeature.PremiumWidgets -> R.string.plus_feature_widgets to R.string.plus_feature_widgets_body
        ProFeature.AdvancedAppearance -> R.string.plus_feature_appearance to R.string.plus_feature_appearance_body
        ProFeature.ExtendedAnalytics -> R.string.plus_feature_analytics to R.string.plus_feature_analytics_body
    }
    PlusSheet(
        title = stringResource(title),
        body = stringResource(body),
        primaryLabel = stringResource(R.string.plus_see_plans),
        onPrimary = onSeePlans,
        onDismiss = onDismiss,
        hero = { FeatureHero(feature) },
        extra = {
            if (feature == ProFeature.Coach) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.plus_coach_sample_label), style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.plus_coach_sample), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.plus_coach_sample_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
    )
}

/** The feature's icon on an expressive shape, with the Plus badge pinned to its corner. */
@Composable
private fun FeatureHero(feature: ProFeature) {
    Box(Modifier.size(112.dp)) {
        Box(
            Modifier
                .size(96.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp))
        }
        PlusBadge(size = 36.dp, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

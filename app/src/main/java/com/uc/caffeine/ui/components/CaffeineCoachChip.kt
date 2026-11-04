package com.uc.caffeine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.util.CaffeineCoach
import com.uc.caffeine.util.CoachReason
import com.uc.caffeine.util.CoachRecommendation
import com.uc.caffeine.util.calculateNextBedtimeMillis
import com.uc.caffeine.util.formatTimestampToTime

// Below ~1h of sleep debt we don't frame the coach message as sleep recovery.
private const val COACH_SLEEP_DEBT_NOTE_MINUTES = 60

/**
 * Compact Caffeine Coach pill that sits beside the Home "My Consumptions" title
 * (`{icon} Coach: ~x mg`). The tone carries the state: primary for an actionable dose,
 * tertiary while winding down for bedtime, secondary when no caffeine is needed. The
 * spoken label is the full one-line summary. Tapping opens [CaffeineCoachSheet], which
 * explains what the Coach recommends and why.
 */
@Composable
fun CaffeineCoachChip(
    recommendation: CoachRecommendation,
    userSettings: UserSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    val container: Color
    val content: Color
    val icon: ImageVector
    val label: String
    val spokenLabel: String
    when (recommendation) {
        is CoachRecommendation.Recommend -> {
            container = colorScheme.primaryContainer
            content = colorScheme.onPrimaryContainer
            icon = Icons.Filled.Bolt
            label = stringResource(R.string.coach_pill_dose, recommendation.doseMg)
            spokenLabel = if (recommendation.isNow) {
                stringResource(R.string.coach_chip_recommend_now, recommendation.doseMg)
            } else {
                stringResource(
                    R.string.coach_chip_recommend_at,
                    recommendation.doseMg,
                    formatTimestampToTime(recommendation.atMillis, userSettings),
                )
            }
        }
        is CoachRecommendation.Hold -> if (recommendation.reason == CoachReason.BEDTIME_PROTECTION) {
            container = colorScheme.tertiaryContainer
            content = colorScheme.onTertiaryContainer
            icon = Icons.Filled.Bedtime
            label = stringResource(R.string.coach_pill_hold_bedtime)
            spokenLabel = stringResource(R.string.coach_chip_hold_bedtime, formatBedtime(userSettings))
        } else {
            container = colorScheme.secondaryContainer
            content = colorScheme.onSecondaryContainer
            icon = Icons.Filled.CheckCircle
            label = stringResource(R.string.coach_pill_hold_alert)
            spokenLabel = stringResource(R.string.coach_chip_hold_alert)
        }
    }
    // Effects springs for colour (never overshoot); a spatial spring lets the pill grow or
    // shrink smoothly when the label changes width.
    val containerColor by animateColorAsState(container, motion.defaultEffectsSpec(), label = "coach_pill_container")
    val contentColor by animateColorAsState(content, motion.defaultEffectsSpec(), label = "coach_pill_content")

    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
        modifier = modifier
            .animateContentSize(motion.defaultSpatialSpec())
            .semantics { contentDescription = spokenLabel },
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * The Caffeine Coach detail sheet: full headline, the reasoning behind the
 * suggestion, the residual-at-bedtime note, and a one-tap "Log this dose"
 * action for actionable recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaffeineCoachSheet(
    recommendation: CoachRecommendation,
    userSettings: UserSettings,
    onLogDose: (doseMg: Int, entryName: String) -> Unit,
    onDismiss: () -> Unit,
    onConnectSleep: (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bedtimeStr = formatBedtime(userSettings)

    val badgeContainer: Color
    val badgeContent: Color
    val icon: ImageVector
    when (recommendation) {
        is CoachRecommendation.Recommend -> {
            badgeContainer = colorScheme.primaryContainer
            badgeContent = colorScheme.onPrimaryContainer
            icon = Icons.Filled.Bolt
        }
        is CoachRecommendation.Hold -> when (recommendation.reason) {
            CoachReason.BEDTIME_PROTECTION -> {
                badgeContainer = colorScheme.tertiaryContainer
                badgeContent = colorScheme.onTertiaryContainer
                icon = Icons.Filled.Bedtime
            }
            else -> {
                badgeContainer = colorScheme.secondaryContainer
                badgeContent = colorScheme.onSecondaryContainer
                icon = Icons.Filled.CheckCircle
            }
        }
    }

    val headline: String
    val supportingLines = mutableListOf<String>()
    when (recommendation) {
        is CoachRecommendation.Recommend -> {
            headline = if (recommendation.isNow) {
                stringResource(R.string.coach_headline_now, recommendation.doseMg)
            } else {
                stringResource(
                    R.string.coach_headline_at,
                    recommendation.doseMg,
                    formatTimestampToTime(recommendation.atMillis, userSettings),
                )
            }
            supportingLines += when (recommendation.reason) {
                CoachReason.SLEEP_DEBT -> stringResource(
                    R.string.coach_reason_sleep_debt,
                    formatSleptDuration(recommendation.sleepDebtMinutes),
                )
                CoachReason.AFTERNOON_DIP -> stringResource(R.string.coach_reason_afternoon_dip)
                else -> stringResource(R.string.coach_reason_stay_sharp)
            }
            if (recommendation.residualAtBedtimeMg > 0) {
                supportingLines += stringResource(
                    R.string.coach_residual_note,
                    recommendation.residualAtBedtimeMg,
                    bedtimeStr,
                )
            }
        }
        is CoachRecommendation.Hold -> {
            headline = when (recommendation.reason) {
                CoachReason.BEDTIME_PROTECTION ->
                    stringResource(R.string.coach_hold_bedtime, bedtimeStr)
                else ->
                    if (recommendation.personalized &&
                        recommendation.sleepDebtMinutes >= COACH_SLEEP_DEBT_NOTE_MINUTES
                    ) {
                        stringResource(
                            R.string.coach_hold_already_alert_debt,
                            formatSleptDuration(recommendation.sleepDebtMinutes),
                        )
                    } else {
                        stringResource(R.string.coach_hold_already_alert)
                    }
            }
        }
    }
    if (!recommendation.personalized) {
        supportingLines += stringResource(R.string.coach_connect_sleep)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = badgeContainer,
                    contentColor = badgeContent,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.coach_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = colorScheme.onSurface,
            )

            supportingLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            // Without last night's sleep the Coach falls back to a generic plan; one tap takes the
            // user to the Health Connect page, where they can turn sleep for bedtime on.
            if (!recommendation.personalized && onConnectSleep != null) {
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onConnectSleep,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.health_connect_logo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.coach_connect_sleep_action),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
            }

            if (recommendation is CoachRecommendation.Recommend) {
                Spacer(modifier = Modifier.height(8.dp))
                val entryName = stringResource(R.string.coach_title)
                Button(
                    onClick = { onLogDose(recommendation.doseMg, entryName) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.coach_log_dose))
                }
            }
        }
    }
}

private fun formatBedtime(settings: UserSettings): String {
    val nextBedtimeMillis = calculateNextBedtimeMillis(System.currentTimeMillis(), settings)
    return formatTimestampToTime(nextBedtimeMillis, settings)
}

/** Formats the user's actual sleep last night (sleep need − debt) as "Xh Ym". */
@Composable
private fun formatSleptDuration(sleepDebtMinutes: Int): String {
    val slept = (CaffeineCoach.DEFAULT_SLEEP_NEED_MINUTES - sleepDebtMinutes).coerceAtLeast(0)
    return stringResource(R.string.coach_sleep_hours, slept / 60, slept % 60)
}

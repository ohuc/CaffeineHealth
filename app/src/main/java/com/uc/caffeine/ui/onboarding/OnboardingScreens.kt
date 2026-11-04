package com.uc.caffeine.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.formatTimeOfDay

@Composable
internal fun IntroScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    /** The edition's "restore my data" action for returning users (Play: Google Drive). */
    restoreAction: (@Composable () -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                )
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_glyph),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = stringResource(R.string.brand_caffeine_health),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                TextButton(onClick = { haptics.toggle(); onSkip() }) {
                    Text(stringResource(R.string.action_skip))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_coffee_cup),
                    contentDescription = null,
                    modifier = Modifier.size(108.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_kicker),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 3.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.onboarding_intro_line_1))
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = FontStyle.Italic,
                        ),
                    ) {
                        append(stringResource(R.string.onboarding_intro_line_2))
                    }
                    append(stringResource(R.string.onboarding_intro_line_3))
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.onboarding_intro_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                    )
                    .padding(top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { haptics.confirm(); onStart() },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_get_started),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_takes_a_minute),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                restoreAction?.invoke()
            }
        }
    }
}

@Composable
internal fun BasicInfoScreen(
    uiState: OnboardingUiState,
    progress: OnboardingProgress,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onAgeBucketSelected: (AgeBucket) -> Unit,
    onWeightIncrement: () -> Unit,
    onWeightDecrement: () -> Unit,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    onWeightChanged: (Int) -> Unit = {},
) {
    val ageBucketLabels = AgeBucket.entries.associateWith { stringResource(it.labelRes) }
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_basic_info_title),
        subtitle = stringResource(R.string.onboarding_basic_info_subtitle),
        step = progress.step,
        stepCount = progress.total,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = stringResource(R.string.action_continue),
        continueEnabled = uiState.answers.isBasicInfoComplete(),
        onContinue = onContinue,
        disabledHint = stringResource(R.string.onboarding_basic_info_disabled_hint),
        enabledHint = stringResource(R.string.onboarding_basic_info_enabled_hint),
    ) {
        OnboardingSection(
            title = stringResource(R.string.onboarding_age_range_section),
            supportingText = stringResource(R.string.onboarding_age_range_supporting),
        ) {
            ConnectedChoiceButtonGroup(
                options = AgeBucket.entries,
                selectedOption = uiState.answers.ageBucket,
                labelFor = { ageBucket -> ageBucketLabels[ageBucket] ?: "" },
                onOptionSelected = onAgeBucketSelected,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        OnboardingSection(
            title = stringResource(R.string.onboarding_weight_section),
            supportingText = stringResource(R.string.onboarding_weight_supporting),
        ) {
            WeightStepperCard(
                weightValue = uiState.answers.weightValue,
                weightUnit = uiState.answers.weightUnit,
                onWeightUnitSelected = onWeightUnitSelected,
                onIncrement = onWeightIncrement,
                onDecrement = onWeightDecrement,
                onWeightChanged = onWeightChanged,
            )
        }
    }
}

@Composable
internal fun SleepScreen(
    uiState: OnboardingUiState,
    progress: OnboardingProgress,
    displaySettings: UserSettings,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onSleepTimeChanged: (java.time.LocalTime) -> Unit,
    onInsomniaChanged: (Boolean) -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_sleep_title),
        subtitle = stringResource(R.string.onboarding_sleep_subtitle),
        step = progress.step,
        stepCount = progress.total,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = stringResource(R.string.action_continue),
        continueEnabled = uiState.answers.isSleepComplete(),
        onContinue = onContinue,
        disabledHint = stringResource(R.string.onboarding_sleep_disabled_hint),
        enabledHint = stringResource(R.string.onboarding_sleep_enabled_hint),
    ) {
        BedtimeWheel(
            selected = Bedtime.from(uiState.answers.sleepTime),
            onSelect = { onSleepTimeChanged(it.time) },
        )

        SleepTimePickerCard(
            displaySettings = displaySettings,
            selectedTime = uiState.answers.sleepTime,
            onSleepTimeChanged = onSleepTimeChanged,
            title = null,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        OnboardingSection(
            title = stringResource(R.string.profile_sleep_sensitivity),
            supportingText = stringResource(R.string.onboarding_sleep_sensitivity_supporting),
        ) {
            YesNoChoiceGroup(
                selected = uiState.answers.hasInsomnia,
                onSelected = onInsomniaChanged,
            )
        }
    }
}

@Composable
internal fun LifestyleScreen(
    uiState: OnboardingUiState,
    progress: OnboardingProgress,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onSmokingHabitChanged: (SmokingHabit) -> Unit,
    onHeavyAlcoholChanged: (Boolean) -> Unit,
    onHeavyCaffeineChanged: (Boolean) -> Unit,
) {
    val smokingHabitLabels = SmokingHabit.entries.associateWith { stringResource(it.buttonLabelRes()) }
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_lifestyle_title),
        subtitle = stringResource(R.string.onboarding_lifestyle_subtitle),
        step = progress.step,
        stepCount = progress.total,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = stringResource(R.string.action_continue),
        continueEnabled = uiState.answers.isLifestyleComplete(),
        onContinue = onContinue,
        disabledHint = stringResource(R.string.onboarding_lifestyle_disabled_hint),
        enabledHint = stringResource(R.string.onboarding_lifestyle_enabled_hint),
    ) {
        OnboardingSection(title = stringResource(R.string.profile_smoking_habit)) {
            GridSingleSelectButtonGroup(
                options = SmokingHabit.entries,
                selectedOption = uiState.answers.smokingHabit,
                labelFor = { smokingHabit -> smokingHabitLabels[smokingHabit] ?: "" },
                onOptionSelected = onSmokingHabitChanged,
            )
        }

        OnboardingSection(title = stringResource(R.string.profile_heavy_alcohol)) {
            YesNoChoiceGroup(
                selected = uiState.answers.heavyAlcohol,
                onSelected = onHeavyAlcoholChanged,
            )
        }

        OnboardingSection(title = stringResource(R.string.profile_high_caffeine)) {
            YesNoChoiceGroup(
                selected = uiState.answers.heavyCaffeine,
                onSelected = onHeavyCaffeineChanged,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        InfoTextWithSource(
            text = stringResource(R.string.onboarding_smoking_info),
            sourceUrl = stringResource(R.string.onboarding_smoking_source_url),
        )
        InfoTextWithSource(
            text = stringResource(R.string.onboarding_alcohol_info),
            sourceUrl = stringResource(R.string.onboarding_alcohol_source_url),
        )
        InfoTextWithSource(
            text = stringResource(R.string.onboarding_caffeine_info),
            sourceUrl = stringResource(R.string.onboarding_caffeine_source_url),
        )
    }
}

@Composable
internal fun MedicalScreen(
    uiState: OnboardingUiState,
    progress: OnboardingProgress,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onLiverDiseaseChanged: (LiverDisease) -> Unit,
    onMedicationToggled: (Medication) -> Unit,
) {
    val liverDiseaseLabels = LiverDisease.entries.associateWith { stringResource(it.buttonLabelRes()) }
    val medicationLabels = Medication.entries.associateWith { stringResource(it.buttonLabelRes()) }
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_medical_title),
        subtitle = stringResource(R.string.onboarding_medical_subtitle),
        step = progress.step,
        stepCount = progress.total,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = stringResource(R.string.onboarding_medical_continue_label),
        continueEnabled = uiState.answers.isMedicalComplete(),
        onContinue = onContinue,
        disabledHint = stringResource(R.string.onboarding_medical_disabled_hint),
        enabledHint = stringResource(R.string.onboarding_medical_enabled_hint),
    ) {
        OnboardingSection(title = stringResource(R.string.profile_liver_context)) {
            GridSingleSelectButtonGroup(
                options = LiverDisease.entries,
                selectedOption = uiState.answers.liverDisease,
                labelFor = { liverDisease -> liverDiseaseLabels[liverDisease] ?: "" },
                onOptionSelected = onLiverDiseaseChanged,
            )
        }

        OnboardingSection(
            title = stringResource(R.string.onboarding_medications_section),
            supportingText = stringResource(R.string.onboarding_medications_supporting),
        ) {
            GridMultiSelectButtonGroup(
                options = Medication.noneAndInhibitors,
                selectedOptions = uiState.answers.medications,
                labelFor = { medication -> medicationLabels[medication] ?: "" },
                onOptionToggled = onMedicationToggled,
            )
            MedicationGroupLabel(text = stringResource(R.string.medication_group_inducers))
            GridMultiSelectButtonGroup(
                options = Medication.inducers,
                selectedOptions = uiState.answers.medications,
                labelFor = { medication -> medicationLabels[medication] ?: "" },
                onOptionToggled = onMedicationToggled,
            )
            InfoTextWithSource(
                text = stringResource(R.string.onboarding_medications_info),
                sourceUrl = stringResource(R.string.onboarding_medication_source_url),
            )
        }
    }
}

@Composable
internal fun ProfileReadyScreen(
    uiState: OnboardingUiState,
    progress: OnboardingProgress,
    displaySettings: UserSettings,
    onBack: () -> Unit,
    onOpenLegalSheet: () -> Unit,
    onDismissLegalSheet: () -> Unit,
    onLegalAcknowledgedChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
) {
    val profile = uiState.currentProfile() ?: OnboardingProfileCalculator.defaultProfile

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_profile_ready_title),
        subtitle = stringResource(R.string.onboarding_profile_ready_subtitle),
        step = progress.step,
        stepCount = progress.total,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = false,
        continueLabel = stringResource(R.string.onboarding_profile_ready_continue_label),
        continueEnabled = true,
        onContinue = onOpenLegalSheet,
        enabledHint = stringResource(R.string.onboarding_profile_ready_enabled_hint),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileMetricRow(
                    title = stringResource(R.string.profile_estimated_half_life),
                    value = formatHalfLife(profile.halfLifeMinutes),
                    description = stringResource(R.string.onboarding_metric_half_life_description),
                    info = stringResource(R.string.onboarding_dialog_half_life_body),
                )
                HorizontalDivider()
                ProfileMetricRow(
                    title = stringResource(R.string.profile_sleep_threshold),
                    value = stringResource(R.string.profile_sleep_threshold_value, profile.sleepThresholdMg),
                    description = stringResource(R.string.onboarding_metric_sleep_threshold_description),
                    info = stringResource(R.string.onboarding_dialog_sleep_threshold_body),
                )
                HorizontalDivider()
                ProfileMetricRow(
                    title = stringResource(R.string.profile_typical_bedtime),
                    value = formatTimeOfDay(
                        hour = profile.sleepTimeHour,
                        minute = profile.sleepTimeMinute,
                        settings = displaySettings,
                    ),
                    description = stringResource(R.string.onboarding_metric_bedtime_description),
                    info = stringResource(R.string.onboarding_dialog_bedtime_body),
                )
            }
        }
    }

    if (uiState.showLegalSheet) {
        LegalSheet(
            legalAcknowledged = uiState.legalAcknowledged,
            isSaving = uiState.isSaving,
            onDismiss = onDismissLegalSheet,
            onAcknowledgedChanged = onLegalAcknowledgedChanged,
            onComplete = onComplete,
        )
    }
}

@Composable
internal fun NotificationsScreen(
    progress: OnboardingProgress,
    permissionNeeded: Boolean,
    /** Access is on: show the reminder controls from Settings instead of the question. */
    configuring: Boolean,
    displaySettings: UserSettings,
    onBack: () -> Unit,
    onTurnOn: () -> Unit,
    onNotNow: () -> Unit,
    onContinue: () -> Unit,
    onInactivityReminderToggle: (Boolean) -> Unit,
    onDailyReminderTimesChange: (Set<String>) -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(if (configuring) R.string.onboarding_notifications_ready_title else R.string.onboarding_notifications_title),
        subtitle = stringResource(if (configuring) R.string.onboarding_notifications_ready_subtitle else R.string.onboarding_notifications_subtitle),
        step = progress.step,
        stepCount = progress.total,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = false,
        continueLabel = stringResource(if (configuring) R.string.action_continue else R.string.onboarding_notifications_turn_on),
        continueEnabled = true,
        onContinue = if (configuring) onContinue else onTurnOn,
        enabledHint = if (!configuring && permissionNeeded) stringResource(R.string.onboarding_notifications_permission_hint) else null,
        secondaryLabel = if (configuring) null else stringResource(R.string.onboarding_notifications_not_now),
        onSecondary = if (configuring) null else onNotNow,
    ) {
        val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val sizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
        AnimatedContent(
            targetState = configuring,
            transitionSpec = {
                (fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec))
                    .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> sizeSpec }))
            },
            label = "onboarding_reminders",
        ) { showControls ->
            if (showControls) {
                com.uc.caffeine.ui.screens.settings.ReminderSettingsSections(
                    userSettings = displaySettings,
                    onInactivityReminderToggle = onInactivityReminderToggle,
                    onDailyReminderTimesChange = onDailyReminderTimesChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.onboarding_notifications_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 18.dp),
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }
}

@Composable
internal fun BasicInfoScreen(
    uiState: OnboardingUiState,
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
        currentStep = OnboardingDestination.BasicInfo.stepNumber,
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
    displaySettings: UserSettings,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onSleepTimeChanged: (java.time.LocalTime) -> Unit,
    onInsomniaChanged: (Boolean) -> Unit,
) {
    val yesLabel = stringResource(R.string.action_yes)
    val noLabel = stringResource(R.string.action_no)
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_sleep_title),
        subtitle = stringResource(R.string.onboarding_sleep_subtitle),
        currentStep = OnboardingDestination.Sleep.stepNumber,
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
            ConnectedChoiceButtonGroup(
                options = listOf(true, false),
                selectedOption = uiState.answers.hasInsomnia,
                labelFor = { if (it) yesLabel else noLabel },
                onOptionSelected = onInsomniaChanged,
            )
        }
    }
}

@Composable
internal fun LifestyleScreen(
    uiState: OnboardingUiState,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onSmokingHabitChanged: (SmokingHabit) -> Unit,
    onHeavyAlcoholChanged: (Boolean) -> Unit,
    onHeavyCaffeineChanged: (Boolean) -> Unit,
) {
    val yesLabel = stringResource(R.string.action_yes)
    val noLabel = stringResource(R.string.action_no)
    val smokingHabitLabels = SmokingHabit.entries.associateWith { stringResource(it.buttonLabelRes()) }
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_lifestyle_title),
        subtitle = stringResource(R.string.onboarding_lifestyle_subtitle),
        currentStep = OnboardingDestination.Lifestyle.stepNumber,
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
            ConnectedChoiceButtonGroup(
                options = listOf(true, false),
                selectedOption = uiState.answers.heavyAlcohol,
                labelFor = { if (it) yesLabel else noLabel },
                onOptionSelected = onHeavyAlcoholChanged,
            )
        }

        OnboardingSection(title = stringResource(R.string.profile_high_caffeine)) {
            ConnectedChoiceButtonGroup(
                options = listOf(true, false),
                selectedOption = uiState.answers.heavyCaffeine,
                labelFor = { if (it) yesLabel else noLabel },
                onOptionSelected = onHeavyCaffeineChanged,
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
        currentStep = OnboardingDestination.Medical.stepNumber,
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
                options = Medication.entries,
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
    displaySettings: UserSettings,
    onBack: () -> Unit,
    onOpenLegalSheet: () -> Unit,
    onDismissLegalSheet: () -> Unit,
    onLegalAcknowledgedChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
) {
    val profile = uiState.currentProfile() ?: OnboardingProfileCalculator.defaultProfile
    var infoDialog by rememberSaveable { mutableStateOf<ProfileInfoDialog?>(null) }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_profile_ready_title),
        subtitle = stringResource(R.string.onboarding_profile_ready_subtitle),
        currentStep = OnboardingDestination.ProfileReady.stepNumber,
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
                    onInfoClick = { infoDialog = ProfileInfoDialog.HalfLife },
                )
                HorizontalDivider()
                ProfileMetricRow(
                    title = stringResource(R.string.profile_sleep_threshold),
                    value = stringResource(R.string.profile_sleep_threshold_value, profile.sleepThresholdMg),
                    description = stringResource(R.string.onboarding_metric_sleep_threshold_description),
                    onInfoClick = { infoDialog = ProfileInfoDialog.SleepThreshold },
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
                    onInfoClick = { infoDialog = ProfileInfoDialog.Bedtime },
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

    infoDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            title = {
                Text(stringResource(dialog.titleRes))
            },
            text = {
                Text(stringResource(dialog.bodyRes))
            },
        )
    }
}

internal enum class ProfileInfoDialog(
    val titleRes: Int,
    val bodyRes: Int,
) {
    HalfLife(
        titleRes = R.string.profile_estimated_half_life,
        bodyRes = R.string.onboarding_dialog_half_life_body,
    ),
    SleepThreshold(
        titleRes = R.string.profile_sleep_threshold,
        bodyRes = R.string.onboarding_dialog_sleep_threshold_body,
    ),
    Bedtime(
        titleRes = R.string.profile_typical_bedtime,
        bodyRes = R.string.onboarding_dialog_bedtime_body,
    ),
}

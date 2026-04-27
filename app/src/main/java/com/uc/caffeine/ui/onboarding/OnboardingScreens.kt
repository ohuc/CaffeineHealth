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
                        text = "Caffeine Health",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                TextButton(onClick = { haptics.toggle(); onSkip() }) {
                    Text("Skip")
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
                text = "WELCOME",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 3.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = buildAnnotatedString {
                    append("Know your\n")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = FontStyle.Italic,
                        ),
                    ) {
                        append("caffeine.")
                    }
                    append("\nProtect\nyour sleep.")
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "A live look at what's still in your system and whether tonight's espresso will cost you sleep.",
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
                        text = "Get started",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = "Takes about a minute · No account needed",
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
    OnboardingScaffold(
        title = "Set your baseline",
        subtitle = "Pick your age range and leave the default weight alone unless you want to tune it now.",
        currentStep = OnboardingDestination.BasicInfo.stepNumber,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = "Continue",
        continueEnabled = uiState.answers.isBasicInfoComplete(),
        onContinue = onContinue,
        disabledHint = "Choose your age range to continue.",
        enabledHint = "Weight starts at 60 kg and can be adjusted later.",
    ) {
        OnboardingSection(
            title = "Age range",
            supportingText = "This keeps the estimate conservative when caffeine may linger longer.",
        ) {
            ConnectedChoiceButtonGroup(
                options = AgeBucket.entries,
                selectedOption = uiState.answers.ageBucket,
                labelFor = { ageBucket -> ageBucket.label },
                onOptionSelected = onAgeBucketSelected,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        OnboardingSection(
            title = "Weight",
            supportingText = "Use the stepper if you want to adjust the default starting weight.",
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
    OnboardingScaffold(
        title = "Map your sleep window",
        subtitle = "Pick your usual bedtime. We'll let you know when caffeine may still be active.",
        currentStep = OnboardingDestination.Sleep.stepNumber,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = "Continue",
        continueEnabled = uiState.answers.isSleepComplete(),
        onContinue = onContinue,
        disabledHint = "Tell us if sleep is sensitive for you.",
        enabledHint = "This powers the bedtime forecast and threshold line.",
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
            title = "Sleep sensitivity",
            supportingText = "If insomnia is already part of the picture, we keep the bedtime target more conservative.",
        ) {
            ConnectedChoiceButtonGroup(
                options = listOf(true, false),
                selectedOption = uiState.answers.hasInsomnia,
                labelFor = { if (it) "Yes" else "No" },
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
    OnboardingScaffold(
        title = "Add the real-life context",
        subtitle = "These habits change how quickly caffeine clears and how careful bedtime guidance should stay.",
        currentStep = OnboardingDestination.Lifestyle.stepNumber,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = "Continue",
        continueEnabled = uiState.answers.isLifestyleComplete(),
        onContinue = onContinue,
        disabledHint = "Tell us about smoking, alcohol, and daily caffeine.",
        enabledHint = "We blend these with your baseline before the final profile.",
    ) {
        OnboardingSection(title = "Smoking habit") {
            GridSingleSelectButtonGroup(
                options = SmokingHabit.entries,
                selectedOption = uiState.answers.smokingHabit,
                labelFor = { smokingHabit -> smokingHabit.buttonLabel() },
                onOptionSelected = onSmokingHabitChanged,
            )
        }

        OnboardingSection(title = "Heavy alcohol use") {
            ConnectedChoiceButtonGroup(
                options = listOf(true, false),
                selectedOption = uiState.answers.heavyAlcohol,
                labelFor = { if (it) "Yes" else "No" },
                onOptionSelected = onHeavyAlcoholChanged,
            )
        }

        OnboardingSection(title = "High daily caffeine") {
            ConnectedChoiceButtonGroup(
                options = listOf(true, false),
                selectedOption = uiState.answers.heavyCaffeine,
                labelFor = { if (it) "Yes" else "No" },
                onOptionSelected = onHeavyCaffeineChanged,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        InfoTextWithSource(
            text = "Smoking can speed up caffeine clearance for some people by affecting CYP1A2 activity.",
            sourceUrl = stringResource(R.string.onboarding_smoking_source_url),
        )
        InfoTextWithSource(
            text = "Heavy alcohol use can slow clearance and lower your effective bedtime threshold.",
            sourceUrl = stringResource(R.string.onboarding_alcohol_source_url),
        )
        InfoTextWithSource(
            text = "Frequent high intake may increase tolerance even when bedtime guidance should still stay conservative.",
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
    OnboardingScaffold(
        title = "Keep the estimate responsible",
        subtitle = "A little health context keeps the first-day profile from being overly optimistic.",
        currentStep = OnboardingDestination.Medical.stepNumber,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = true,
        onSkip = onSkip,
        continueLabel = "See my profile",
        continueEnabled = uiState.answers.isMedicalComplete(),
        onContinue = onContinue,
        disabledHint = "Pick liver context and any medicines that matter here.",
        enabledHint = "This only sets a safer starting profile.",
    ) {
        OnboardingSection(title = "Liver context") {
            GridSingleSelectButtonGroup(
                options = LiverDisease.entries,
                selectedOption = uiState.answers.liverDisease,
                labelFor = { liverDisease -> liverDisease.buttonLabel() },
                onOptionSelected = onLiverDiseaseChanged,
            )
        }

        OnboardingSection(
            title = "Medicines that may slow caffeine clearance",
            supportingText = "Pick any that apply. If none do, select None.",
        ) {
            GridMultiSelectButtonGroup(
                options = Medication.entries,
                selectedOptions = uiState.answers.medications,
                labelFor = { medication -> medication.buttonLabel() },
                onOptionToggled = onMedicationToggled,
            )
            InfoTextWithSource(
                text = "Some CYP1A2 inhibitors can make caffeine hang around much longer than usual.",
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
        title = "Profile ready",
        subtitle = "Here’s the starting profile Caffeine will use for your live curve and bedtime forecast.",
        currentStep = OnboardingDestination.ProfileReady.stepNumber,
        showBackButton = true,
        onBack = onBack,
        showSkipButton = false,
        continueLabel = "Review legal bits",
        continueEnabled = true,
        onContinue = onOpenLegalSheet,
        enabledHint = "You can fine-tune these values later in Settings.",
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
                    title = "Estimated half-life",
                    value = formatHalfLife(profile.halfLifeMinutes),
                    description = "How long it takes your body to clear half of the caffeine you consume.",
                    onInfoClick = { infoDialog = ProfileInfoDialog.HalfLife },
                )
                HorizontalDivider()
                ProfileMetricRow(
                    title = "Sleep threshold",
                    value = "${profile.sleepThresholdMg} mg",
                    description = "A conservative bedtime target for how much active caffeine may still be in your system.",
                    onInfoClick = { infoDialog = ProfileInfoDialog.SleepThreshold },
                )
                HorizontalDivider()
                ProfileMetricRow(
                    title = "Typical bedtime",
                    value = formatTimeOfDay(
                        hour = profile.sleepTimeHour,
                        minute = profile.sleepTimeMinute,
                        settings = displaySettings,
                    ),
                    description = "Used to project the sleep forecast card and chart markers each day.",
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
                    Text("Close")
                }
            },
            title = {
                Text(dialog.title)
            },
            text = {
                Text(dialog.body)
            },
        )
    }
}

internal enum class ProfileInfoDialog(
    val title: String,
    val body: String,
) {
    HalfLife(
        title = "Estimated half-life",
        body = "Half-life is the estimated time it takes your body to clear half of the caffeine still active in your system. A longer half-life means caffeine may linger later into the day.",
    ),
    SleepThreshold(
        title = "Sleep threshold",
        body = "This is a conservative bedtime target for how much active caffeine may still be in your system before sleep is more likely to be affected.",
    ),
    Bedtime(
        title = "Typical bedtime",
        body = "Bedtime powers the chart marker and the sleep forecast card so the app can estimate how much caffeine may still be active when you try to sleep.",
    ),
}

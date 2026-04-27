package com.uc.caffeine.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.data.AhrGenotype
import com.uc.caffeine.data.Cyp1a2Genotype
import com.uc.caffeine.data.HormonalStatus
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.onboarding.AgeBucket
import com.uc.caffeine.ui.onboarding.ConnectedChoiceButtonGroup
import com.uc.caffeine.ui.onboarding.GridMultiSelectButtonGroup
import com.uc.caffeine.ui.onboarding.GridSingleSelectButtonGroup
import com.uc.caffeine.ui.onboarding.LiverDisease
import com.uc.caffeine.ui.onboarding.Medication
import com.uc.caffeine.ui.onboarding.SleepTimePickerCard
import com.uc.caffeine.ui.onboarding.SmokingHabit
import com.uc.caffeine.ui.onboarding.WeightStepperCard
import com.uc.caffeine.ui.onboarding.WeightUnit
import com.uc.caffeine.ui.onboarding.buttonLabel
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.util.formatTimeOfDay
import java.time.LocalTime

@Composable
internal fun CaffeineProfileSettingsScreen(
    userSettings: UserSettings,
    viewModel: CaffeineViewModel,
    onBack: () -> Unit,
    onRedoOnboarding: () -> Unit,
) {
    val halfLifeHours = userSettings.halfLifeMinutes / 60
    val effectiveHalfLifeHours = userSettings.effectiveHalfLifeMinutes / 60
    val hasModifiers = userSettings.clearanceFactor != 1.0
    val bedtime = formatTimeOfDay(
        hour = userSettings.sleepTimeHour,
        minute = userSettings.sleepTimeMinute,
        settings = userSettings,
    )
    val factors = userSettings.profileFactors
    val currentAgeBucket = factors.ageBucket?.let {
        runCatching { AgeBucket.valueOf(it) }.getOrNull()
    }
    val currentWeightUnit = runCatching { WeightUnit.valueOf(factors.weightUnit) }
        .getOrDefault(WeightUnit.Kilograms)
    val currentSmokingHabit = factors.smokingHabit?.let {
        runCatching { SmokingHabit.valueOf(it) }.getOrNull()
    }
    val currentLiverDisease = factors.liverDisease?.let {
        runCatching { LiverDisease.valueOf(it) }.getOrNull()
    }
    val currentMedications = factors.medications.mapNotNull {
        runCatching { Medication.valueOf(it) }.getOrNull()
    }.toSet()

    SettingsPageScaffold(
        title = "Caffeine Profile",
        showBackButton = true,
        onBack = onBack,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileSnapshotMetric(
                        title = "Estimated half-life",
                        value = if (hasModifiers) "$effectiveHalfLifeHours hr (base $halfLifeHours hr)" else "$halfLifeHours hr",
                        description = if (hasModifiers) "Adjusted for your genetic and hormonal modifiers." else "How long caffeine tends to linger in your system.",
                    )
                    HorizontalDivider()
                    ProfileSnapshotMetric(
                        title = "Typical bedtime",
                        value = bedtime,
                        description = "Used for the bedtime marker and sleep prediction.",
                    )
                    HorizontalDivider()
                    ProfileSnapshotMetric(
                        title = "Sleep threshold",
                        value = "${userSettings.sleepThresholdMg} mg",
                        description = "The active-caffeine target the sleep guidance compares against.",
                    )
                }
            }

            ReAdjustHealthProfileCard(
                userSettings = userSettings,
                halfLifeHours = halfLifeHours,
                currentAgeBucket = currentAgeBucket,
                currentWeightUnit = currentWeightUnit,
                currentSmokingHabit = currentSmokingHabit,
                currentLiverDisease = currentLiverDisease,
                currentMedications = currentMedications,
                viewModel = viewModel,
            )

            AdvancedMetabolismCard(
                userSettings = userSettings,
                onCyp1a2Change = viewModel::updateCyp1a2Genotype,
                onAhrChange = viewModel::updateAhrGenotype,
                onHormonalStatusChange = viewModel::updateHormonalStatus,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Redo onboarding",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Walk through the profile setup again without deleting any logged drinks or history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onRedoOnboarding) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                        )
                        Text(
                            text = "Redo onboarding",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReAdjustHealthProfileCard(
    userSettings: UserSettings,
    halfLifeHours: Int,
    currentAgeBucket: AgeBucket?,
    currentWeightUnit: WeightUnit,
    currentSmokingHabit: SmokingHabit?,
    currentLiverDisease: LiverDisease?,
    currentMedications: Set<Medication>,
    viewModel: CaffeineViewModel,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    val factors = userSettings.profileFactors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.toggle()
                        expanded = !expanded
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Re-Adjust Caffeine Health Profile",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = "Fine-tune your half-life, sleep settings, and metabolism factors.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExpressiveStepperCard(
                        title = "Caffeine Half-Life",
                        value = "$halfLifeHours hr",
                        supportingText = "Average is around 5 hours, but you can make it more personal here.",
                        hint = "Adjust in 1-hour steps",
                        onDecrease = { viewModel.updateHalfLife((halfLifeHours - 1).coerceIn(2, 12)) },
                        onIncrease = { viewModel.updateHalfLife((halfLifeHours + 1).coerceIn(2, 12)) },
                        decreaseEnabled = halfLifeHours > 2,
                        increaseEnabled = halfLifeHours < 12,
                    )

                    SleepTimePickerCard(
                        displaySettings = userSettings,
                        selectedTime = LocalTime.of(
                            userSettings.effectiveSleepTimeHour,
                            userSettings.effectiveSleepTimeMinute,
                        ),
                        onSleepTimeChanged = { time ->
                            viewModel.updateSleepTime(time.hour, time.minute)
                        },
                        // Disabled only when HC has actual data to provide; if HC is
                        // connected but returned nothing, keep the picker editable as fallback.
                        enabled = !(userSettings.hcSleepEnabled && userSettings.hcSleepTimeHour != null),
                        hint = when {
                            userSettings.hcSleepEnabled && userSettings.hcSleepTimeHour != null ->
                                "Health Connect is providing this value"
                            userSettings.hcSleepEnabled ->
                                "No sleep data in Health Connect yet — your setting is used as fallback"
                            else -> null
                        },
                    )

                    ExpressiveStepperCard(
                        title = "Sleep Threshold",
                        value = "${userSettings.sleepThresholdMg} mg",
                        supportingText = "Keep this lower if caffeine affects your sleep easily, higher if your usual cutoff is more forgiving.",
                        hint = "Adjust in 5 mg steps",
                        onDecrease = { viewModel.updateSleepThreshold((userSettings.sleepThresholdMg - 5).coerceIn(20, 200)) },
                        onIncrease = { viewModel.updateSleepThreshold((userSettings.sleepThresholdMg + 5).coerceIn(20, 200)) },
                        decreaseEnabled = userSettings.sleepThresholdMg > 20,
                        increaseEnabled = userSettings.sleepThresholdMg < 200,
                    )

                    HorizontalDivider()

                    Text(
                        text = "Health Profile",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    SelectorField(
                        title = "Age range",
                        description = "Caffeine may linger longer for older adults.",
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = AgeBucket.entries,
                            selectedOption = currentAgeBucket,
                            labelFor = { it.label },
                            onOptionSelected = { viewModel.updateProfileAgeBucket(it) },
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Weight",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        WeightStepperCard(
                            weightValue = factors.weightValue,
                            weightUnit = currentWeightUnit,
                            onWeightUnitSelected = { viewModel.updateProfileWeightUnit(it) },
                            onIncrement = { viewModel.updateProfileWeight(factors.weightValue + 1) },
                            onDecrement = { viewModel.updateProfileWeight(factors.weightValue - 1) },
                            onWeightChanged = { viewModel.updateProfileWeight(it) },
                        )
                    }

                    SelectorField(
                        title = "Sleep sensitivity",
                        description = "If insomnia is part of the picture, the threshold stays more conservative.",
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = listOf(true, false),
                            selectedOption = factors.hasInsomnia,
                            labelFor = { if (it) "Yes" else "No" },
                            onOptionSelected = { viewModel.updateProfileInsomnia(it) },
                        )
                    }

                    SelectorField(
                        title = "Smoking habit",
                        description = "Smoking can speed up caffeine clearance by inducing CYP1A2.",
                    ) {
                        GridSingleSelectButtonGroup(
                            options = SmokingHabit.entries,
                            selectedOption = currentSmokingHabit,
                            labelFor = { it.buttonLabel() },
                            onOptionSelected = { viewModel.updateProfileSmokingHabit(it) },
                        )
                    }

                    SelectorField(
                        title = "Heavy alcohol use",
                        description = "Heavy alcohol use can slow clearance and lower your effective threshold.",
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = listOf(true, false),
                            selectedOption = factors.heavyAlcohol,
                            labelFor = { if (it) "Yes" else "No" },
                            onOptionSelected = { viewModel.updateProfileHeavyAlcohol(it) },
                        )
                    }

                    SelectorField(
                        title = "High daily caffeine",
                        description = "Frequent high intake may increase tolerance.",
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = listOf(true, false),
                            selectedOption = factors.heavyCaffeine,
                            labelFor = { if (it) "Yes" else "No" },
                            onOptionSelected = { viewModel.updateProfileHeavyCaffeine(it) },
                        )
                    }

                    SelectorField(
                        title = "Liver context",
                        description = "Liver disease can substantially slow caffeine clearance.",
                    ) {
                        GridSingleSelectButtonGroup(
                            options = LiverDisease.entries,
                            selectedOption = currentLiverDisease,
                            labelFor = { it.buttonLabel() },
                            onOptionSelected = { viewModel.updateProfileLiverDisease(it) },
                        )
                    }

                    SelectorField(
                        title = "Medications",
                        description = "Pick any that may slow caffeine clearance. Select None if none apply.",
                    ) {
                        GridMultiSelectButtonGroup(
                            options = Medication.entries,
                            selectedOptions = currentMedications,
                            labelFor = { it.buttonLabel() },
                            onOptionToggled = { viewModel.toggleProfileMedication(it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSnapshotMetric(
    title: String,
    value: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpressiveStepperCard(
    title: String,
    value: String,
    supportingText: String,
    hint: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
) {
    val haptics = rememberAppHaptics()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = {
                        haptics.toggle()
                        onDecrease()
                    },
                    enabled = decreaseEnabled,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease $title",
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    RollingNumberText(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        labelPrefix = "stepper_${title.lowercase().replace(' ', '_')}",
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        haptics.toggle()
                        onIncrease()
                    },
                    enabled = increaseEnabled,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase $title",
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorField(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun AdvancedMetabolismCard(
    userSettings: UserSettings,
    onCyp1a2Change: (Cyp1a2Genotype) -> Unit,
    onAhrChange: (AhrGenotype) -> Unit,
    onHormonalStatusChange: (HormonalStatus) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberAppHaptics()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.toggle()
                        expanded = !expanded
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Advanced Metabolism",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = "Optional genetic and hormonal factors that fine-tune your half-life estimate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "If you know your CYP1A2 or AHR genotype (e.g. from 23andMe or similar), you can enter it here. These are entirely optional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    SelectorField(
                        title = "CYP1A2 rs762551",
                        description = "Controls caffeine metabolism speed. A/A is the fast-metabolizer reference.",
                    ) {
                        GridSingleSelectButtonGroup(
                            options = Cyp1a2Genotype.entries,
                            selectedOption = userSettings.cyp1a2Genotype,
                            labelFor = { it.label },
                            onOptionSelected = onCyp1a2Change,
                        )
                    }

                    SelectorField(
                        title = "AHR rs2066853",
                        description = "Affects CYP1A2 expression level. G/G is normal.",
                    ) {
                        GridSingleSelectButtonGroup(
                            options = AhrGenotype.entries,
                            selectedOption = userSettings.ahrGenotype,
                            labelFor = { it.label },
                            onOptionSelected = onAhrChange,
                        )
                    }

                    SelectorField(
                        title = "Hormonal status",
                        description = "Pregnancy and oral contraceptives can substantially slow caffeine clearance.",
                    ) {
                        GridSingleSelectButtonGroup(
                            options = HormonalStatus.entries,
                            selectedOption = userSettings.hormonalStatus,
                            labelFor = { it.label },
                            onOptionSelected = onHormonalStatusChange,
                        )
                    }

                    if (userSettings.clearanceFactor != 1.0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Effective half-life: ${userSettings.effectiveHalfLifeMinutes / 60} hr ${userSettings.effectiveHalfLifeMinutes % 60} min",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    text = "Base ${userSettings.halfLifeMinutes / 60} hr adjusted by a combined clearance factor of ${"%.2f".format(userSettings.clearanceFactor)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.uc.caffeine.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
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
import com.uc.caffeine.ui.onboarding.buttonLabelRes
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.util.formatTimeOfDay
import java.time.LocalTime

@Composable
internal fun CaffeineProfileSettingsScreen(
    userSettings: UserSettings,
    viewModel: CaffeineViewModel,
    onBack: () -> Unit,
    onRedoOnboarding: () -> Unit,
    onWeeklySleepRotaClick: () -> Unit = {},
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
        title = stringResource(R.string.settings_caffeine_profile_title),
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
                        title = stringResource(R.string.profile_estimated_half_life),
                        value = if (hasModifiers) {
                            stringResource(R.string.profile_half_life_value_modified, effectiveHalfLifeHours, halfLifeHours)
                        } else {
                            stringResource(R.string.profile_half_life_value, halfLifeHours)
                        },
                        description = if (hasModifiers) {
                            stringResource(R.string.profile_half_life_description_modified)
                        } else {
                            stringResource(R.string.profile_half_life_description_default)
                        },
                    )
                    HorizontalDivider()
                    ProfileSnapshotMetric(
                        title = stringResource(R.string.profile_typical_bedtime),
                        value = bedtime,
                        description = stringResource(R.string.profile_typical_bedtime_description),
                    )
                    HorizontalDivider()
                    ProfileSnapshotMetric(
                        title = stringResource(R.string.profile_sleep_threshold),
                        value = stringResource(R.string.profile_sleep_threshold_value, userSettings.sleepThresholdMg),
                        description = stringResource(R.string.profile_sleep_threshold_description),
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

            WeeklySleepRotaNavCard(
                enabled = userSettings.weeklySleepRotaEnabled,
                customDayCount = userSettings.weeklySleepRota.size,
                onClick = onWeeklySleepRotaClick,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_redo_onboarding),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.profile_redo_onboarding_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onRedoOnboarding) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.profile_redo_onboarding),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySleepRotaNavCard(
    enabled: Boolean,
    customDayCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_weekly_sleep_rota_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (enabled && customDayCount > 0) {
                        stringResource(R.string.weekly_sleep_rota_nav_on)
                    } else {
                        stringResource(R.string.profile_weekly_sleep_rota_summary)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            text = stringResource(R.string.profile_re_adjust_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = stringResource(R.string.profile_re_adjust_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val yesLabel = stringResource(R.string.action_yes)
                val noLabel = stringResource(R.string.action_no)
                val ageBucketLabels = AgeBucket.entries.associateWith { stringResource(it.labelRes) }
                val smokingHabitLabels = SmokingHabit.entries.associateWith { stringResource(it.buttonLabelRes()) }
                val liverDiseaseLabels = LiverDisease.entries.associateWith { stringResource(it.buttonLabelRes()) }
                val medicationLabels = Medication.entries.associateWith { stringResource(it.buttonLabelRes()) }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExpressiveStepperCard(
                        title = stringResource(R.string.profile_caffeine_half_life),
                        value = stringResource(R.string.profile_half_life_value, halfLifeHours),
                        supportingText = stringResource(R.string.profile_caffeine_half_life_supporting),
                        hint = stringResource(R.string.profile_caffeine_half_life_hint),
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
                                stringResource(R.string.health_connect_sleep_hint_active)
                            userSettings.hcSleepEnabled ->
                                stringResource(R.string.health_connect_sleep_hint_no_data)
                            else -> null
                        },
                    )

                    ExpressiveStepperCard(
                        title = stringResource(R.string.profile_sleep_threshold),
                        value = stringResource(R.string.profile_sleep_threshold_value, userSettings.sleepThresholdMg),
                        supportingText = stringResource(R.string.profile_sleep_threshold_supporting),
                        hint = stringResource(R.string.profile_sleep_threshold_hint),
                        onDecrease = { viewModel.updateSleepThreshold((userSettings.sleepThresholdMg - 5).coerceIn(20, 200)) },
                        onIncrease = { viewModel.updateSleepThreshold((userSettings.sleepThresholdMg + 5).coerceIn(20, 200)) },
                        decreaseEnabled = userSettings.sleepThresholdMg > 20,
                        increaseEnabled = userSettings.sleepThresholdMg < 200,
                    )

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.profile_health_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    SelectorField(
                        title = stringResource(R.string.profile_age_range),
                        description = stringResource(R.string.profile_age_range_description),
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = AgeBucket.entries,
                            selectedOption = currentAgeBucket,
                            labelFor = { ageBucketLabels[it] ?: "" },
                            onOptionSelected = { viewModel.updateProfileAgeBucket(it) },
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.profile_weight),
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
                        title = stringResource(R.string.profile_sleep_sensitivity),
                        description = stringResource(R.string.profile_sleep_sensitivity_description),
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = listOf(true, false),
                            selectedOption = factors.hasInsomnia,
                            labelFor = { if (it) yesLabel else noLabel },
                            onOptionSelected = { viewModel.updateProfileInsomnia(it) },
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_smoking_habit),
                        description = stringResource(R.string.profile_smoking_habit_description),
                    ) {
                        GridSingleSelectButtonGroup(
                            options = SmokingHabit.entries,
                            selectedOption = currentSmokingHabit,
                            labelFor = { smokingHabitLabels[it] ?: "" },
                            onOptionSelected = { viewModel.updateProfileSmokingHabit(it) },
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_heavy_alcohol),
                        description = stringResource(R.string.profile_heavy_alcohol_description),
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = listOf(true, false),
                            selectedOption = factors.heavyAlcohol,
                            labelFor = { if (it) yesLabel else noLabel },
                            onOptionSelected = { viewModel.updateProfileHeavyAlcohol(it) },
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_high_caffeine),
                        description = stringResource(R.string.profile_high_caffeine_description),
                    ) {
                        ConnectedChoiceButtonGroup(
                            options = listOf(true, false),
                            selectedOption = factors.heavyCaffeine,
                            labelFor = { if (it) yesLabel else noLabel },
                            onOptionSelected = { viewModel.updateProfileHeavyCaffeine(it) },
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_liver_context),
                        description = stringResource(R.string.profile_liver_context_description),
                    ) {
                        GridSingleSelectButtonGroup(
                            options = LiverDisease.entries,
                            selectedOption = currentLiverDisease,
                            labelFor = { liverDiseaseLabels[it] ?: "" },
                            onOptionSelected = { viewModel.updateProfileLiverDisease(it) },
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_medications),
                        description = stringResource(R.string.profile_medications_description),
                    ) {
                        GridMultiSelectButtonGroup(
                            options = Medication.entries,
                            selectedOptions = currentMedications,
                            labelFor = { medicationLabels[it] ?: "" },
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
                        contentDescription = stringResource(R.string.stepper_decrease_cd, title),
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
                        contentDescription = stringResource(R.string.stepper_increase_cd, title),
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            text = stringResource(R.string.profile_advanced_metabolism),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = stringResource(R.string.profile_advanced_metabolism_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.profile_advanced_metabolism_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    SelectorField(
                        title = stringResource(R.string.profile_cyp1a2),
                        description = stringResource(R.string.profile_cyp1a2_description),
                    ) {
                        GridSingleSelectButtonGroup(
                            options = Cyp1a2Genotype.entries,
                            selectedOption = userSettings.cyp1a2Genotype,
                            labelFor = { it.label },
                            onOptionSelected = onCyp1a2Change,
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_ahr),
                        description = stringResource(R.string.profile_ahr_description),
                    ) {
                        GridSingleSelectButtonGroup(
                            options = AhrGenotype.entries,
                            selectedOption = userSettings.ahrGenotype,
                            labelFor = { it.label },
                            onOptionSelected = onAhrChange,
                        )
                    }

                    SelectorField(
                        title = stringResource(R.string.profile_hormonal_status),
                        description = stringResource(R.string.profile_hormonal_status_description),
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
                                    text = stringResource(
                                        R.string.profile_effective_half_life,
                                        userSettings.effectiveHalfLifeMinutes / 60,
                                        userSettings.effectiveHalfLifeMinutes % 60,
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.profile_clearance_factor_explanation,
                                        userSettings.halfLifeMinutes / 60,
                                        "%.2f".format(userSettings.clearanceFactor),
                                    ),
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

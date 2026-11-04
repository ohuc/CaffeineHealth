package com.uc.caffeine.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.AhrGenotype
import com.uc.caffeine.data.Cyp1a2Genotype
import com.uc.caffeine.data.HormonalStatus
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.ExpressiveIntSlider
import com.uc.caffeine.ui.components.SettingsActionRow
import com.uc.caffeine.ui.components.SettingsExpandableGroup
import com.uc.caffeine.ui.components.settingsCollapseExit
import com.uc.caffeine.ui.components.settingsExpandEnter
import com.uc.caffeine.ui.components.SettingsField
import com.uc.caffeine.ui.components.SettingsNavGroup
import com.uc.caffeine.ui.components.SettingsNavItem
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.SettingsSwitchRow
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.onboarding.AgeBucket
import com.uc.caffeine.ui.onboarding.ConnectedChoiceButtonGroup
import com.uc.caffeine.ui.onboarding.GridMultiSelectButtonGroup
import com.uc.caffeine.ui.onboarding.GridSingleSelectButtonGroup
import com.uc.caffeine.ui.onboarding.LiverDisease
import com.uc.caffeine.ui.onboarding.Medication
import com.uc.caffeine.ui.onboarding.MedicationGroupLabel
import com.uc.caffeine.ui.onboarding.YesNoChoiceGroup
import com.uc.caffeine.ui.onboarding.SleepTimePickerCard
import com.uc.caffeine.ui.onboarding.SmokingHabit
import com.uc.caffeine.ui.onboarding.WeightStepperCard
import com.uc.caffeine.ui.onboarding.WeightUnit
import com.uc.caffeine.ui.onboarding.buttonLabelRes
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.util.formatTimeOfDay
import com.uc.caffeine.util.CaffeineTolerance
import com.uc.caffeine.monetization.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime

@Composable
internal fun CaffeineProfileSettingsScreen(
    userSettings: UserSettings,
    viewModel: CaffeineViewModel,
    onBack: () -> Unit,
    onRedoOnboarding: () -> Unit,
    onWeeklySleepRotaClick: () -> Unit = {},
    onTimeToFinishClick: () -> Unit = {},
) {
    val coachLocked = isPlusFeatureLocked(ProFeature.Coach)
    val openPlus = LocalOpenPlus.current
    val requestPlus = LocalRequestPlusFeature.current
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
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

            Column {
                SettingsSectionLabel(stringResource(R.string.profile_group_health))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }

            Column {
                SettingsSectionLabel(stringResource(R.string.profile_group_schedules))
                SettingsNavGroup(
                    items = listOf(
                        SettingsNavItem(
                            title = stringResource(R.string.profile_weekly_sleep_rota_title),
                            summary = if (userSettings.weeklySleepRotaEnabled && userSettings.weeklySleepRota.isNotEmpty()) {
                                stringResource(R.string.weekly_sleep_rota_nav_on)
                            } else {
                                stringResource(R.string.profile_weekly_sleep_rota_summary)
                            },
                            icon = Icons.Rounded.CalendarMonth,
                            onClick = onWeeklySleepRotaClick,
                        ),
                        SettingsNavItem(
                            title = stringResource(R.string.settings_duration_title),
                            summary = stringResource(R.string.settings_duration_summary),
                            icon = Icons.Rounded.Timer,
                            onClick = onTimeToFinishClick,
                        ),
                    ),
                )
            }

            if (!coachLocked) {
                // No parent spacing here: the tolerance group animates, and its gap lives inside it.
                Column {
                    SettingsSectionLabel(stringResource(R.string.profile_group_coach))
                    CaffeineCoachToggleCard(
                        enabled = userSettings.caffeineCoachEnabled,
                        onToggle = viewModel::updateCaffeineCoachEnabled,
                    )
                    AnimatedVisibility(
                        visible = userSettings.caffeineCoachEnabled,
                        enter = settingsExpandEnter(),
                        exit = settingsCollapseExit(),
                    ) {
                        val toleranceMgPerDay by viewModel.coachToleranceMgPerDay.collectAsStateWithLifecycle()
                        Box(Modifier.padding(top = 8.dp)) {
                            CoachToleranceCard(
                                enabled = userSettings.coachToleranceEnabled,
                                seedMg = userSettings.coachToleranceSeedMg,
                                toleranceMgPerDay = toleranceMgPerDay,
                                onToggle = viewModel::updateCoachToleranceEnabled,
                                onSeedChange = viewModel::updateCoachToleranceSeedMg,
                            )
                        }
                    }
                }
            }

            Column {
                SettingsSectionLabel(stringResource(R.string.profile_group_setup))
                SettingsActionRow(
                    icon = Icons.Rounded.Refresh,
                    title = stringResource(R.string.profile_redo_onboarding),
                    summary = stringResource(R.string.profile_redo_onboarding_description),
                    onClick = onRedoOnboarding,
                )
            }

            if (coachLocked) {
                PlusDivider(stringResource(R.string.plus_divider_label), openPlus)
                LockedCoachSettings(userSettings,
                    onDisable = { viewModel.updateCaffeineCoachEnabled(false) },
                    onDisableTolerance = { viewModel.updateCoachToleranceEnabled(false) },
                    onExplain = { requestPlus(ProFeature.Coach) })
            }
        }
    }
}

@Composable
internal fun LockedCoachSettings(
    userSettings: UserSettings,
    onDisable: () -> Unit,
    onDisableTolerance: () -> Unit,
    onExplain: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CaffeineCoachToggleCard(
            enabled = userSettings.caffeineCoachEnabled,
            onToggle = { onDisable() },
            locked = true,
            onExplain = onExplain,
        )
        if (userSettings.caffeineCoachEnabled) CoachToleranceCard(
            enabled = userSettings.coachToleranceEnabled,
            seedMg = userSettings.coachToleranceSeedMg,
            toleranceMgPerDay = null,
            onToggle = { onDisableTolerance() },
            onSeedChange = { onExplain() },
            locked = true,
            onExplain = onExplain,
        )
    }
}

/** Coach tolerance (#59): opt-in, with the habitual intake the baseline starts from. */
@Composable
private fun CoachToleranceCard(
    enabled: Boolean,
    seedMg: Int,
    toleranceMgPerDay: Int?,
    onToggle: (Boolean) -> Unit,
    onSeedChange: (Int) -> Unit,
    locked: Boolean = false,
    onExplain: () -> Unit = {},
) {
    val haptics = rememberAppHaptics()
    val seedSteps = 0..(CaffeineTolerance.MAX_SEED_MG_PER_DAY / CaffeineTolerance.SEED_STEP_MG)
    // Saved when the drag ends, not on every step.
    var seedStep by rememberSaveable(seedMg) { mutableIntStateOf(seedMg / CaffeineTolerance.SEED_STEP_MG) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = CaffeineSurfaceDefaults.groupedListContainerColor,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .plusLockedSemantics(locked, enabled,
                        if (locked && enabled) stringResource(R.string.plus_turn_off_saved, stringResource(R.string.coach_tolerance_label))
                        else stringResource(R.string.plus_locked_click_label))
                    .clickable {
                        haptics.toggle()
                        if (locked && !enabled) onExplain() else onToggle(!enabled)
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .plusLockedContent(locked),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.coach_tolerance_label),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                    Text(
                        text = stringResource(R.string.coach_tolerance_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (locked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    onCheckedChange = if (locked) null else { checked ->
                        haptics.toggle()
                        onToggle(checked)
                    },
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = settingsExpandEnter(),
                exit = settingsCollapseExit(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).plusLockedContent(locked),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.coach_tolerance_seed_label),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(
                                R.string.coach_tolerance_mg_per_day,
                                seedStep * CaffeineTolerance.SEED_STEP_MG,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Box(Modifier.plusLockedSemantics(locked).then(if (locked) Modifier.clickable {
                        haptics.toggle(); onExplain()
                    } else Modifier)) {
                        ExpressiveIntSlider(
                            value = seedStep,
                            onValueChange = { seedStep = it },
                            valueRange = seedSteps,
                            onValueChangeFinished = { onSeedChange(seedStep * CaffeineTolerance.SEED_STEP_MG) },
                            modifier = if (locked) Modifier.clearAndSetSemantics {}.focusProperties { canFocus = false } else Modifier,
                        )
                        if (locked) Box(Modifier.matchParentSize().clearAndSetSemantics {}.clickable {
                            haptics.toggle(); onExplain()
                        })
                    }
                    Text(
                        text = stringResource(R.string.coach_tolerance_seed_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (locked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (toleranceMgPerDay != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileSnapshotMetric(
                            title = stringResource(R.string.coach_tolerance_current),
                            value = stringResource(R.string.coach_tolerance_mg_per_day, toleranceMgPerDay),
                            description = stringResource(R.string.coach_tolerance_current_description),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaffeineCoachToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    locked: Boolean = false,
    onExplain: () -> Unit = {},
) {
    SettingsSwitchRow(
        title = stringResource(R.string.coach_setting_label),
        summary = stringResource(R.string.coach_setting_description),
        checked = enabled,
        onCheckedChange = { if (locked && !enabled) onExplain() else onToggle(it) },
        icon = Icons.Filled.Bolt,
        locked = locked,
        modifier = Modifier.plusLockedSemantics(locked, enabled,
            if (locked && enabled) stringResource(R.string.plus_turn_off_saved, stringResource(R.string.coach_setting_label))
            else stringResource(R.string.plus_locked_click_label)),
    )
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
    val factors = userSettings.profileFactors

    SettingsExpandableGroup(
        icon = Icons.Filled.Tune,
        title = stringResource(R.string.profile_re_adjust_title),
        summary = stringResource(R.string.profile_re_adjust_description),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        val ageBucketLabels = AgeBucket.entries.associateWith { stringResource(it.labelRes) }
        val smokingHabitLabels = SmokingHabit.entries.associateWith { stringResource(it.buttonLabelRes()) }
        val liverDiseaseLabels = LiverDisease.entries.associateWith { stringResource(it.buttonLabelRes()) }
        val medicationLabels = Medication.entries.associateWith { stringResource(it.buttonLabelRes()) }

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
                userSettings.weeklySleepRotaEnabled ->
                    stringResource(R.string.profile_sleep_time_rota_hint)
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
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )

        SettingsField(
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

        SettingsField(title = stringResource(R.string.profile_weight), description = null) {
            WeightStepperCard(
                weightValue = factors.weightValue,
                weightUnit = currentWeightUnit,
                onWeightUnitSelected = { viewModel.updateProfileWeightUnit(it) },
                onIncrement = { viewModel.updateProfileWeight(factors.weightValue + 1) },
                onDecrement = { viewModel.updateProfileWeight(factors.weightValue - 1) },
                onWeightChanged = { viewModel.updateProfileWeight(it) },
            )
        }

        SettingsField(
            title = stringResource(R.string.profile_sleep_sensitivity),
            description = stringResource(R.string.profile_sleep_sensitivity_description),
        ) {
            YesNoChoiceGroup(
                selected = factors.hasInsomnia,
                onSelected = { viewModel.updateProfileInsomnia(it) },
            )
        }

        SettingsField(
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

        SettingsField(
            title = stringResource(R.string.profile_heavy_alcohol),
            description = stringResource(R.string.profile_heavy_alcohol_description),
        ) {
            YesNoChoiceGroup(
                selected = factors.heavyAlcohol,
                onSelected = { viewModel.updateProfileHeavyAlcohol(it) },
            )
        }

        SettingsField(
            title = stringResource(R.string.profile_high_caffeine),
            description = stringResource(R.string.profile_high_caffeine_description),
        ) {
            YesNoChoiceGroup(
                selected = factors.heavyCaffeine,
                onSelected = { viewModel.updateProfileHeavyCaffeine(it) },
            )
        }

        SettingsField(
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

        SettingsField(
            title = stringResource(R.string.profile_medications),
            description = stringResource(R.string.profile_medications_description),
        ) {
            GridMultiSelectButtonGroup(
                options = Medication.noneAndInhibitors,
                selectedOptions = currentMedications,
                labelFor = { medicationLabels[it] ?: "" },
                onOptionToggled = { viewModel.toggleProfileMedication(it) },
            )
            MedicationGroupLabel(text = stringResource(R.string.medication_group_inducers))
            GridMultiSelectButtonGroup(
                options = Medication.inducers,
                selectedOptions = currentMedications,
                labelFor = { medicationLabels[it] ?: "" },
                onOptionToggled = { viewModel.toggleProfileMedication(it) },
            )
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
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
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
        shape = MaterialTheme.shapes.large,
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
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
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
                    shapes = IconButtonDefaults.shapes(),
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
                        style = MaterialTheme.typography.bodyMedium,
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
                    shapes = IconButtonDefaults.shapes(),
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
private fun AdvancedMetabolismCard(
    userSettings: UserSettings,
    onCyp1a2Change: (Cyp1a2Genotype) -> Unit,
    onAhrChange: (AhrGenotype) -> Unit,
    onHormonalStatusChange: (HormonalStatus) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    SettingsExpandableGroup(
        icon = Icons.Filled.Science,
        title = stringResource(R.string.profile_advanced_metabolism),
        summary = stringResource(R.string.profile_advanced_metabolism_description),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        Text(
            text = stringResource(R.string.profile_advanced_metabolism_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsField(
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

        SettingsField(
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

        SettingsField(
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
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.profile_effective_half_life,
                            userSettings.effectiveHalfLifeMinutes / 60,
                            userSettings.effectiveHalfLifeMinutes % 60,
                        ),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_clearance_factor_explanation,
                            userSettings.halfLifeMinutes / 60,
                            "%.2f".format(userSettings.clearanceFactor),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

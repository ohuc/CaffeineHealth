package com.uc.caffeine.ui.onboarding

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.uc.caffeine.R
import com.uc.caffeine.data.DerivedOnboardingProfile
import java.time.LocalTime
import kotlin.math.roundToInt

enum class OnboardingDestination(
    val stepNumber: Int,
) : NavKey {
    Intro(0),
    BasicInfo(1),
    Sleep(2),
    Lifestyle(3),
    Medical(4),
    ProfileReady(5),
}

enum class AgeBucket(@StringRes val labelRes: Int) {
    Under65(R.string.age_under_65),
    Over65(R.string.age_65_or_older),
}

enum class WeightUnit(@StringRes val labelRes: Int) {
    Kilograms(R.string.weight_unit_kg),
    Pounds(R.string.weight_unit_lb),
    ;

    fun clamp(value: Int): Int = value.coerceIn(minSelectable(), maxSelectable())

    fun convertFrom(value: Int, from: WeightUnit): Int {
        if (this == from) return clamp(value)
        return when (this) {
            Kilograms -> (value / PoundsPerKilogram).roundToInt()
            Pounds -> (value * PoundsPerKilogram).roundToInt()
        }.let(::clamp)
    }

    fun minSelectable(): Int {
        return when (this) {
            Kilograms -> 30
            Pounds -> (30 * PoundsPerKilogram).roundToInt()
        }
    }

    fun maxSelectable(): Int {
        return when (this) {
            Kilograms -> 220
            Pounds -> (220 * PoundsPerKilogram).roundToInt()
        }
    }

    companion object {
        private const val PoundsPerKilogram = 2.2046226218
    }
}

enum class SmokingHabit(@StringRes val labelRes: Int) {
    None(R.string.smoking_none),
    Occasional(R.string.smoking_occasional),
    Daily(R.string.smoking_daily),
    Heavy(R.string.smoking_heavy),
}

enum class LiverDisease(@StringRes val labelRes: Int) {
    None(R.string.liver_no_disease),
    Compensated(R.string.liver_compensated),
    Decompensated(R.string.liver_decompensated),
}

enum class Medication(
    @StringRes val labelRes: Int,
    val halfLifeDeltaMinutes: Int,
) {
    None(R.string.medication_none, 0),
    Cimetidine(R.string.medication_cimetidine, 45),
    OralContraceptives(R.string.medication_oral_contraceptives, 0),
    Ciprofloxacin(R.string.medication_ciprofloxacin, 120),
    Fluvoxamine(R.string.medication_fluvoxamine, 180),
    OtherCyp1A2Inhibitor(R.string.medication_other_cyp1a2, 60),
}

data class OnboardingAnswers(
    val ageBucket: AgeBucket? = null,
    val weightValue: Int = 60,
    val weightUnit: WeightUnit = WeightUnit.Kilograms,
    val sleepTime: LocalTime = LocalTime.of(23, 0),
    val hasInsomnia: Boolean? = null,
    val smokingHabit: SmokingHabit? = null,
    val heavyAlcohol: Boolean? = null,
    val heavyCaffeine: Boolean? = null,
    val liverDisease: LiverDisease? = null,
    val medications: Set<Medication> = emptySet(),
) {
    fun weightKg(): Double {
        val weight = weightUnit.clamp(weightValue).toDouble()
        return when (weightUnit) {
            WeightUnit.Kilograms -> weight
            WeightUnit.Pounds -> weight / 2.2046226218
        }
    }

    fun isBasicInfoComplete(): Boolean = ageBucket != null

    fun isSleepComplete(): Boolean = hasInsomnia != null

    fun isLifestyleComplete(): Boolean {
        return smokingHabit != null && heavyAlcohol != null && heavyCaffeine != null
    }

    fun isMedicalComplete(): Boolean = liverDisease != null && medications.isNotEmpty()

    fun isProfileReady(): Boolean {
        return isBasicInfoComplete() && isSleepComplete() && isLifestyleComplete() && isMedicalComplete()
    }

    fun adjustedWeight(delta: Int): OnboardingAnswers {
        return copy(weightValue = weightUnit.clamp(weightValue + delta))
    }

    fun toggleMedication(medication: Medication): OnboardingAnswers {
        val nextMedications = medications.toMutableSet()

        when (medication) {
            Medication.None -> {
                if (medications == setOf(Medication.None)) {
                    nextMedications.clear()
                } else {
                    nextMedications.clear()
                    nextMedications.add(Medication.None)
                }
            }

            else -> {
                nextMedications.remove(Medication.None)
                if (nextMedications.contains(medication)) {
                    nextMedications.remove(medication)
                } else {
                    nextMedications.add(medication)
                }
            }
        }

        return copy(medications = nextMedications)
    }
}

data class OnboardingUiState(
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val useDefaultProfile: Boolean = false,
    val showLegalSheet: Boolean = false,
    val legalAcknowledged: Boolean = false,
    val isSaving: Boolean = false,
    val isCelebrating: Boolean = false,
) {
    fun currentProfile(): DerivedOnboardingProfile? {
        return when {
            useDefaultProfile -> OnboardingProfileCalculator.defaultProfile
            answers.isProfileReady() -> OnboardingProfileCalculator.calculateProfile(answers)
            else -> null
        }
    }

    fun canContinue(destination: OnboardingDestination): Boolean {
        return when (destination) {
            OnboardingDestination.Intro -> true
            OnboardingDestination.BasicInfo -> answers.isBasicInfoComplete()
            OnboardingDestination.Sleep -> answers.isSleepComplete()
            OnboardingDestination.Lifestyle -> answers.isLifestyleComplete()
            OnboardingDestination.Medical -> answers.isMedicalComplete()
            OnboardingDestination.ProfileReady -> currentProfile() != null && legalAcknowledged && !isSaving
        }
    }
}

@StringRes
internal fun SmokingHabit.buttonLabelRes(): Int = when (this) {
    SmokingHabit.Occasional -> R.string.smoking_some_days
    else -> labelRes
}

@StringRes
internal fun LiverDisease.buttonLabelRes(): Int = labelRes

@StringRes
internal fun Medication.buttonLabelRes(): Int = labelRes

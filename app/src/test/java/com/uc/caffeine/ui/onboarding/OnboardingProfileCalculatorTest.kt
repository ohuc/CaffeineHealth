package com.uc.caffeine.ui.onboarding

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingProfileCalculatorTest {

    @Test
    fun calculateProfile_usesMetricWeightAndLifestyleAdjustments() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            OnboardingAnswers(
                ageBucket = AgeBucket.Under65,
                weightValue = 80,
                weightUnit = WeightUnit.Kilograms,
                sleepTime = LocalTime.of(22, 45),
                hasInsomnia = false,
                smokingHabit = SmokingHabit.Daily,
                heavyAlcohol = true,
                heavyCaffeine = true,
                liverDisease = LiverDisease.None,
                medications = setOf(Medication.None),
            ),
        )

        assertEquals(285, profile.halfLifeMinutes)
        assertEquals(83, profile.sleepThresholdMg)
        assertEquals(22, profile.sleepTimeHour)
        assertEquals(45, profile.sleepTimeMinute)
    }

    @Test
    fun calculateProfile_convertsImperialWeightAndAppliesAgeAndSleepSensitivity() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            OnboardingAnswers(
                ageBucket = AgeBucket.Over65,
                weightValue = 220,
                weightUnit = WeightUnit.Pounds,
                sleepTime = LocalTime.of(23, 0),
                hasInsomnia = true,
                smokingHabit = SmokingHabit.None,
                heavyAlcohol = false,
                heavyCaffeine = false,
                liverDisease = LiverDisease.None,
                medications = setOf(Medication.None),
            ),
        )

        assertEquals(360, profile.halfLifeMinutes)
        assertEquals(55, profile.sleepThresholdMg)
    }

    @Test
    fun calculateProfile_usesLargestMedicationAdjustmentOnly() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            OnboardingAnswers(
                ageBucket = AgeBucket.Under65,
                weightValue = 70,
                weightUnit = WeightUnit.Kilograms,
                sleepTime = LocalTime.of(23, 0),
                hasInsomnia = false,
                smokingHabit = SmokingHabit.None,
                heavyAlcohol = false,
                heavyCaffeine = false,
                liverDisease = LiverDisease.None,
                medications = setOf(
                    Medication.Cimetidine,
                    Medication.Fluvoxamine,
                    Medication.OtherCyp1A2Inhibitor,
                ),
            ),
        )

        assertEquals(480, profile.halfLifeMinutes)
        assertEquals(60, profile.sleepThresholdMg)
    }

    @Test
    fun calculateProfile_clampsHalfLifeAndThresholdToSafeBounds() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            OnboardingAnswers(
                ageBucket = AgeBucket.Over65,
                weightValue = 40,
                weightUnit = WeightUnit.Kilograms,
                sleepTime = LocalTime.of(21, 30),
                hasInsomnia = true,
                smokingHabit = SmokingHabit.Heavy,
                heavyAlcohol = true,
                heavyCaffeine = false,
                liverDisease = LiverDisease.Decompensated,
                medications = setOf(Medication.Fluvoxamine),
            ),
        )

        assertEquals(600, profile.halfLifeMinutes)
        assertEquals(20, profile.sleepThresholdMg)
    }

    @Test
    fun calculateProfile_usesDefaultBedtimeWhenUserKeepsPrefilledSleepTime() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            OnboardingAnswers(
                ageBucket = AgeBucket.Under65,
                weightValue = 60,
                hasInsomnia = false,
                smokingHabit = SmokingHabit.None,
                heavyAlcohol = false,
                heavyCaffeine = false,
                liverDisease = LiverDisease.None,
                medications = setOf(Medication.None),
            ),
        )

        assertEquals(23, profile.sleepTimeHour)
        assertEquals(0, profile.sleepTimeMinute)
    }

    @Test
    fun calculateProfile_inducerShortensHalfLife() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            baselineAnswers(medications = setOf(Medication.Rifampicin)),
        )

        assertEquals(240, profile.halfLifeMinutes)
    }

    @Test
    fun calculateProfile_usesLargestInducerAdjustmentOnly() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            baselineAnswers(
                medications = setOf(
                    Medication.Phenobarbital,
                    Medication.Carbamazepine,
                    Medication.OtherCyp1A2Inducer,
                ),
            ),
        )

        assertEquals(180, profile.halfLifeMinutes)
    }

    @Test
    fun calculateProfile_combinesStrongestInhibitorAndStrongestInducer() {
        val profile = OnboardingProfileCalculator.calculateProfile(
            baselineAnswers(
                medications = setOf(
                    Medication.Fluvoxamine,
                    Medication.Cimetidine,
                    Medication.Rifampicin,
                ),
            ),
        )

        // 300 base + 180 (fluvoxamine) − 60 (rifampicin)
        assertEquals(420, profile.halfLifeMinutes)
    }

    @Test
    fun medicationEffects_signMatchesDirection() {
        Medication.entries.forEach { medication ->
            when (medication.effect) {
                MedicationEffect.None -> assertEquals(0, medication.halfLifeDeltaMinutes)
                MedicationEffect.Inhibitor -> assertTrue(medication.halfLifeDeltaMinutes >= 0)
                MedicationEffect.Inducer -> assertTrue(medication.halfLifeDeltaMinutes < 0)
            }
        }
    }

    private fun baselineAnswers(medications: Set<Medication>) = OnboardingAnswers(
        ageBucket = AgeBucket.Under65,
        weightValue = 70,
        weightUnit = WeightUnit.Kilograms,
        sleepTime = LocalTime.of(23, 0),
        hasInsomnia = false,
        smokingHabit = SmokingHabit.None,
        heavyAlcohol = false,
        heavyCaffeine = false,
        liverDisease = LiverDisease.None,
        medications = medications,
    )
}

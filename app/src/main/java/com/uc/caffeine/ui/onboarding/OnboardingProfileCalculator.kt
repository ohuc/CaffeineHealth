package com.uc.caffeine.ui.onboarding

import com.uc.caffeine.data.DerivedOnboardingProfile
import java.time.LocalTime
import kotlin.math.roundToInt

object OnboardingProfileCalculator {
    val defaultProfile = DerivedOnboardingProfile(
        halfLifeMinutes = 300,
        sleepThresholdMg = 60,
        sleepTimeHour = 23,
        sleepTimeMinute = 0,
    )

    fun calculateProfile(answers: OnboardingAnswers): DerivedOnboardingProfile {
        require(answers.isProfileReady()) { "Onboarding answers must be complete before calculating a profile." }

        var halfLifeMinutes = 300
        var sleepThresholdMg = (answers.weightKg().times(0.85)).roundToInt().coerceIn(40, 100)

        if (answers.ageBucket == AgeBucket.Over65) {
            halfLifeMinutes += 60
            sleepThresholdMg -= 10
        }

        if (answers.hasInsomnia == true) {
            sleepThresholdMg -= 20
        }

        when (answers.smokingHabit) {
            SmokingHabit.None -> Unit
            SmokingHabit.Occasional -> halfLifeMinutes -= 30
            SmokingHabit.Daily -> {
                halfLifeMinutes -= 60
                sleepThresholdMg += 10
            }

            SmokingHabit.Heavy -> {
                halfLifeMinutes -= 90
                sleepThresholdMg += 15
            }

            null -> Unit
        }

        if (answers.heavyAlcohol == true) {
            halfLifeMinutes += 45
            sleepThresholdMg -= 10
        }

        if (answers.heavyCaffeine == true) {
            sleepThresholdMg += 15
        }

        when (answers.liverDisease) {
            LiverDisease.None -> Unit
            LiverDisease.Compensated -> {
                halfLifeMinutes += 90
                sleepThresholdMg -= 15
            }

            LiverDisease.Decompensated -> {
                halfLifeMinutes += 180
                sleepThresholdMg -= 25
            }

            null -> Unit
        }

        // Overlapping drugs on the same side don't stack linearly, so only the strongest
        // inhibitor and the strongest inducer count; opposing effects partly cancel.
        halfLifeMinutes += answers.medications
            .filter { it.effect == MedicationEffect.Inhibitor }
            .maxOfOrNull { it.halfLifeDeltaMinutes } ?: 0
        halfLifeMinutes += answers.medications
            .filter { it.effect == MedicationEffect.Inducer }
            .minOfOrNull { it.halfLifeDeltaMinutes } ?: 0

        val sleepTime = answers.sleepTime

        return DerivedOnboardingProfile(
            halfLifeMinutes = halfLifeMinutes.coerceIn(180, 600),
            sleepThresholdMg = sleepThresholdMg.coerceIn(20, 140),
            sleepTimeHour = sleepTime.hour,
            sleepTimeMinute = sleepTime.minute,
        )
    }
}

package com.uc.caffeine.ui.onboarding

import com.uc.caffeine.ui.onboarding.OnboardingDestination.BasicInfo
import com.uc.caffeine.ui.onboarding.OnboardingDestination.Intro
import com.uc.caffeine.ui.onboarding.OnboardingDestination.Lifestyle
import com.uc.caffeine.ui.onboarding.OnboardingDestination.Medical
import com.uc.caffeine.ui.onboarding.OnboardingDestination.Notifications
import com.uc.caffeine.ui.onboarding.OnboardingDestination.PlusIntro
import com.uc.caffeine.ui.onboarding.OnboardingDestination.ProfileReady
import com.uc.caffeine.ui.onboarding.OnboardingDestination.Sleep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingSequenceTest {

    @Test
    fun fossFirstRun_hasSixStepsAfterIntro() {
        val sequence = onboardingSequence(plusIntro = false, remindersAnswered = false, redo = false)
        assertEquals(listOf(Intro, BasicInfo, Sleep, Lifestyle, Medical, Notifications, ProfileReady), sequence.steps)
        assertEquals(6, sequence.progressTotal)
    }

    @Test
    fun playFirstRun_insertsPlusBetweenNotificationsAndProfile() {
        val sequence = onboardingSequence(plusIntro = true, remindersAnswered = false, redo = false)
        assertEquals(
            listOf(Intro, BasicInfo, Sleep, Lifestyle, Medical, Notifications, PlusIntro, ProfileReady),
            sequence.steps,
        )
        assertEquals(7, sequence.progressTotal)
    }

    @Test
    fun redo_neverShowsPlusAndSkipsAnsweredReminders() {
        val sequence = onboardingSequence(plusIntro = true, remindersAnswered = true, redo = true)
        assertEquals(listOf(Intro, BasicInfo, Sleep, Lifestyle, Medical, ProfileReady), sequence.steps)
        assertFalse(PlusIntro in onboardingSequence(plusIntro = true, remindersAnswered = false, redo = true).steps)
    }

    @Test
    fun skipTarget_isNotificationsWhenPresentElseProfile() {
        assertEquals(Notifications, onboardingSequence(true, false, false).skipTarget())
        assertEquals(ProfileReady, onboardingSequence(true, true, true).skipTarget())
    }

    @Test
    fun nextFromMedical_equalsSkipTarget() {
        val sequence = onboardingSequence(plusIntro = true, remindersAnswered = false, redo = false)
        assertEquals(sequence.skipTarget(), sequence.next(Medical))
        assertNull(sequence.next(ProfileReady))
    }

    @Test
    fun progress_isOneBasedAndZeroForIntro() {
        val sequence = onboardingSequence(plusIntro = false, remindersAnswered = false, redo = false)
        assertEquals(0, sequence.progressOf(Intro))
        assertEquals(1, sequence.progressOf(BasicInfo))
        assertEquals(6, sequence.progressOf(ProfileReady))
    }

    @Test
    fun stepOutsideSequence_hasNoNext() {
        val sequence = onboardingSequence(plusIntro = false, remindersAnswered = false, redo = false)
        assertNull(sequence.next(PlusIntro))
    }
}

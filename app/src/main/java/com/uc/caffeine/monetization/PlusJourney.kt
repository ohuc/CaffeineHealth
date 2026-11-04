package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the app should show next on its own, at most one thing at a time. */
enum class JourneyPrompt { NONE, SHOWCASE, TRIAL_OFFER, TRIAL_ENDED }

data class JourneyState(
    val prompt: JourneyPrompt = JourneyPrompt.NONE,
    val canStartTrial: Boolean = false,
    val openDays: Int = 0,
    /** First-run setup may include the Caffeine Health+ introduction (it replaces the post-setup showcase). */
    val onboardingIntroEligible: Boolean = false,
)

/** Local-only engagement record. Never exported, imported or sent anywhere. */
data class JourneyRecord(
    val openDays: Int = 0,
    val lastOpenEpochDay: Long? = null,
    val showcasePending: Boolean = false,
    val showcaseShown: Boolean = false,
    val offerShown: Boolean = false,
    val endedPromptDue: Boolean = false,
    val endedPromptShown: Boolean = false,
)

/**
 * Play journey: a Plus showcase after setup, then one free day offered only after the
 * app was opened on [TRIAL_UNLOCK_DAYS] separate calendar days, then one follow-up.
 */
object PlusJourneyPolicy {
    const val TRIAL_UNLOCK_DAYS = 3

    fun recordOpen(record: JourneyRecord, epochDay: Long, trial: PreviewRecord?, nowMillis: Long): JourneyRecord {
        val newDay = record.lastOpenEpochDay == null || epochDay > record.lastOpenEpochDay
        val counted = if (newDay) record.copy(openDays = record.openDays + 1, lastOpenEpochDay = epochDay) else record
        val trialEnded = trial != null && maxOf(nowMillis, trial.latestSeenMillis) >= trial.endsAtMillis
        return if (trialEnded && !counted.endedPromptShown) counted.copy(endedPromptDue = true) else counted
    }

    /** Only queued while Plus is active, so turning Plus on later never surprises existing users. */
    fun onboardingCompleted(record: JourneyRecord, policyEnabled: Boolean): JourneyRecord =
        if (!policyEnabled || record.showcaseShown) record else record.copy(showcasePending = true)

    /**
     * The setup introduction is the one Plus pitch of first run, so it stands in for the showcase:
     * absent once the showcase was shown, for owners, during a free day, or while Plus is off.
     */
    fun onboardingIntroEligible(
        record: JourneyRecord,
        trial: PreviewRecord?,
        nowMillis: Long,
        paid: Boolean,
        policyEnabled: Boolean,
    ): Boolean {
        val trialActive = trial != null && maxOf(nowMillis, trial.latestSeenMillis) < trial.endsAtMillis
        return policyEnabled && !paid && !trialActive && !record.showcaseShown
    }

    fun handled(record: JourneyRecord, prompt: JourneyPrompt): JourneyRecord = when (prompt) {
        JourneyPrompt.NONE -> record
        JourneyPrompt.SHOWCASE -> record.copy(showcasePending = false, showcaseShown = true)
        JourneyPrompt.TRIAL_OFFER -> record.copy(offerShown = true)
        JourneyPrompt.TRIAL_ENDED -> record.copy(endedPromptDue = false, endedPromptShown = true)
    }

    fun canStartTrial(record: JourneyRecord, trial: PreviewRecord?, paid: Boolean, policyEnabled: Boolean): Boolean =
        policyEnabled && !paid && trial == null && record.openDays >= TRIAL_UNLOCK_DAYS

    fun prompt(record: JourneyRecord, trial: PreviewRecord?, nowMillis: Long, paid: Boolean, policyEnabled: Boolean): JourneyPrompt {
        val trialActive = trial != null && maxOf(nowMillis, trial.latestSeenMillis) < trial.endsAtMillis
        return when {
            !policyEnabled || paid || trialActive -> JourneyPrompt.NONE
            record.showcasePending && !record.showcaseShown -> JourneyPrompt.SHOWCASE
            record.endedPromptDue && !record.endedPromptShown -> JourneyPrompt.TRIAL_ENDED
            canStartTrial(record, trial, paid, policyEnabled) && !record.offerShown -> JourneyPrompt.TRIAL_OFFER
            else -> JourneyPrompt.NONE
        }
    }
}

enum class PlusStatusCard { NONE, BILLING, TRIAL_ACTIVE, PAID_ENDED }

/** A finished free day is followed up by a one-time prompt, not a lingering Home card. */
fun plusStatusCard(access: AccessSnapshot): PlusStatusCard = when {
    access.billingIssue -> PlusStatusCard.BILLING
    access.source == AccessSource.PREVIEW -> PlusStatusCard.TRIAL_ACTIVE
    access.expired && access.paidExpiresAtMillis != null && !access.hasVerifiedPurchase -> PlusStatusCard.PAID_ENDED
    else -> PlusStatusCard.NONE
}

interface PlusJourney {
    val state: StateFlow<JourneyState>
    fun recordAppOpen() {}
    fun onboardingCompleted() {}
    /** The setup introduction was shown: Home must not pitch Plus again right after setup. */
    fun onboardingIntroShown() {}
    fun promptHandled(prompt: JourneyPrompt) {}
    fun startTrial() {}

    object None : PlusJourney {
        override val state: StateFlow<JourneyState> = MutableStateFlow(JourneyState()).asStateFlow()
    }
}

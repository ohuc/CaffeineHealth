package com.uc.caffeine.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SinglePaneWindow
import com.uc.caffeine.util.notifications.NotificationAccess
import com.uc.caffeine.util.notifications.NotificationConsent
import com.uc.caffeine.util.notifications.notificationPromptAllowed

/**
 * First-run setup, and the "redo" copy of it started from Settings.
 *
 * [plusIntro] is the edition's Caffeine Health+ introduction; null (FOSS, policy off, owners, redo)
 * leaves the step out of the sequence. [onSeePlans] opens the edition's Plus page from that step.
 */
@Composable
fun OnboardingRoot(
    displaySettings: UserSettings,
    modifier: Modifier = Modifier,
    resetSessionToken: Int? = null,
    redo: Boolean = false,
    plusIntro: (@Composable (onContinue: () -> Unit, onSeePlans: () -> Unit) -> Unit)? = null,
    onSeePlans: () -> Unit = {},
    /** The edition's restore action on the welcome step (first run only, never on a redo). */
    restoreAction: (@Composable () -> Unit)? = null,
    onExit: (() -> Unit)? = null,
    onFinished: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(OnboardingDestination.Intro)
    val context = LocalContext.current
    val hasPlusIntro = plusIntro != null
    // On a redo, someone who already answered the reminders question is not asked again.
    val remindersAnswered = remember(redo) {
        redo && (
            NotificationConsent.setupAnswered(context) ||
                NotificationConsent.requested(context) ||
                NotificationAccess.read(context).appEnabled
            )
    }
    val sequence = remember(hasPlusIntro, redo, remindersAnswered) {
        onboardingSequence(plusIntro = hasPlusIntro, remindersAnswered = remindersAnswered, redo = redo)
    }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    LaunchedEffect(resetSessionToken) {
        if (resetSessionToken != null) {
            viewModel.resetSession()
        }
    }

    fun navigateTo(destination: OnboardingDestination) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    fun goNext(from: OnboardingDestination) {
        navigateTo(sequence.next(from) ?: OnboardingDestination.ProfileReady)
    }

    fun skipQuestionnaire() {
        viewModel.skipWithDefaultProfile()
        navigateTo(sequence.skipTarget())
    }

    fun progress(destination: OnboardingDestination) =
        OnboardingProgress(sequence.progressOf(destination), sequence.progressTotal)

    // Once notifications can actually be delivered, the Notifications step turns into the same
    // reminder controls as Settings instead of moving straight on.
    var configuringReminders by rememberSaveable { mutableStateOf(false) }

    fun afterRemindersTurnedOn() {
        if (NotificationAccess.read(context).appEnabled) {
            configuringReminders = true
        } else {
            goNext(OnboardingDestination.Notifications)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Granted or denied, the system dialog has now been used; the answer is the user's, and a
        // denial keeps the saved times so Settings can offer recovery.
        NotificationConsent.markRequested(context)
        viewModel.answerReminders(turnOn = true)
        afterRemindersTurnedOn()
    }
    val promptAllowed = notificationPromptAllowed(Build.VERSION.SDK_INT, NotificationConsent.requested(context))

    SinglePaneWindow {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                } else {
                    onExit?.invoke()
                }
            },
            modifier = Modifier.fillMaxSize(),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
            ),
            transitionSpec = {
                slideInHorizontally(spatial, initialOffsetX = { it }) togetherWith
                    (slideOutHorizontally(spatial, targetOffsetX = { -it / 4 }) + fadeOut(effects))
            },
            popTransitionSpec = {
                (slideInHorizontally(spatial, initialOffsetX = { -it / 4 }) + fadeIn(effects)) togetherWith
                    slideOutHorizontally(spatial, targetOffsetX = { it })
            },
            predictivePopTransitionSpec = {
                (slideInHorizontally(spatial, initialOffsetX = { -it / 4 }) + fadeIn(effects)) togetherWith
                    slideOutHorizontally(spatial, targetOffsetX = { it })
            },
            entryProvider = entryProvider {
                entry<OnboardingDestination> { destination ->
                    when (destination) {
                        OnboardingDestination.Intro -> IntroScreen(
                            onStart = {
                                viewModel.startQuestionnaire()
                                goNext(OnboardingDestination.Intro)
                            },
                            onSkip = ::skipQuestionnaire,
                            restoreAction = restoreAction.takeUnless { redo },
                        )

                        OnboardingDestination.BasicInfo -> BasicInfoScreen(
                            uiState = uiState,
                            progress = progress(destination),
                            onBack = { backStack.removeLastOrNull() },
                            onSkip = ::skipQuestionnaire,
                            onContinue = { goNext(destination) },
                            onAgeBucketSelected = viewModel::updateAgeBucket,
                            onWeightIncrement = viewModel::incrementWeight,
                            onWeightDecrement = viewModel::decrementWeight,
                            onWeightUnitSelected = viewModel::updateWeightUnit,
                            onWeightChanged = viewModel::setWeight,
                        )

                        OnboardingDestination.Sleep -> SleepScreen(
                            uiState = uiState,
                            displaySettings = displaySettings,
                            progress = progress(destination),
                            onBack = { backStack.removeLastOrNull() },
                            onSkip = ::skipQuestionnaire,
                            onContinue = { goNext(destination) },
                            onSleepTimeChanged = viewModel::updateSleepTime,
                            onInsomniaChanged = viewModel::updateInsomnia,
                        )

                        OnboardingDestination.Lifestyle -> LifestyleScreen(
                            uiState = uiState,
                            progress = progress(destination),
                            onBack = { backStack.removeLastOrNull() },
                            onSkip = ::skipQuestionnaire,
                            onContinue = { goNext(destination) },
                            onSmokingHabitChanged = viewModel::updateSmokingHabit,
                            onHeavyAlcoholChanged = viewModel::updateHeavyAlcohol,
                            onHeavyCaffeineChanged = viewModel::updateHeavyCaffeine,
                        )

                        OnboardingDestination.Medical -> MedicalScreen(
                            uiState = uiState,
                            progress = progress(destination),
                            onBack = { backStack.removeLastOrNull() },
                            onSkip = ::skipQuestionnaire,
                            onContinue = { goNext(destination) },
                            onLiverDiseaseChanged = viewModel::updateLiverDisease,
                            onMedicationToggled = viewModel::toggleMedication,
                        )

                        OnboardingDestination.Notifications -> NotificationsScreen(
                            progress = progress(destination),
                            permissionNeeded = promptAllowed,
                            configuring = configuringReminders,
                            displaySettings = displaySettings,
                            onBack = { backStack.removeLastOrNull() },
                            onTurnOn = {
                                if (promptAllowed) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.answerReminders(turnOn = true)
                                    afterRemindersTurnedOn()
                                }
                            },
                            onNotNow = {
                                viewModel.answerReminders(turnOn = false)
                                goNext(destination)
                            },
                            onContinue = { goNext(destination) },
                            onInactivityReminderToggle = viewModel::updateInactivityReminder,
                            onDailyReminderTimesChange = viewModel::updateDailyReminderTimes,
                        )

                        OnboardingDestination.PlusIntro -> {
                            val intro = plusIntro
                            if (intro != null) {
                                intro({ goNext(destination) }, onSeePlans)
                            } else {
                                // The edition stopped offering the step while it was on the stack.
                                LaunchedEffect(Unit) {
                                    backStack.removeLastOrNull()
                                    navigateTo(OnboardingDestination.ProfileReady)
                                }
                            }
                        }

                        OnboardingDestination.ProfileReady -> ProfileReadyScreen(
                            uiState = uiState,
                            displaySettings = displaySettings,
                            progress = progress(destination),
                            onBack = { backStack.removeLastOrNull() },
                            onOpenLegalSheet = viewModel::showLegalSheet,
                            onDismissLegalSheet = viewModel::hideLegalSheet,
                            onLegalAcknowledgedChanged = viewModel::setLegalAcknowledged,
                            onComplete = {
                                viewModel.startCelebration()
                            },
                        )
                    }
                }
            },
        )

            if (uiState.isCelebrating) {
                BackHandler {}

                CompletionCelebration(
                    onComplete = {
                        viewModel.completeOnboarding {
                            onFinished?.invoke()
                        }
                    },
                )
            }
        }
    }
    }
}

package com.uc.caffeine

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import com.uc.caffeine.ui.components.SinglePaneWindow
import com.uc.caffeine.ui.components.rememberWindowLayoutPolicy
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import com.uc.caffeine.ui.viewmodel.SettingsDeepLink
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.frostedGlass
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.onboarding.OnboardingRoot
import com.uc.caffeine.ui.onboarding.StartupDestination
import com.uc.caffeine.ui.onboarding.resolveStartupDestination
import com.uc.caffeine.ui.screens.AddScreen
import com.uc.caffeine.ui.screens.AnalyticsScreen
import com.uc.caffeine.ui.screens.HomeScreen
import com.uc.caffeine.ui.screens.settings.SettingsScreen
import com.uc.caffeine.ui.theme.CaffeineTheme
import com.uc.caffeine.ui.theme.MontserratFamily
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel
import com.uc.caffeine.widget.CaffeineWidgetUpdater
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.uc.caffeine.util.AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the Android 12+ splash is themed correctly
        // and hands off to postSplashScreenTheme on the first frame. We intentionally do
        // NOT set a custom exit-animation listener: on some OEM/custom ROMs (e.g. the
        // OnePlus 5T on an Android 15 GSI) the platform hands core-splashscreen a null
        // splash view, and the library NPEs inside its exit dispatch before our lambda
        // even runs (crash reported in #35). The system's default exit is used instead —
        // the branded background and animated icon still show.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        com.uc.caffeine.util.notifications.NotificationChannels.createChannels(this)
        CaffeineWidgetUpdater.schedulePeriodicRefresh(this)
        intent.getStringExtra("plus_feature")?.let { value ->
            com.uc.caffeine.monetization.ProFeature.entries.firstOrNull { it.name == value }?.let { feature ->
                androidx.lifecycle.ViewModelProvider(this)[CaffeineViewModel::class.java].requestFeature(feature)
            }
        }
        lifecycleScope.launch {
            CaffeineWidgetUpdater.publishWidgetPreviews(applicationContext)
        }
        setContent {
            CaffeineApp()
        }
    }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

val LocalAppScaffoldPadding = compositionLocalOf {
    PaddingValues(0.dp)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun CaffeineApp(
    viewModel: CaffeineViewModel = viewModel(),
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isUserSettingsLoaded by viewModel.isUserSettingsLoaded.collectAsStateWithLifecycle()
    val isConsumptionEntriesLoading by viewModel.isConsumptionEntriesLoading.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onAppOpened()
        onPauseOrDispose {}
    }
    val hasExistingConsumptionHistory by viewModel.hasExistingConsumptionHistory.collectAsStateWithLifecycle()
    val darkTheme = when (userSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val startupDestination = resolveStartupDestination(
        isSettingsLoaded = isUserSettingsLoaded,
        isConsumptionHistoryLoading = isConsumptionEntriesLoading,
        hasExistingConsumptionHistory = hasExistingConsumptionHistory,
        isOnboardingComplete = userSettings.isOnboardingComplete,
    )
    // First-run setup finishing in this session queues the Play Plus showcase (no-op in FOSS).
    var sawOnboarding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startupDestination) {
        when (startupDestination) {
            StartupDestination.Onboarding -> sawOnboarding = true
            StartupDestination.Main -> if (sawOnboarding) {
                sawOnboarding = false
                viewModel.distributionServices.journey.onboardingCompleted()
            }
            else -> Unit
        }
    }
    // Play's Caffeine Health+ introduction is offered once eligibility is first seen. Showing it marks
    // the post-setup showcase as shown, which flips eligibility off, so the offer is latched here
    // instead of letting the step vanish from the sequence while the user is looking at it.
    val journeyState by viewModel.distributionServices.journey.state.collectAsStateWithLifecycle()
    var introOffered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(journeyState.onboardingIntroEligible) {
        if (journeyState.onboardingIntroEligible) introOffered = true
    }
    var plusPlansOpen by rememberSaveable { mutableStateOf(false) }
    CaffeineTheme(
        darkTheme = darkTheme,
        colorPalette = userSettings.colorPalette,
    ) {
        // Backdrop for the startup transition: the Onboarding→Main scaleIn from 0.92 leaves a
        // margin around the incoming shell, which would otherwise expose the windowBackground.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimatedContent(
                targetState = startupDestination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    when {
                        initialState == StartupDestination.Onboarding &&
                            targetState == StartupDestination.Main -> {
                            (
                                fadeIn(animationSpec = tween(durationMillis = 500)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(durationMillis = 600, easing = EaseOut),
                                    )
                                ) togetherWith (
                                fadeOut(animationSpec = tween(durationMillis = 400)) +
                                    scaleOut(
                                        targetScale = 1.06f,
                                        animationSpec = tween(durationMillis = 400),
                                    )
                                )
                        }

                        else -> {
                            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 40)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 180))
                        }
                    }.using(SizeTransform(clip = false))
                },
                label = "startup_destination_transition",
            ) { destination ->
                when (destination) {
                    StartupDestination.Loading -> StartupLoadingScreen()
                    StartupDestination.Onboarding -> {
                        val distributionUi = viewModel.distributionServices.distributionUi
                        Box(Modifier.fillMaxSize()) {
                            OnboardingRoot(
                                displaySettings = userSettings,
                                plusIntro = if (introOffered) {
                                    { onContinue, onSeePlans -> distributionUi.OnboardingPlusIntro(onContinue, onSeePlans) }
                                } else null,
                                onSeePlans = { plusPlansOpen = true },
                                restoreAction = {
                                    distributionUi.OnboardingRestore(onRestore = viewModel::restoreBackupDuringSetup)
                                },
                            )
                            // The shell's Plus overlay does not exist during setup, so host the page here.
                            AnimatedVisibility(
                                visible = plusPlansOpen,
                                enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
                                exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
                            ) {
                                HingeSafePage {
                                    distributionUi.PlusPage(showcase = true, onDismiss = { plusPlansOpen = false })
                                }
                            }
                        }
                    }
                    StartupDestination.Main -> MainAppShell(userSettings = userSettings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MainAppShell(
    userSettings: UserSettings,
    viewModel: CaffeineViewModel = viewModel(),
) {
    val featurePrompt by viewModel.featurePrompt.collectAsStateWithLifecycle()
    val plusPage by viewModel.plusPage.collectAsStateWithLifecycle()
    val distributionUi = viewModel.distributionServices.distributionUi
    val journey = viewModel.distributionServices.journey
    val journeyState by journey.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(AppDestinations.HOME)
    val currentRoute = backStack.lastOrNull() as? AppRoute ?: AppDestinations.HOME
    val haptics = rememberAppHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    var manualOnboardingVisible by rememberSaveable { mutableStateOf(false) }
    var manualOnboardingSession by rememberSaveable { mutableIntStateOf(0) }
    // Set while a screen shows something full-screen over everything, like the barcode scanner.
    var fullScreenContent by remember { mutableStateOf(false) }
    val selectedToolbarDestination = currentRoute.resolveToolbarDestination()
    val shouldShowHomeFab = !manualOnboardingVisible && currentRoute.shouldShowHomeFab()
    // Screens scroll under the floating toolbar (they pad their lists by the bottom inset rather
    // than the layout), so the toolbar samples them and draws a frosted backdrop.
    val hazeState = rememberHazeState()
    // Null while expressive blur is off: the toolbar and Add button fall back to solid surfaces.
    val blurState = hazeState.takeIf { userSettings.expressiveBlur }

    // Navigation is always the floating toolbar (no rail). The window policy only decides how it
    // is arranged: on wide windows (landscape phones, foldables, tablets) Add sits beside the
    // toolbar at the same height, and every destination shows its label when they fit.
    val layoutPolicy = rememberWindowLayoutPolicy()

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val navTextMeasurer = rememberTextMeasurer()
    val navLabelStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    val navLabel = stringResource(selectedToolbarDestination.labelRes)
    val navLabelWidth = with(density) {
        navTextMeasurer.measure(AnnotatedString(navLabel), style = navLabelStyle, softWrap = false).size.width.toDp()
    }
    val showToolbarLabel = navLabelWidth + 248.dp <= maxWidth
    val inlineAdd = layoutPolicy.wideToolbar
    val allNavLabels = toolbarDestinations.map { stringResource(it.labelRes) }
    val allNavLabelsWidth = with(density) {
        allNavLabels.sumOf {
            navTextMeasurer.measure(AnnotatedString(it), style = navLabelStyle, softWrap = false).size.width
        }.toDp()
    }
    // Icon, spacing and button padding per destination, the toolbar's own padding, the Add
    // button beside it and the side margins.
    val showAllToolbarLabels = layoutPolicy.wideToolbar &&
        allNavLabelsWidth + 72.dp * toolbarDestinations.size + 16.dp + (if (inlineAdd) AddButtonReservedWidth else 0.dp) + 48.dp <= maxWidth
    val navigate: (AppDestinations) -> Unit = { destination ->
        if (currentRoute != destination) {
            haptics.navigation()
            while (backStack.size > 1) backStack.removeLastOrNull()
            if (destination != AppDestinations.HOME) backStack.add(destination)
        }
    }
    val openAdd: () -> Unit = {
        haptics.confirm()
        backStack.add(AddRoute)
    }
    var toolbarHeightPx by remember { mutableIntStateOf(0) }

    val toolbar: @Composable () -> Unit = {
        val buttonBounds = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
        val currentIndex = toolbarDestinations.indexOf(selectedToolbarDestination)
        val targetRect = buttonBounds[currentIndex]
        val button0Rect = buttonBounds[0]
        val pillRelativeX = (targetRect?.left ?: 0f) - (button0Rect?.left ?: 0f)

        val pillAnimatedX by animateFloatAsState(
            targetValue = pillRelativeX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            label = "pillX",
        )
        val pillAnimatedWidth by animateFloatAsState(
            targetValue = targetRect?.width ?: 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            label = "pillWidth",
        )
        val pillColor = MaterialTheme.colorScheme.primary

        HorizontalFloatingToolbar(
            expanded = true,
            // The container is drawn by the blur tint instead, so content behind
            // the toolbar shows through frosted rather than being hidden.
            modifier = Modifier
                .frostedContainer(blurState, CircleShape)
                .onSizeChanged { toolbarHeightPx = it.height },
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContainerColor = Color.Transparent,
                toolbarContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                toolbarDestinations.forEachIndexed { index, destination ->
                    val selected = selectedToolbarDestination == destination

                    ToggleButton(
                        checked = selected,
                        onCheckedChange = { navigate(destination) },
                        modifier = Modifier
                            .height(56.dp)
                            .onGloballyPositioned { coords ->
                                buttonBounds[index] = coords.boundsInParent()
                            }
                            .then(
                                if (index == 0) {
                                    Modifier.drawWithContent {
                                        if (pillAnimatedWidth > 0f) {
                                            drawRoundRect(
                                                color = pillColor,
                                                topLeft = Offset(pillAnimatedX, 0f),
                                                size = Size(pillAnimatedWidth, size.height),
                                                cornerRadius = CornerRadius(size.height / 2f),
                                            )
                                        }
                                        drawContent()
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            checkedContainerColor = Color.Transparent,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        shapes = ToggleButtonDefaults.shapes(
                            shape = CircleShape,
                            pressedShape = CircleShape,
                            checkedShape = CircleShape,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DestinationIcon(
                                destination = destination,
                                selected = selected,
                            )
                            AnimatedVisibility(
                                visible = showAllToolbarLabels || (selected && showToolbarLabel),
                                enter = expandHorizontally(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                ),
                                exit = shrinkHorizontally(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                ),
                            ) {
                                Text(
                                    text = stringResource(destination.labelRes),
                                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            if (!manualOnboardingVisible) {
                AnimatedVisibility(
                    visible = !fullScreenContent,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (inlineAdd) {
                            // Wide windows: one centred row, Add beside the toolbar and exactly as
                            // tall, so neither steals height from a landscape screen.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                toolbar()
                                AnimatedVisibility(
                                    visible = shouldShowHomeFab,
                                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                                        expandHorizontally(
                                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                            expandFrom = Alignment.Start,
                                        ),
                                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                                        shrinkHorizontally(
                                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                            shrinkTowards = Alignment.Start,
                                        ),
                                ) {
                                    AddConsumptionButton(
                                        hazeState = blurState,
                                        height = if (toolbarHeightPx > 0) {
                                            with(density) { toolbarHeightPx.toDp() }
                                        } else {
                                            AddButtonHeight
                                        },
                                        modifier = Modifier.padding(start = 12.dp),
                                        onClick = openAdd,
                                    )
                                }
                            }
                        } else {
                            // Phones in portrait: Add stacked above the toolbar.
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AnimatedVisibility(
                                    visible = shouldShowHomeFab,
                                    enter = fadeIn(tween(300)) + expandVertically(
                                        expandFrom = Alignment.Bottom,
                                        animationSpec = tween(300),
                                    ),
                                    exit = shrinkVertically(
                                        shrinkTowards = Alignment.Bottom,
                                        animationSpec = tween(250),
                                    ) + fadeOut(tween(250)),
                                ) {
                                    AddConsumptionButton(
                                        hazeState = blurState,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        onClick = openAdd,
                                    )
                                }
                                toolbar()
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalAppScaffoldPadding provides innerPadding,
        ) {
            // The page background is painted inside the haze source (after hazeSource) so the
            // blurred copy is opaque. Without it, text on the bare background blurs to near
            // transparency and the sharp original shows through the frosted toolbar's tint.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurState?.let { Modifier.hazeSource(it) } ?: Modifier)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Every route owns its window adaptation: Settings and Analytics are list/detail
                // scenes, Home and Add are main/supporting layouts (AdaptiveMainSupportingLayout),
                // and each caps a single pane and keeps it clear of hinges itself.
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize(),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                    ),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    popTransitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    predictivePopTransitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    entryProvider = entryProvider {
                        entry<AppDestinations> { key ->
                            when (key) {
                                AppDestinations.HOME -> HomeScreen(
                                    onOpenPlus = { viewModel.openPlus() },
                                    onOpenHealthConnect = {
                                        viewModel.requestFeature(com.uc.caffeine.monetization.ProFeature.HealthConnect) {
                                            viewModel.openSettingsPage(SettingsDeepLink.HealthConnect)
                                            navigate(AppDestinations.SETTINGS)
                                        }
                                    },
                                )
                                AppDestinations.ANALYTICS -> AnalyticsScreen()
                                AppDestinations.SETTINGS -> SettingsScreen(
                                    onRedoOnboarding = {
                                        haptics.navigation()
                                        manualOnboardingSession += 1
                                        manualOnboardingVisible = true
                                    },
                                )
                            }
                        }
                        entry<AddRoute> {
                            AddScreen(
                                onDrinkLogged = {
                                    backStack.removeLastOrNull()
                                },
                                onFullScreenChange = { fullScreenContent = it },
                            )
                        }
                    },
                )

                featurePrompt?.let { feature ->
                    viewModel.distributionServices.distributionUi.FeatureSheet(
                        feature = feature,
                        onSeePlans = {
                            viewModel.dismissFeaturePrompt()
                            viewModel.openPlus()
                        },
                        onDismiss = viewModel::dismissFeaturePrompt,
                    )
                }
                LaunchedEffect(journeyState.prompt, manualOnboardingVisible) {
                    if (journeyState.prompt == com.uc.caffeine.monetization.JourneyPrompt.SHOWCASE && !manualOnboardingVisible) {
                        journey.promptHandled(com.uc.caffeine.monetization.JourneyPrompt.SHOWCASE)
                        viewModel.openPlus(showcase = true)
                    }
                }
                if (featurePrompt == null && !manualOnboardingVisible) {
                    viewModel.distributionServices.distributionUi.JourneySheet(
                        prompt = journeyState.prompt,
                        onSeePlans = { viewModel.openPlus() },
                    )
                }
                if (manualOnboardingVisible) {
                    OnboardingRoot(
                        displaySettings = userSettings,
                        resetSessionToken = manualOnboardingSession,
                        redo = true,
                        onExit = { manualOnboardingVisible = false },
                        onFinished = { manualOnboardingVisible = false },
                    )
                }
            }
        }
    }
    // Full-screen layers above everything, the toolbar included (slid up like a sheet).
    var lastShowcase by remember { mutableStateOf(false) }
    plusPage?.let { lastShowcase = it }
    AnimatedVisibility(
        visible = plusPage != null,
        enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
        exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
    ) {
        HingeSafePage {
            distributionUi.PlusPage(showcase = lastShowcase, onDismiss = viewModel::closePlus)
        }
    }
    distributionUi.Overlays()
    }
}

/**
 * A full-screen page (the Plus page) kept on one side of a separating or occluding hinge, over an
 * opaque background so the app underneath never shows on the other side of the fold.
 */
@Composable
private fun HingeSafePage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SinglePaneWindow { content() }
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestinations,
    selected: Boolean,
) {
    val vectorIcon = if (selected) {
        destination.iconFilledVector
    } else {
        destination.iconOutlinedVector
    }

    if (vectorIcon != null) {
        Icon(
            imageVector = vectorIcon,
            contentDescription = stringResource(destination.labelRes),
            modifier = Modifier.size(24.dp),
        )
        return
    }

    val iconRes = if (selected) {
        destination.iconFilledRes
    } else {
        destination.iconOutlinedRes
    }

    if (iconRes != null) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(destination.labelRes),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * The container shared by the floating toolbar and the Add Consumption button. With a [hazeState]
 * it is frosted glass: whatever scrolls behind (the [hazeSource] content) is blurred and tinted
 * with primaryContainer. Without one (expressive blur turned off) it is plain primaryContainer.
 */
@Composable
private fun Modifier.frostedContainer(hazeState: HazeState?, shape: Shape): Modifier {
    val tint = MaterialTheme.colorScheme.primaryContainer
    if (hazeState == null) return clip(shape).background(tint)
    return clip(shape).frostedGlass(hazeState, tint)
}

private val AddButtonHeight = 72.dp

/** Room kept for the Add button when it sits beside the toolbar (label plus burst and padding). */
private val AddButtonReservedWidth = 240.dp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
private fun AddConsumptionButton(
    hazeState: HazeState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AddButtonHeight,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "burst_rotation")
    val burstRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "burst_rotation",
    )

    val shape = RoundedCornerShape(50)
    // Frosted like the toolbar below it. No shadow: the toolbar has none, and an elevation shadow
    // would show through the translucent tint as a dark smudge.
    Surface(
        onClick = onClick,
        modifier = modifier
            .frostedContainer(hazeState, shape)
            .height(height),
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = burstRotation }
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(Color.White),
            )
            Text(
                text = stringResource(R.string.main_add_consumption),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

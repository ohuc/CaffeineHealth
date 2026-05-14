package com.uc.caffeine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.data.UserSettings
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

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled by the system — no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.uc.caffeine.util.notifications.NotificationChannels.createChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        CaffeineWidgetUpdater.schedulePeriodicRefresh(this)
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
                    StartupDestination.Onboarding -> OnboardingRoot(displaySettings = userSettings)
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
) {
    val backStack = rememberNavBackStack(AppDestinations.HOME)
    val currentRoute = backStack.lastOrNull() as? AppRoute ?: AppDestinations.HOME
    val haptics = rememberAppHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    var manualOnboardingVisible by rememberSaveable { mutableStateOf(false) }
    var manualOnboardingSession by rememberSaveable { mutableIntStateOf(0) }
    val selectedToolbarDestination = currentRoute.resolveToolbarDestination()
    val shouldShowHomeFab = !manualOnboardingVisible && currentRoute.shouldShowHomeFab()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            if (!manualOnboardingVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
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
                                modifier = Modifier.padding(bottom = 8.dp),
                                onClick = {
                                    haptics.confirm()
                                    backStack.add(AddRoute)
                                },
                            )
                        }
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
                            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                                toolbarContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                toolbarContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                                toolbarDestinations.forEachIndexed { index, destination ->
                                val selected = selectedToolbarDestination == destination

                                ToggleButton(
                                    checked = selected,
                                    onCheckedChange = {
                                        if (!selected) {
                                            haptics.navigation()
                                            if (currentRoute != AppDestinations.HOME) {
                                                backStack.removeLastOrNull()
                                            }
                                            if (destination != AppDestinations.HOME) {
                                                backStack.add(destination)
                                            }
                                        }
                                    },
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
                                            visible = selected,
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
                                            )
                                        }
                                    }
                                }
                            }
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                                AppDestinations.HOME -> HomeScreen()
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
                            )
                        }
                    },
                )

                if (manualOnboardingVisible) {
                    OnboardingRoot(
                        displaySettings = userSettings,
                        resetSessionToken = manualOnboardingSession,
                        onExit = { manualOnboardingVisible = false },
                        onFinished = { manualOnboardingVisible = false },
                    )
                }
            }
        }
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

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
private fun AddConsumptionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
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

@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.uc.caffeine.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uc.caffeine.R
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.RollingNumberText
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.theme.MontserratFamily
import com.uc.caffeine.util.formatTimeOfDay
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill

internal const val OnboardingStepCount = 5

internal enum class OnboardingLayoutProfile {
    Compact,
    Regular,
    Tall,
}

internal data class OnboardingLayoutMetrics(
    val profile: OnboardingLayoutProfile,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val heroHeight: Dp,
    val heroContentSpacing: Dp,
    val titleTopSpacing: Dp,
    val subtitleTopSpacing: Dp,
    val preCardSpacing: Dp,
    val cardPadding: Dp,
    val cardSpacing: Dp,
    val postCardSpacerWeight: Float,
)

@Composable
private fun rememberOnboardingLayoutMetrics(maxHeight: Dp): OnboardingLayoutMetrics {
    return remember(maxHeight) {
        when {
            maxHeight < 700.dp -> OnboardingLayoutMetrics(
                profile = OnboardingLayoutProfile.Compact,
                horizontalPadding = 14.dp,
                verticalPadding = 8.dp,
                heroHeight = 104.dp,
                heroContentSpacing = 8.dp,
                titleTopSpacing = 8.dp,
                subtitleTopSpacing = 4.dp,
                preCardSpacing = 10.dp,
                cardPadding = 14.dp,
                cardSpacing = 12.dp,
                postCardSpacerWeight = 0f,
            )

            maxHeight < 860.dp -> OnboardingLayoutMetrics(
                profile = OnboardingLayoutProfile.Regular,
                horizontalPadding = 16.dp,
                verticalPadding = 10.dp,
                heroHeight = 116.dp,
                heroContentSpacing = 8.dp,
                titleTopSpacing = 10.dp,
                subtitleTopSpacing = 5.dp,
                preCardSpacing = 10.dp,
                cardPadding = 16.dp,
                cardSpacing = 14.dp,
                postCardSpacerWeight = 0.08f,
            )

            else -> OnboardingLayoutMetrics(
                profile = OnboardingLayoutProfile.Tall,
                horizontalPadding = 20.dp,
                verticalPadding = 14.dp,
                heroHeight = 140.dp,
                heroContentSpacing = 10.dp,
                titleTopSpacing = 12.dp,
                subtitleTopSpacing = 6.dp,
                preCardSpacing = 12.dp,
                cardPadding = 18.dp,
                cardSpacing = 16.dp,
                postCardSpacerWeight = 0.15f,
            )
        }
    }
}

@Composable
internal fun OnboardingScaffold(
    title: String,
    subtitle: String,
    currentStep: Int,
    showBackButton: Boolean,
    onBack: (() -> Unit)? = null,
    showSkipButton: Boolean,
    onSkip: (() -> Unit)? = null,
    continueLabel: String,
    continueEnabled: Boolean,
    onContinue: () -> Unit,
    disabledHint: String = "Answer this section to continue.",
    enabledHint: String? = null,
    pushCardUpward: Boolean = false,
    leadingHeaderContent: (@Composable (OnboardingLayoutMetrics) -> Unit)? = null,
    heroContent: (@Composable ColumnScope.(OnboardingLayoutMetrics) -> Unit)? = null,
    content: @Composable ColumnScope.(OnboardingLayoutMetrics) -> Unit,
) {
    val haptics = rememberAppHaptics()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingBackdropDecoration(currentStep = currentStep)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val layout = rememberOnboardingLayoutMetrics(maxHeight)
                val scrollState = rememberScrollState()

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                                ),
                            )
                            .padding(
                                horizontal = layout.horizontalPadding,
                                vertical = layout.verticalPadding,
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                        ) {
                            if (currentStep > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (showBackButton && onBack != null) {
                                            IconButton(
                                                onClick = {
                                                    haptics.navigation()
                                                    onBack()
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Back",
                                                )
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "STEP $currentStep OF $OnboardingStepCount",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                letterSpacing = 1.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        if (showSkipButton && onSkip != null) {
                                            TextButton(
                                                onClick = {
                                                    haptics.toggle()
                                                    onSkip()
                                                },
                                            ) {
                                                Text("Skip")
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    repeat(OnboardingStepCount) { index ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    if (index < currentStep) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                                    },
                                                ),
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    leadingHeaderContent?.invoke(layout) ?: Spacer(modifier = Modifier.size(48.dp))
                                    if (showSkipButton && onSkip != null) {
                                        TextButton(
                                            onClick = {
                                                haptics.toggle()
                                                onSkip()
                                            },
                                        ) {
                                            Text("Skip for now")
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(48.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            heroContent?.let { hero ->
                                Spacer(modifier = Modifier.height(layout.titleTopSpacing))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(layout.heroContentSpacing),
                                    content = { hero(layout) },
                                )
                            }

                            Text(
                                text = title,
                                style = if (layout.profile == OnboardingLayoutProfile.Compact) {
                                    MaterialTheme.typography.headlineSmall
                                } else {
                                    MaterialTheme.typography.headlineMedium
                                },
                                modifier = Modifier.padding(top = layout.titleTopSpacing),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitle,
                                style = if (layout.profile == OnboardingLayoutProfile.Compact) {
                                    MaterialTheme.typography.bodyMedium
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = layout.subtitleTopSpacing),
                            )

                            Spacer(modifier = Modifier.height(layout.preCardSpacing))

                            OnboardingQuestionCard(layout = layout) {
                                content(layout)
                            }

                            Spacer(
                                modifier = Modifier.height(
                                    if (pushCardUpward) {
                                        when (layout.profile) {
                                            OnboardingLayoutProfile.Compact -> 14.dp
                                            OnboardingLayoutProfile.Regular -> 12.dp
                                            OnboardingLayoutProfile.Tall -> 16.dp
                                        }
                                    } else {
                                        8.dp
                                    },
                                ),
                            )
                        }
                    }

                    OnboardingActionArea(
                        layout = layout,
                        continueEnabled = continueEnabled,
                        continueLabel = continueLabel,
                        onContinue = {
                            haptics.confirm()
                            onContinue()
                        },
                        disabledHint = disabledHint,
                        enabledHint = enabledHint,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingBackdropDecoration(currentStep: Int) {
    val stepProgress by animateFloatAsState(
        targetValue = currentStep.coerceIn(0, OnboardingStepCount).toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "onboardingBackdropStep",
    )
    val accentAlpha by animateFloatAsState(
        targetValue = if (currentStep > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "onboardingBackdropAccent",
    )
    val topPolygons = remember {
        listOf(
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Arch,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie12Sided,
        )
    }
    val sidePolygons = remember {
        listOf(
            MaterialShapes.Arch,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Arch,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Cookie6Sided,
        )
    }
    val bottomPolygons = remember {
        listOf(
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Arch,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Arch,
            MaterialShapes.Cookie12Sided,
        )
    }
    val accentPolygons = remember {
        listOf(
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Arch,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Arch,
            MaterialShapes.Cookie6Sided,
        )
    }
    val topShape = rememberBackdropMorphShape(
        polygons = topPolygons,
        progress = stepProgress,
        startAngle = -90,
    )
    val sideShape = rememberBackdropMorphShape(
        polygons = sidePolygons,
        progress = stepProgress,
        startAngle = -90,
    )
    val bottomShape = rememberBackdropMorphShape(
        polygons = bottomPolygons,
        progress = stepProgress,
        startAngle = -90,
    )
    val accentShape = rememberBackdropMorphShape(
        polygons = accentPolygons,
        progress = stepProgress,
        startAngle = -90,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MorphingBackdropBlob(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 118.dp, y = (-94).dp)
                .size(188.dp),
            shape = topShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
        )
        MorphingBackdropBlob(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 156.dp, y = 18.dp)
                .size(126.dp)
                .graphicsLayer(rotationZ = 18f)
                .graphicsLayer(alpha = 0.92f),
            shape = sideShape,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f),
        )
        MorphingBackdropBlob(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 68.dp)
                .size(170.dp)
                .graphicsLayer(rotationZ = -12f),
            shape = bottomShape,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.045f),
        )
        if (accentAlpha > 0.01f) {
            MorphingBackdropBlob(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-42).dp, y = 112.dp)
                    .size(92.dp)
                    .graphicsLayer(
                        rotationZ = -8f,
                        alpha = accentAlpha * 0.9f,
                    ),
                shape = accentShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.028f),
            )
        }
    }
}

@Composable
private fun rememberBackdropMorphShape(
    polygons: List<RoundedPolygon>,
    progress: Float,
    startAngle: Int,
): Shape {
    if (polygons.size == 1) {
        return polygons.first().toShape(startAngle = startAngle)
    }
    val clampedProgress = progress.coerceIn(0f, polygons.lastIndex.toFloat())
    val startIndex = clampedProgress.toInt().coerceAtMost(polygons.lastIndex - 1)
    val endIndex = (startIndex + 1).coerceAtMost(polygons.lastIndex)
    val localProgress = (clampedProgress - startIndex).coerceIn(0f, 1f)
    val startPolygon = polygons[startIndex]
    val endPolygon = polygons[endIndex]
    val shapePath = if (startPolygon == endPolygon || localProgress == 0f) {
        startPolygon.toPath(startAngle = startAngle)
    } else {
        remember(startPolygon, endPolygon, localProgress, startAngle) {
            Morph(startPolygon, endPolygon).toPath(
                progress = localProgress,
                startAngle = startAngle,
            )
        }
    }

    return remember(shapePath) {
        object : Shape {
            private var workPath: Path? = null
            private var lastSize = Size.Unspecified

            override fun createOutline(
                size: Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density,
            ): Outline {
                if (size != lastSize || workPath == null) {
                    lastSize = size
                    workPath = Path()
                } else {
                    workPath!!.rewind()
                }
                val path = workPath!!
                path.addPath(shapePath)
                val scaleMatrix = Matrix().apply { scale(x = size.width, y = size.height) }
                path.transform(scaleMatrix)
                path.translate(size.center - path.getBounds().center)
                return Outline.Generic(path)
            }
        }
    }
}

@Composable
private fun MorphingBackdropBlob(
    modifier: Modifier,
    shape: Shape,
    color: Color,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color),
    )
}


@Composable
private fun OnboardingQuestionCard(
    layout: OnboardingLayoutMetrics,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(if (layout.profile == OnboardingLayoutProfile.Compact) 28.dp else 32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = layout.cardPadding,
                vertical = layout.cardPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(layout.cardSpacing),
            content = content,
        )
    }
}

@Composable
private fun OnboardingActionArea(
    layout: OnboardingLayoutMetrics,
    continueEnabled: Boolean,
    continueLabel: String,
    onContinue: () -> Unit,
    disabledHint: String,
    enabledHint: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                    horizontal = if (layout.profile == OnboardingLayoutProfile.Compact) 14.dp else 18.dp,
                    vertical = if (layout.profile == OnboardingLayoutProfile.Compact) 12.dp else 14.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (layout.profile == OnboardingLayoutProfile.Compact) 58.dp else 64.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = continueEnabled,
                    enter = fadeIn() + slideInVertically { it / 4 } + scaleIn(initialScale = 0.96f),
                    exit = fadeOut() + slideOutVertically { it / 4 } + scaleOut(targetScale = 0.96f),
                ) {
                    Button(
                        onClick = onContinue,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 28.dp,
                            vertical = if (layout.profile == OnboardingLayoutProfile.Compact) 16.dp else 18.dp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = continueLabel,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = !continueEnabled,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = disabledHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (continueEnabled && !enabledHint.isNullOrBlank()) {
                Text(
                    text = enabledHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val OnboardingBadgeShape = RoundedCornerShape(18.dp)
private val OnboardingToggleShape = RoundedCornerShape(18.dp)
private val OnboardingPillShape = RoundedCornerShape(999.dp)

@Composable
internal fun OnboardingBrandLockup(
    layout: OnboardingLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val mainIconSize = if (layout.profile == OnboardingLayoutProfile.Compact) 42.dp else 48.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Caffeine Health Logo",
            modifier = Modifier.size(mainIconSize),
        )

        Text(
            text = "Caffeine Health",
            style = if (layout.profile == OnboardingLayoutProfile.Compact) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.titleMedium
            }.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun OnboardingSection(
    title: String,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            content()
        },
    )
}

@Composable
internal fun OnboardingInfoBadge(
    label: String,
) {
    Surface(
        shape = OnboardingBadgeShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
internal fun OnboardingMarquee(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tickerTrack = remember(text) {
        List(8) { text.uppercase() }.joinToString(
            separator = "   •   ",
            postfix = "   •   ",
        )
    }
    val tickerStyle = MaterialTheme.typography.labelMedium.copy(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.9.sp,
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OnboardingPillShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
        tonalElevation = 1.dp,
    ) {
        Text(
            text = tickerTrack,
            style = tickerStyle,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 9.dp)
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 34.dp,
                ),
        )
    }
}

@Composable
internal fun InfoTextWithSource(
    text: String,
    sourceUrl: String,
) {
    val uriHandler = LocalUriHandler.current
    val hasSourceLink = remember(sourceUrl) {
        sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )

        if (hasSourceLink) {
            TextButton(
                onClick = { uriHandler.openUri(sourceUrl) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Source")
            }
        }
    }
}

@Composable
internal fun <T> ConnectedChoiceButtonGroup(
    options: List<T>,
    selectedOption: T?,
    labelFor: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option
            OnboardingChoiceTile(
                label = labelFor(option),
                checked = isSelected,
                onCheckedChange = { checked ->
                    if (checked && !isSelected) {
                        haptics.toggle()
                        onOptionSelected(option)
                    }
                },
                modifier = Modifier.weight(1f),
                role = Role.RadioButton,
                minHeight = 50.dp,
                shapes = expressiveChoiceShapes(index = index, count = options.size),
            )
        }
    }
}

@Composable
internal fun <T> GridSingleSelectButtonGroup(
    options: List<T>,
    selectedOption: T?,
    labelFor: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()

    AdaptiveExpressiveChoiceGroup(
        options = options,
        labelFor = labelFor,
        isChecked = { option -> selectedOption == option },
        onCheckedChange = { option, checked ->
            if (checked && selectedOption != option) {
                haptics.toggle()
                onOptionSelected(option)
            }
        },
        role = Role.RadioButton,
        modifier = modifier,
        minHeight = 52.dp,
    )
}

@Composable
internal fun <T> GridMultiSelectButtonGroup(
    options: List<T>,
    selectedOptions: Set<T>,
    labelFor: (T) -> String,
    onOptionToggled: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()

    AdaptiveExpressiveChoiceGroup(
        options = options,
        labelFor = labelFor,
        isChecked = { option -> option in selectedOptions },
        onCheckedChange = { option, _ ->
            haptics.toggle()
            onOptionToggled(option)
        },
        role = Role.Checkbox,
        modifier = modifier,
        minHeight = 52.dp,
    )
}

@Composable
private fun <T> AdaptiveExpressiveChoiceGroup(
    options: List<T>,
    labelFor: (T) -> String,
    isChecked: (T) -> Boolean,
    onCheckedChange: (T, Boolean) -> Unit,
    role: Role,
    modifier: Modifier = Modifier,
    minHeight: Dp,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useSingleRow = shouldUseConnectedRow(
            maxWidth = maxWidth,
            optionCount = options.size,
            maxLabelLength = options.maxOfOrNull { option -> labelFor(option).length } ?: 0,
        )

        if (useSingleRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                options.forEachIndexed { index, option ->
                    OnboardingChoiceTile(
                        label = labelFor(option),
                        checked = isChecked(option),
                        onCheckedChange = { checked -> onCheckedChange(option, checked) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        role = role,
                        minHeight = minHeight,
                        shapes = expressiveChoiceShapes(index = index, count = options.size),
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        rowOptions.forEachIndexed { colIndex, option ->
                            OnboardingChoiceTile(
                                label = labelFor(option),
                                checked = isChecked(option),
                                onCheckedChange = { checked -> onCheckedChange(option, checked) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                role = role,
                                minHeight = minHeight,
                                shapes = expressiveChoiceShapes(
                                    index = colIndex,
                                    count = rowOptions.size,
                                ),
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun shouldUseConnectedRow(
    maxWidth: Dp,
    optionCount: Int,
    maxLabelLength: Int,
): Boolean {
    return when {
        optionCount <= 2 -> true
        optionCount == 3 -> maxWidth >= 380.dp && maxLabelLength <= 16
        optionCount == 4 -> maxWidth >= 560.dp && maxLabelLength <= 12
        else -> false
    }
}

@Composable
private fun expressiveChoiceShapes(
    index: Int,
    count: Int,
): ToggleButtonShapes {
    if (count <= 1) {
        return ToggleButtonDefaults.shapes(
            shape = OnboardingToggleShape,
            pressedShape = OnboardingToggleShape,
            checkedShape = OnboardingToggleShape,
        )
    }

    return when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
}

@Composable
private fun OnboardingChoiceTile(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    role: Role,
    minHeight: Dp,
    shapes: ToggleButtonShapes,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .semantics { this.role = role },
        colors = ToggleButtonDefaults.toggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shapes = shapes,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun SelectedSummary(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun TypingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    charDelayMillis: Long = 10L,
) {
    var visibleCharacters by rememberSaveable(text) { mutableIntStateOf(0) }
    val displayText = remember(text, visibleCharacters) {
        if (visibleCharacters >= text.length) text else text.take(visibleCharacters) + " "
    }

    LaunchedEffect(text) {
        if (visibleCharacters >= text.length) return@LaunchedEffect
        delay(120)
        while (visibleCharacters < text.length) {
            visibleCharacters += 1
            delay(charDelayMillis)
        }
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun OnboardingHero(
    layout: OnboardingLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val heroScale = remember { Animatable(0.82f) }
    val heroOffset = remember { Animatable(22f) }
    val accentRotation = remember { Animatable(-18f) }

    LaunchedEffect(Unit) {
        launch {
            heroScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            heroOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                ),
            )
        }
        launch {
            accentRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                ),
            )
        }
    }

    val mainHeroSize = when (layout.profile) {
        OnboardingLayoutProfile.Compact -> 92.dp
        OnboardingLayoutProfile.Regular -> 112.dp
        OnboardingLayoutProfile.Tall -> 124.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(layout.heroHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = 2.dp)
                .size(mainHeroSize * 0.5f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
        )

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(mainHeroSize)
                .graphicsLayer(
                    scaleX = heroScale.value,
                    scaleY = heroScale.value,
                )
                .offset(y = heroOffset.value.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(mainHeroSize * 0.34f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 18.dp, y = 10.dp)
                .graphicsLayer(rotationZ = accentRotation.value),
        ) {
            Box(
                modifier = Modifier.padding(
                    horizontal = if (layout.profile == OnboardingLayoutProfile.Compact) 10.dp else 12.dp,
                    vertical = if (layout.profile == OnboardingLayoutProfile.Compact) 10.dp else 12.dp,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_moon),
                    contentDescription = null,
                    modifier = Modifier.size(if (layout.profile == OnboardingLayoutProfile.Compact) 18.dp else 20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Surface(
            shape = OnboardingPillShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-2).dp)
                .graphicsLayer(
                    scaleX = heroScale.value,
                    scaleY = heroScale.value,
                ),
        ) {
            Text(
                text = "Bedtime-aware from the first cup",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun WeightStepperCard(
    weightValue: Int,
    weightUnit: WeightUnit,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onWeightChanged: (Int) -> Unit = {},
) {
    val haptics = rememberAppHaptics()
    var showInputDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConnectedChoiceButtonGroup(
            options = WeightUnit.entries,
            selectedOption = weightUnit,
            labelFor = { unit -> unit.label },
            onOptionSelected = onWeightUnitSelected,
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledIconButton(
                    onClick = {
                        haptics.toggle()
                        onDecrement()
                    },
                    enabled = weightValue > weightUnit.minSelectable(),
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Lower weight",
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    RollingNumberText(
                        text = weightValue.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        labelPrefix = "weight_stepper",
                    )
                    Text(
                        text = weightUnit.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }

                FilledIconButton(
                    onClick = {
                        haptics.toggle()
                        onIncrement()
                    },
                    enabled = weightValue < weightUnit.maxSelectable(),
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Raise weight",
                    )
                }
            }
        }
    }

    if (showInputDialog) {
        WeightInputDialog(
            currentValue = weightValue,
            weightUnit = weightUnit,
            onConfirm = { newValue ->
                onWeightChanged(newValue)
                showInputDialog = false
            },
            onDismiss = { showInputDialog = false },
        )
    }
}

@Composable
private fun WeightInputDialog(
    currentValue: Int,
    weightUnit: WeightUnit,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentValue.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed in weightUnit.minSelectable()..weightUnit.maxSelectable()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(weightUnit.clamp(it)) } },
                enabled = isValid,
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Enter weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    label = { Text("Weight (${weightUnit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = text.isNotEmpty() && !isValid,
                )
                Text(
                    text = "Range: ${weightUnit.minSelectable()}–${weightUnit.maxSelectable()} ${weightUnit.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
internal fun SleepTimePickerCard(
    displaySettings: UserSettings,
    selectedTime: java.time.LocalTime,
    onSleepTimeChanged: (java.time.LocalTime) -> Unit,
    enabled: Boolean = true,
    hint: String? = null,
    title: String? = "Typical bedtime",
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberAppHaptics()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = hint ?: "Starts at 11 PM. Adjust only if yours is different.",
                    style = MaterialTheme.typography.bodySmall,
                    // Primary color only when the field is locked by HC; muted otherwise
                    color = if (!enabled && hint != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = {
                    haptics.toggle()
                    showTimePicker = true
                },
                enabled = enabled,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = formatTimeOfDay(
                        hour = selectedTime.hour,
                        minute = selectedTime.minute,
                        settings = displaySettings,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (showTimePicker) {
        OnboardingTimePickerDialog(
            is24HourClock = displaySettings.use24HourClock,
            initialTime = selectedTime,
            onTimeSelected = { time ->
                onSleepTimeChanged(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun OnboardingTimePickerDialog(
    is24HourClock: Boolean,
    initialTime: java.time.LocalTime,
    onTimeSelected: (java.time.LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24HourClock,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pick your usual bedtime")
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(java.time.LocalTime.of(timePickerState.hour, timePickerState.minute))
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun ProfileMetricRow(
    title: String,
    value: String,
    description: String,
    onInfoClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RollingNumberText(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    labelPrefix = "profile_metric_$title",
                )
            }
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "About $title",
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LegalSheet(
    legalAcknowledged: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAcknowledgedChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Legal stuff",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "One quick acknowledgement before you start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Medical disclaimer",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Caffeine is for general wellness tracking only. It is not medical advice, diagnosis, or treatment. If you have health concerns, take medicines that affect caffeine, or notice severe symptoms, please talk to a clinician.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = legalAcknowledged,
                        onCheckedChange = onAcknowledgedChanged,
                    )
                    Column(
                        modifier = Modifier.weight(1f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "I understand this app offers estimates for personal tracking only and should not replace medical guidance.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Button(
                onClick = onComplete,
                enabled = legalAcknowledged && !isSaving,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(if (isSaving) "Saving profile..." else "Let's start tracking")
            }
        }
    }
}

internal fun formatHalfLife(halfLifeMinutes: Int): String {
    val hours = halfLifeMinutes / 60
    val minutes = halfLifeMinutes % 60
    return if (minutes == 0) {
        "$hours h"
    } else {
        "$hours h $minutes m"
    }
}

@Composable
internal fun BedtimeDotSlider(
    selectedTime: java.time.LocalTime,
    onTimeSelected: (java.time.LocalTime) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val times = remember {
        listOf(21, 22, 23, 0, 1).map { hour -> java.time.LocalTime.of(hour, 0) }
    }
    val labels = remember { listOf("9 PM", "10 PM", "11 PM", "12 AM", "1 AM") }

    val targetIndex = remember(selectedTime.hour) {
        times.indexOfFirst { it.hour == selectedTime.hour }.coerceAtLeast(0)
    }
    var displayedIndex by remember {
        mutableIntStateOf(times.indexOfFirst { it.hour == selectedTime.hour }.coerceAtLeast(0))
    }

    LaunchedEffect(targetIndex) {
        displayedIndex = targetIndex
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Typical bedtime",
                style = MaterialTheme.typography.titleMedium,
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .padding(top = 17.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(999.dp),
                        ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                ) {
                    times.forEachIndexed { index, time ->
                        val isVisuallySelected = index == displayedIndex
                        val dotSize by animateDpAsState(
                            targetValue = if (isVisuallySelected) 18.dp else 12.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh,
                            ),
                            label = "dotSize_$index",
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isVisuallySelected) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            animationSpec = tween(durationMillis = 200),
                            label = "dotColor_$index",
                        )
                        val bgColor by animateColorAsState(
                            targetValue = if (isVisuallySelected) MaterialTheme.colorScheme.primaryContainer
                                          else Color.Transparent,
                            animationSpec = tween(durationMillis = 200),
                            label = "bgColor_$index",
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isVisuallySelected) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 200),
                            label = "textColor_$index",
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Surface(
                                    onClick = {
                                        haptics.toggle()
                                        onTimeSelected(time)
                                    },
                                    shape = CircleShape,
                                    color = bgColor,
                                    modifier = Modifier.size(36.dp),
                                ) {}
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .background(dotColor, CircleShape),
                                )
                            }
                            Text(
                                text = labels[index],
                                style = if (isVisuallySelected) {
                                    MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.labelSmall
                                },
                                color = textColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SampleCaffeineChart(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val modelProducer = remember { CartesianChartModelProducer() }

    val xData = remember { (0..17).map { it.toDouble() } }
    val yData = remember {
        listOf(
            10.0, 68.0, 185.0, 245.0, 215.0, 188.0, 168.0, 152.0,
            178.0, 158.0, 132.0, 108.0, 85.0, 65.0, 48.0, 34.0, 22.0, 13.0,
        )
    }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries {
                series(xData, yData)
            }
        }
    }

    val lineColor = colorScheme.primary
    val lineFill = remember(lineColor) {
        LineCartesianLayer.LineFill.single(Fill(lineColor))
    }
    val areaFill = remember(lineColor) {
        LineCartesianLayer.AreaFill.single(
            Fill(
                Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.40f), Color.Transparent),
                ),
            ),
        )
    }
    val line = remember(lineFill, areaFill) {
        LineCartesianLayer.Line(
            fill = lineFill,
            areaFill = areaFill,
            pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = 0.5f),
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val chartHeight = maxHeight - 20.dp

        CartesianChartHost(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = 20.dp),
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(line),
                    rangeProvider = CartesianLayerRangeProvider.fixed(
                        minX = 0.0,
                        maxX = 17.0,
                        minY = 0.0,
                        maxY = 280.0,
                    ),
                ),
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            // Dashed bedtime vertical line (11 PM = rightmost data point)
            val bedtimeX = size.width * (15.8f / 17f)
            drawLine(
                color = lineColor.copy(alpha = 0.45f),
                start = Offset(bedtimeX, 0f),
                end = Offset(bedtimeX, size.height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
            )

            // Consumption event dots on the curve
            val dotRadius = 5.dp.toPx()
            listOf(1 to 68.0, 5 to 188.0, 9 to 158.0).forEach { (xIndex, yValue) ->
                val dotX = size.width * (xIndex / 17f)
                val dotY = size.height * (1f - (yValue / 280.0).toFloat())
                drawCircle(color = lineColor, radius = dotRadius, center = Offset(dotX, dotY))
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 4.dp),
            shape = RoundedCornerShape(999.dp),
            color = colorScheme.secondaryContainer,
        ) {
            Text(
                text = "BEDTIME",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "6 AM",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
            Text(
                text = "NOON",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
            Text(
                text = "11 PM",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

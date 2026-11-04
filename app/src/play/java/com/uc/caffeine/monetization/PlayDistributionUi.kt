package com.uc.caffeine.monetization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uc.caffeine.R
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.theme.CaffeineSurfaceDefaults
import com.uc.caffeine.util.formatTimestampToDateTime

internal class PlayDistributionUi(
    private val access: FeatureAccess,
    private val store: StoreCatalog,
    private val journey: PlusJourney,
) : DistributionUi {
    /**
     * Highlighted at the top of Settings, like a selected row. Where Plus is not on sale it stays
     * (owners still manage purchases there) but sits as a plain row instead of calling attention.
     */
    @Composable override fun SettingsEntry(onOpen: () -> Unit) {
        val snapshot by access.snapshots.collectAsStateWithLifecycle()
        val haptics = rememberAppHaptics()
        val owned = snapshot.hasVerifiedPurchase
        if (!BillingVariantSupport.PLUS_SURFACES && !owned) return
        SegmentedListItem(
            selected = owned || snapshot.plusOnSale,
            onClick = { haptics.toggle(); onOpen() },
            leadingContent = {
                // Same treatment as every other settings icon: one tone from the colour scheme.
                Icon(
                    painter = painterResource(R.drawable.ic_plus_mark),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            supportingContent = {
                Text(stringResource(when (snapshot.source) {
                    AccessSource.SUBSCRIPTION -> R.string.plus_status_active
                    AccessSource.LIFETIME -> R.string.plus_status_lifetime
                    AccessSource.PREVIEW -> R.string.plus_status_preview
                    else -> R.string.plus_header_free_summary
                }), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
            verticalAlignment = Alignment.CenterVertically,
            shapes = segmentedListItemShapes(0, 1),
            colors = ListItemDefaults.segmentedColors(
                containerColor = CaffeineSurfaceDefaults.groupedListContainerColor,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingContentColor = MaterialTheme.colorScheme.primary,
                selectedSupportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                selectedTrailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.heightIn(min = 72.dp),
        ) {
            Text(
                stringResource(if (owned) R.string.plus_tier_name else R.string.plus_header_get),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
    }

    @Composable override fun PlusPage(showcase: Boolean, onDismiss: () -> Unit) {
        BackHandler(onBack = onDismiss)
        CaffeineProScreen(access, store, journey, onDismiss, showcase,
            onRequestPlusIcon = { (store as? RevenueCatBillingRepository)?.requestIconChange(true) })
    }

    /** Google Drive backup, free for every Play user (not a Plus feature). */
    @Composable override fun CloudBackupSettings(onRestore: (String) -> Unit) =
        com.uc.caffeine.cloud.CloudBackupSettingsSection(onRestore)

    @Composable override fun OnboardingRestore(onRestore: suspend (String) -> Boolean) =
        com.uc.caffeine.cloud.OnboardingDriveRestore(onRestore)

    @Composable override fun OnboardingPlusIntro(onContinue: () -> Unit, onSeePlans: () -> Unit) =
        PlusOnboardingIntro(onContinue, onSeePlans, onShown = journey::onboardingIntroShown)

    /** The full-screen app-icon switch, slid up over everything. */
    @Composable override fun Overlays() {
        val repository = store as? RevenueCatBillingRepository ?: return
        val transition by repository.iconTransition.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var last by remember { mutableStateOf(IconTransition.WELCOME) }
        transition?.let { last = it }
        AnimatedVisibility(
            visible = transition != null,
            enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
            exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
        ) {
            val toPlus = last == IconTransition.WELCOME || last == IconTransition.TO_PLUS
            IconTransitionScreen(
                kind = last,
                onRestartNow = { context.findActivity()?.let { IconSwitcher.restartNow(it, toPlus) } },
                onLater = {
                    IconSwitcher.applyWhenAppCloses(context, toPlus)
                    repository.dismissIconTransition()
                },
                onClose = repository::dismissIconTransition,
            )
        }
    }

    @Composable override fun aboutBrand(): AboutBrand {
        val snapshot by access.snapshots.collectAsStateWithLifecycle()
        return if (snapshot.hasVerifiedPurchase) AboutBrand(stringResource(R.string.plus_tier_name), R.mipmap.ic_launcher_plus)
        else AboutBrand(stringResource(R.string.app_name), R.mipmap.ic_launcher)
    }
    @Composable override fun FeatureSheet(feature: ProFeature, onSeePlans: () -> Unit, onDismiss: () -> Unit) = ProFeatureSheet(feature, onSeePlans, onDismiss)

    @Composable override fun AppIconSettings(onOpenPlus: () -> Unit) {
        val snapshot by access.snapshots.collectAsStateWithLifecycle()
        val repository = store as? RevenueCatBillingRepository ?: return
        if (!BillingVariantSupport.PLUS_SURFACES && !snapshot.hasVerifiedPurchase) return
        val transition by repository.iconTransition.collectAsStateWithLifecycle()
        AppIconPicker(
            owned = snapshot.hasVerifiedPurchase,
            refreshKey = transition,
            onRequest = repository::requestIconChange,
            onOpenPlus = onOpenPlus,
        )
    }

    override val hasDeveloperSettings: Boolean get() = BillingVariantSupport.HAS_DEVELOPER_SETTINGS
    @Composable override fun DeveloperSettings() = BillingVariantSupport.DemoControls(store, journey)

    @Composable override fun JourneySheet(prompt: JourneyPrompt, onSeePlans: () -> Unit) {
        val state by journey.state.collectAsStateWithLifecycle()
        when (prompt) {
            JourneyPrompt.TRIAL_OFFER -> TrialOfferSheet(
                openDays = state.openDays,
                onStart = journey::startTrial,
                onDismiss = { journey.promptHandled(JourneyPrompt.TRIAL_OFFER) },
            )
            JourneyPrompt.TRIAL_ENDED -> TrialEndedSheet(
                onSeePlans = { journey.promptHandled(JourneyPrompt.TRIAL_ENDED); onSeePlans() },
                onDismiss = { journey.promptHandled(JourneyPrompt.TRIAL_ENDED) },
            )
            JourneyPrompt.NONE, JourneyPrompt.SHOWCASE -> Unit
        }
    }

    @Composable override fun StatusCard(onOpen: () -> Unit) {
        val snapshot by access.snapshots.collectAsStateWithLifecycle()
        val kind = plusStatusCard(snapshot)
        val key = "$kind:${snapshot.previewEndsAtMillis}:${snapshot.paidExpiresAtMillis}"
        var dismissed by rememberSaveable(key) { mutableStateOf(store.statusDismissed) }
        // Where Plus is not on sale only a payment problem on an existing purchase is worth a card.
        if (kind == PlusStatusCard.NONE || dismissed || (!snapshot.plusOnSale && kind != PlusStatusCard.BILLING)) return
        val context = LocalContext.current
        val settingsRepo = remember(context) { SettingsRepository(context) }
        val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = settingsRepo.defaultSettings)
        val haptics = rememberAppHaptics()
        val problem = kind == PlusStatusCard.BILLING
        Column {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (problem) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlusBadge(size = 40.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(when (kind) {
                                    PlusStatusCard.BILLING -> R.string.plus_card_billing_title
                                    PlusStatusCard.PAID_ENDED -> R.string.plus_card_ended_title
                                    else -> R.string.plus_card_trial_title
                                }),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                            )
                            Text(
                                when (kind) {
                                    PlusStatusCard.BILLING -> stringResource(R.string.plus_status_billing)
                                    PlusStatusCard.PAID_ENDED -> stringResource(R.string.plus_status_expired)
                                    else -> stringResource(R.string.plus_card_trial_body,
                                        formatTimestampToDateTime(snapshot.previewEndsAtMillis ?: 0L, settings))
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = { haptics.toggle(); dismissed = true; store.dismissStatus() }, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.plus_dismiss_status))
                        }
                        FilledTonalButton(onClick = { haptics.toggle(); onOpen() }, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.plus_see_plans))
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

package com.uc.caffeine.monetization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.util.formatTimestampToDate
import com.uc.caffeine.util.formatTimestampToDateTime

@Composable
internal fun CaffeineProScreen(
    access: FeatureAccess,
    store: StoreCatalog,
    journey: PlusJourney,
    onBack: () -> Unit,
    showcase: Boolean = false,
    onRequestPlusIcon: () -> Unit = {},
) {
    val snapshot by access.snapshots.collectAsStateWithLifecycle()
    val catalog by store.snapshots.collectAsStateWithLifecycle()
    val journeyState by journey.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsRepo = remember(context) { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = settingsRepo.defaultSettings)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var dialog by rememberSaveable { mutableStateOf<PlusInfoDialog?>(null) }
    val haptics = rememberAppHaptics()
    val screen = ProScreenState.from(snapshot, catalog, selectedId)
    // An unknown country counts as not on sale, except while the store is still loading (the
    // country is fetched in that same refresh), so Indian users never flash the closed message.
    val region = (store as? RevenueCatBillingRepository)?.saleRegion?.collectAsStateWithLifecycle()?.value ?: SaleRegion.OPEN
    val model = PlusPageModel.from(snapshot, catalog, journeyState, screen, showcase, BillingVariantSupport.purchasesActive(store),
        saleRegionOpen = region == SaleRegion.OPEN ||
            (region == SaleRegion.UNKNOWN && catalog.availability == StoreAvailability.LOADING))
    LaunchedEffect(catalog.plans) {
        if (selectedId == null) selectedId = (catalog.plans.firstOrNull { it.duration == PlanDuration.YEARLY } ?: catalog.plans.firstOrNull())?.id
    }
    LifecycleResumeEffect(store) { store.refresh(); onPauseOrDispose {} }

    val onAction: (PlusAction) -> Unit = { action ->
        haptics.toggle()
        when (action) {
            PlusAction.REFRESH -> store.refresh()
            PlusAction.RESTORE -> store.restore()
            PlusAction.MANAGE -> {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/account/subscriptions?sku=pro&package=${context.packageName}"))) }
                    .onFailure { dialog = PlusInfoDialog.STORE_ERROR }
            }
        }
    }
    val chipText = model.chip?.let { chip ->
        if (chip.timestamp == null) stringResource(chip.text)
        else stringResource(chip.text, if (chip.includeTime) formatTimestampToDateTime(chip.timestamp, settings)
            else formatTimestampToDate(chip.timestamp, settings))
    }
    PlusPageContent(model, selectedId,
        PlusPageActions(
            onDismiss = { haptics.toggle(); onBack() },
            onSelect = { haptics.toggle(); selectedId = it },
            onPrimary = {
                haptics.toggle()
                when (val cta = model.cta) {
                    PlusCta.RefreshToContinue -> store.refresh()
                    is PlusCta.Subscribe -> context.findActivity()?.let { store.purchase(it, cta.plan.id) }
                    else -> Unit
                }
            },
            onStartTrial = { haptics.toggle(); journey.startTrial() },
            onAction = onAction,
            onInfo = { haptics.toggle(); dialog = it },
        ), chipText = chipText, activityAvailable = context.findActivity() != null,
        iconOffer = { AppIconOffer(onUsePlusIcon = onRequestPlusIcon) })
    dialog?.let { PlusInfoDialog(it) { haptics.toggle(); dialog = null } }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

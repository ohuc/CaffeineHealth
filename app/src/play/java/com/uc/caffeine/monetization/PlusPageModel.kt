package com.uc.caffeine.monetization

import androidx.annotation.StringRes
import com.uc.caffeine.R

internal enum class PlusLayout { PITCH, OWNER }
internal enum class PlusSeverity { NEUTRAL, POSITIVE, PROBLEM, PROGRESS }
internal enum class PlusAction { REFRESH, RESTORE, MANAGE }

internal data class PlusChip(@param:StringRes val text: Int, val timestamp: Long? = null, val includeTime: Boolean = false)
internal data class PlusStrip(
    @param:StringRes val text: Int,
    val severity: PlusSeverity,
    val actions: List<PlusAction> = emptyList(),
)
internal sealed interface PlusPlans {
    data object Loading : PlusPlans
    data class Ready(val plans: List<StorePlan>, val enabled: Boolean) : PlusPlans
}
internal sealed interface PlusCta {
    data class Subscribe(val plan: StorePlan) : PlusCta
    data class Disabled(@param:StringRes val label: Int, val progress: Boolean = false) : PlusCta
    data object RefreshToContinue : PlusCta
    data object None : PlusCta
}

internal data class PlusPageModel(
    val layout: PlusLayout,
    val chip: PlusChip?,
    val strip: PlusStrip?,
    val plans: PlusPlans?,
    val selectedPlan: StorePlan?,
    val cta: PlusCta,
    val freeDay: Boolean,
    val included: Boolean,
    val iconOffer: Boolean,
    val showRestore: Boolean,
    val restoreEnabled: Boolean,
    val showManage: Boolean,
    val refreshEnabled: Boolean,
    val previewPurchaseNote: Boolean,
    @param:StringRes val dismissLabel: Int,
    @param:StringRes val ownerSubtitle: Int,
) {
    val managementActions: List<PlusAction> get() = buildList {
        if (showManage) add(PlusAction.MANAGE)
        if (layout == PlusLayout.OWNER && showRestore) add(PlusAction.RESTORE)
    }

    companion object {
        fun from(
            access: AccessSnapshot,
            store: StoreSnapshot,
            journey: JourneyState,
            screen: ProScreenState,
            showcase: Boolean,
            purchasesActive: Boolean,
            saleRegionOpen: Boolean = true,
        ): PlusPageModel {
            val verificationFailed = !store.ownershipLoaded && store.message == StoreMessage.VERIFYING &&
                store.operation == StoreOperation.IDLE && store.availability != StoreAvailability.LOADING
            val loading = store.availability == StoreAvailability.LOADING ||
                (!store.ownershipLoaded && store.availability == StoreAvailability.AVAILABLE && !verificationFailed)
            val owner = access.hasVerifiedPurchase || (!screen.showPlans && store.ownershipLoaded)
            val layout = if (owner) PlusLayout.OWNER else PlusLayout.PITCH
            val paidConfirmed = access.hasVerifiedPurchase &&
                access.verification !in setOf(VerificationStatus.ERROR, VerificationStatus.UNVERIFIED)
            // Before the first catalog response this flag is still false, even in an enabled build.
            val active = purchasesActive && saleRegionOpen &&
                (store.purchasesEnabled || loading || store.availability != StoreAvailability.AVAILABLE)
            val restoreEnabled = store.availability !in setOf(StoreAvailability.NOT_CONFIGURED, StoreAvailability.LOADING, StoreAvailability.NOT_SUPPORTED) &&
                store.operation !in setOf(StoreOperation.PURCHASING, StoreOperation.RESTORING)
            val showRestore = active || owner
            val plans = when {
                owner || !active -> null
                loading -> PlusPlans.Loading
                store.availability != StoreAvailability.AVAILABLE || !screen.showPlans -> null
                else -> PlusPlans.Ready(store.plans.sortedBy { it.duration.displayOrder() }, store.operation == StoreOperation.IDLE)
            }
            val cta = when {
                owner || !active -> PlusCta.None
                loading -> PlusCta.Disabled(R.string.plus_cta_loading, true)
                store.availability != StoreAvailability.AVAILABLE -> PlusCta.None
                store.operation != StoreOperation.IDLE -> PlusCta.Disabled(store.operation.actionLabel(), store.operation != StoreOperation.PENDING)
                screen.canPurchase && screen.selectedPlan != null -> PlusCta.Subscribe(screen.selectedPlan)
                verificationFailed || access.verification != VerificationStatus.VERIFIED -> PlusCta.RefreshToContinue
                else -> PlusCta.Disabled(R.string.plus_cta_choose)
            }
            val chip = if (showcase || (owner && !paidConfirmed)) null else when (access.source) {
                AccessSource.PREVIEW -> access.previewEndsAtMillis?.let { PlusChip(R.string.plus_preview_until, it, true) }
                    ?: PlusChip(R.string.plus_status_preview)
                AccessSource.LIFETIME -> PlusChip(R.string.plus_status_lifetime)
                AccessSource.SUBSCRIPTION -> when {
                    !access.willRenew -> access.paidExpiresAtMillis?.let { PlusChip(R.string.plus_status_canceled_until, it) }
                        ?: PlusChip(R.string.plus_status_canceled)
                    else -> access.paidExpiresAtMillis?.let { PlusChip(R.string.plus_status_until, it) }
                        ?: PlusChip(R.string.plus_status_active)
                }
                else -> null
            }
            val strip = when {
                store.operation != StoreOperation.IDLE -> PlusStrip(store.operation.noticeLabel(),
                    if (store.operation == StoreOperation.PENDING) PlusSeverity.NEUTRAL else PlusSeverity.PROGRESS)
                access.billingIssue -> PlusStrip(R.string.plus_status_billing, PlusSeverity.PROBLEM,
                    if (access.manageSubscription) listOf(PlusAction.MANAGE) else listOf(PlusAction.REFRESH))
                store.message in setOf(StoreMessage.NETWORK, StoreMessage.STORE, StoreMessage.NOT_ALLOWED, StoreMessage.UNKNOWN) ->
                    PlusStrip(checkNotNull(store.message.plusResource()), PlusSeverity.PROBLEM, listOf(PlusAction.REFRESH))
                store.message in setOf(StoreMessage.RESTORED, StoreMessage.NOTHING_TO_RESTORE, StoreMessage.SUCCESS) ->
                    PlusStrip(checkNotNull(store.message.plusResource()),
                        if (store.message == StoreMessage.NOTHING_TO_RESTORE) PlusSeverity.NEUTRAL else PlusSeverity.POSITIVE)
                access.source == AccessSource.LIFETIME && access.willRenew ->
                    PlusStrip(R.string.plus_mixed_ownership, PlusSeverity.PROBLEM,
                        if (access.manageSubscription) listOf(PlusAction.MANAGE) else listOf(PlusAction.REFRESH))
                (owner && !paidConfirmed) || verificationFailed -> PlusStrip(R.string.plus_status_unverified, PlusSeverity.PROBLEM,
                    listOf(PlusAction.REFRESH) + if (showRestore) listOf(PlusAction.RESTORE) else emptyList())
                !active -> PlusStrip(if (purchasesActive && !saleRegionOpen) R.string.plus_region_closed
                    else R.string.plus_release_off_neutral, PlusSeverity.NEUTRAL)
                store.availability in setOf(StoreAvailability.NOT_CONFIGURED, StoreAvailability.UNAVAILABLE, StoreAvailability.NOT_SUPPORTED) ->
                    PlusStrip(if (store.availability == StoreAvailability.NOT_SUPPORTED) R.string.plus_store_not_supported
                        else R.string.plus_no_configuration, PlusSeverity.NEUTRAL, listOf(PlusAction.REFRESH))
                access.expired && !showcase && !owner -> PlusStrip(R.string.plus_status_expired, PlusSeverity.NEUTRAL)
                else -> null
            }
            return PlusPageModel(
                layout = layout, chip = chip, strip = strip, plans = plans, selectedPlan = screen.selectedPlan,
                cta = cta, freeDay = journey.canStartTrial && !showcase && !owner,
                included = paidConfirmed, iconOffer = paidConfirmed,
                showRestore = showRestore, restoreEnabled = restoreEnabled,
                showManage = screen.showManagement,
                refreshEnabled = store.availability != StoreAvailability.LOADING &&
                    store.operation !in setOf(StoreOperation.PURCHASING, StoreOperation.RESTORING),
                previewPurchaseNote = access.source == AccessSource.PREVIEW,
                dismissLabel = when {
                    showcase && !owner -> R.string.plus_continue
                    access.source == AccessSource.PREVIEW -> R.string.plus_keep_preview
                    owner -> R.string.plus_back
                    else -> R.string.plus_continue
                },
                ownerSubtitle = when {
                    !paidConfirmed -> R.string.plus_short_unverified
                    access.source == AccessSource.LIFETIME -> R.string.plus_short_lifetime
                    else -> R.string.plus_short_active
                },
            )
        }
    }
}

private fun PlanDuration.displayOrder() = when (this) {
    PlanDuration.YEARLY -> 0
    PlanDuration.MONTHLY -> 1
    PlanDuration.LIFETIME -> 2
}
internal fun PlanDuration.plusLabelResource() = when (this) {
    PlanDuration.MONTHLY -> R.string.plus_monthly
    PlanDuration.YEARLY -> R.string.plus_yearly
    PlanDuration.LIFETIME -> R.string.plus_lifetime
}
internal fun PlanDuration.plusCaptionResource() = when (this) {
    PlanDuration.MONTHLY -> R.string.plus_plan_caption_month
    PlanDuration.YEARLY -> R.string.plus_plan_caption_year
    PlanDuration.LIFETIME -> R.string.plus_plan_caption_once
}
internal fun PlanDuration.plusTermsResource() = when (this) {
    PlanDuration.MONTHLY -> R.string.plus_monthly_terms
    PlanDuration.YEARLY -> R.string.plus_yearly_terms
    PlanDuration.LIFETIME -> R.string.plus_lifetime_terms
}
internal fun PlanDuration.plusActionResource() = when (this) {
    PlanDuration.MONTHLY -> R.string.plus_subscribe_monthly
    PlanDuration.YEARLY -> R.string.plus_subscribe_yearly
    PlanDuration.LIFETIME -> R.string.plus_buy_lifetime
}
private fun StoreOperation.actionLabel() = when (this) {
    StoreOperation.PURCHASING -> R.string.plus_purchase_loading
    StoreOperation.RESTORING -> R.string.plus_restore_loading
    StoreOperation.VERIFYING -> R.string.plus_cta_confirming
    StoreOperation.PENDING -> R.string.plus_cta_pending
    StoreOperation.IDLE -> error("Idle operation has no busy label")
}
private fun StoreOperation.noticeLabel() = when (this) {
    StoreOperation.VERIFYING -> R.string.plus_verifying
    StoreOperation.PENDING -> R.string.plus_pending
    else -> actionLabel()
}
internal fun StoreMessage.plusResource(): Int? = when (this) {
    StoreMessage.NONE -> null
    StoreMessage.DISABLED -> R.string.plus_release_disabled
    StoreMessage.NETWORK -> R.string.plus_network_error
    StoreMessage.STORE -> R.string.plus_store_error
    StoreMessage.NOT_ALLOWED -> R.string.plus_not_allowed
    StoreMessage.UNAVAILABLE -> R.string.plus_no_configuration
    StoreMessage.VERIFYING -> R.string.plus_verifying
    StoreMessage.PENDING -> R.string.plus_pending
    StoreMessage.RESTORED -> R.string.plus_restored
    StoreMessage.NOTHING_TO_RESTORE -> R.string.plus_nothing_restored
    StoreMessage.SUCCESS -> R.string.plus_success
    StoreMessage.UNKNOWN -> R.string.plus_unknown_error
}

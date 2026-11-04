package com.uc.caffeine.monetization

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.SubscriptionOption
import com.uc.caffeine.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDate

/** Debug-build demo overrides for the owner's UI review; release ignores them. */
internal enum class DemoAccess { FREE, SUBSCRIBED, LIFETIME, BILLING_ISSUE, PAID_ENDED }
internal data class DemoState(
    val policy: Boolean = false,
    val access: DemoAccess? = null,
    val sampleStore: Boolean = false,
    val openDays: Int = 0,
    val outsideRegion: Boolean = false,
    val storefrontCountry: String? = null,
    val saleCountries: String = "",
)

/** Which full-screen app-icon screen to show, if any. */
internal enum class IconTransition { WELCOME, TO_PLUS, TO_CLASSIC, PLUS_ENDED }

/** The only SDK boundary. No health history, identifiers, or purchase tokens leave through app code. */
internal class RevenueCatBillingRepository(
    private val context: Context,
    private val access: PlayFeatureAccess,
) : StoreCatalog, PlusJourney {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val storeState = MutableStateFlow(StoreSnapshot(StoreAvailability.NOT_CONFIGURED))
    override val snapshots = storeState.asStateFlow()
    private val journeyState = MutableStateFlow(JourneyState())
    override val state = journeyState.asStateFlow()
    private val iconState = MutableStateFlow<IconTransition?>(null)
    internal val iconTransition = iconState.asStateFlow()
    private val demo = MutableStateFlow(DemoState())
    internal val demoState = demo.asStateFlow()
    private lateinit var privateState: PlayPrivateState
    private val loaded = CompletableDeferred<Unit>()
    @Volatile private var grant: PaidGrant? = null
    @Volatile private var trial: PreviewRecord? = null
    @Volatile private var journeyRecord = JourneyRecord()
    @Volatile private var refreshFailed = false
    @Volatile private var demoPolicy = false
    @Volatile private var demoAccess: DemoAccess? = null
    @Volatile private var demoStore = false
    @Volatile private var demoOutsideRegion = false
    @Volatile private var storefrontCountry: String? = null
    @Volatile private var remoteSaleCountries: String? = null
    private val regionState = MutableStateFlow(SaleRegion.UNKNOWN)
    /** Whether Plus is on sale to this user's Google Play country; see [PlusSaleRegion]. */
    internal val saleRegion = regionState.asStateFlow()
    private var refreshing = false
    private var packages = emptyMap<String, com.revenuecat.purchases.Package>()
    private var options = emptyMap<String, SubscriptionOption>()
    private var sdk: Purchases? = null
    private var dismissed = false
    override val statusDismissed: Boolean get() = dismissed
    private val saleCountries: String
        get() = PlusSaleRegion.effectiveSpec(remoteSaleCountries, BuildConfig.PLUS_SALE_COUNTRIES)
    /** Plus locks: the same everywhere, on sale or not. */
    private val policyEnabled: Boolean get() = BuildConfig.PLUS_POLICY_ENABLED || demoPolicy
    /**
     * Anything that advertises Plus (setup intro, showcase, free-day offer and follow-up) runs only
     * where Plus can actually be bought. Unknown country counts as not on sale.
     */
    private val promotionEnabled: Boolean get() = policyEnabled && regionState.value == SaleRegion.OPEN
    private val purchasesEnabled: Boolean
        get() = BuildConfig.PLUS_PURCHASES_ENABLED && regionState.value == SaleRegion.OPEN

    private fun currentRegion(): SaleRegion = when {
        BuildConfig.DEBUG && demoOutsideRegion -> SaleRegion.CLOSED
        // Debug review without a RevenueCat key (or with the sample store) has no real store country.
        BuildConfig.DEBUG && (sdk == null || demoStore) -> SaleRegion.OPEN
        else -> PlusSaleRegion.resolve(saleCountries, storefrontCountry)
    }

    init {
        (context.applicationContext as? android.app.Application)?.let(IconSwitcher::install)
        access.beginLoading()
        access.currentSnapshot = { resolveAccess(System.currentTimeMillis()) }
        val key = BuildConfig.RC_PUBLIC_KEY
        if (key.isNotBlank()) {
            synchronized(RevenueCatBillingRepository::class.java) {
                if (!Purchases.isConfigured) {
                    Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.WARN else LogLevel.ERROR
                    Purchases.configure(
                        PurchasesConfiguration.Builder(context.applicationContext, key)
                            .appUserID(null)
                            .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
                            .automaticDeviceIdentifierCollectionEnabled(false)
                            .diagnosticsEnabled(false)
                            .showInAppMessagesAutomatically(false)
                            .build(),
                    )
                }
                sdk = Purchases.sharedInstance
            }
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                privateState = PlayPrivateState(File(context.noBackupFilesDir,
                    if (BuildConfig.DEBUG) "plus-debug/access.properties" else "plus/access.properties"))
                grant = privateState.grant
                trial = runCatching { privateState.trial(System.currentTimeMillis()) }.getOrNull()
                journeyRecord = privateState.journey
                storefrontCountry = privateState.storefrontCountry
                remoteSaleCountries = privateState.saleCountries
                if (BuildConfig.DEBUG) {
                    demoOutsideRegion = privateState.demoValue("outsideRegion").toBoolean()
                    // Debug builds start with the Plus rules on, so a fresh install behaves like
                    // a free Play user. The developer page can still switch them off (saved as "false").
                    demoPolicy = privateState.demoValue("policy")?.toBoolean() ?: true
                    demoAccess = privateState.demoValue("access")?.let { runCatching { DemoAccess.valueOf(it) }.getOrNull() }
                    demoStore = privateState.demoValue("store").toBoolean()
                }
            }
            if (privateState.pending) storeState.value = storeState.value.copy(operation = StoreOperation.PENDING, message = StoreMessage.PENDING)
            publishAccess()
            loaded.complete(Unit)
            access.finishLoading()
            sdk?.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
                scope.launch { accept(info) }
            }
            refresh()
            while (isActive) {
                delay(60_000L)
                trial = withContext(Dispatchers.IO) {
                    runCatching { privateState.trial(System.currentTimeMillis()) }.getOrDefault(trial)
                }
                publishAccess()
            }
        }
    }

    private fun resolveAccess(nowMillis: Long): AccessSnapshot {
        val real = AccessPolicy.resolve(grant, trial, nowMillis, refreshFailed)
        if (!BuildConfig.DEBUG) return real
        return demoAccess?.let { demoSnapshot(it, nowMillis) }
            ?: if (demoStore) real.copy(verification = VerificationStatus.VERIFIED) else real
    }

    private fun publishAccess() {
        val now = System.currentTimeMillis()
        regionState.value = currentRegion()
        if (!(BuildConfig.DEBUG && demoStore) && storeState.value.availability == StoreAvailability.AVAILABLE &&
            storeState.value.purchasesEnabled != purchasesEnabled) {
            storeState.value = storeState.value.copy(purchasesEnabled = purchasesEnabled)
        }
        access.enforceLocks = policyEnabled
        access.update(resolveAccess(now).copy(
            paymentPending = storeState.value.operation == StoreOperation.PENDING,
            locksEnforced = policyEnabled,
            plusOnSale = regionState.value == SaleRegion.OPEN,
        ))
        dismissed = privateState.isStatusDismissed(statusKey())
        publishJourney(now)
    }

    private fun publishJourney(nowMillis: Long) {
        val paid = access.snapshots.value.hasVerifiedPurchase
        journeyState.value = JourneyState(
            prompt = PlusJourneyPolicy.prompt(journeyRecord, trial, nowMillis, paid, promotionEnabled),
            canStartTrial = PlusJourneyPolicy.canStartTrial(journeyRecord, trial, paid, promotionEnabled),
            openDays = journeyRecord.openDays,
            onboardingIntroEligible = PlusJourneyPolicy.onboardingIntroEligible(journeyRecord, trial, nowMillis, paid, promotionEnabled),
        )
        if (BuildConfig.DEBUG) demo.value = DemoState(demoPolicy, demoAccess, demoStore, journeyRecord.openDays,
            demoOutsideRegion, storefrontCountry, saleCountries)
    }

    private fun statusKey(): String = when {
        access.snapshots.value.billingIssue -> "billing:${grant?.verifiedAtMillis}"
        access.snapshots.value.expired -> "expired:${trial?.endsAtMillis}:${grant?.expiresAtMillis}"
        else -> "trial:${trial?.endsAtMillis}"
    }

    /** Runs after local state has loaded, so early lifecycle calls are never lost. */
    private fun afterLoad(block: suspend () -> Unit) {
        scope.launch { loaded.await(); block() }
    }

    private suspend fun saveJourney(record: JourneyRecord) {
        journeyRecord = record
        withContext(Dispatchers.IO) { runCatching { privateState.saveJourney(record) } }
    }

    override fun recordAppOpen() = afterLoad {
        val now = System.currentTimeMillis()
        trial = withContext(Dispatchers.IO) { runCatching { privateState.trial(now) }.getOrDefault(trial) }
        saveJourney(PlusJourneyPolicy.recordOpen(journeyRecord, LocalDate.now().toEpochDay(), trial, now))
        publishAccess()
        // Plus ended while the Plus icon is in use: show the countdown back to the classic icon.
        if (!access.snapshots.value.hasVerifiedPurchase) {
            if (IconSwitcher.pending(context) == true) IconSwitcher.clearPending(context)
            if (AppIconManager.isPlusIcon(context)) iconState.value = IconTransition.PLUS_ENDED
        }
    }

    override fun onboardingCompleted() = afterLoad {
        saveJourney(PlusJourneyPolicy.onboardingCompleted(journeyRecord, promotionEnabled))
        publishAccess()
    }

    /** The setup introduction replaces the post-setup showcase, so mark the showcase as shown. */
    override fun onboardingIntroShown() = afterLoad {
        saveJourney(PlusJourneyPolicy.handled(journeyRecord, JourneyPrompt.SHOWCASE))
        publishAccess()
    }

    override fun promptHandled(prompt: JourneyPrompt) = afterLoad {
        saveJourney(PlusJourneyPolicy.handled(journeyRecord, prompt))
        publishAccess()
    }

    override fun startTrial() = afterLoad {
        val paid = access.snapshots.value.hasVerifiedPurchase
        if (!PlusJourneyPolicy.canStartTrial(journeyRecord, trial, paid, promotionEnabled)) return@afterLoad
        trial = withContext(Dispatchers.IO) { privateState.startTrial(System.currentTimeMillis()) }
        saveJourney(PlusJourneyPolicy.handled(journeyRecord, JourneyPrompt.TRIAL_OFFER))
        publishAccess()
    }

    override fun dismissStatus() {
        dismissed = true
        scope.launch(Dispatchers.IO) { privateState.dismissStatus(statusKey()) }
    }

    override fun refresh() {
        if (BuildConfig.DEBUG && demoStore) {
            storeState.value = sampleStore()
            return
        }
        val client = sdk ?: return
        if (!::privateState.isInitialized || refreshing || storeState.value.operation in setOf(StoreOperation.PURCHASING, StoreOperation.RESTORING)) return
        refreshing = true
        storeState.value = storeState.value.copy(availability = StoreAvailability.LOADING, plans = emptyList(), ownershipLoaded = false)
        packages = emptyMap()
        options = emptyMap()
        scope.launch {
            try {
                // The Play country decides where Plus is sold. Failure keeps the last known country.
                runCatching { client.awaitStorefrontCountryCode() }.getOrNull()
                    ?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
                    ?.let { country ->
                        storefrontCountry = country
                        withContext(Dispatchers.IO) { runCatching { privateState.setStorefrontCountry(country) } }
                    }
                if (!privateState.autoRestoreDone) {
                    // Purchases belong to the Google Play account, but a fresh install starts with a new
                    // anonymous RevenueCat ID that owns nothing. Restore once per install, silently, so a
                    // reinstall or new phone gets Plus back without an account or a tap. A failure
                    // (offline) retries on the next refresh; the visible Restore button stays as a fallback.
                    runCatching { client.awaitRestore() }.getOrNull()?.let { info ->
                        accept(info)
                        withContext(Dispatchers.IO) { runCatching { privateState.setAutoRestoreDone() } }
                    }
                }
                accept(client.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT))
                val offering = client.awaitOfferings().current
                if (offering != null) {
                    // Dashboard override of the build's sale countries; removing the key returns to the build default.
                    val remote = offering.getMetadataString(PlusSaleRegion.METADATA_KEY, "").trim().ifBlank { null }
                    remoteSaleCountries = remote
                    withContext(Dispatchers.IO) { runCatching { privateState.setSaleCountries(remote) } }
                }
                regionState.value = currentRegion()
                val available = offering?.availablePackages.orEmpty()
                val mapped = available.mapNotNull { pkg ->
                    val product = pkg.product
                    if (product.type !in setOf(ProductType.SUBS, ProductType.INAPP)) return@mapNotNull null
                    // Never let an eligible Play trial silently replace the local no-card free day.
                    val debugBase = BillingVariantSupport.baseOption(product)
                    val base = product.subscriptionOptions?.firstOrNull {
                        it.id == product.id.substringAfter(':') && it.isBasePlan && !it.isPrepaid
                    } ?: debugBase
                    val plan = catalogPlan(pkg.identifier, product.id, product.price.formatted,
                        product.type == ProductType.SUBS,
                        if (debugBase != null) product.id.substringAfter(':') else base?.id,
                        billingPeriod = base?.billingPeriod?.iso8601,
                    ) ?: return@mapNotNull null
                    if (base != null) options = options + (pkg.identifier to base)
                    packages = packages + (pkg.identifier to pkg)
                    plan
                }.distinctBy { it.duration }
                storeState.value = storeState.value.copy(
                    availability = if (mapped.isEmpty()) StoreAvailability.UNAVAILABLE else StoreAvailability.AVAILABLE,
                    plans = mapped,
                    purchasesEnabled = purchasesEnabled,
                )
                publishAccess()
            } catch (error: PurchasesException) {
                refreshFailed = true
                storeState.value = storeState.value.copy(availability = StoreAvailability.UNAVAILABLE, plans = emptyList(), message = messageFor(error.error.code))
                publishAccess()
            } finally {
                refreshing = false
            }
        }
    }

    private suspend fun accept(info: CustomerInfo): Boolean {
        // On-device fallback cannot establish new paid ownership; keep the last server-verified grant.
        if (info.entitlements.verification != VerificationResult.VERIFIED) {
            refreshFailed = true
            storeState.value = storeState.value.copy(message = StoreMessage.VERIFYING, ownershipLoaded = false)
            publishAccess()
            return false
        }
        val entitlement = info.entitlements["pro"]
        if (!BuildConfig.DEBUG && entitlement?.isSandbox == true) return false
        val subscription = info.subscriptionsByProductIdentifier.values.filter { it.productIdentifier.substringBefore(':') == "pro" }
        val incoming = PaidGrant(
            active = entitlement?.isActive == true,
            lifetime = entitlement?.productIdentifier == "pro_lifetime",
            verifiedAtMillis = info.requestDate.time,
            expiresAtMillis = entitlement?.expirationDate?.let { expiry -> maxOf(expiry.time,
                subscription.filter { it.isActive }.mapNotNull { it.gracePeriodExpiresDate?.time }.maxOrNull() ?: 0L) },
            hasSubscription = subscription.isNotEmpty(),
            willRenew = subscription.any { it.isActive && it.willRenew },
            billingIssue = entitlement?.billingIssueDetectedAt != null || subscription.any { it.billingIssuesDetectedAt != null },
            previouslyOwned = entitlement != null || grant?.previouslyOwned == true || subscription.isNotEmpty(),
            activeSubscription = subscription.any { it.isActive },
        )
        val accepted = AccessPolicy.acceptGrant(grant, incoming, verified = true)
        if (accepted != incoming) return false
        grant = accepted
        withContext(Dispatchers.IO) {
            runCatching {
                privateState.saveGrant(incoming)
                if (incoming.active) privateState.setPending(false)
            }
        }
        refreshFailed = false
        storeState.value = storeState.value.copy(ownershipLoaded = true,
            operation = if (incoming.active) StoreOperation.IDLE else storeState.value.operation)
        publishAccess()
        return true
    }

    override fun purchase(activity: Activity, planId: String) {
        if (BuildConfig.DEBUG && demoStore) return demoPurchase(planId)
        val client = sdk ?: return
        if (currentRegion() != SaleRegion.OPEN) return
        if (refreshing || !ProScreenState.from(access.snapshots.value, storeState.value, planId).canPurchase) return
        val pkg = packages[planId] ?: return
        storeState.value = storeState.value.copy(operation = StoreOperation.PURCHASING, message = StoreMessage.NONE)
        scope.launch {
            try {
                // Recheck ownership before checkout, including subscriptions masked by Lifetime.
                if (!accept(client.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)) || access.snapshots.value.hasVerifiedPurchase || access.snapshots.value.activeSubscription) {
                    storeState.value = storeState.value.copy(operation = StoreOperation.IDLE)
                    return@launch
                }
                val params = options[planId]?.let { PurchaseParams.Builder(activity, it) }
                    ?: PurchaseParams.Builder(activity, pkg)
                val result = client.awaitPurchase(params.build())
                storeState.value = storeState.value.copy(operation = StoreOperation.VERIFYING)
                val verified = accept(result.customerInfo)
                storeState.value = storeState.value.copy(
                    operation = if (verified) StoreOperation.IDLE else StoreOperation.VERIFYING,
                    message = if (verified && access.snapshots.value.hasVerifiedPurchase) StoreMessage.SUCCESS else StoreMessage.VERIFYING,
                )
                if (verified && access.snapshots.value.hasVerifiedPurchase) offerPlusIcon()
            } catch (error: PurchasesTransactionException) {
                when {
                    error.userCancelled -> storeState.value = storeState.value.copy(operation = StoreOperation.IDLE, message = StoreMessage.NONE)
                    error.error.code == PurchasesErrorCode.PaymentPendingError -> {
                        withContext(Dispatchers.IO) { runCatching { privateState.setPending(true) } }
                        storeState.value = storeState.value.copy(operation = StoreOperation.PENDING, message = StoreMessage.PENDING)
                    }
                    error.error.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> {
                        storeState.value = storeState.value.copy(operation = StoreOperation.IDLE)
                        refresh()
                    }
                    else -> storeState.value = storeState.value.copy(operation = StoreOperation.IDLE, message = messageFor(error.error.code))
                }
            } catch (error: PurchasesException) {
                refreshFailed = true
                storeState.value = storeState.value.copy(operation = StoreOperation.IDLE, message = messageFor(error.error.code))
            } finally { publishAccess() }
        }
    }

    override fun restore() {
        if (BuildConfig.DEBUG && demoStore) {
            storeState.value = storeState.value.copy(message =
                if (access.snapshots.value.hasVerifiedPurchase) StoreMessage.RESTORED else StoreMessage.NOTHING_TO_RESTORE)
            return
        }
        val client = sdk ?: return
        if (refreshing || storeState.value.operation !in setOf(StoreOperation.IDLE, StoreOperation.PENDING, StoreOperation.VERIFYING)) return
        storeState.value = storeState.value.copy(operation = StoreOperation.RESTORING, message = StoreMessage.NONE)
        scope.launch {
            try {
                val verified = accept(client.awaitRestore())
                if (verified) withContext(Dispatchers.IO) { runCatching { privateState.setPending(false) } }
                storeState.value = storeState.value.copy(message = when {
                    !verified -> StoreMessage.VERIFYING
                    access.snapshots.value.hasVerifiedPurchase -> StoreMessage.RESTORED
                    else -> StoreMessage.NOTHING_TO_RESTORE
                })
                if (verified && access.snapshots.value.hasVerifiedPurchase) offerPlusIcon()
            } catch (error: PurchasesException) {
                refreshFailed = true
                storeState.value = storeState.value.copy(message = messageFor(error.error.code))
            } finally {
                storeState.value = storeState.value.copy(operation = if (privateState.pending) StoreOperation.PENDING else StoreOperation.IDLE)
                publishAccess()
            }
        }
    }

    /** After buying or restoring Plus: welcome screen offering the Plus icon (once it is not in use). */
    private fun offerPlusIcon() {
        if (!AppIconManager.isPlusIcon(context)) iconState.value = IconTransition.WELCOME
    }

    /** Settings asks to change the icon; the full-screen screen explains the restart first. */
    internal fun requestIconChange(plus: Boolean) {
        if (plus && !access.snapshots.value.hasVerifiedPurchase) return
        iconState.value = if (plus) IconTransition.TO_PLUS else IconTransition.TO_CLASSIC
    }

    internal fun dismissIconTransition() { iconState.value = null }

    // ---- Debug-build demo controls. Every entry point is a no-op in release. ----

    internal fun demoShowIconScreen(kind: IconTransition) {
        if (BuildConfig.DEBUG) iconState.value = kind
    }

    private fun demoSnapshot(kind: DemoAccess, nowMillis: Long): AccessSnapshot {
        val day = 86_400_000L
        val free = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED, verifiedAtMillis = nowMillis)
        val subscribed = free.copy(source = AccessSource.SUBSCRIPTION, paidExpiresAtMillis = nowMillis + 30 * day,
            manageSubscription = true, willRenew = true, activeSubscription = true)
        return when (kind) {
            DemoAccess.FREE -> free
            DemoAccess.SUBSCRIBED -> subscribed
            DemoAccess.LIFETIME -> free.copy(source = AccessSource.LIFETIME)
            DemoAccess.BILLING_ISSUE -> subscribed.copy(billingIssue = true)
            DemoAccess.PAID_ENDED -> free.copy(expired = true, paidExpiresAtMillis = nowMillis - day, manageSubscription = true)
        }
    }

    private fun sampleStore() = StoreSnapshot(
        availability = StoreAvailability.AVAILABLE,
        plans = listOf(
            StorePlan("demo_monthly", PlanDuration.MONTHLY, "₹99.00"),
            StorePlan("demo_yearly", PlanDuration.YEARLY, "₹599.00"),
            StorePlan("demo_lifetime", PlanDuration.LIFETIME, "₹1,299.00"),
        ),
        ownershipLoaded = true,
        purchasesEnabled = true,
        operation = StoreOperation.IDLE,
        message = storeState.value.message,
    )

    private fun demoPurchase(planId: String) {
        if (!ProScreenState.from(access.snapshots.value, storeState.value, planId).canPurchase) return
        storeState.value = storeState.value.copy(operation = StoreOperation.PURCHASING, message = StoreMessage.NONE)
        scope.launch {
            delay(900L)
            storeState.value = storeState.value.copy(operation = StoreOperation.VERIFYING)
            delay(700L)
            demoAccess = if (planId == "demo_lifetime") DemoAccess.LIFETIME else DemoAccess.SUBSCRIBED
            privateState.setDemoValue("access", demoAccess?.name)
            publishAccess()
            storeState.value = storeState.value.copy(operation = StoreOperation.IDLE, message = StoreMessage.SUCCESS)
            offerPlusIcon()
        }
    }

    internal fun setDemoPolicy(enabled: Boolean) = demoChange {
        demoPolicy = enabled
        privateState.setDemoValue("policy", enabled.toString())
    }

    internal fun setDemoAccess(kind: DemoAccess?) = demoChange {
        demoAccess = kind
        privateState.setDemoValue("access", kind?.name)
    }

    internal fun setDemoStore(enabled: Boolean) = demoChange {
        demoStore = enabled
        privateState.setDemoValue("store", enabled.toString().takeIf { enabled })
        storeState.value = if (enabled) sampleStore() else StoreSnapshot(StoreAvailability.NOT_CONFIGURED)
        if (!enabled) refresh()
    }

    internal fun setDemoOutsideRegion(outside: Boolean) = demoChange {
        demoOutsideRegion = outside
        privateState.setDemoValue("outsideRegion", outside.toString().takeIf { outside })
    }

    internal fun demoReplayShowcase() = demoChange {
        enableDemoPolicy()
        // A pretend purchase hides the showcase, exactly like a real one would.
        if (demoAccess in setOf(DemoAccess.SUBSCRIBED, DemoAccess.LIFETIME, DemoAccess.BILLING_ISSUE)) {
            demoAccess = null
            privateState.setDemoValue("access", null)
        }
        saveJourney(journeyRecord.copy(showcasePending = true, showcaseShown = false))
    }

    internal fun demoAddOpenDay() = demoChange {
        enableDemoPolicy()
        saveJourney(journeyRecord.copy(openDays = journeyRecord.openDays + 1, lastOpenEpochDay = LocalDate.now().toEpochDay()))
    }

    internal fun demoEndTrialNow() = demoChange {
        enableDemoPolicy()
        val now = System.currentTimeMillis()
        privateState.setTrialForDemo(now - AccessPolicy.PREVIEW_MILLIS - 60_000L, now)
        trial = privateState.trial(now)
        saveJourney(journeyRecord.copy(offerShown = true, endedPromptDue = true, endedPromptShown = false))
    }

    internal fun demoResetJourney() = demoChange {
        privateState.setTrialForDemo(null, System.currentTimeMillis())
        trial = null
        saveJourney(JourneyRecord())
        demoAccess = null
        privateState.setDemoValue("access", null)
    }

    /** Journey demos need the Plus rules; the prompts never appear while they are off. */
    private fun enableDemoPolicy() {
        if (demoPolicy) return
        demoPolicy = true
        privateState.setDemoValue("policy", "true")
    }

    private fun demoChange(block: suspend () -> Unit) {
        if (!BuildConfig.DEBUG) return
        // Debug-only, tiny private file: run on the main scope so store refreshes stay on one thread.
        afterLoad {
            block()
            publishAccess()
        }
    }
}

internal fun messageFor(code: PurchasesErrorCode): StoreMessage = when (code) {
    PurchasesErrorCode.NetworkError -> StoreMessage.NETWORK
    PurchasesErrorCode.StoreProblemError -> StoreMessage.STORE
    PurchasesErrorCode.PurchaseNotAllowedError -> StoreMessage.NOT_ALLOWED
    PurchasesErrorCode.ProductNotAvailableForPurchaseError, PurchasesErrorCode.ConfigurationError,
    PurchasesErrorCode.InvalidCredentialsError -> StoreMessage.UNAVAILABLE
    PurchasesErrorCode.PaymentPendingError -> StoreMessage.PENDING
    else -> StoreMessage.UNKNOWN
}

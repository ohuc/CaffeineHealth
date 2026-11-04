package com.uc.caffeine.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred

class PlayFeatureAccess(enforceLocks: Boolean = false) : FeatureAccess {
    /** Compile-time policy; debug builds may also switch it on from the Plus demo controls. */
    @Volatile internal var enforceLocks = enforceLocks
    private val state = MutableStateFlow(
        AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.UNVERIFIED),
    )
    override val snapshots = state.asStateFlow()
    internal var currentSnapshot: (() -> AccessSnapshot)? = null
    private var ready = CompletableDeferred(Unit)
    internal fun beginLoading() { ready = CompletableDeferred() }
    internal fun finishLoading() { ready.complete(Unit) }
    override suspend fun awaitReady() { ready.await() }

    internal fun update(snapshot: AccessSnapshot) { state.value = snapshot }
    override fun canUse(feature: ProFeature): Boolean = FeaturePolicy.canUse(currentSnapshot?.invoke() ?: state.value, feature, enforceLocks)
}

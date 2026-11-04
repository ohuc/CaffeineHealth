package com.uc.caffeine.monetization

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

enum class StoreAvailability { NOT_SUPPORTED, NOT_CONFIGURED, LOADING, AVAILABLE, UNAVAILABLE }
enum class PlanDuration { MONTHLY, YEARLY, LIFETIME }
enum class StoreOperation { IDLE, PURCHASING, RESTORING, VERIFYING, PENDING }
enum class StoreMessage { NONE, DISABLED, NETWORK, STORE, NOT_ALLOWED, UNAVAILABLE, VERIFYING, PENDING, RESTORED, NOTHING_TO_RESTORE, SUCCESS, UNKNOWN }

/** Prices must come from the configured store; the foundation supplies no plans. */
data class StorePlan(val id: String, val duration: PlanDuration, val localizedPrice: String)
data class StoreSnapshot(
    val availability: StoreAvailability,
    val plans: List<StorePlan> = emptyList(),
    val ownershipLoaded: Boolean = false,
    val purchasesEnabled: Boolean = false,
    val operation: StoreOperation = StoreOperation.IDLE,
    val message: StoreMessage = StoreMessage.NONE,
)

interface StoreCatalog {
    val snapshots: StateFlow<StoreSnapshot>
    fun refresh() {}
    fun purchase(activity: Activity, planId: String) {}
    fun restore() {}
    fun dismissStatus() {}
    val statusDismissed: Boolean get() = false
}

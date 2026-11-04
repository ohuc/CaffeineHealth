package com.uc.caffeine.monetization

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.TestStoreProduct
import com.uc.caffeine.BuildConfig
import com.uc.caffeine.ui.components.SettingsSectionLabel
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.segmentedListItemShapes
import com.uc.caffeine.ui.components.settingsRowColors

internal object BillingVariantSupport {
    /** Debug Play builds open these controls by double-tapping the app icon in Settings → About. */
    const val HAS_DEVELOPER_SETTINGS = true

    /** Debug builds always show the Plus surfaces so every screen can be reviewed. */
    val PLUS_SURFACES: Boolean = true

    fun baseOption(product: StoreProduct): SubscriptionOption? =
        (product as? TestStoreProduct)?.subscriptionOptions?.firstOrNull { it.isBasePlan && !it.isPrepaid }

    @Composable fun purchasesActive(store: StoreCatalog): Boolean {
        val demo = (store as? RevenueCatBillingRepository)?.demoState?.collectAsStateWithLifecycle()?.value
        return (BuildConfig.PLUS_PURCHASES_ENABLED && BuildConfig.PLUS_POLICY_ENABLED) || demo?.sampleStore == true
    }

    /** Debug-build-only controls for reviewing every Plus screen. English-only: never shipped. */
    @Composable fun DemoControls(store: StoreCatalog, journey: PlusJourney) {
        val repository = store as? RevenueCatBillingRepository ?: return
        val demo by repository.demoState.collectAsStateWithLifecycle()
        val haptics = rememberAppHaptics()

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingsSectionLabel("Caffeine Health+ rules")
            val policyLocked = BuildConfig.PLUS_POLICY_ENABLED
            val switches = listOf(
                Triple("Plus rules on", "Locks, the free-day offer and prompts", demo.policy || policyLocked) to
                    { on: Boolean -> if (!policyLocked) repository.setDemoPolicy(on) },
                Triple("Sample store", "Fake ₹ plans; buying simulates success. No RevenueCat needed", demo.sampleStore) to
                    { on: Boolean -> repository.setDemoStore(on) },
                Triple("Pretend outside sale countries",
                    "Play country: ${demo.storefrontCountry ?: "unknown"} · on sale in: ${demo.saleCountries}. On = locks stay, no Plus prompts or locked Coach chip, page says not on sale",
                    demo.outsideRegion) to
                    { on: Boolean -> repository.setDemoOutsideRegion(on) },
            )
            switches.forEachIndexed { index, (labels, onChange) ->
                val (title, summary, checked) = labels
                SegmentedListItem(
                    checked = checked,
                    onCheckedChange = { haptics.toggle(); onChange(it) },
                    supportingContent = { Text(summary) },
                    trailingContent = { Switch(checked = checked, onCheckedChange = null) },
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = segmentedListItemShapes(index, switches.size),
                    colors = settingsRowColors(),
                ) { Text(title, style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingsSectionLabel("Journey (turns the rules on)")
            val actions = listOf(
                DemoAction(Icons.Rounded.AutoAwesome, "Replay after-setup showcase", "Opens the \"You're all set\" page", repository::demoReplayShowcase),
                DemoAction(Icons.Rounded.AddTask, "Add an app-open day", "Now ${demo.openDays} of ${PlusJourneyPolicy.TRIAL_UNLOCK_DAYS}; day 3 shows the free-day offer", repository::demoAddOpenDay),
                DemoAction(Icons.Rounded.HourglassBottom, "End the free day now", "Shows \"How was your Caffeine Health+ day?\"", repository::demoEndTrialNow),
                DemoAction(Icons.Rounded.RestartAlt, "Reset journey", "Day 0, free day unused, offers unseen", repository::demoResetJourney),
            )
            actions.forEachIndexed { index, action ->
                SegmentedListItem(
                    onClick = { haptics.toggle(); action.onClick() },
                    leadingContent = { Icon(action.icon, contentDescription = null) },
                    supportingContent = { Text(action.summary) },
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = segmentedListItemShapes(index, actions.size),
                    colors = settingsRowColors(),
                ) { Text(action.title, style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingsSectionLabel("App icon screens")
            val screens = listOf(
                DemoAction(Icons.Rounded.AutoAwesome, "Welcome to Caffeine Health+", "The screen shown right after a purchase") {
                    repository.demoShowIconScreen(IconTransition.WELCOME)
                },
                DemoAction(Icons.Rounded.RestartAlt, "Switch back to classic", "The screen shown when turning the Plus icon off") {
                    repository.demoShowIconScreen(IconTransition.TO_CLASSIC)
                },
                DemoAction(Icons.Rounded.HourglassBottom, "Plus ended countdown", "Counts down 5 seconds, then really restarts the app") {
                    repository.demoShowIconScreen(IconTransition.PLUS_ENDED)
                },
            )
            screens.forEachIndexed { index, action ->
                SegmentedListItem(
                    onClick = { haptics.toggle(); action.onClick() },
                    leadingContent = { Icon(action.icon, contentDescription = null) },
                    supportingContent = { Text(action.summary) },
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = segmentedListItemShapes(index, screens.size),
                    colors = settingsRowColors(),
                ) { Text(action.title, style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingsSectionLabel("Pretend access")
            val options = listOf<Pair<String, DemoAccess?>>(
                "Real state" to null,
                "Free (locked)" to DemoAccess.FREE,
                "Subscribed" to DemoAccess.SUBSCRIBED,
                "Lifetime" to DemoAccess.LIFETIME,
                "Payment problem" to DemoAccess.BILLING_ISSUE,
                "Subscription ended" to DemoAccess.PAID_ENDED,
            )
            options.forEachIndexed { index, (label, kind) ->
                SegmentedListItem(
                    selected = demo.access == kind,
                    onClick = { haptics.toggle(); repository.setDemoAccess(kind) },
                    leadingContent = { RadioButton(selected = demo.access == kind, onClick = null) },
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = segmentedListItemShapes(index, options.size),
                    colors = settingsRowColors(),
                ) { Text(label, style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }
    }
}

private data class DemoAction(val icon: ImageVector, val title: String, val summary: String, val onClick: () -> Unit)


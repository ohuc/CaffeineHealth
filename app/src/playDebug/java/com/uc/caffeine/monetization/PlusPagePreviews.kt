package com.uc.caffeine.monetization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.uc.caffeine.R
import com.uc.caffeine.data.AppColorPalette
import com.uc.caffeine.ui.theme.CaffeineTheme

internal data class PlusPreviewCase(
    val name: String,
    val access: AccessSnapshot = previewFree,
    val store: StoreSnapshot = previewStore,
    val showcase: Boolean = false,
    val trial: Boolean = false,
    val purchasesActive: Boolean = true,
) {
    override fun toString() = name
    fun model(selected: String? = "year") = PlusPageModel.from(access, store, JourneyState(canStartTrial = trial),
        ProScreenState.from(access, store, selected), showcase, purchasesActive)
}

private val previewFree = AccessSnapshot(Distribution.PLAY, AccessSource.NONE, VerificationStatus.VERIFIED)
private val previewStore = StoreSnapshot(StoreAvailability.AVAILABLE, listOf(
    StorePlan("month", PlanDuration.MONTHLY, "$0.00"),
    StorePlan("year", PlanDuration.YEARLY, "$0.00"),
    StorePlan("lifetime", PlanDuration.LIFETIME, "$0.00"),
), ownershipLoaded = true, purchasesEnabled = true)
private val previewPaid = previewFree.copy(source = AccessSource.SUBSCRIPTION, activeSubscription = true,
    manageSubscription = true, willRenew = true, paidExpiresAtMillis = 1804896000000L)
private val previewCases = listOf(
    PlusPreviewCase("S1 Showcase", showcase = true),
    PlusPreviewCase("S2 Free from Settings"),
    PlusPreviewCase("S3 Loading", store = StoreSnapshot(StoreAvailability.LOADING)),
    PlusPreviewCase("S4 Refresh needed", access = previewFree.copy(verification = VerificationStatus.CACHED)),
    PlusPreviewCase("S4b Verification rejected", access = previewFree.copy(verification = VerificationStatus.ERROR),
        store = previewStore.copy(ownershipLoaded = false, message = StoreMessage.VERIFYING)),
    PlusPreviewCase("S5 Store unavailable", store = previewStore.copy(availability = StoreAvailability.UNAVAILABLE)),
    PlusPreviewCase("S6 Purchases off", purchasesActive = false),
    PlusPreviewCase("S7 Purchasing", store = previewStore.copy(operation = StoreOperation.PURCHASING)),
    PlusPreviewCase("S8 Pending", store = previewStore.copy(operation = StoreOperation.PENDING)),
    PlusPreviewCase("S9 Network error", store = previewStore.copy(message = StoreMessage.NETWORK)),
    PlusPreviewCase("S10 Restored", store = previewStore.copy(message = StoreMessage.RESTORED)),
    PlusPreviewCase("S11 Free day", access = previewFree.copy(source = AccessSource.PREVIEW, previewEndsAtMillis = 1804896000000L)),
    PlusPreviewCase("S12 Free day eligible", trial = true),
    PlusPreviewCase("S13 Expired", access = previewFree.copy(expired = true, manageSubscription = true, willRenew = false)),
    PlusPreviewCase("O1 Subscribed", access = previewPaid),
    PlusPreviewCase("O2 Canceled", access = previewPaid.copy(willRenew = false)),
    PlusPreviewCase("O3 Lifetime", access = previewFree.copy(source = AccessSource.LIFETIME)),
    PlusPreviewCase("O4 Mixed ownership", access = previewPaid.copy(source = AccessSource.LIFETIME)),
    PlusPreviewCase("O5 Billing issue", access = previewPaid.copy(billingIssue = true)),
    PlusPreviewCase("O6 Unverified", access = previewPaid.copy(source = AccessSource.NONE, verification = VerificationStatus.ERROR)),
    PlusPreviewCase("O7 Icon offer", access = previewPaid),
    PlusPreviewCase("O8 Restoring", access = previewPaid, store = previewStore.copy(operation = StoreOperation.RESTORING)),
)

internal class PlusPagePreviewProvider : PreviewParameterProvider<PlusPreviewCase> {
    override val values = previewCases.asSequence()
}
internal class PlusLargeTypePreviewProvider : PreviewParameterProvider<PlusPreviewCase> {
    override val values = previewCases.filter { it.name.substringBefore(' ') in setOf("S1", "S3", "S9", "O1", "O5") }.asSequence()
}

@Preview(name = "Espresso light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PlusEspressoLight(@PreviewParameter(PlusPagePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, false, AppColorPalette.ESPRESSO)
}
@Preview(name = "Espresso dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PlusEspressoDark(@PreviewParameter(PlusPagePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, true, AppColorPalette.ESPRESSO)
}
@Preview(name = "Matcha dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PlusMatchaDark(@PreviewParameter(PlusPagePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, true, AppColorPalette.MATCHA)
}
@Preview(name = "Sakura light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PlusSakuraLight(@PreviewParameter(PlusPagePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, false, AppColorPalette.SAKURA)
}
@Preview(name = "Ocean dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PlusOceanDark(@PreviewParameter(PlusPagePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, true, AppColorPalette.OCEAN)
}
@Preview(name = "Large type light", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PlusLargeType(@PreviewParameter(PlusLargeTypePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, false, AppColorPalette.ESPRESSO)
}
@Preview(name = "Large type dark", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PlusLargeTypeDark(@PreviewParameter(PlusLargeTypePreviewProvider::class) case: PlusPreviewCase) {
    PreviewPage(case, true, AppColorPalette.MATCHA)
}
@Preview(name = "Short phone", widthDp = 360, heightDp = 640)
@Preview(name = "Narrow phone", widthDp = 320, heightDp = 640)
@Preview(name = "Tablet width cap", widthDp = 840, heightDp = 1000)
@Preview(name = "Landscape", widthDp = 640, heightDp = 360)
@Preview(name = "Short phone large type", widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun PlusSizes() { PreviewPage(previewCases.first(), false, AppColorPalette.ESPRESSO) }

@Composable
private fun PreviewPage(case: PlusPreviewCase, dark: Boolean, palette: AppColorPalette) {
    CaffeineTheme(darkTheme = dark, colorPalette = palette) {
        var selected by remember(case) { mutableStateOf("year") }
        var dialog by remember { mutableStateOf<PlusInfoDialog?>(null) }
        val model = case.model(selected)
        val chip = model.chip?.let { if (it.timestamp == null) stringResource(it.text) else stringResource(it.text, "12 Mar 2027") }
        PlusPageContent(model, selected, PlusPageActions(
            onDismiss = {}, onSelect = { selected = it }, onPrimary = {}, onStartTrial = {}, onAction = {}, onInfo = { dialog = it },
        ), chipText = chip, iconOffer = { AppIconOffer(onUsePlusIcon = {}) })
        dialog?.let { PlusInfoDialog(it) { dialog = null } }
    }
}

@Preview(name = "Hero and facts", widthDp = 360, showBackground = true)
@Composable
private fun PlusHeroPreview() = PreviewPrimitives {
    PlusHero(null)
    PlusOwnerHeader(stringResource(R.string.plus_short_active), null)
    PlusStatusChip(stringResource(R.string.plus_short_lifetime))
}
@Preview(name = "Benefits and sample art", widthDp = 360, showBackground = true)
@Composable
private fun PlusFeaturesPreview() = PreviewPrimitives {
    PlusFeatureShowcase()
    PlusIncludedList()
}
@Preview(name = "Plans and free day", widthDp = 360, showBackground = true)
@Composable
private fun PlusPlansPreview() = PreviewPrimitives {
    PlusPlanCards(PlusPlans.Ready(previewStore.plans.sortedBy { if (it.duration == PlanDuration.YEARLY) 0 else 1 }, true), "year") {}
    PlusFreeDayRow {}
}
@Preview(name = "Progress and problem", widthDp = 360, showBackground = true)
@Composable
private fun PlusStatusPreview() = PreviewPrimitives {
    PlusStatusStrip(PlusStrip(R.string.plus_network_error, PlusSeverity.PROBLEM, listOf(PlusAction.REFRESH)), true, true) {}
    PlusStatusStrip(PlusStrip(R.string.plus_restore_loading, PlusSeverity.PROGRESS), true, true) {}
}
@Preview(name = "Footer", widthDp = 360, showBackground = true)
@Composable
private fun PlusFooterPreview() = PreviewPrimitives { PlusFooter(true, true, {}, {}) }

@Preview(name = "Titled terms dialog", widthDp = 360, heightDp = 640)
@Composable
private fun PlusInfoDialogPreview() = CaffeineTheme(colorPalette = AppColorPalette.ESPRESSO) {
    PlusInfoDialog(PlusInfoDialog.TERMS) {}
}

@Preview(name = "Selected plan purchase bar", widthDp = 360, showBackground = true)
@Composable
private fun PlusBarPreview() = CaffeineTheme(colorPalette = AppColorPalette.ESPRESSO) {
    CompositionLocalProvider(LocalPlusMotionEnabled provides false) {
        val plan = previewStore.plans.first()
        PlusPurchaseBar(PlusCta.Subscribe(plan), plan, true, R.string.plus_keep_preview, true, {}, {})
    }
}

@Composable
private fun PreviewPrimitives(content: @Composable ColumnScope.() -> Unit) {
    CaffeineTheme(colorPalette = AppColorPalette.ESPRESSO) {
        CompositionLocalProvider(LocalPlusMotionEnabled provides false) {
            Surface { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(32.dp), content = content) }
        }
    }
}

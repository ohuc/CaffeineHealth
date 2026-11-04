package com.uc.caffeine.monetization

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/** Only constructed beneath Context.noBackupFilesDir; never passed to health-data import/export. */
internal class PlayPrivateState(private val file: File) {
    private val values = Properties().apply {
        if (file.exists()) runCatching { file.inputStream().use(::load) }
            .onFailure { setProperty("trial.started", "corrupt") }
    }

    val grant: PaidGrant?
        @Synchronized get() = runCatching {
            values.getProperty("grant.verified")?.toLong()?.let { verified ->
                PaidGrant(
                    active = values.getProperty("grant.active").toBooleanStrict(),
                    lifetime = values.getProperty("grant.lifetime").toBooleanStrict(),
                    verifiedAtMillis = verified,
                    expiresAtMillis = values.getProperty("grant.expiry")?.toLong(),
                    hasSubscription = values.getProperty("grant.subscription").toBooleanStrict(),
                    willRenew = values.getProperty("grant.renew").toBooleanStrict(),
                    billingIssue = values.getProperty("grant.issue").toBooleanStrict(),
                    previouslyOwned = values.getProperty("grant.owned")?.toBooleanStrict()
                        ?: (values.getProperty("grant.active").toBoolean() || values.getProperty("grant.expiry") != null),
                    activeSubscription = values.getProperty("grant.activeSubscription")?.toBooleanStrict()
                        ?: (values.getProperty("grant.subscription").toBoolean() && values.getProperty("grant.active").toBoolean() && !values.getProperty("grant.lifetime").toBoolean()),
                )
            }
        }.getOrNull()

    @Synchronized fun saveGrant(grant: PaidGrant) {
        values.setProperty("grant.active", grant.active.toString())
        values.setProperty("grant.lifetime", grant.lifetime.toString())
        values.setProperty("grant.verified", grant.verifiedAtMillis.toString())
        values.remove("grant.expiry")
        grant.expiresAtMillis?.let { values.setProperty("grant.expiry", it.toString()) }
        values.setProperty("grant.subscription", grant.hasSubscription.toString())
        values.setProperty("grant.renew", grant.willRenew.toString())
        values.setProperty("grant.issue", grant.billingIssue.toString())
        values.setProperty("grant.owned", grant.previouslyOwned.toString())
        values.setProperty("grant.activeSubscription", grant.activeSubscription.toString())
        persist()
    }

    /** The user-started free day, or null while it is unused. A corrupt record counts as used. */
    @Synchronized fun trial(nowMillis: Long): PreviewRecord? {
        if (!values.containsKey("trial.started")) return null
        val start = values.getProperty("trial.started")?.toLongOrNull() ?: 0L
        val latest = values.getProperty("trial.latest")?.toLongOrNull() ?: start
        // A backward clock jump cannot earn more free time. Small network-clock adjustments are tolerated.
        val effectiveNow = if (nowMillis < latest - 300_000L) maxOf(latest, start + AccessPolicy.PREVIEW_MILLIS)
            else maxOf(latest, nowMillis)
        values.setProperty("trial.started", start.toString())
        values.setProperty("trial.latest", effectiveNow.toString())
        persist()
        return PreviewRecord(start, effectiveNow)
    }

    @Synchronized fun startTrial(nowMillis: Long): PreviewRecord {
        if (!values.containsKey("trial.started")) values.setProperty("trial.started", nowMillis.toString())
        return requireNotNull(trial(nowMillis))
    }

    val journey: JourneyRecord
        @Synchronized get() = JourneyRecord(
            openDays = values.getProperty("journey.days")?.toIntOrNull() ?: 0,
            lastOpenEpochDay = values.getProperty("journey.lastDay")?.toLongOrNull(),
            showcasePending = values.getProperty("journey.showcasePending").toBoolean(),
            showcaseShown = values.getProperty("journey.showcaseShown").toBoolean(),
            offerShown = values.getProperty("journey.offerShown").toBoolean(),
            endedPromptDue = values.getProperty("journey.endedDue").toBoolean(),
            endedPromptShown = values.getProperty("journey.endedShown").toBoolean(),
        )

    @Synchronized fun saveJourney(record: JourneyRecord) {
        values.setProperty("journey.days", record.openDays.toString())
        values.remove("journey.lastDay")
        record.lastOpenEpochDay?.let { values.setProperty("journey.lastDay", it.toString()) }
        values.setProperty("journey.showcasePending", record.showcasePending.toString())
        values.setProperty("journey.showcaseShown", record.showcaseShown.toString())
        values.setProperty("journey.offerShown", record.offerShown.toString())
        values.setProperty("journey.endedDue", record.endedPromptDue.toString())
        values.setProperty("journey.endedShown", record.endedPromptShown.toString())
        persist()
    }

    /** Debug-build demo controls only; release code never reads or writes these keys. */
    @Synchronized fun demoValue(key: String): String? = values.getProperty("demo.$key")
    @Synchronized fun setDemoValue(key: String, value: String?) {
        if (value == null) values.remove("demo.$key") else values.setProperty("demo.$key", value)
        persist()
    }
    @Synchronized fun setTrialForDemo(startedAtMillis: Long?, nowMillis: Long) {
        values.remove("trial.started"); values.remove("trial.latest")
        startedAtMillis?.let { values.setProperty("trial.started", it.toString()); values.setProperty("trial.latest", nowMillis.toString()) }
        persist()
    }

    /** Last known Google Play country and remote sale-country list, so an offline launch keeps the same rules. */
    val storefrontCountry: String? @Synchronized get() = values.getProperty("region.storefront")
    val saleCountries: String? @Synchronized get() = values.getProperty("region.saleCountries")
    @Synchronized fun setStorefrontCountry(code: String) { values.setProperty("region.storefront", code); persist() }
    @Synchronized fun setSaleCountries(spec: String?) {
        if (spec == null) values.remove("region.saleCountries") else values.setProperty("region.saleCountries", spec)
        persist()
    }

    /** Set after this install's one silent restore. The file is not backed up, so a reinstall restores again. */
    val autoRestoreDone: Boolean @Synchronized get() = values.getProperty("restore.auto").toBoolean()
    @Synchronized fun setAutoRestoreDone() { values.setProperty("restore.auto", "true"); persist() }

    @Synchronized fun dismissStatus(key: String) { values.setProperty("status.dismissed", key); persist() }
    @Synchronized fun isStatusDismissed(key: String): Boolean = values.getProperty("status.dismissed") == key
    val pending: Boolean @Synchronized get() = values.getProperty("checkout.pending").toBoolean()
    @Synchronized fun setPending(pending: Boolean) { values.setProperty("checkout.pending", pending.toString()); persist() }

    private fun persist() {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.outputStream().use { values.store(it, null) }
        runCatching {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

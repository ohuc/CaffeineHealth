package com.uc.caffeine.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata as HCMetadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class ImportedCaffeineRecord(
    val healthConnectRecordId: String,
    val caffeineMg: Int,
    val startedAtMillis: Long,
    val durationMinutes: Int,
)

/** Last night's sleep, used by the caffeine coach for sleep-debt and time-awake. */
data class LastNightSleep(
    val bedtimeMillis: Long,
    val wakeMillis: Long,
    /** Minutes actually asleep (excludes in-bed awake stages when stage data exists). */
    val asleepMinutes: Int,
)

class HealthConnectManager(private val context: Context) {

    private val nutritionWrite = HealthPermission.getWritePermission(NutritionRecord::class)
    private val nutritionRead  = HealthPermission.getReadPermission(NutritionRecord::class)
    private val sleepRead      = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val hydrationWrite = HealthPermission.getWritePermission(HydrationRecord::class)

    val allPermissions = setOf(nutritionWrite, nutritionRead, sleepRead, hydrationWrite)
    val sleepPermissions = setOf(sleepRead)

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient?
        get() = if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    // Null when Health Connect is unreachable (not installed, updating, binder failure) —
    // callers that act on "permission revoked" must distinguish that from "can't tell".
    private suspend fun grantedPermissionsOrNull(): Set<String>? {
        val c = client ?: return null
        return runCatching { c.permissionController.getGrantedPermissions() }.getOrNull()
    }

    suspend fun hasPermission(): Boolean =
        grantedPermissionsOrNull()?.contains(nutritionWrite) == true

    suspend fun hasNutritionReadPermission(): Boolean =
        grantedPermissionsOrNull()?.contains(nutritionRead) == true

    suspend fun hasSleepPermission(): Boolean =
        grantedPermissionsOrNull()?.contains(sleepRead) == true

    suspend fun hasHydrationWritePermission(): Boolean =
        grantedPermissionsOrNull()?.contains(hydrationWrite) == true

    /** True only when Health Connect answered and the sleep permission is definitively not granted. */
    suspend fun isSleepPermissionRevoked(): Boolean =
        grantedPermissionsOrNull()?.let { sleepRead !in it } == true

    private fun clientRecordId(entryId: Int): String = "caffeine_entry_$entryId"
    private fun clientHydrationRecordId(entryId: Int): String = "caffeine_entry_${entryId}_hydration"

    private fun ConsumptionEntry.toNutritionRecord(zoneId: ZoneId): NutritionRecord {
        val start = Instant.ofEpochMilli(startedAtMillis)
        val end = start.plusSeconds(normalizedDurationMinutes * 60L)
        val zoneRules = zoneId.rules
        return NutritionRecord(
            startTime = start,
            endTime = end,
            caffeine = Mass.milligrams(caffeineMg.toDouble()),
            name = drinkName,
            startZoneOffset = zoneRules.getOffset(start),
            endZoneOffset = zoneRules.getOffset(end),
            metadata = HCMetadata.manualEntry(
                clientRecordId = clientRecordId(id),
                clientRecordVersion = System.currentTimeMillis(),
            ),
        )
    }

    private fun ConsumptionEntry.toHydrationRecord(volumeMl: Double, zoneId: ZoneId): HydrationRecord {
        val start = Instant.ofEpochMilli(startedAtMillis)
        val end = start.plusSeconds(normalizedDurationMinutes * 60L)
        val zoneRules = zoneId.rules
        return HydrationRecord(
            startTime = start,
            endTime = end,
            volume = Volume.liters(volumeMl / 1000.0),
            startZoneOffset = zoneRules.getOffset(start),
            endZoneOffset = zoneRules.getOffset(end),
            metadata = HCMetadata.manualEntry(
                clientRecordId = clientHydrationRecordId(id),
                clientRecordVersion = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun writeEntry(
        entry: ConsumptionEntry,
        zoneId: ZoneId = ZoneId.systemDefault(),
        volumeMl: Double? = null,
    ) {
        val c = client ?: return
        // Imported records are owned by another app — never echo them back
        if (entry.healthConnectRecordId != null) return
        if (!hasPermission()) return
        val records = mutableListOf<Record>(entry.toNutritionRecord(zoneId))
        if (volumeMl != null && volumeMl > 0.0 && hasHydrationWritePermission()) {
            records.add(entry.toHydrationRecord(volumeMl, zoneId))
        }
        runCatching { c.insertRecords(records) }
    }

    suspend fun syncAll(entries: List<ConsumptionEntry>, zoneId: ZoneId = ZoneId.systemDefault()) {
        val c = client ?: return
        if (!hasPermission()) return
        // Batched inserts: one permission check and one IPC per chunk, instead of two per
        // entry — Health Connect rate-limits foreground calls, so per-entry inserts can
        // silently drop most of a large history.
        entries
            .filter { it.healthConnectRecordId == null }
            .map { it.toNutritionRecord(zoneId) }
            .chunked(INSERT_BATCH_SIZE)
            .forEach { batch ->
                runCatching { c.insertRecords(batch) }
            }
    }

    suspend fun deleteEntry(entry: ConsumptionEntry) {
        if (entry.healthConnectRecordId != null) return
        deleteOwnRecords(listOf(entry.id))
    }

    suspend fun deleteEntries(entries: List<ConsumptionEntry>) {
        val ownIds = entries.filter { it.healthConnectRecordId == null }.map { it.id }
        if (ownIds.isEmpty()) return
        deleteOwnRecords(ownIds)
    }

    private suspend fun deleteOwnRecords(entryIds: List<Int>) {
        val c = client ?: return
        if (!hasPermission()) return
        val clientIds = entryIds.map { clientRecordId(it) }
        runCatching {
            c.deleteRecords(NutritionRecord::class, emptyList(), clientIds)
        }
        // Also delete corresponding hydration records
        if (hasHydrationWritePermission()) {
            val hydrationClientIds = entryIds.map { clientHydrationRecordId(it) }
            runCatching {
                c.deleteRecords(HydrationRecord::class, emptyList(), hydrationClientIds)
            }
        }
    }

    suspend fun readForeignCaffeineRecords(
        sinceMillis: Long,
        excludeRecordIds: Set<String>,
    ): List<ImportedCaffeineRecord> {
        val c = client ?: return emptyList()
        if (!hasNutritionReadPermission()) return emptyList()
        val results = mutableListOf<ImportedCaffeineRecord>()
        var pageToken: String? = null
        val ownPackage = context.packageName
        do {
            val response = runCatching {
                c.readRecords(
                    ReadRecordsRequest(
                        recordType = NutritionRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(Instant.ofEpochMilli(sinceMillis)),
                        pageToken = pageToken,
                    )
                )
            }.getOrNull() ?: break

            for (record in response.records) {
                // Skip our own records
                if (record.metadata.dataOrigin.packageName == ownPackage) continue
                val caffeineMass = record.caffeine ?: continue
                val mg = caffeineMass.inMilligrams.toInt()
                if (mg <= 0) continue
                val id = record.metadata.id
                if (id.isBlank() || id in excludeRecordIds) continue
                val durationSec = record.endTime.epochSecond - record.startTime.epochSecond
                val durationMinutes = (durationSec / 60).toInt().coerceAtLeast(1)
                results += ImportedCaffeineRecord(
                    healthConnectRecordId = id,
                    caffeineMg = mg,
                    startedAtMillis = record.startTime.toEpochMilli(),
                    durationMinutes = durationMinutes,
                )
            }
            pageToken = response.pageToken
        } while (pageToken != null)

        return results
    }

    suspend fun readSleepBedtime(mode: HcSleepMode, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime? {
        val c = client ?: return null
        if (!hasSleepPermission()) return null
        val now = ZonedDateTime.now(zoneId)
        val rangeStart = when (mode) {
            HcSleepMode.PREVIOUS_DAY -> now.minusDays(2).toInstant()
            HcSleepMode.SEVEN_DAY_AVERAGE -> now.minusDays(8).toInstant()
        }
        val response = runCatching {
            c.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(rangeStart, now.toInstant()),
                )
            )
        }.getOrNull() ?: return null

        // Naps would otherwise register as "bedtime" (an afternoon nap is the most
        // recent session) — only sessions long enough to be a night's sleep count.
        val nightSessions = response.records.filter {
            Duration.between(it.startTime, it.endTime).toMinutes() >= MIN_NIGHT_SLEEP_MINUTES
        }
        if (nightSessions.isEmpty()) return null

        return when (mode) {
            HcSleepMode.PREVIOUS_DAY -> {
                val latest = nightSessions.maxByOrNull { it.startTime } ?: return null
                val zdt = latest.startTime.atZone(zoneId)
                LocalTime.of(zdt.hour, zdt.minute)
            }
            HcSleepMode.SEVEN_DAY_AVERAGE -> {
                averageSleepStartTime(
                    sessions = nightSessions.sortedByDescending { it.startTime }.take(7),
                    zoneId = zoneId,
                )
            }
        }
    }

    /**
     * Reads the most recent night's sleep session (start, end, and minutes asleep).
     *
     * Unlike [readSleepBedtime] — which only needs the bedtime to anchor a forecast — the
     * coach needs the wake time (for time-awake) and the actual sleep duration (for sleep
     * debt). When the source provides sleep stages we sum the non-awake stages; otherwise
     * we fall back to the raw session span.
     */
    suspend fun readLastNightSleep(zoneId: ZoneId = ZoneId.systemDefault()): LastNightSleep? {
        val c = client ?: return null
        if (!hasSleepPermission()) return null
        val now = ZonedDateTime.now(zoneId)
        val response = runCatching {
            c.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(now.minusDays(2).toInstant(), now.toInstant()),
                )
            )
        }.getOrNull() ?: return null

        val latestNight = response.records
            .filter { Duration.between(it.startTime, it.endTime).toMinutes() >= MIN_NIGHT_SLEEP_MINUTES }
            .maxByOrNull { it.startTime }
            ?: return null

        val stagedAsleepMinutes = latestNight.stages
            .filter { it.stage !in AWAKE_STAGE_TYPES }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        val asleepMinutes = stagedAsleepMinutes
            .takeIf { it > 0 }
            ?: Duration.between(latestNight.startTime, latestNight.endTime).toMinutes()

        return LastNightSleep(
            bedtimeMillis = latestNight.startTime.toEpochMilli(),
            wakeMillis = latestNight.endTime.toEpochMilli(),
            asleepMinutes = asleepMinutes.toInt().coerceAtLeast(0),
        )
    }

    private fun averageSleepStartTime(sessions: List<SleepSessionRecord>, zoneId: ZoneId): LocalTime? {
        if (sessions.isEmpty()) return null
        val minutesList = sessions.map { session ->
            val zdt = session.startTime.atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            if (minuteOfDay < 720) minuteOfDay + 1440 else minuteOfDay
        }
        val avgMinutes = (minutesList.sum() / minutesList.size) % 1440
        return LocalTime.of(avgMinutes / 60, avgMinutes % 60)
    }

    private companion object {
        const val INSERT_BATCH_SIZE = 100
        const val MIN_NIGHT_SLEEP_MINUTES = 180L

        // Stages that count as "in bed but not asleep" — excluded from sleep-debt math.
        val AWAKE_STAGE_TYPES = setOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
        )
    }
}

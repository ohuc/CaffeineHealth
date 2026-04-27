package com.uc.caffeine.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata as HCMetadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectManager(private val context: Context) {

    val permissions = setOf(HealthPermission.getWritePermission(NutritionRecord::class))
    val sleepPermissions = setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))
    val allPermissions = permissions + sleepPermissions

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient?
        get() = if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermission(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun hasSleepPermission(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(sleepPermissions)
    }

    suspend fun writeEntry(entry: ConsumptionEntry, zoneId: ZoneId = ZoneId.systemDefault()) {
        val c = client ?: return
        val start = Instant.ofEpochMilli(entry.startedAtMillis)
        val end = start.plusSeconds(entry.durationMinutes * 60L)
        val zoneRules = zoneId.rules
        c.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = start,
                    endTime = end,
                    caffeine = Mass.milligrams(entry.caffeineMg.toDouble()),
                    startZoneOffset = zoneRules.getOffset(start),
                    endZoneOffset = zoneRules.getOffset(end),
                    metadata = HCMetadata.unknownRecordingMethod(),
                )
            )
        )
    }

    suspend fun syncAll(entries: List<ConsumptionEntry>, zoneId: ZoneId = ZoneId.systemDefault()) {
        entries.forEach { writeEntry(it, zoneId) }
    }

    suspend fun readSleepBedtime(mode: HcSleepMode, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime? {
        val c = client ?: return null
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

        if (response.records.isEmpty()) return null

        return when (mode) {
            HcSleepMode.PREVIOUS_DAY -> {
                val latest = response.records.maxByOrNull { it.startTime } ?: return null
                val zdt = latest.startTime.atZone(zoneId)
                LocalTime.of(zdt.hour, zdt.minute)
            }
            HcSleepMode.SEVEN_DAY_AVERAGE -> {
                averageSleepStartTime(
                    sessions = response.records.sortedByDescending { it.startTime }.take(7),
                    zoneId = zoneId,
                )
            }
        }
    }

    private fun averageSleepStartTime(sessions: List<SleepSessionRecord>, zoneId: ZoneId): LocalTime? {
        if (sessions.isEmpty()) return null
        // Times before noon are post-midnight bedtimes; add 24hr so averaging doesn't wrap wrong.
        val minutesList = sessions.map { session ->
            val zdt = session.startTime.atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            if (minuteOfDay < 720) minuteOfDay + 1440 else minuteOfDay
        }
        val avgMinutes = (minutesList.sum() / minutesList.size) % 1440
        return LocalTime.of(avgMinutes / 60, avgMinutes % 60)
    }
}

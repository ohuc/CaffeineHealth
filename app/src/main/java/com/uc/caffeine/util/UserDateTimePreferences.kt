package com.uc.caffeine.util

import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.data.model.ConsumptionEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.abs

private const val TWELVE_HOUR_TIME_PATTERN = "h:mm a"
private const val CHART_TWELVE_HOUR_PATTERN = "ha"
private const val TWENTY_FOUR_HOUR_TIME_PATTERN = "HH:mm"
private const val CHART_TWENTY_FOUR_HOUR_PATTERN = "HH"

fun UserSettings.resolvedZoneId(): ZoneId {
    return runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }
}

fun UserSettings.timeFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter {
    val pattern = if (use24HourClock) TWENTY_FOUR_HOUR_TIME_PATTERN else TWELVE_HOUR_TIME_PATTERN
    return DateTimeFormatter.ofPattern(pattern, locale)
}

fun UserSettings.chartTimeFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter {
    val pattern = if (use24HourClock) CHART_TWENTY_FOUR_HOUR_PATTERN else CHART_TWELVE_HOUR_PATTERN
    return DateTimeFormatter.ofPattern(pattern, locale)
}

fun UserSettings.dateFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter {
    return DateTimeFormatter.ofPattern(dateFormat.pattern, locale)
}

fun formatTimeOfDay(
    hour: Int,
    minute: Int,
    settings: UserSettings,
    locale: Locale = Locale.getDefault(),
): String {
    return LocalTime.of(hour, minute).format(settings.timeFormatter(locale))
}

fun formatTimestampToTime(
    timestampMillis: Long,
    settings: UserSettings,
    locale: Locale = Locale.getDefault(),
): String {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(settings.resolvedZoneId())
        .format(settings.timeFormatter(locale))
}

fun formatTimestampToDate(
    timestampMillis: Long,
    settings: UserSettings,
    locale: Locale = Locale.getDefault(),
): String {
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(settings.resolvedZoneId())
        .format(settings.dateFormatter(locale))
}

fun formatTimestampToDateTime(
    timestampMillis: Long,
    settings: UserSettings,
    locale: Locale = Locale.getDefault(),
): String {
    val zonedDateTime = Instant.ofEpochMilli(timestampMillis).atZone(settings.resolvedZoneId())
    return "${zonedDateTime.format(settings.dateFormatter(locale))} ${zonedDateTime.format(settings.timeFormatter(locale))}"
}

fun groupConsumptionEntriesByLocalDate(
    entries: List<ConsumptionEntry>,
    settings: UserSettings,
): Map<LocalDate, List<ConsumptionEntry>> {
    val zoneId = settings.resolvedZoneId()
    return entries
        .sortedByDescending { it.startedAtMillis }
        .groupBy { entry ->
            Instant.ofEpochMilli(entry.startedAtMillis)
                .atZone(zoneId)
                .toLocalDate()
        }
        .toList()
        .sortedByDescending { (date, _) -> date }
        .toMap(LinkedHashMap())
}

fun formatConsumptionDateHeader(
    date: LocalDate,
    settings: UserSettings,
    referenceTimeMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault(),
): String {
    val zoneId = settings.resolvedZoneId()
    val today = Instant.ofEpochMilli(referenceTimeMillis).atZone(zoneId).toLocalDate()

    return when (date) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> {
            val formatter = DateTimeFormatter.ofPattern(
                "EEEE, ${settings.dateFormat.pattern}",
                locale
            )
            date.format(formatter).uppercase(locale)
        }
    }
}

fun startOfDayMillis(
    currentTimeMillis: Long,
    zoneId: ZoneId,
): Long {
    return Instant.ofEpochMilli(currentTimeMillis)
        .atZone(zoneId)
        .toLocalDate()
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun nextStartOfDayMillis(
    currentTimeMillis: Long,
    zoneId: ZoneId,
): Long {
    return Instant.ofEpochMilli(currentTimeMillis)
        .atZone(zoneId)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun calculateNextBedtimeMillis(
    currentTimeMillis: Long,
    settings: UserSettings,
): Long {
    val zoneId = settings.resolvedZoneId()
    val currentZonedTime = Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId)
    var bedtime = currentZonedTime
        .withHour(settings.effectiveSleepTimeHour)
        .withMinute(settings.effectiveSleepTimeMinute)
        .withSecond(0)
        .withNano(0)

    if (bedtime.toInstant().toEpochMilli() <= currentTimeMillis) {
        bedtime = bedtime.plusDays(1)
    }

    return bedtime.toInstant().toEpochMilli()
}

fun combineDateWithTime(
    baseTimestamp: Long,
    hour: Int,
    minute: Int,
    settings: UserSettings,
): Long {
    val zoneId = settings.resolvedZoneId()
    val date = Instant.ofEpochMilli(baseTimestamp).atZone(zoneId).toLocalDate()
    return ZonedDateTime.of(date, LocalTime.of(hour, minute), zoneId)
        .withSecond(0)
        .withNano(0)
        .toInstant()
        .toEpochMilli()
}

fun formatDurationMinutes(
    durationMinutes: Int,
): String {
    val safeMinutes = durationMinutes.coerceAtLeast(1)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60

    return when {
        hours == 0 -> if (minutes == 1) "1 minute" else "$minutes minutes"
        minutes == 0 -> if (hours == 1) "1 hour" else "$hours hours"
        else -> {
            val hourPart = if (hours == 1) "1 hour" else "$hours hours"
            val minutePart = if (minutes == 1) "1 minute" else "$minutes minutes"
            "$hourPart $minutePart"
        }
    }
}

fun formatTimeZoneOffset(
    zoneId: ZoneId,
    currentTimeMillis: Long = System.currentTimeMillis(),
): String {
    val totalSeconds = zoneId.rules.getOffset(Instant.ofEpochMilli(currentTimeMillis)).totalSeconds
    val sign = if (totalSeconds >= 0) "+" else "-"
    val absoluteSeconds = abs(totalSeconds)
    val hours = absoluteSeconds / 3600
    val minutes = (absoluteSeconds % 3600) / 60
    return "UTC%s%02d:%02d".format(sign, hours, minutes)
}

fun formatTimeZoneName(
    timeZoneId: String,
    currentTimeMillis: Long = System.currentTimeMillis(),
): String {
    val zoneId = runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }
    return "${zoneId.id.replace('_', ' ')} • ${formatTimeZoneOffset(zoneId, currentTimeMillis)}"
}

package com.uc.caffeine.util.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class NotificationSchedulerTest {

    // parseTime

    @Test
    fun parseTime_validMorning_returnsCorrectPair() {
        assertEquals(11 to 0, NotificationScheduler.parseTime("11:00"))
    }

    @Test
    fun parseTime_validAfternoon_returnsCorrectPair() {
        assertEquals(14 to 30, NotificationScheduler.parseTime("14:30"))
    }

    @Test
    fun parseTime_midnight_returnsZeroPair() {
        assertEquals(0 to 0, NotificationScheduler.parseTime("00:00"))
    }

    @Test
    fun parseTime_endOfDay_returnsCorrectPair() {
        assertEquals(23 to 59, NotificationScheduler.parseTime("23:59"))
    }

    @Test
    fun parseTime_missingColon_returnsNull() {
        assertNull(NotificationScheduler.parseTime("1100"))
    }

    @Test
    fun parseTime_emptyString_returnsNull() {
        assertNull(NotificationScheduler.parseTime(""))
    }

    @Test
    fun parseTime_hourOutOfRange_returnsNull() {
        assertNull(NotificationScheduler.parseTime("24:00"))
    }

    @Test
    fun parseTime_minuteOutOfRange_returnsNull() {
        assertNull(NotificationScheduler.parseTime("12:60"))
    }

    @Test
    fun parseTime_nonNumeric_returnsNull() {
        assertNull(NotificationScheduler.parseTime("aa:bb"))
    }

    // dailyReminderRequestCode

    @Test
    fun dailyReminderRequestCode_differentTimes_uniqueCodes() {
        val code1100 = NotificationScheduler.dailyReminderRequestCode(11, 0)
        val code1400 = NotificationScheduler.dailyReminderRequestCode(14, 0)
        val code1430 = NotificationScheduler.dailyReminderRequestCode(14, 30)
        val code0800 = NotificationScheduler.dailyReminderRequestCode(8, 0)

        assertNotEquals(code1100, code1400)
        assertNotEquals(code1400, code1430)
        assertNotEquals(code0800, code1100)
    }

    @Test
    fun dailyReminderRequestCode_sameTime_sameCode() {
        assertEquals(
            NotificationScheduler.dailyReminderRequestCode(9, 15),
            NotificationScheduler.dailyReminderRequestCode(9, 15),
        )
    }

    @Test
    fun dailyReminderRequestCode_rangeIsAbove1000() {
        val code = NotificationScheduler.dailyReminderRequestCode(0, 0)
        assert(code >= 1000) { "Request code should be >= 1000, was $code" }
    }

    // formatTimeKey

    @Test
    fun formatTimeKey_singleDigits_paddedCorrectly() {
        assertEquals("08:05", NotificationScheduler.formatTimeKey(8, 5))
    }

    @Test
    fun formatTimeKey_doubleDigits_noChange() {
        assertEquals("14:30", NotificationScheduler.formatTimeKey(14, 30))
    }

    @Test
    fun formatTimeKey_midnight_paddedCorrectly() {
        assertEquals("00:00", NotificationScheduler.formatTimeKey(0, 0))
    }

    // nextAlarmMillis

    @Test
    fun nextAlarmMillis_futureTimeToday_returnsTodayMillis() {
        val now = Calendar.getInstance()
        val futureHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
        // Only test if we're not in the last hour (to avoid day rollover)
        if (now.get(Calendar.HOUR_OF_DAY) < 23) {
            val result = NotificationScheduler.nextAlarmMillis(futureHour, 0)
            assert(result > System.currentTimeMillis()) { "Should be in the future" }
            assert(result < System.currentTimeMillis() + 2 * 60 * 60 * 1000L) { "Should be within 2 hours" }
        }
    }

    @Test
    fun nextAlarmMillis_pastTimeToday_returnsTomorrowMillis() {
        val now = Calendar.getInstance()
        val pastHour = maxOf(0, now.get(Calendar.HOUR_OF_DAY) - 1)
        val result = NotificationScheduler.nextAlarmMillis(pastHour, 0)
        val oneDayMs = 24 * 60 * 60 * 1000L
        assert(result > System.currentTimeMillis()) { "Should be in the future" }
        assert(result <= System.currentTimeMillis() + oneDayMs + 60_000L) {
            "Should be within ~24 hours"
        }
    }
}

package ca.gbc.comp3074.nicholas.plantpal

import ca.gbc.comp3074.nicholas.plantpal.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** Checks when the daily reminder (FR6) fires next. */
class ReminderSchedulerTest {

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 10, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun laterToday_waitsUntilThen() {
        assertEquals(2 * hour + 30 * minute, ReminderScheduler.delayUntilNext(at(6, 30), 9, 0))
    }

    @Test
    fun alreadyPassed_waitsUntilTomorrow() {
        assertEquals(23 * hour, ReminderScheduler.delayUntilNext(at(10, 0), 9, 0))
    }

    @Test
    fun exactlyNow_waitsAFullDay() {
        assertEquals(24 * hour, ReminderScheduler.delayUntilNext(at(9, 0), 9, 0))
    }
}

package ca.gbc.comp3074.nicholas.plantpal.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ca.gbc.comp3074.nicholas.plantpal.data.SettingsStore
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** FR6: schedules the daily watering reminder with WorkManager. */
object ReminderScheduler {
    private const val DAILY_WORK = "daily_watering_reminder"
    private const val TEST_WORK = "test_watering_reminder"

    /**
     * Starts or stops the daily reminder to match the settings.
     * [reschedule] = true after the user changes the time, so the new time takes effect;
     * false on app start, so an existing schedule is left alone.
     */
    fun apply(context: Context, settings: SettingsStore, reschedule: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.notificationsEnabled) {
            workManager.cancelUniqueWork(DAILY_WORK)
            return
        }
        val delay = delayUntilNext(System.currentTimeMillis(), settings.reminderHour, settings.reminderMinute)
        val request = PeriodicWorkRequestBuilder<WateringReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            if (reschedule) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Runs the reminder right away (Settings > Send test reminder), even if nothing is due. */
    fun sendTestNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WateringReminderWorker>()
            .setInputData(workDataOf(WateringReminderWorker.KEY_TEST to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TEST_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** Milliseconds from [now] until the next [hour]:[minute] (tomorrow if that time has passed). */
    fun delayUntilNext(now: Long, hour: Int, minute: Int): Long {
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now
    }
}

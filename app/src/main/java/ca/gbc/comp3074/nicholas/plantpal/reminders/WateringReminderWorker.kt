package ca.gbc.comp3074.nicholas.plantpal.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.gbc.comp3074.nicholas.plantpal.MainActivity
import ca.gbc.comp3074.nicholas.plantpal.PlantPalApp
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import java.util.Locale

/**
 * FR6: once a day, posts a notification listing the plants that are due or overdue.
 * If rain-aware tips are on and today's rain reaches the threshold, it also says that
 * the outdoor plants can skip watering (FR7).
 */
class WateringReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PlantPalApp
        val settings = app.settings
        val isTest = inputData.getBoolean(KEY_TEST, false)
        if (!isTest && !settings.notificationsEnabled) return Result.success()
        if (!canNotify(applicationContext)) return Result.success()

        val now = System.currentTimeMillis()
        val due = app.repository.allPlants().filter { it.isDue(now) }
        if (due.isEmpty() && !isTest) return Result.success()

        val rainMm = if (settings.rainTipsEnabled && due.any { it.isOutdoor }) todayRainMm(app, now) else null
        val rainSkip = rainMm != null && rainMm >= settings.rainThresholdMm

        val res = applicationContext.resources
        val title: String
        val text: String
        if (due.isEmpty()) {
            title = res.getString(R.string.reminder_none_title)
            text = res.getString(R.string.reminder_none_text)
        } else {
            title = res.getQuantityString(R.plurals.reminder_title, due.size, due.size)
            val lines = due.sortedBy { it.daysUntilWatering(now) }.map { describe(it, now) }.toMutableList()
            if (rainSkip) {
                lines += res.getString(
                    R.string.reminder_rain_line,
                    String.format(Locale.getDefault(), "%.1f", rainMm)
                )
            }
            text = lines.joinToString("\n")
        }
        post(title, text)
        return Result.success()
    }

    private fun describe(plant: Plant, now: Long): String {
        val days = plant.daysUntilWatering(now)
        val res = applicationContext.resources
        return if (days < 0) {
            res.getQuantityString(R.plurals.reminder_line_overdue, -days, plant.name, -days)
        } else {
            res.getString(R.string.reminder_line_due, plant.name)
        }
    }

    /** Today's forecast rain, using a fresh download or a forecast saved earlier today. */
    private suspend fun todayRainMm(app: PlantPalApp, now: Long): Double? {
        val forecast = try { app.weather.forecast() } catch (e: Exception) { null } ?: return null
        val sameDay = DateUtils.startOfDay(forecast.fetchedAt) == DateUtils.startOfDay(now)
        return if (sameDay) forecast.todayRainMm else null
    }

    private fun post(title: String, text: String) {
        val open = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the post; nothing to show.
        }
    }

    companion object {
        const val KEY_TEST = "test"
        // High importance so the reminder pops up on screen. Android never raises the importance
        // of an existing channel, so this uses a new ID and removes the old default-importance one.
        const val CHANNEL_ID = "watering_reminders_high"
        private const val OLD_CHANNEL_ID = "watering_reminders"
        private const val NOTIFICATION_ID = 1001

        fun canNotify(context: Context): Boolean =
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()

        /** Creates the notification channel (Android 8+). Safe to call more than once. */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.reminder_channel_description) }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(OLD_CHANNEL_ID)
            manager.createNotificationChannel(channel)
        }
    }
}

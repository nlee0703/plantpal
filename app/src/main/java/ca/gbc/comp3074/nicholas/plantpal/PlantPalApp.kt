package ca.gbc.comp3074.nicholas.plantpal

import android.app.Application
import ca.gbc.comp3074.nicholas.plantpal.data.AppDatabase
import ca.gbc.comp3074.nicholas.plantpal.data.PlantRepository
import ca.gbc.comp3074.nicholas.plantpal.data.SettingsStore
import ca.gbc.comp3074.nicholas.plantpal.reminders.ReminderScheduler
import ca.gbc.comp3074.nicholas.plantpal.reminders.WateringReminderWorker
import ca.gbc.comp3074.nicholas.plantpal.weather.WeatherRepository

/**
 * Application class that owns the single instances (simple manual dependency injection)
 * shared by every ViewModel.
 */
class PlantPalApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val repository: PlantRepository by lazy {
        PlantRepository(database.plantDao(), database.careLogDao(), filesDir)
    }
    val weather: WeatherRepository by lazy { WeatherRepository(settings) }

    override fun onCreate() {
        super.onCreate()
        // FR6: make sure the daily reminder exists (keeps an existing schedule as is).
        WateringReminderWorker.createChannel(this)
        ReminderScheduler.apply(this, settings, reschedule = false)
    }
}

package ca.gbc.comp3074.nicholas.plantpal.data

import android.content.Context
import androidx.core.content.edit

/** User settings (FR9) persisted in SharedPreferences. Defaults to Toronto. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("plantpal_settings", Context.MODE_PRIVATE)

    var cityName: String
        get() = prefs.getString(KEY_CITY, "Toronto, Ontario") ?: "Toronto, Ontario"
        set(value) = prefs.edit { putString(KEY_CITY, value) }

    var latitude: Double
        get() = prefs.getFloat(KEY_LAT, 43.70011f).toDouble()
        set(value) = prefs.edit { putFloat(KEY_LAT, value.toFloat()) }

    var longitude: Double
        get() = prefs.getFloat(KEY_LON, -79.4163f).toDouble()
        set(value) = prefs.edit { putFloat(KEY_LON, value.toFloat()) }

    var reminderHour: Int
        get() = prefs.getInt(KEY_HOUR, 9)
        set(value) = prefs.edit { putInt(KEY_HOUR, value) }

    var reminderMinute: Int
        get() = prefs.getInt(KEY_MINUTE, 0)
        set(value) = prefs.edit { putInt(KEY_MINUTE, value) }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFY, value) }

    var rainTipsEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAIN_TIPS, true)
        set(value) = prefs.edit { putBoolean(KEY_RAIN_TIPS, value) }

    var rainThresholdMm: Double
        get() = prefs.getFloat(KEY_THRESHOLD, 5f).toDouble()
        set(value) = prefs.edit { putFloat(KEY_THRESHOLD, value.toFloat()) }

    /** Last successful forecast JSON, used when the device is offline. */
    var cachedForecastJson: String?
        get() = prefs.getString(KEY_CACHE, null)
        set(value) = prefs.edit { putString(KEY_CACHE, value) }

    var cachedForecastTime: Long
        get() = prefs.getLong(KEY_CACHE_TIME, 0L)
        set(value) = prefs.edit { putLong(KEY_CACHE_TIME, value) }

    /** Sample plants are added only on the very first launch. */
    var sampleDataSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit { putBoolean(KEY_SEEDED, value) }

    fun clearForecastCache() = prefs.edit { remove(KEY_CACHE); remove(KEY_CACHE_TIME) }

    private companion object {
        const val KEY_CITY = "city_name"
        const val KEY_LAT = "city_lat"
        const val KEY_LON = "city_lon"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
        const val KEY_NOTIFY = "notifications_enabled"
        const val KEY_RAIN_TIPS = "rain_tips_enabled"
        const val KEY_THRESHOLD = "rain_threshold_mm"
        const val KEY_CACHE = "forecast_cache"
        const val KEY_CACHE_TIME = "forecast_cache_time"
        const val KEY_SEEDED = "sample_data_seeded"
    }
}

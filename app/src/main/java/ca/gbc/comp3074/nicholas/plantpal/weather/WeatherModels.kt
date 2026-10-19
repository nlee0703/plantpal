package ca.gbc.comp3074.nicholas.plantpal.weather

import ca.gbc.comp3074.nicholas.plantpal.R

data class DayForecast(
    val date: String,
    val weatherCode: Int,
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationMm: Double
)

data class Forecast(
    val cityName: String,
    val currentTempC: Double,
    val currentCode: Int,
    val days: List<DayForecast>,
    /** When the data was downloaded (epoch millis). */
    val fetchedAt: Long,
    /** True when the network failed and this is the last saved forecast. */
    val fromCache: Boolean
) {
    val todayRainMm: Double get() = days.firstOrNull()?.precipitationMm ?: 0.0
}

data class CityResult(
    val name: String,
    val admin: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = listOf(name, admin, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

/** Maps WMO weather codes used by Open-Meteo to a short description and an icon. */
object WeatherCodes {
    fun description(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown"
    }

    fun icon(code: Int): Int = when (code) {
        0, 1 -> R.drawable.ic_weather_sunny
        2, 3, 45, 48 -> R.drawable.ic_weather_cloudy
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
        else -> R.drawable.ic_weather_rain
    }
}

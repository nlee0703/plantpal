package ca.gbc.comp3074.nicholas.plantpal.weather

import ca.gbc.comp3074.nicholas.plantpal.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Talks to the free Open-Meteo APIs (no API key needed) - FR7.
 * Successful forecasts are cached so the app degrades gracefully offline.
 */
class WeatherRepository(private val settings: SettingsStore) {

    /** Downloads the 5-day forecast; falls back to the cached copy if the request fails. */
    suspend fun forecast(): Forecast? = withContext(Dispatchers.IO) {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,weather_code" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum" +
                "&timezone=auto&forecast_days=5",
            settings.latitude, settings.longitude
        )
        try {
            val json = httpGet(url)
            val parsed = parseForecast(json, System.currentTimeMillis(), fromCache = false)
            settings.cachedForecastJson = json
            settings.cachedForecastTime = parsed.fetchedAt
            parsed
        } catch (e: Exception) {
            cachedForecast()
        }
    }

    /** Last saved forecast without touching the network. */
    fun cachedForecast(): Forecast? {
        val json = settings.cachedForecastJson ?: return null
        return try {
            parseForecast(json, settings.cachedForecastTime, fromCache = true)
        } catch (e: Exception) {
            null
        }
    }

    /** Looks up cities by name with the Open-Meteo Geocoding API (FR9). */
    suspend fun searchCity(query: String): List<CityResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(
            httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json")
        )
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            CityResult(
                name = r.optString("name"),
                admin = r.optString("admin1"),
                country = r.optString("country"),
                latitude = r.getDouble("latitude"),
                longitude = r.getDouble("longitude")
            )
        }
    }

    private fun parseForecast(json: String, fetchedAt: Long, fromCache: Boolean): Forecast {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxT = daily.getJSONArray("temperature_2m_max")
        val minT = daily.getJSONArray("temperature_2m_min")
        val rain = daily.getJSONArray("precipitation_sum")
        val days = (0 until dates.length()).map { i ->
            DayForecast(
                date = dates.getString(i),
                weatherCode = codes.optInt(i),
                maxTempC = maxT.optDouble(i, 0.0),
                minTempC = minT.optDouble(i, 0.0),
                precipitationMm = rain.optDouble(i, 0.0).let { if (it.isNaN()) 0.0 else it }
            )
        }
        return Forecast(
            cityName = settings.cityName,
            currentTempC = current.optDouble("temperature_2m", 0.0),
            currentCode = current.optInt("weather_code"),
            days = days,
            fetchedAt = fetchedAt,
            fromCache = fromCache
        )
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

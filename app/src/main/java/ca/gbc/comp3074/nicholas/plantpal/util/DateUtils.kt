package ca.gbc.comp3074.nicholas.plantpal.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Calendar-based date helpers (works on minSdk 24 without java.time desugaring). */
object DateUtils {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    /** Whole days from [from] to [to]; rounding absorbs daylight-saving shifts. */
    fun daysBetween(from: Long, to: Long): Int = ((to - from).toDouble() / DAY_MS).roundToInt()

    fun formatDate(millis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

    fun formatDateTime(millis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

    fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)
    }

    /** "2026-10-19" (Open-Meteo format) -> "Mon". */
    fun shortWeekday(isoDate: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
        SimpleDateFormat("EEE", Locale.getDefault()).format(parsed!!)
    } catch (e: Exception) {
        isoDate
    }
}

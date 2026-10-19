package ca.gbc.comp3074.nicholas.plantpal.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils

/** A plant the user owns (FR1). Stored in the "plants" table. */
@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: String = "",
    @ColumnInfo(name = "is_outdoor") val isOutdoor: Boolean = false,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 7,
    /** Epoch millis of the last time the plant was watered. */
    @ColumnInfo(name = "last_watered") val lastWatered: Long = System.currentTimeMillis(),
    val notes: String = "",
    /** Absolute path of the photo copied into app-private storage, or null. */
    @ColumnInfo(name = "photo_path") val photoPath: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    // Functions (not properties) so Room does not treat them as columns.
    /** Start of the day on which the plant should next be watered. */
    fun nextWatering(): Long = DateUtils.addDays(DateUtils.startOfDay(lastWatered), intervalDays)

    /** Days from today until the next watering: negative = overdue, 0 = due today. */
    fun daysUntilWatering(now: Long = System.currentTimeMillis()): Int =
        DateUtils.daysBetween(DateUtils.startOfDay(now), nextWatering())

    fun isDue(now: Long = System.currentTimeMillis()): Boolean = daysUntilWatering(now) <= 0
}

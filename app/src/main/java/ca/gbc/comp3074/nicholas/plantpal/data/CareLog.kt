package ca.gbc.comp3074.nicholas.plantpal.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One entry in a plant's care history (FR5, FR8). Deleted together with its plant. */
@Entity(
    tableName = "care_logs",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plant_id")]
)
data class CareLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plant_id") val plantId: Long,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    companion object {
        const val TYPE_WATERED = "WATERED"
        const val TYPE_SKIPPED_RAIN = "SKIPPED_RAIN"
    }
}

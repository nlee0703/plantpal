package ca.gbc.comp3074.nicholas.plantpal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CareLogDao {
    @Query("SELECT * FROM care_logs WHERE plant_id = :plantId ORDER BY timestamp DESC")
    fun observeForPlant(plantId: Long): Flow<List<CareLog>>

    @Insert
    suspend fun insert(log: CareLog): Long
}

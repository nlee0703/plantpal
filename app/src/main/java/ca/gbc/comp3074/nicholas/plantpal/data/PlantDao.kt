package ca.gbc.comp3074.nicholas.plantpal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Plant>>

    @Query("SELECT * FROM plants")
    suspend fun getAll(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observe(id: Long): Flow<Plant?>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun get(id: Long): Plant?

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun count(): Int

    @Insert
    suspend fun insert(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)
}

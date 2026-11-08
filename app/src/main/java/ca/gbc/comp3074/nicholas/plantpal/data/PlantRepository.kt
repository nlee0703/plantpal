package ca.gbc.comp3074.nicholas.plantpal.data

import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import ca.gbc.comp3074.nicholas.plantpal.util.SamplePhotos
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Single entry point the ViewModels use for plant data. */
class PlantRepository(
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao,
    private val filesDir: File
) {
    val plants: Flow<List<Plant>> = plantDao.observeAll()

    fun plant(id: Long): Flow<Plant?> = plantDao.observe(id)
    fun careHistory(plantId: Long): Flow<List<CareLog>> = careLogDao.observeForPlant(plantId)

    suspend fun get(id: Long): Plant? = plantDao.get(id)

    /** One-off snapshot of every plant, used by the daily reminder (FR6). */
    suspend fun allPlants(): List<Plant> = plantDao.getAll()

    suspend fun save(plant: Plant): Long =
        if (plant.id == 0L) plantDao.insert(plant) else plantDao.update(plant).let { plant.id }

    /** Deletes the plant; its care logs go with it (ON DELETE CASCADE) and so does its photo. */
    suspend fun delete(plant: Plant) {
        plantDao.delete(plant)
        plant.photoPath?.takeIf { !SamplePhotos.isBuiltIn(it) }?.let { File(it).delete() }
    }

    /** FR5: record a watering and restart the plant's schedule from now. */
    suspend fun markWatered(plant: Plant, now: Long = System.currentTimeMillis()) {
        plantDao.update(plant.copy(lastWatered = now))
        careLogDao.insert(CareLog(plantId = plant.id, type = CareLog.TYPE_WATERED, timestamp = now))
    }

    /** FR7: rain did the watering, so log a skip and restart the schedule. */
    suspend fun markSkippedForRain(plant: Plant, rainMm: Double, now: Long = System.currentTimeMillis()) {
        plantDao.update(plant.copy(lastWatered = now))
        careLogDao.insert(
            CareLog(
                plantId = plant.id,
                type = CareLog.TYPE_SKIPPED_RAIN,
                timestamp = now,
                note = String.format(java.util.Locale.getDefault(), "%.1f mm forecast", rainMm)
            )
        )
    }

    /** Adds a few example plants on first launch so the prototype has something to show. */
    suspend fun seedIfEmpty() {
        if (plantDao.count() > 0) return
        val now = System.currentTimeMillis()
        val samples = listOf(
            Plant(name = "Monstera", species = "Monstera deliciosa", intervalDays = 7,
                lastWatered = DateUtils.addDays(now, -7), notes = "Bright, indirect light."),
            Plant(name = "Basil", species = "Ocimum basilicum", intervalDays = 2,
                lastWatered = DateUtils.addDays(now, -4), notes = "Kitchen window. Likes moist soil."),
            Plant(name = "Balcony Tomatoes", species = "Solanum lycopersicum", isOutdoor = true,
                intervalDays = 2, lastWatered = DateUtils.addDays(now, -2)),
            Plant(name = "Snake Plant", species = "Dracaena trifasciata", intervalDays = 14,
                lastWatered = DateUtils.addDays(now, -3), notes = "Let soil dry out completely."),
            Plant(name = "Lavender", species = "Lavandula", isOutdoor = true, intervalDays = 4,
                lastWatered = DateUtils.addDays(now, -1))
        )
        samples.forEach { sample ->
            val p = sample.copy(photoPath = SamplePhotos.forSampleName[sample.name])
            val id = plantDao.insert(p)
            careLogDao.insert(CareLog(plantId = id, type = CareLog.TYPE_WATERED, timestamp = p.lastWatered))
        }
    }

    /** Gives sample plants created by an earlier version (no photo) their pictures. */
    suspend fun attachSamplePhotos() {
        plantDao.getAll()
            .filter { it.photoPath == null && it.name in SamplePhotos.forSampleName }
            .forEach { plantDao.update(it.copy(photoPath = SamplePhotos.forSampleName[it.name])) }
    }

    /** Folder where plant photos are copied (app-private storage, FR2). */
    fun photoDir(): File = File(filesDir, "photos").apply { mkdirs() }
}

package ca.gbc.comp3074.nicholas.plantpal.ui.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import ca.gbc.comp3074.nicholas.plantpal.PlantPalApp
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Holds the form state for Add / Edit Plant so it survives rotation.
 * Text fields restore themselves; the non-text values live here.
 */
class AddEditPlantViewModel(app: Application, state: SavedStateHandle) : AndroidViewModel(app) {

    private val repository = (app as PlantPalApp).repository

    val plantId: Long = state.get<Long>("plantId") ?: 0L
    val isEdit: Boolean get() = plantId != 0L

    private var original: Plant? = null
    private var loaded = false

    var intervalDays = 7
    var isOutdoor = false
    var lastWatered: Long = System.currentTimeMillis()
    var photoPath: String? = null
        private set

    /** Uri the camera app is writing to while it is open. */
    var pendingCameraUri: Uri? = null

    /** Loads the plant once. Returns it only on the first call so the form is filled once. */
    suspend fun loadOnce(): Plant? {
        if (loaded) return null
        loaded = true
        if (!isEdit) return null
        val plant = repository.get(plantId) ?: return null
        original = plant
        intervalDays = plant.intervalDays
        isOutdoor = plant.isOutdoor
        lastWatered = plant.lastWatered
        photoPath = plant.photoPath
        return plant
    }

    /** Copies a picked or captured photo into app storage and makes it the current photo. */
    suspend fun setPhoto(source: Uri): Boolean = withContext(Dispatchers.IO) {
        val path = PhotoStorage.copyToPrivateStorage(getApplication<Application>(), source, repository.photoDir())
            ?: return@withContext false
        discardUnsavedPhoto()
        photoPath = path
        true
    }

    suspend fun save(name: String, species: String, notes: String) {
        val base = original ?: Plant(name = name)
        val plant = base.copy(
            name = name.trim(),
            species = species.trim(),
            notes = notes.trim(),
            isOutdoor = isOutdoor,
            intervalDays = intervalDays,
            lastWatered = lastWatered,
            photoPath = photoPath
        )
        repository.save(plant)
        // The old photo was replaced, so remove it from storage.
        original?.photoPath?.let { old -> if (old != photoPath) File(old).delete() }
        original = plant
    }

    /** Deletes a photo the user picked but never saved. */
    private fun discardUnsavedPhoto() {
        val current = photoPath ?: return
        if (current != original?.photoPath) File(current).delete()
    }

    fun cancel() = discardUnsavedPhoto()

    companion object {
        const val MIN_INTERVAL = 1
        const val MAX_INTERVAL = 60
    }
}

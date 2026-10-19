package ca.gbc.comp3074.nicholas.plantpal.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.gbc.comp3074.nicholas.plantpal.PlantPalApp
import ca.gbc.comp3074.nicholas.plantpal.data.CareLog
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlantDetailViewModel(app: Application, state: SavedStateHandle) : AndroidViewModel(app) {

    private val repository = (app as PlantPalApp).repository
    val plantId: Long = state.get<Long>("plantId") ?: 0L

    val plant: StateFlow<Plant?> = repository.plant(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<CareLog>> = repository.careHistory(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markWatered() {
        val p = plant.value ?: return
        viewModelScope.launch { repository.markWatered(p) }
    }

    suspend fun delete() {
        plant.value?.let { repository.delete(it) }
    }
}

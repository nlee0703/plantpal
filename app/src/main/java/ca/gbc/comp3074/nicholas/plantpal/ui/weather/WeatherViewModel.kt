package ca.gbc.comp3074.nicholas.plantpal.ui.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.gbc.comp3074.nicholas.plantpal.PlantPalApp
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.weather.Forecast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val plantPal = app as PlantPalApp
    val settings = plantPal.settings

    private val _forecast = MutableStateFlow(plantPal.weather.cachedForecast())
    val forecast: StateFlow<Forecast?> = _forecast.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val outdoorPlants: StateFlow<List<Plant>> = plantPal.repository.plants
        .map { list -> list.filter { it.isOutdoor }.sortedBy { it.daysUntilWatering() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refresh() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _forecast.value = plantPal.weather.forecast()
            _loading.value = false
        }
    }

    fun skipForRain(plant: Plant) {
        val rain = _forecast.value?.todayRainMm ?: return
        viewModelScope.launch { plantPal.repository.markSkippedForRain(plant, rain) }
    }
}

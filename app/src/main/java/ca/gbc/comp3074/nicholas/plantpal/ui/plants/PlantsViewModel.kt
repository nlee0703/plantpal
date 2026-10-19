package ca.gbc.comp3074.nicholas.plantpal.ui.plants

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlantsViewModel(app: Application) : AndroidViewModel(app) {

    enum class Filter { ALL, DUE, INDOOR, OUTDOOR }

    private val plantPal = app as PlantPalApp
    private val repository = plantPal.repository
    val settings = plantPal.settings

    private val filter = MutableStateFlow(Filter.ALL)
    private val query = MutableStateFlow("")

    /** All plants, kept so the screen can tell "no plants yet" from "no matches". */
    val allPlants: StateFlow<List<Plant>> = repository.plants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Filtered (FR10) list, most urgent first (FR3). */
    val plants: StateFlow<List<Plant>> = combine(repository.plants, filter, query) { list, f, q ->
        list.filter { plant ->
            when (f) {
                Filter.ALL -> true
                Filter.DUE -> plant.isDue()
                Filter.INDOOR -> !plant.isOutdoor
                Filter.OUTDOOR -> plant.isOutdoor
            } && (q.isBlank() || plant.name.contains(q.trim(), ignoreCase = true))
        }.sortedWith(compareBy<Plant> { it.daysUntilWatering() }.thenBy { it.name.lowercase() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _forecast = MutableStateFlow(plantPal.weather.cachedForecast())
    val forecast: StateFlow<Forecast?> = _forecast.asStateFlow()

    init {
        if (!settings.sampleDataSeeded) {
            settings.sampleDataSeeded = true
            viewModelScope.launch { repository.seedIfEmpty() }
        }
    }

    fun setFilter(value: Filter) { filter.value = value }
    fun setQuery(value: String) { query.value = value }
    val currentQuery: String get() = query.value

    fun refreshWeather() {
        viewModelScope.launch { _forecast.value = plantPal.weather.forecast() }
    }

    /** True when today's forecast rain means outdoor plants can skip watering. */
    fun isRainyToday(): Boolean {
        val f = _forecast.value ?: return false
        return settings.rainTipsEnabled && f.todayRainMm >= settings.rainThresholdMm
    }

    fun water(plant: Plant) {
        viewModelScope.launch { repository.markWatered(plant) }
    }
}

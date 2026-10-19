package ca.gbc.comp3074.nicholas.plantpal.ui.plants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.gbc.comp3074.nicholas.plantpal.MainActivity
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.databinding.FragmentPlantsBinding
import ca.gbc.comp3074.nicholas.plantpal.weather.Forecast
import ca.gbc.comp3074.nicholas.plantpal.weather.WeatherCodes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Screen 1 - My Plants (home). */
class PlantsFragment : Fragment() {

    private var _binding: FragmentPlantsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlantsViewModel by viewModels()
    private lateinit var adapter: PlantAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = PlantAdapter(onOpen = ::openPlant, onWater = ::waterPlant)
        binding.plantList.adapter = adapter

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.addEditPlantFragment, bundleOf("plantId" to 0L))
        }
        binding.weatherBanner.setOnClickListener {
            (requireActivity() as MainActivity).showTab(R.id.weatherFragment)
        }
        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setFilter(
                when (checkedIds.firstOrNull()) {
                    R.id.chip_due -> PlantsViewModel.Filter.DUE
                    R.id.chip_indoor -> PlantsViewModel.Filter.INDOOR
                    R.id.chip_outdoor -> PlantsViewModel.Filter.OUTDOOR
                    else -> PlantsViewModel.Filter.ALL
                }
            )
        }
        setUpSearchMenu()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(viewModel.plants, viewModel.allPlants) { shown, all -> shown to all }
                        .collect { (shown, all) ->
                            adapter.submitList(shown)
                            binding.emptyView.isVisible = shown.isEmpty()
                            binding.emptyView.setText(
                                if (all.isEmpty()) R.string.empty_no_plants else R.string.empty_no_matches
                            )
                            binding.summaryText.text = summary(all)
                        }
                }
                launch {
                    viewModel.forecast.collect { forecast ->
                        renderBanner(forecast)
                        adapter.rainyToday = viewModel.isRainyToday()
                    }
                }
            }
        }
        viewModel.refreshWeather()
    }

    private fun summary(all: List<Plant>): String {
        val due = all.count { it.isDue() }
        return resources.getQuantityString(R.plurals.summary_plants, all.size, all.size, due)
    }

    private fun renderBanner(forecast: Forecast?) {
        if (forecast == null) {
            binding.weatherIcon.setImageResource(R.drawable.ic_weather_cloudy)
            binding.weatherTitle.setText(R.string.weather_unavailable)
            binding.weatherSubtitle.setText(R.string.weather_unavailable_hint)
            return
        }
        val city = forecast.cityName.substringBefore(',')
        binding.weatherIcon.setImageResource(WeatherCodes.icon(forecast.currentCode))
        binding.weatherTitle.text = getString(
            R.string.banner_title, city, forecast.currentTempC.roundToInt(),
            WeatherCodes.description(forecast.currentCode)
        )
        val rain = forecast.todayRainMm
        val sub = when {
            !viewModel.settings.rainTipsEnabled -> getString(R.string.banner_tips_off)
            rain >= viewModel.settings.rainThresholdMm -> getString(R.string.banner_rain, rain)
            else -> getString(R.string.banner_no_rain, rain)
        }
        binding.weatherSubtitle.text =
            if (forecast.fromCache) getString(R.string.banner_offline_suffix, sub) else sub
    }

    private fun setUpSearchMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_plants, menu)
                val searchView = menu.findItem(R.id.action_search).actionView as SearchView
                searchView.queryHint = getString(R.string.search_hint)
                if (viewModel.currentQuery.isNotEmpty()) {
                    menu.findItem(R.id.action_search).expandActionView()
                    searchView.setQuery(viewModel.currentQuery, false)
                }
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = true
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setQuery(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun openPlant(plant: Plant) {
        findNavController().navigate(R.id.plantDetailFragment, bundleOf("plantId" to plant.id))
    }

    private fun waterPlant(plant: Plant) {
        viewModel.water(plant)
        Snackbar.make(binding.root, getString(R.string.watered_message, plant.name), Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAdd)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package ca.gbc.comp3074.nicholas.plantpal.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.databinding.FragmentWeatherBinding
import ca.gbc.comp3074.nicholas.plantpal.databinding.ItemForecastDayBinding
import ca.gbc.comp3074.nicholas.plantpal.databinding.ItemWeatherTipBinding
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import ca.gbc.comp3074.nicholas.plantpal.weather.Forecast
import ca.gbc.comp3074.nicholas.plantpal.weather.WeatherCodes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Screen 4 - Weather Tips (FR7). */
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
                menuInflater.inflate(R.menu.menu_weather, menu)

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_refresh) { viewModel.refresh(); return true }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.loading.collect { binding.progress.isVisible = it } }
                launch {
                    combine(viewModel.forecast, viewModel.outdoorPlants) { f, p -> f to p }
                        .collect { (forecast, plants) -> render(forecast, plants) }
                }
            }
        }
        viewModel.refresh()
    }

    private fun render(forecast: Forecast?, outdoor: List<Plant>) {
        binding.content.isVisible = forecast != null
        binding.errorText.isVisible = forecast == null
        if (forecast == null) return

        binding.offlineBanner.isVisible = forecast.fromCache
        binding.offlineBanner.text =
            getString(R.string.offline_notice, DateUtils.formatDateTime(forecast.fetchedAt))

        binding.cityName.text = forecast.cityName
        binding.currentTemp.text = getString(R.string.temp_c, forecast.currentTempC.roundToInt())
        binding.currentDesc.text = WeatherCodes.description(forecast.currentCode)
        binding.currentIcon.setImageResource(WeatherCodes.icon(forecast.currentCode))
        binding.updatedText.text = getString(R.string.updated_at, DateUtils.formatDateTime(forecast.fetchedAt))

        val threshold = viewModel.settings.rainThresholdMm
        binding.forecastRow.removeAllViews()
        forecast.days.forEachIndexed { index, day ->
            val item = ItemForecastDayBinding.inflate(layoutInflater, binding.forecastRow, false)
            item.dayName.text = if (index == 0) getString(R.string.today) else DateUtils.shortWeekday(day.date)
            item.dayIcon.setImageResource(WeatherCodes.icon(day.weatherCode))
            item.dayTemp.text = getString(R.string.temp_c, day.maxTempC.roundToInt())
            item.dayRain.text = getString(R.string.rain_mm, day.precipitationMm)
            val rainy = day.precipitationMm >= threshold
            item.root.setBackgroundResource(if (rainy) R.drawable.bg_rainy_day else 0)
            if (rainy) item.dayRain.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rain_fg))
            binding.forecastRow.addView(item.root)
        }
        binding.thresholdNote.text = getString(R.string.threshold_note, threshold)

        renderTips(forecast, outdoor)
    }

    private fun renderTips(forecast: Forecast, outdoor: List<Plant>) {
        binding.tipsContainer.removeAllViews()
        val tipsOn = viewModel.settings.rainTipsEnabled
        binding.tipsOff.isVisible = !tipsOn
        binding.tipsEmpty.isVisible = outdoor.isEmpty()

        val threshold = viewModel.settings.rainThresholdMm
        val rainToday = tipsOn && forecast.todayRainMm >= threshold
        // First upcoming day (after today) with enough rain.
        val nextRainIndex = forecast.days.indexOfFirst { it.precipitationMm >= threshold }
            .takeIf { it > 0 }

        outdoor.forEach { plant ->
            val tip = ItemWeatherTipBinding.inflate(layoutInflater, binding.tipsContainer, false)
            val days = plant.daysUntilWatering()
            tip.tipPlantName.text = plant.name
            var canSkip = false
            tip.tipText.text = when {
                rainToday && days <= 0 -> {
                    canSkip = true
                    getString(R.string.tip_skip_today, forecast.todayRainMm)
                }
                rainToday -> getString(R.string.tip_rain_not_due, forecast.todayRainMm)
                days <= 0 -> getString(R.string.tip_water_today)
                tipsOn && nextRainIndex != null && nextRainIndex <= days -> {
                    val d = forecast.days[nextRainIndex]
                    getString(R.string.tip_rain_coming, DateUtils.shortWeekday(d.date), d.precipitationMm)
                }
                else -> resources.getQuantityString(R.plurals.tip_next_in, days, days)
            }
            tip.tipIcon.setImageResource(if (canSkip) R.drawable.ic_weather_rain else R.drawable.ic_water_drop)
            tip.btnSkip.isVisible = canSkip
            tip.btnSkip.setOnClickListener {
                viewModel.skipForRain(plant)
                Snackbar.make(binding.root, getString(R.string.skip_logged, plant.name), Snackbar.LENGTH_SHORT).show()
            }
            binding.tipsContainer.addView(tip.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

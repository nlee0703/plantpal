package ca.gbc.comp3074.nicholas.plantpal.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ca.gbc.comp3074.nicholas.plantpal.BuildConfigInfo
import ca.gbc.comp3074.nicholas.plantpal.PlantPalApp
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.databinding.FragmentSettingsBinding
import ca.gbc.comp3074.nicholas.plantpal.reminders.ReminderScheduler
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import ca.gbc.comp3074.nicholas.plantpal.weather.CityResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch

/** Screen 5 - Settings (FR9). Every change is saved immediately. */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as PlantPalApp
    private val settings get() = app.settings

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            settings.notificationsEnabled = granted
            _binding?.switchNotifications?.isChecked = granted
            updateReminderSchedule()
            if (!granted) {
                Toast.makeText(requireContext(), R.string.notifications_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderCity()
        binding.btnSearchCity.setOnClickListener { searchCity() }
        binding.cityInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { searchCity(); true } else false
        }

        renderReminderTime()
        binding.reminderTimeRow.setOnClickListener { pickReminderTime() }

        binding.switchNotifications.isChecked = settings.notificationsEnabled
        binding.switchNotifications.setOnCheckedChangeListener { _, checked ->
            if (checked && needsNotificationPermission()) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                settings.notificationsEnabled = checked
                updateReminderSchedule()
            }
        }

        binding.btnTestReminder.setOnClickListener {
            if (needsNotificationPermission()) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderScheduler.sendTestNow(requireContext())
                Toast.makeText(requireContext(), R.string.test_reminder_sent, Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchRainTips.isChecked = settings.rainTipsEnabled
        binding.switchRainTips.setOnCheckedChangeListener { _, checked ->
            settings.rainTipsEnabled = checked
            binding.thresholdLayout.isEnabled = checked
        }
        binding.thresholdLayout.isEnabled = settings.rainTipsEnabled

        if (savedInstanceState == null) {
            binding.thresholdInput.setText(formatMm(settings.rainThresholdMm))
        }
        binding.thresholdInput.doAfterTextChanged { text ->
            val value = text?.toString()?.toDoubleOrNull()
            if (value == null || value <= 0 || value > 100) {
                binding.thresholdLayout.error = getString(R.string.error_threshold)
            } else {
                binding.thresholdLayout.error = null
                settings.rainThresholdMm = value
            }
        }

        binding.aboutText.text = getString(R.string.about_text, BuildConfigInfo.versionName(requireContext()))
    }

    private fun renderCity() {
        binding.currentCity.text = getString(R.string.saved_city, settings.cityName)
    }

    private fun renderReminderTime() {
        binding.reminderTimeValue.text = DateUtils.formatTime(settings.reminderHour, settings.reminderMinute)
    }

    private fun searchCity() {
        val query = binding.cityInput.text?.toString()?.trim().orEmpty()
        if (query.length < 2) {
            binding.cityLayout.error = getString(R.string.error_city_short)
            return
        }
        binding.cityLayout.error = null
        binding.searchProgress.isVisible = true
        binding.btnSearchCity.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val results = try {
                app.weather.searchCity(query)
            } catch (e: Exception) {
                null
            }
            val b = _binding ?: return@launch
            b.searchProgress.isVisible = false
            b.btnSearchCity.isEnabled = true
            when {
                results == null -> b.cityLayout.error = getString(R.string.error_city_offline)
                results.isEmpty() -> b.cityLayout.error = getString(R.string.error_city_none, query)
                else -> chooseCity(results)
            }
        }
    }

    private fun chooseCity(results: List<CityResult>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_city)
            .setItems(results.map { it.displayName }.toTypedArray()) { _, which ->
                val city = results[which]
                settings.cityName = city.displayName
                settings.latitude = city.latitude
                settings.longitude = city.longitude
                settings.clearForecastCache()
                binding.cityInput.setText("")
                renderCity()
                Toast.makeText(requireContext(), getString(R.string.city_saved, city.name), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pickReminderTime() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(settings.reminderHour)
            .setMinute(settings.reminderMinute)
            .setTitleText(R.string.reminder_time)
            .build()
        picker.addOnPositiveButtonClickListener {
            settings.reminderHour = picker.hour
            settings.reminderMinute = picker.minute
            renderReminderTime()
            updateReminderSchedule()
        }
        picker.show(childFragmentManager, "reminder_time")
    }

    /** FR6: start, move or stop the daily WorkManager reminder to match the settings. */
    private fun updateReminderSchedule() =
        ReminderScheduler.apply(requireContext().applicationContext, settings, reschedule = true)

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun formatMm(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

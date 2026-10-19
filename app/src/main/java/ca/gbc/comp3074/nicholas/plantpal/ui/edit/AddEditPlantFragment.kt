package ca.gbc.comp3074.nicholas.plantpal.ui.edit

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.databinding.FragmentAddEditPlantBinding
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import ca.gbc.comp3074.nicholas.plantpal.util.PhotoStorage
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/** Screen 2 - Add / Edit Plant (FR1, FR2, FR4). */
class AddEditPlantFragment : Fragment() {

    private var _binding: FragmentAddEditPlantBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditPlantViewModel by viewModels()

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri)
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = viewModel.pendingCameraUri
        viewModel.pendingCameraUri = null
        if (saved && uri != null) importPhoto(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(if (viewModel.isEdit) R.string.title_edit_plant else R.string.title_add_plant)

        binding.btnGallery.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnCamera.setOnClickListener { launchCamera() }

        binding.locationToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) viewModel.isOutdoor = checkedId == R.id.btn_outdoor
        }
        binding.btnIntervalMinus.setOnClickListener { changeInterval(-1) }
        binding.btnIntervalPlus.setOnClickListener { changeInterval(+1) }
        binding.dateInput.setOnClickListener { pickLastWateredDate() }
        binding.nameInput.doAfterTextChanged { if (!it.isNullOrBlank()) binding.nameLayout.error = null }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
                menuInflater.inflate(R.menu.menu_edit, menu)

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_save) { save(); return true }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.cancel()
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            })

        viewLifecycleOwner.lifecycleScope.launch {
            val plant = viewModel.loadOnce()
            val b = _binding ?: return@launch
            if (plant != null) {
                b.nameInput.setText(plant.name)
                b.speciesInput.setText(plant.species)
                b.notesInput.setText(plant.notes)
            }
            renderState()
        }
    }

    private fun renderState() {
        val b = _binding ?: return
        b.locationToggle.check(if (viewModel.isOutdoor) R.id.btn_outdoor else R.id.btn_indoor)
        b.intervalValue.text = resources.getQuantityString(
            R.plurals.interval_days, viewModel.intervalDays, viewModel.intervalDays
        )
        b.btnIntervalMinus.isEnabled = viewModel.intervalDays > AddEditPlantViewModel.MIN_INTERVAL
        b.btnIntervalPlus.isEnabled = viewModel.intervalDays < AddEditPlantViewModel.MAX_INTERVAL
        b.dateInput.setText(DateUtils.formatDate(viewModel.lastWatered))
        PhotoStorage.load(b.photoPreview, viewModel.photoPath)
    }

    private fun changeInterval(delta: Int) {
        viewModel.intervalDays = (viewModel.intervalDays + delta)
            .coerceIn(AddEditPlantViewModel.MIN_INTERVAL, AddEditPlantViewModel.MAX_INTERVAL)
        renderState()
    }

    private fun launchCamera() {
        val dir = File(requireContext().cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        viewModel.pendingCameraUri = uri
        try {
            takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.pendingCameraUri = null
            Toast.makeText(requireContext(), R.string.no_camera_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importPhoto(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = viewModel.setPhoto(uri)
            if (!ok) Toast.makeText(requireContext(), R.string.photo_failed, Toast.LENGTH_SHORT).show()
            renderState()
        }
    }

    private fun pickLastWateredDate() {
        // MaterialDatePicker works in UTC midnight millis; convert to/from the local date.
        val local = Calendar.getInstance().apply { timeInMillis = viewModel.lastWatered }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.last_watered)
            .setSelection(utc.timeInMillis)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            val result = Calendar.getInstance().apply {
                timeInMillis = viewModel.lastWatered
                set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
            }
            viewModel.lastWatered = minOf(result.timeInMillis, System.currentTimeMillis())
            renderState()
        }
        picker.show(childFragmentManager, "last_watered")
    }

    private fun save() {
        val name = binding.nameInput.text?.toString().orEmpty()
        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_name_required)
            binding.nameInput.requestFocus()
            return
        }
        val species = binding.speciesInput.text?.toString().orEmpty()
        val notes = binding.notesInput.text?.toString().orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.save(name, species, notes)
            Toast.makeText(requireContext(), getString(R.string.saved_message, name.trim()), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

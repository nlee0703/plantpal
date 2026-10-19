package ca.gbc.comp3074.nicholas.plantpal.ui.detail

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.databinding.FragmentPlantDetailBinding
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import ca.gbc.comp3074.nicholas.plantpal.util.PhotoStorage
import ca.gbc.comp3074.nicholas.plantpal.util.PlantStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Screen 3 - Plant Detail (FR4, FR5, FR8). */
class PlantDetailFragment : Fragment() {

    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlantDetailViewModel by viewModels()
    private val historyAdapter = CareLogAdapter()
    private var deleting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.historyList.adapter = historyAdapter
        binding.btnMarkWatered.setOnClickListener {
            viewModel.markWatered()
            Snackbar.make(binding.root, R.string.watered_short, Snackbar.LENGTH_SHORT).show()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
                menuInflater.inflate(R.menu.menu_detail, menu)

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                R.id.action_edit -> {
                    findNavController().navigate(
                        R.id.addEditPlantFragment, bundleOf("plantId" to viewModel.plantId)
                    )
                    true
                }
                R.id.action_delete -> { confirmDelete(); true }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.plant.collect { it?.let(::render) } }
                launch {
                    viewModel.history.collect { logs ->
                        historyAdapter.submitList(logs)
                        binding.historyEmpty.isVisible = logs.isEmpty()
                    }
                }
            }
        }
    }

    private fun render(plant: Plant) {
        (requireActivity() as AppCompatActivity).supportActionBar?.title = plant.name
        PhotoStorage.load(binding.detailPhoto, plant.photoPath)
        binding.detailName.text = plant.name
        binding.detailSpecies.text = plant.species.ifBlank { getString(R.string.species_unknown) }
        binding.detailLocation.setText(if (plant.isOutdoor) R.string.outdoor else R.string.indoor)
        binding.detailLocation.setChipIconResource(
            if (plant.isOutdoor) R.drawable.ic_weather_sunny else R.drawable.ic_nav_plants
        )

        val status = PlantStatus.of(requireContext(), plant)
        binding.statusBadge.text = status.label
        binding.statusBadge.setTextColor(status.textColor)
        binding.statusBadge.backgroundTintList = ColorStateList.valueOf(status.backgroundColor)
        binding.nextDate.text = DateUtils.formatDate(plant.nextWatering())
        binding.detailInterval.text = resources.getQuantityString(
            R.plurals.every_days, plant.intervalDays, plant.intervalDays
        )
        binding.detailLastWatered.text = DateUtils.formatDate(plant.lastWatered)

        binding.notesCard.isVisible = plant.notes.isNotBlank()
        binding.detailNotes.text = plant.notes
    }

    private fun confirmDelete() {
        val name = viewModel.plant.value?.name ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_title, name))
            .setMessage(R.string.delete_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (deleting) return@setPositiveButton
                deleting = true
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.delete()
                    Toast.makeText(requireContext(), getString(R.string.deleted_message, name), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

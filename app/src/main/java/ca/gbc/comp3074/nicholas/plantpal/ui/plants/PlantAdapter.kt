package ca.gbc.comp3074.nicholas.plantpal.ui.plants

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.databinding.ItemPlantBinding
import ca.gbc.comp3074.nicholas.plantpal.util.PhotoStorage
import ca.gbc.comp3074.nicholas.plantpal.util.PlantStatus

/** RecyclerView adapter for the plant cards on My Plants. */
class PlantAdapter(
    private val onOpen: (Plant) -> Unit,
    private val onWater: (Plant) -> Unit
) : ListAdapter<Plant, PlantAdapter.PlantViewHolder>(Diff) {

    /** When true, due outdoor plants show a "rain" badge instead of "due". */
    var rainyToday: Boolean = false
        set(value) {
            if (field != value) { field = value; notifyItemRangeChanged(0, itemCount) }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder =
        PlantViewHolder(ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) = holder.bind(getItem(position))

    inner class PlantViewHolder(private val b: ItemPlantBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(plant: Plant) {
            val ctx = b.root.context
            b.plantName.text = plant.name
            val where = ctx.getString(if (plant.isOutdoor) R.string.outdoor else R.string.indoor)
            b.plantMeta.text = ctx.resources.getQuantityString(
                R.plurals.meta_every_days, plant.intervalDays, where, plant.intervalDays
            )
            PhotoStorage.load(b.plantPhoto, plant.photoPath)

            if (rainyToday && plant.isOutdoor && plant.isDue()) {
                b.statusBadge.text = ctx.getString(R.string.status_rain_skip)
                b.statusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_rain_fg))
                b.statusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_rain_bg))
            } else {
                val status = PlantStatus.of(ctx, plant)
                b.statusBadge.text = status.label
                b.statusBadge.setTextColor(status.textColor)
                b.statusBadge.backgroundTintList = ColorStateList.valueOf(status.backgroundColor)
            }

            b.root.setOnClickListener { onOpen(plant) }
            b.waterButton.setOnClickListener { onWater(plant) }
            b.waterButton.contentDescription = ctx.getString(R.string.cd_water_plant, plant.name)
        }
    }

    object Diff : DiffUtil.ItemCallback<Plant>() {
        override fun areItemsTheSame(oldItem: Plant, newItem: Plant) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Plant, newItem: Plant) = oldItem == newItem
    }
}

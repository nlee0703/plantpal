package ca.gbc.comp3074.nicholas.plantpal.util

import android.content.Context
import androidx.core.content.ContextCompat
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.Plant

/** Text and colours for a plant's watering badge (FR3). */
data class PlantStatus(val label: String, val textColor: Int, val backgroundColor: Int) {
    companion object {
        fun of(context: Context, plant: Plant): PlantStatus {
            val days = plant.daysUntilWatering()
            val (label, fg, bg) = when {
                days < 0 -> Triple(
                    context.resources.getQuantityString(R.plurals.status_overdue, -days, -days),
                    R.color.status_overdue_fg, R.color.status_overdue_bg
                )
                days == 0 -> Triple(
                    context.getString(R.string.status_due_today),
                    R.color.status_due_fg, R.color.status_due_bg
                )
                else -> Triple(
                    context.resources.getQuantityString(R.plurals.status_in_days, days, days),
                    R.color.status_ok_fg, R.color.status_ok_bg
                )
            }
            return PlantStatus(label, ContextCompat.getColor(context, fg), ContextCompat.getColor(context, bg))
        }
    }
}

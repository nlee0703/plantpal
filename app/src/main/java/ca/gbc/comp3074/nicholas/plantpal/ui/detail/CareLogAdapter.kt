package ca.gbc.comp3074.nicholas.plantpal.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.gbc.comp3074.nicholas.plantpal.R
import ca.gbc.comp3074.nicholas.plantpal.data.CareLog
import ca.gbc.comp3074.nicholas.plantpal.databinding.ItemCareLogBinding
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils

/** Care history rows on Plant Detail (FR8). */
class CareLogAdapter : ListAdapter<CareLog, CareLogAdapter.LogViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LogViewHolder(ItemCareLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) = holder.bind(getItem(position))

    class LogViewHolder(private val b: ItemCareLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(log: CareLog) {
            val ctx = b.root.context
            val rain = log.type == CareLog.TYPE_SKIPPED_RAIN
            b.logIcon.setImageResource(if (rain) R.drawable.ic_weather_rain else R.drawable.ic_water_drop)
            b.logTitle.text = ctx.getString(if (rain) R.string.log_skipped_rain else R.string.log_watered)
            b.logTime.text = if (log.note.isBlank()) DateUtils.formatDateTime(log.timestamp)
            else ctx.getString(R.string.log_time_with_note, DateUtils.formatDateTime(log.timestamp), log.note)
        }
    }

    object Diff : DiffUtil.ItemCallback<CareLog>() {
        override fun areItemsTheSame(oldItem: CareLog, newItem: CareLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CareLog, newItem: CareLog) = oldItem == newItem
    }
}

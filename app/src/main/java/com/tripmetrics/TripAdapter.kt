package com.tripmetrics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tripmetrics.databinding.ItemTripBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter : ListAdapter<TripRecord, TripAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        with(holder.binding) {
            tvTripName.text = record.name.ifBlank { root.context.getString(R.string.unnamed_trip) }
            tvDate.text = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())
                .format(Date(record.startTimeMs))
            tvDistance.text = formatDistance(record.distanceMeters)
            tvDuration.text = formatDuration(record.durationSeconds)
            tvAvgSpeed.text = formatSpeed(record.averageSpeedKmh)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TripRecord>() {
            override fun areItemsTheSame(a: TripRecord, b: TripRecord) = a.id == b.id
            override fun areContentsTheSame(a: TripRecord, b: TripRecord) = a == b
        }
    }
}
